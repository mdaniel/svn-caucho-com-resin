/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.cache;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.OutputStreamWithBuffer;
import com.caucho.v5.vfs.RandomAccessStream;
import com.caucho.v5.vfs.SendfileOutputStream;

/**
 * Represents the final servlet in a filter chain.
 */
class InodeHttpCache {
  private static final L10N L = new L10N(InodeHttpCache.class);
  private static final Logger log
    = Logger.getLogger(InodeHttpCache.class.getName());
  
  private static final int SIZE = BlockHttpCache.SIZE;

  private final BlockManagerHttpCache _store;
  private final AtomicInteger _useCount = new AtomicInteger(1);

  private ArrayList<BlockHttpCache> _blockList = new ArrayList<>();
  private BlockHttpCache []_blockArray;
  private long _length;
  
  private boolean _isWritten;

  InodeHttpCache(BlockManagerHttpCache store)
  {
    Objects.requireNonNull(store);
    
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
    if (writeSendfile(os, offset, length)) {
      return true;
    }
    
    length = Math.min(_length, length);

    BlockHttpCache []blockArray = _blockArray;
    
    if (blockArray == null) {
      return false;
    }

    while (length > 0) {
      int sublen = (int) length;

      BlockHttpCache block = blockArray[(int) (offset / SIZE)];
      int blockOffset = (int) (offset % SIZE);

      sublen = Math.min(sublen, SIZE - blockOffset);

      block.read(blockOffset, os, sublen);

      offset += sublen;
      length -= sublen;
    }

    if (_useCount.get() <= 0) {
      throw new IllegalStateException(L.l("Unexpected close of cache inode"));
    }
    
    return true;
  }

  public boolean writeSendfile(OutputStreamWithBuffer os,
                               long offset, long length)
  {
    return false;
    
    /*
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
    */
  }
  
  private boolean isWritten()
  {
    /*
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
    */
    
    return false;
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
    ArrayList<BlockHttpCache> blockList;
    BlockHttpCache []blockArray;

    synchronized (this) {
      blockList = _blockList;
      _blockList = null;

      blockArray = _blockArray;
      _blockArray = null;
    }

    if (blockArray != null) {
      for (BlockHttpCache block : blockArray) {
        block.free();
      }
    }
    else if (blockList != null) {
      for (BlockHttpCache block : blockList) {
        block.free();
      }
    }
  }

  class CacheOutputStream extends OutputStream {
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
        while (_blockList.size() <= _length / SIZE) {
          BlockHttpCache block = _store.allocate();

          _blockList.add(block);
        }

        int blockOffset = (int) (_length % SIZE);
        BlockHttpCache block = _blockList.get(_blockList.size() - 1);

        int sublen = Math.min(length, SIZE - blockOffset);

        _length += sublen;
        
        block.write(blockOffset, buffer, offset, sublen);

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

      _blockArray = new BlockHttpCache[_blockList.size()];

      for (int i = 0; i < _blockList.size(); i++) {
        _blockArray[i] = _blockList.get(i);
      }
    }
  }
}
