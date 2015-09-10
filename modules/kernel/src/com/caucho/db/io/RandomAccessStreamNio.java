/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.db.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.server.util.CauchoSystem;
import com.caucho.util.JniTroubleshoot;
import com.caucho.util.JniUtil;
import com.caucho.vfs.Path;
import com.caucho.vfs.RandomAccessStream;
import com.caucho.vfs.SendfileOutputStream;

/**
 * Stream using with JNI.
 */
public class RandomAccessStreamNio extends RandomAccessStream
{
  private static final Logger log
    = Logger.getLogger(RandomAccessStreamNio.class.getName());
  
  private static final Object _openLock = new Object();


  private final AtomicLong _file = new AtomicLong();

  //private final long _fileLength;
  private final Path _path;
  private StoreReadWrite _store;
  
  private final AtomicBoolean _isClosed = new AtomicBoolean();

  /**
   * Create a new JniStream based on the java.io.* stream.
   */
  private RandomAccessStreamNio(Path path, StoreReadWrite store)
  {
    _path = path;
    
    _store = store;
  }

  public static RandomAccessStreamNio open(Path path,
                                           long fileLength)
    throws IOException
  {
    long currentFileLength = path.getLength();

    fileLength = Math.max(fileLength, currentFileLength);

    StoreBuilder builder = new StoreBuilder(path);

    StoreReadWrite store = builder.build();
    
    if (path.exists()) {
      store.init();
    }
    else {
      store.create();
    }

    return new RandomAccessStreamNio(path, store);
  }
  
  /**
   * Returns true for mmap
   */
  @Override
  public boolean isMmap()
  {
    return false;
  }
  
  /**
   * Returns the length.
   */
  @Override
  public long getLength()
    throws IOException
  {
    return _store.getFileSize();
  }

  /**
   * Reads data from the file.
   */
  @Override
  public int read(long pos, byte []buf, int offset, int length)
    throws IOException
  {
    if (buf == null) {
      throw new NullPointerException();
    }
    else if (offset < 0 || buf.length < offset + length) {
      throw new ArrayIndexOutOfBoundsException();
    }
    /*
    else if (_fileLength < pos + length) {
      throw new ArrayIndexOutOfBoundsException("FileLength: 0x" + Long.toHexString(_fileLength)
                                               + " pos: 0x" + Long.toHexString(pos)
                                               + " len: 0x" + Long.toHexString(length));
    }
    */
    
    InStore is = null;

    try {
      if (! allocate()) {
        return -1;
      }
      
      is = _store.openRead(pos, length);
      
      if (is.read(pos, buf, offset, length)) {
        return length;
      }
      else {
        return -1;
      }
    } finally {
      free();
      
      if (is != null) {
        is.close();
      }
    }
  }
  
  /*
  @Override
  public long getMmapAddress()
  {
    throw new UnsupportedOperationEreturn _mmapAddress;
  }
  */

  /**
   * Writes data to the file.
   */
  @Override
  public void write(long pos, byte []buf, int offset, int length)
    throws IOException
  {
    if (buf == null)
      throw new NullPointerException();
    else if (offset < 0 || buf.length < offset + length)
      throw new ArrayIndexOutOfBoundsException();
    /*
    else if (_fileLength < pos + length) {
      throw new ArrayIndexOutOfBoundsException("FileLength: 0x" + Long.toHexString(_fileLength)
                                               + " pos: 0x" + Long.toHexString(pos)
                                               + " len: 0x" + Long.toHexString(length));
    }
    */

    OutStore os = null;
    
    try {
      if (! allocate()) {
        return;
      }
      
      os = _store.openWrite(pos, length);
      
      os.write(pos, buf, offset, length);
    } finally {
      free();
    }
  }

  /**
   * Writes data to the file.
   */
  /*
  @Override
  public boolean writeToStream(SendfileOutputStream os, 
                               long offset, long length,
                               long []blockAddresses, long blockLength)
    throws IOException
  {
    if (os == null)
      throw new NullPointerException();
    else if (blockAddresses == null)
      throw new NullPointerException();
    
    if (blockLength < length)
      length = blockLength;

    InStore is = null;
    
    try {
      if (allocate()) {
        is = _store.openRead(offset, length);
        
        os.write(pos, buf, offset, length);
        os.writeMmap(getMmapAddress(), blockAddresses, offset, length);
        return true;
      }
    } finally {
      free();
    }
    
    return false;
  }
  */
  
  @Override
  public void fsync()
    throws IOException
  {
    flushToDisk();
  }

  public void flushToDisk()
    throws IOException
  {
    try {
      if (allocate()) {
        // nativeFlushToDisk(_file.get());
        _store.fsync();
      }
    } finally {
      free();
    }
  }

  /**
   * Reads data from the file.
   */
  @Override
  public int read(byte []buf, int offset, int length)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Reads data from the file.
   */
  @Override
  public int read(char []buf, int offset, int length)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Writes data to the file.
   */
  @Override
  public void write(byte []buf, int offset, int length)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Seeks to the given position in the file.
   */
  @Override
  public boolean seek(long position)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns an OutputStream for this stream.
   */
  @Override
  public OutputStream getOutputStream()
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns an InputStream for this stream.
   */
  @Override
  public InputStream getInputStream()
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Read a byte from the file, advancing the pointer.
   */
  @Override
  public int read()
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Write a byte to the file, advancing the pointer.
   */
  @Override
  public void write(int b)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }


  /**
   * Returns the current position of the file pointer.
   */
  @Override
  public long getFilePointer()
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  protected void closeImpl()
    throws IOException
  {
    long file = _file.getAndSet(0);
    
    if (getUseCount() > 0) {
      log.warning(this + " free with use " + file + " " + getUseCount());
      System.err.println(this + " DOUBLE_CLOSE: " + Long.toHexString(file) + " " + getUseCount());
    }

    /*
    if (file != 0) {
      //System.out.println("CLOSE: " + this);
      nativeClose(file);
    }
    else {
      log.warning(this + " double close " + file + " " + getUseCount());
      System.err.println(this + " DOUBLE_CLOSE: " + Long.toHexString(file) + " " + getUseCount());
    }
    */
    
    StoreReadWrite store = _store;
    _store = null;
    
    if (store != null) {
      store.close();
    }
    
    super.closeImpl();
  }

  @Override
  protected void finalize()
    throws IOException
  {
    if (_file.get() != 0) {
      closeImpl();
    }
  }

  /**
   * Returns the debug name for the stream.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _path + "]";
  }
}

