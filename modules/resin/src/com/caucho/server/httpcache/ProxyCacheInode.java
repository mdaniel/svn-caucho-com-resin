/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.httpcache;

import com.caucho.db.block.Block;
import com.caucho.db.block.BlockStore;
// import com.caucho.db.lock.Lock;
import com.caucho.db.xa.RawTransaction;
import com.caucho.db.xa.StoreTransaction;
import com.caucho.util.L10N;
import com.caucho.vfs.OutputStreamWithBuffer;
import com.caucho.vfs.RandomAccessStream;
import com.caucho.vfs.SendfileOutputStream;
import com.caucho.vfs.TempCharBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the final servlet in a filter chain.
 */
class ProxyCacheInode {
  private static final L10N L = new L10N(ProxyCacheInode.class);
  private static final Logger log
    = Logger.getLogger(ProxyCacheInode.class.getName());

  private final BlockStore _store;
  private final AtomicInteger _useCount = new AtomicInteger(1);

  private ArrayList<Long> _blockList = new ArrayList<Long>();
  private long []_blockArray;
  private long _length;
  
  private boolean _isWritten;

  ProxyCacheInode(BlockStore store)
  {
    _store = store;
  }

  public long getLength()
  {
    return _length;
  }
  
  boolean isValid()
  {
    return _blockArray != null && _useCount.get() > 0;
  }

  boolean isClosed()
  {
    return _useCount.get() <= 0;
  }
  
  /**
   * Allocates access to the inode.
   */
  public boolean allocate()
  {
    int count;

    do {
      count = _useCount.get();

      if (count <= 0)
        return false;
    } while (! _useCount.compareAndSet(count, count + 1));

    return _blockArray != null;
  }

  public OutputStream openOutputStream()
  {
    return new CacheOutputStream();
  }

  public Writer openWriter()
  {
    return new CacheWriter();
  }

  /**
   * Writes the inode value to a stream.
   */
  public boolean writeToStream(OutputStreamWithBuffer os)
    throws IOException
  {
    return writeToStream(os, 0, _length);
  }

  /**
   * Writes the inode value to a stream.
   */
  public boolean writeToStream(OutputStreamWithBuffer os,
                               long offset, long length)
    throws IOException
  {
    RandomAccessStream mmap = _store.getMmap();
    
    if (length >= 8 * 1024 
        && mmap != null
        && mmap.isMmap()
        && os instanceof SendfileOutputStream
        && isWritten()) {
      SendfileOutputStream mmapOut = (SendfileOutputStream) os;
      
      if (mmapOut.isMmapEnabled()) {
        if (mmap.writeToStream(mmapOut, offset, length,
                               _blockArray, _length)) {
          return true;
        }
      }
    }
    
    if (_length < length)
      length = _length;

    long []blockArray = _blockArray;
    
    if (blockArray == null)
      return false;

    while (length > 0) {
      int sublen = (int) length;

      long blockAddress = blockArray[(int) (offset / BlockStore.BLOCK_SIZE)];
      int blockOffset = (int) (offset % BlockStore.BLOCK_SIZE);

      if (BlockStore.BLOCK_SIZE - blockOffset < sublen)
        sublen = BlockStore.BLOCK_SIZE - blockOffset;

      _store.readBlock(blockAddress, blockOffset, os, sublen);

      offset += sublen;
      length -= sublen;
    }

    if (_useCount.get() <= 0) {
      throw new IllegalStateException(L.l("Unexpected close of cache inode"));
    }
    
    return true;
  }
  
  private boolean isWritten()
  {
    if (_isWritten)
      return true;

    long []blockArray = _blockArray;
    
    if (blockArray == null)
      return false;
    
    boolean isWritten = true;

    try {
      for (int i = 0; i < blockArray.length; i++) {
        long blockAddress = blockArray[i];

        Block block = _store.loadBlock(blockAddress);
        try {
          if (block.isDirty()) {
            isWritten = false;
            block.save();
          }
        } finally {
          block.free();
        }
      }
    } catch (Exception e) {
      isWritten = false;
      
      log.log(Level.WARNING, e.toString(), e);
    }

    if (isWritten) {
      _isWritten = true;
    }
    
    return _isWritten;
  }
  /**
   * Writes the inode value to a stream.
   */
  private boolean writeToMmap(RandomAccessStream mmapStream,
                              SendfileOutputStream os,
                              long offset, long length)
    throws IOException
  {
    long mmapAddress = mmapStream.getMmapAddress();
    
    if (mmapAddress <= 0) {
      //throw new IllegalStateException(L.l("Invalid mmap call"));
      return false;
    }
    
    if (_length < length)
      length = _length;

    long []blockArray = _blockArray;
    
    if (blockArray == null) {
      return false;
    }
    
    os.writeMmap(mmapAddress, blockArray, offset, length);

    if (_useCount.get() <= 0) {
      throw new IllegalStateException(L.l("Unexpected close of cache inode"));
    }
    
    return true;

  }

  /**
   * Writes the inode value to a stream.
   */
  public void writeToWriter(Writer out)
    throws IOException
  {
    TempCharBuffer charBuffer = TempCharBuffer.allocate();
    char []buffer = charBuffer.getBuffer();

    long offset = 0;

    long length = _length;

    while (length > 0) {
      long blockAddress
        = _blockArray[(int) (offset / BlockStore.BLOCK_SIZE)];
      int blockOffset = (int) (offset % BlockStore.BLOCK_SIZE);

      int sublen = (BlockStore.BLOCK_SIZE - blockOffset) / 2;

      if (buffer.length < sublen)
        sublen = buffer.length;

      if (length < 2 * sublen)
        sublen = (int) (length / 2);

      int len = _store.readBlock(blockAddress, blockOffset, buffer, 0, sublen);

      if (len <= 0)
        break;

      out.write(buffer, 0, len);

      offset += 2 * len;
      length -= 2 * len;
    }

    TempCharBuffer.free(charBuffer);

    if (_useCount.get() <= 0)
      throw new IllegalStateException(L.l("Unexpected close of cache inode"));
  }

  /**
   * Allocates access to the inode.
   */
  public void free()
  {
    int useCount = _useCount.decrementAndGet();

    if (useCount == 0) {
      remove();
    }
    else if (useCount < 0) {
      //System.out.println("BAD: " + useCount);
      throw new IllegalStateException();
    }
  }

  private void remove()
  {
    ArrayList<Long> blockList;
    long []blockArray;

    synchronized (this) {
      blockList = _blockList;
      _blockList = null;

      blockArray = _blockArray;
      _blockArray = null;
    }

    if (blockArray != null) {
      if (_useCount.get() > 0)
        Thread.dumpStack();
      for (long blockId : blockArray) {
        Block block = null;

        Lock lock = null;
        
        try {
          block = _store.loadBlock(blockId);
          lock = block.getWriteLock();

          lock.tryLock(60000L, TimeUnit.MILLISECONDS);
          block.deallocate();
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        } finally {
          if (lock != null)
            lock.unlock();
          
          if (block != null)
            block.free();
        }
      }
      if (_useCount.get() > 0)
        Thread.dumpStack();
    }
    else if (blockList != null) {
      //System.out.println("FRAGMENT-LIST: " + fragmentList);

      for (long blockId : blockList) {
        Block block = null;
        try {
          block = _store.loadBlock(blockId);
          block.deallocate();
        } catch (IOException e) {
          log.log(Level.WARNING, e.toString(), e);
        } finally {
          if (block != null)
            block.free();
        }
      }
    }
  }

  class CacheOutputStream extends OutputStream {
    // private final StoreTransaction _xa = RawTransaction.create();
    private final byte []_tempBuffer = new byte[8];

    @Override
    public void write(int ch)
      throws IOException
    {
      _tempBuffer[0] = (byte) ch;

      write(_tempBuffer, 0, 1);
    }

    @Override
    public void write(byte []buffer, int offset, int length)
      throws IOException
    {
      while (length > 0) {
        while (_blockList.size() <= _length / BlockStore.BLOCK_SIZE) {
          Block block = _store.allocateBlock();
          long blockId = block.getBlockId();
          block.free();

          _blockList.add(blockId);
        }

        int blockOffset = (int) (_length % BlockStore.BLOCK_SIZE);
        long blockAddress = _blockList.get(_blockList.size() - 1);

        int sublen = BlockStore.BLOCK_SIZE - blockOffset;
        if (length < sublen)
          sublen = length;

        _length += sublen;
        Block block = _store.writeBlock(blockAddress, blockOffset,
                                        buffer, offset, sublen);

        length -= sublen;
        offset += sublen;
      }
    }

    public void flush()
    {
    }

    @Override
    public void close()
    {
      if (_blockList == null)
        return;

      _blockArray = new long[_blockList.size()];

      for (int i = 0; i < _blockList.size(); i++) {
        _blockArray[i] = _blockList.get(i);
      }
    }
  }

  class CacheWriter extends Writer {
    private final StoreTransaction _xa = RawTransaction.create();
    private final char []_tempBuffer = new char[8];

    public void write(char ch)
      throws IOException
    {
      _tempBuffer[0] = ch;

      write(_tempBuffer, 0, 1);
    }

    public void write(char []buffer, int offset, int length)
      throws IOException
    {
      while (length > 0) {
        while (_blockList.size() <= _length / BlockStore.BLOCK_SIZE) {
          Block block = _store.allocateBlock();
          long blockId = block.getBlockId();
          block.free();

          _blockList.add(blockId);
        }

        int blockOffset = (int) (_length % BlockStore.BLOCK_SIZE);
        long blockAddress = _blockList.get(_blockList.size() - 1);

        int sublen = (BlockStore.BLOCK_SIZE - blockOffset) / 2;
        if (length < sublen)
          sublen = length;

        _length += 2 * sublen;
        Block writeBlock = _store.writeBlock(blockAddress, blockOffset,
                                             buffer, offset, sublen);
        _xa.addUpdateBlock(writeBlock);

        length -= sublen;
        offset += sublen;
      }
    }

    @Override
    public void flush()
    {
    }

    @Override
    public void close()
    {
      synchronized (this) {
        if (_blockList == null)
          return;

        _blockArray = new long[_blockList.size()];

        for (int i = 0; i < _blockList.size(); i++) {
          _blockArray[i] = _blockList.get(i);
        }

        _blockList = null;
      }
    }
  }
}
