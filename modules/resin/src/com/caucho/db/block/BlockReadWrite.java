/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.db.block;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.util.FreeList;
import com.caucho.util.L10N;
import com.caucho.util.SQLExceptionWrapper;
import com.caucho.vfs.Path;
import com.caucho.vfs.RandomAccessStream;
import com.caucho.vfs.WriteStream;

/**
 * Filesystem access for the BlockStore.
 */
public class BlockReadWrite {
  private final static Logger log
    = Logger.getLogger(BlockReadWrite.class.getName());
  private final static L10N L = new L10N(BlockReadWrite.class);
  
  private final static long FILE_SIZE_INCREMENT = 32L * 1024 * 1024; 

  private final BlockStore _store;
  private final BlockManager _blockManager;

  private Path _path;

  private long _fileSize;

  private Object _fileLock = new Object();

  private AtomicReference<RandomAccessStream> _mmapFile
    = new AtomicReference<RandomAccessStream>();
  
  private boolean _isEnableMmap = true;
  private boolean _isMmap = false;
  
  private FreeList<RandomAccessWrapper> _cachedRowFile
    = new FreeList<RandomAccessWrapper>(4);

  private final Semaphore _rowFileSemaphore = new Semaphore(8);

  /**
   * Creates a new store.
   *
   * @param database the owning database.
   * @param name the store name
   * @param lock the table lock
   * @param path the path to the files
   */
  public BlockReadWrite(BlockStore store, 
                        Path path,
                        boolean isEnableMmap)
  {
    _store = store;
    _blockManager = store.getBlockManager();
    _path = path;

    // XXX: cache 64k stress test
    _isEnableMmap = isEnableMmap;

    if (path == null)
      throw new NullPointerException();
  }

  /**
   * Returns the file size.
   */
  public long getFileSize()
  {
    return _fileSize;
  }

  /**
   * Creates the store.
   */
  void create()
    throws IOException, SQLException
  {
    _path.getParent().mkdirs();

    if (_path.exists()) {
      throw new SQLException(L.l("CREATE for path '{0}' failed, because the file already exists.  CREATE can not override an existing table.",
                                 _path.getNativePath()));
    }
    
    WriteStream os = _path.openWrite();
    os.close();
  }

  boolean isFileExist()
  {
    return _path.exists();
  }

  void init()
    throws IOException
  {
    RandomAccessWrapper wrapper = openRowFile(true);
    boolean isPriority = true;

    try {
      RandomAccessStream file = wrapper.getFile();

      _fileSize = file.getLength();
    } finally {
      closeRowFile(wrapper, isPriority);
    }
  }

  public void remove()
    throws SQLException
  {
    try {
      Path path = _path;
      _path = null;

      close();

      if (path != null)
        path.remove();
    } catch (IOException e) {
      throw new SQLExceptionWrapper(e);
    }
  }

  /**
   * Reads a block into the buffer.
   */
  public void readBlock(long blockId, byte []buffer, int offset, int length)
    throws IOException
  {
    int retry = 10;
    
    while (retry-- >= 0) {
      if (readBlockImpl(blockId, buffer, offset, length))
        return;
    }
    
    throw new IllegalStateException("Error reading for block " + Long.toHexString(blockId));
  }

  /**
   * Reads a block into the buffer.
   */
  private boolean readBlockImpl(long blockId, 
                                byte []buffer, int offset, int length)
      throws IOException
  {
    boolean isPriority = false;
    RandomAccessWrapper wrapper = openRowFile(isPriority);

    try {
      RandomAccessStream is = wrapper.getFile();

      long blockAddress = blockId & BlockStore.BLOCK_MASK;

      if (blockAddress < 0 || _fileSize < blockAddress + length) {
        throw new IllegalStateException(L.l("block at 0x{0} is invalid for file {1} (length 0x{2})",
                                            Long.toHexString(blockAddress),
                                            _path,
                                            Long.toHexString(_fileSize)));
      }

      // System.out.println("READ: " + Long.toHexString(blockAddress));
      int readLen = is.read(blockAddress, buffer, offset, length);

      if (readLen < 0) {
        return false;
      }

      if (readLen < length) {
        System.out.println("BAD-READ: " + Long.toHexString(blockAddress));
        if (readLen < 0)
          readLen = 0;

        for (int i = readLen; i < BlockStore.BLOCK_SIZE; i++)
          buffer[i] = 0;
      }

      _blockManager.addBlockRead();

      freeRowFile(wrapper, isPriority);
      wrapper = null;
      
      return true;
    } finally {
      closeRowFile(wrapper, isPriority);
    }
  }

  /**
   * Saves the buffer to the database.
   */
  public void writeBlock(long blockAddress,
                         byte []buffer, int offset, int length,
                         boolean isPriority)
    throws IOException
  {
    RandomAccessWrapper wrapper;
    
    synchronized (_fileLock) {
      while (_fileSize < blockAddress + length) {
        _fileSize += FILE_SIZE_INCREMENT;
      }
    }

    wrapper = openRowFile(isPriority);

    try {
      RandomAccessStream os = wrapper.getFile();
      /*
      if (blockAddress > 2 * 0x2000000) {
      System.out.println("BLOCK: " + Long.toHexString(blockAddress) + " " + offset);
      Thread.dumpStack();
      }
      */
      if (buffer == null || offset < 0 || length < 0 || buffer.length < offset + length)
        System.out.println("BUFFER: " + buffer + " " + offset + " " + length);

      os.write(blockAddress, buffer, offset, length);
      freeRowFile(wrapper, isPriority);
      wrapper = null;

      _blockManager.addBlockWrite();
    } finally {
      closeRowFile(wrapper, isPriority);
    }
  }

  /**
   * Opens the underlying file to the database.
   */
  private RandomAccessWrapper openRowFile(boolean isPriority)
    throws IOException
  {
    // limit number of active row files

    if (! isPriority && ! _isMmap) {
      try {
        Thread.interrupted();
        _rowFileSemaphore.acquire();
      } catch (InterruptedException e) {
        log.log(Level.FINE, e.toString(), e);

        return null;
      }
    }

    RandomAccessWrapper wrapper = null;
    try {
      wrapper = openRowFileImpl();

      return wrapper;
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(e);
    } finally {
      if (wrapper == null && ! _isMmap)
        _rowFileSemaphore.release();
    }
  }

  /**
   * Opens the underlying file to the database.
   */
  private RandomAccessWrapper openRowFileImpl()
    throws IOException
  {
    RandomAccessStream file = null;
    RandomAccessWrapper wrapper = null;

    // SoftReference<RandomAccessWrapper> ref = _cachedRowFile.allocate();
    wrapper = _cachedRowFile.allocate();

    /*
    if (ref != null) {
      wrapper = ref.get();
    }
    */

    if (wrapper != null) {
      file = wrapper.getFile();
      
      if (_mmapFile.get() != null && file.getLength() != _fileSize)
        file = null;
    }

    while (file == null || ! file.allocate()) {
      Path path = _path;
      
      file = null;

      if (path != null) {
        file = streamOpenRead();
      }
      
      if (file == null)
        throw new IllegalStateException("Cannot open file");
    }
    
    return new RandomAccessWrapper(file);
  }
  
  private RandomAccessStream streamOpenRead()
    throws IOException
  {
    int retry = 8;
    
    if (! _isEnableMmap) {
      return _path.openRandomAccess();
    }
    
    while (retry-- >= 0) {
      RandomAccessStream mmapFile = _mmapFile.get();
      
      if (mmapFile != null && mmapFile.getLength() == _fileSize) {
        return mmapFile;
      }
      
      synchronized (_mmapFile) {
        mmapFile = _mmapFile.get();

        if (mmapFile != null && mmapFile.getLength() == _fileSize) {
          return mmapFile;
        }

        RandomAccessStream file = null;

        if (_isEnableMmap) {
          long fileSize = _fileSize;

          if (fileSize == 0)
            fileSize = FILE_SIZE_INCREMENT;

          file = _path.openMemoryMappedFile(fileSize);

          if (file != null) {
            _fileSize = fileSize;

            _isMmap = true;

            if (_mmapFile.compareAndSet(mmapFile, file)) {
              if (mmapFile != null)
                mmapFile.close();

              return file;
            }
            else {
              file.close();
              file = null;
            }
          }
          
          _isEnableMmap = false;
        }
        
        if (! _isEnableMmap) {
          return _path.openRandomAccess();
        }
      }
    }
    
    return null;
  }

  private void freeRowFile(RandomAccessWrapper wrapper, boolean isPriority)
    throws IOException
  {
    if (wrapper == null)
      return;
    
    if (! isPriority && ! _isMmap)
      _rowFileSemaphore.release();

    /*
    SoftReference<RandomAccessWrapper> fileRef
      = new SoftReference<RandomAccessWrapper>(wrapper);
      */
    
    
    if (! _store.isClosed() && _cachedRowFile.free(wrapper)) {
      wrapper.free();
      return;
    }

    if (wrapper.getFile() != _mmapFile.get()) {
      wrapper.close();
    }
    else {
      wrapper.free();
    }
  }

  private void closeRowFile(RandomAccessWrapper wrapper, boolean isPriority)
    throws IOException
  {
    if (wrapper == null)
      return;

    if (! isPriority && ! _isMmap)
      _rowFileSemaphore.release();

    RandomAccessStream file = wrapper.getFile();
    
    if (file != null)
      file.free();
  }

  /**
   * Closes the store.
   */
  void close()
  {
    _path = null;

    RandomAccessStream mmap = _mmapFile.getAndSet(null);
    
    if (mmap != null) {
      try {
        mmap.close();
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }
    }
    RandomAccessWrapper wrapper = null;

    /*
    SoftReference<RandomAccessWrapper> ref = _cachedRowFile.allocate();

    if (ref != null)
      wrapper = ref.get();
      */
    while ((wrapper = _cachedRowFile.allocate()) != null) {
      try {
        wrapper.close();
      } catch (Throwable e) {
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _store.getId() + "]";
  }

  static class RandomAccessWrapper {
    private RandomAccessStream _file;

    RandomAccessWrapper(RandomAccessStream file)
    {
      _file = file;
    }

    RandomAccessStream getFile()
    {
      return _file;
    }
    
    void free()
    {
      RandomAccessStream file = _file;
      // _file = null;

      if (file != null)
        file.free();
    }

    void close()
      throws IOException
    {
      RandomAccessStream file = _file;
      _file = null;

      if (file != null) {
        file.free();
        file.close();
      }
    }
  }
}
