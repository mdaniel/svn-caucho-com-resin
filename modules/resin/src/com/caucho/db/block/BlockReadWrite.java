/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import com.caucho.util.CurrentTime;
import com.caucho.util.FreeRing;
import com.caucho.util.IoUtil;
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
  
  private final static long FILE_SIZE_INCREMENT = 8L * 1024 * 1024; 

  private final BlockStore _store;
  private final BlockManager _blockManager;

  private Path _path;

  private long _fileSize;

  private Object _fileLock = new Object();

  private AtomicReference<RandomAccessStream> _mmapFile
    = new AtomicReference<RandomAccessStream>();

  private boolean _isEnableMmap = true;
  private boolean _isMmap = false;

  private FreeRing<RandomAccessWrapper> _cachedRowFile
    = new FreeRing<RandomAccessWrapper>(4);

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
  
  private void setFileSize(long fileSize)
  {
    if (fileSize < 0) {
      throw new IllegalStateException(L.l("Invalid file size {0} for {1}", fileSize, _path));
    }
    
    _fileSize = fileSize;
  }

  /**
   * Creates the store.
   */
  void create()
    throws IOException, SQLException
  {
    _path.getParent().mkdirs();

    if (_path.getLength() > 0) {
      throw new SQLException(L.l("CREATE for path '{0}' failed, because the file already exists.  CREATE can not override an existing table.",
                                 _path.getNativePath()));
    }

    WriteStream os = null;
    
    try {
      _path.remove();
      
      os = _path.openWrite();
    } finally {
      IoUtil.close(os);
    }
  }

  boolean isFileExist()
  {
    return _path.exists();
  }

  void init()
    throws IOException
  {
    boolean isPriority = true;

    RandomAccessWrapper wrapper = openRowFile(true, FILE_SIZE_INCREMENT);

    try {
      RandomAccessStream file = wrapper.getFile();

      setFileSize(file.getLength());

      freeRowFile(wrapper, isPriority);
      wrapper = null;
    } finally {
      closeRowFile(wrapper, isPriority);
    }
  }
  
  public void removeInit()
    throws IOException
  {
    _path.remove();
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
    long blockAddress = blockId & BlockStore.BLOCK_MASK;

    boolean isPriority = false;
    RandomAccessWrapper wrapper = openRowFile(isPriority,
                                              blockAddress + length);

    try {
      RandomAccessStream is = wrapper.getFile();

      long fileSize = is.getLength();
      if (blockAddress < 0 || fileSize < blockAddress + length) {
        throw new IllegalStateException(L.l("block at 0x{0} is invalid for file {1} (length 0x{2})\n  {3}",
                                            Long.toHexString(blockAddress),
                                            _path,
                                            Long.toHexString(fileSize),
                                            is + ":" + is.getClass()));
      }

      // System.out.println("READ: " + Long.toHexString(blockAddress));
      int readLen = is.read(blockAddress, buffer, offset, length);

      if (readLen < 0) {
        return false;
      }

      if (readLen < length) {
        System.err.println("BAD-READ: " + Long.toHexString(blockAddress));
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
    if (buffer == null || offset < 0 || length < 0
        || buffer.length < offset + length) {
      System.err.println("BUFFER: " + buffer + " " + offset + " " + length);
    }
    
    if (blockAddress == 0
        && (buffer[offset] != BlockStore.ALLOC_DATA
            || buffer[offset + 2] != BlockStore.ALLOC_DATA)) {
      System.err.println("Bad meta-block write: " + blockAddress + " " + buffer[offset]);
      Thread.dumpStack();
    }
    
    RandomAccessWrapper wrapper;

    wrapper = openRowFile(isPriority, blockAddress + length);

    try {
      RandomAccessStream os = wrapper.getFile();
      /*
      if (blockAddress > 2 * 0x2000000) {
      System.out.println("BLOCK: " + Long.toHexString(blockAddress) + " " + offset);
      Thread.dumpStack();
      }
      */
      
      os.write(blockAddress, buffer, offset, length);
      freeRowFile(wrapper, isPriority);
      wrapper = null;

      _blockManager.addBlockWrite();
    } finally {
      closeRowFile(wrapper, isPriority);
    }
  }

  RandomAccessStream getMmap()
  {
    return _mmapFile.get();
  }

  /**
   * sync the output stream with the filesystem when possible.
   */
  public void fsync()
      throws IOException
  {
    boolean isPriority = true;

    RandomAccessWrapper wrapper = openRowFile(isPriority, 0);

    try {
      RandomAccessStream os = wrapper.getFile();

      os.fsync();

      freeRowFile(wrapper, isPriority);
      wrapper = null;
    } finally {
      closeRowFile(wrapper, isPriority);
    }
  }

  /**
   * Opens the underlying file to the database.
   */
  private RandomAccessWrapper openRowFile(boolean isPriority,
                                          long addressMax)
    throws IOException
  {
    long fileSize = extendFile(addressMax);

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
      wrapper = openRowFileImpl(fileSize);

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

  private long extendFile(long addressMax)
    throws IOException
  {
    long fileSize = _fileSize;

    if (addressMax <= fileSize) {
      return fileSize;
    }

    long newFileSize = fileSize;

    while (newFileSize < addressMax) {
      newFileSize += FILE_SIZE_INCREMENT;
    }

    synchronized (_fileLock) {
      if (_fileSize < newFileSize) {
        RandomAccessStream stream = null;

        RandomAccessWrapper wrapper = openRowFileImpl(newFileSize);

        try {
          if (wrapper != null) {
            stream = wrapper.getFile();
          }

          if (stream == null)
            throw new IllegalStateException(this + " cannot open");

          long fileLength = stream.getLength();

          if (fileLength < newFileSize) {
            stream.write(newFileSize - 1, new byte[1], 0, 1);
          }

          fileLength = stream.getLength();

          newFileSize = Math.max(newFileSize, fileLength);

          freeRowFile(wrapper, false);
          wrapper = null;
        } finally {
          closeRowFile(wrapper, true);
        }

        /* XXX: stream is closed
        if (stream.getLength() < newFileSize)
          throw new IllegalStateException("bad file length: " + stream + " " + stream.getLength());
          */

        setFileSize(newFileSize);
      }

      return _fileSize;
    }
  }

  /**
   * Opens the underlying file to the database.
   */
  private RandomAccessWrapper openRowFileImpl(long fileSize)
    throws IOException
  {
    RandomAccessStream file = _mmapFile.get();

    if (file != null) {
      /*
      if (file.getLength() < fileSize) {
        file = null;
      }
      */
    }
    else {
      RandomAccessWrapper wrapper = _cachedRowFile.allocate();

      if (wrapper != null) {
        file = wrapper.getFile();
      }
    }

    int count = 10;
    for (; count > 0 && (file == null || ! file.allocate()); count--) {
      Path path = _path;

      file = null;

      if (path != null) {
        file = streamOpen(fileSize);
      }
    }

    if (file == null)
      throw new IllegalStateException("Cannot open file");

    return new RandomAccessWrapper(file);
  }

  private RandomAccessStream streamOpen(long fileSize)
    throws IOException
  {
    int retry = 8;

    if (! _isEnableMmap) {
      RandomAccessStream file = _path.openRandomAccess();

      return file;
    }

    while (retry-- >= 0) {
      RandomAccessStream mmapFile = _mmapFile.get();

      if (mmapFile != null
          && mmapFile.isOpen()
          && mmapFile.getLength() == fileSize) {
        return mmapFile;
      }

      synchronized (_mmapFile) {
        mmapFile = _mmapFile.get();

        if (mmapFile != null) { // && fileSize <= mmapFile.getLength()) {
          return mmapFile;
        }

        if (! _mmapFile.compareAndSet(mmapFile, null)) {
          System.err.println("INVALID-MMAP-FILE");
        }

        closeMmapFile(mmapFile);

        RandomAccessStream file = null;

        if (_isEnableMmap) {
          file = _path.openMemoryMappedFile(fileSize);

          if (file != null) {
            _isMmap = true;

            // System.err.println("OPEN: " + file + " " + Long.toHexString(file.getLength()));

            // System.out.println("REZIE: " + Long.toHexString(fileSize));
            // mmap has extra allocation because it's not automatically closed
            // XXX: file.allocate();

            if (_mmapFile.compareAndSet(null, file)) {
              return file;
            }
            else {
              System.err.println("CANNOT SET");
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

  private void closeMmapFile(RandomAccessStream mmapFile)
  {
    try {
      if (mmapFile != null) {
        _mmapFile.compareAndSet(mmapFile, null);

        mmapFile.close();
        
        long timeout = 15000L;
        long expires = CurrentTime.getCurrentTimeActual() + timeout;

        while (mmapFile.isOpen()
               && CurrentTime.getCurrentTimeActual() < expires) {
          // wait for close
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void freeRowFile(RandomAccessWrapper wrapper, boolean isPriority)
    throws IOException
  {
    if (wrapper == null)
      return;

    if (! isPriority && ! _isMmap) {
      _rowFileSemaphore.release();
    }

    wrapper.free();

    if (wrapper.getFile() == _mmapFile.get()) {
      return;
    }

    if (_store.isClosed() || ! _cachedRowFile.free(wrapper)) {
      wrapper.close();
    }
  }

  private void closeRowFile(RandomAccessWrapper wrapper, boolean isPriority)
    throws IOException
  {
    if (wrapper == null)
      return;

    if (! isPriority && ! _isMmap) {
      _rowFileSemaphore.release();
    }

    // This is a forced close. Normal close is a free()
    wrapper.closeFromException();
    // wrapper.close();
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
    return getClass().getSimpleName() + "[" + _store.getId() + "," + _store + "]";
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

      if (file != null) {
        file.free();
      }
    }

    void close()
      throws IOException
    {
      RandomAccessStream file = _file;
      _file = null;
      
      if (file != null) {
        file.close();
      }
    }

    void closeFromException()
      throws IOException
    {
      RandomAccessStream file = _file;
      _file = null;

      if (file != null) {
        file.free();
        file.close();
      }
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + _file + "]";
    }
  }
}
