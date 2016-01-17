/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.protocol;

import io.baratine.io.Buffer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Objects;

import com.caucho.v5.io.OutputStreamWithBuffer;
import com.caucho.v5.io.TempBuffer;
import com.caucho.v5.io.i18n.Encoding;
import com.caucho.v5.io.i18n.EncodingWriter;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.TempCharBuffer;

/**
 * API for handling the output stream.
 */
public abstract class OutResponseBase2 
  extends OutputStreamWithBuffer
{
  private static final int SIZE = TempBuffer.SIZE;
  //protected static final int DEFAULT_SIZE = 8 * SIZE;
  private static final int DEFAULT_SIZE = SIZE;
  private static final int CHAR_SIZE = 1024;

  private State _state = State.START;
  private boolean _isAutoFlush = true;
  
  private char []_charBuffer = new char[CHAR_SIZE];
  private int _charLength;
  
  private final byte []_singleByteBuffer = new byte[1];

  // head of the expandable buffer
  private TempBuffer _head = TempBuffer.allocate();
  private TempBuffer _tail;

  private byte []_tailByteBuffer;
  private int _tailByteLength;
  private int _tailByteStart;

  // total buffer length
  private int _bufferCapacity;
  // extended buffer length
  private int _bufferSize;

  private long _contentLength;

  // true if character data should be ignored
  private boolean _isOutputStreamOnly;
  // true while char buffer is flushing for length/chunked
  private boolean _isCharFlushing;
  
  private EncodingWriter _toByte = Encoding.getLatin1Writer();

  
  /*
  public void setCauchoResponse(ResponseFacade res)
  {
  }
  */
  
  //
  // state predicates
  //

  /**
   * Set true for HEAD requests.
   */
  public final boolean isHead()
  {
    return _state.isHead();
  }

  /**
   * Test if data has been flushed to the client.
   */
  public boolean isCommitted()
  {
    return _state.isCommitted();
  }
  
  /**
   * Test if the request is closing.
   */
  public boolean isClosing()
  {
    return _state.isClosing();
  }
  
  @Override
  public boolean isClosed()
  {
    return _state.isClosed();
  }
  
  /**
   * Test if the request is closing.
   */
  public boolean isCloseComplete()
  {
    return _state.isClosing();
  }

  /**
   * Returns true for a Caucho response stream.
   */
  abstract public boolean isCauchoResponseStream();

  public String getEncoding()
  {
    return null;
  }

  /**
   * Sets the encoding.
   */
  public void setEncoding(String encoding)
    throws UnsupportedEncodingException
  {
  }

  /**
   * Set true for output stream only request.
   */
  public void setOutputStreamOnly(boolean isOutputStreamOnly)
  {
  }

  /**
   * Sets the locale.
   */
  public void setLocale(Locale locale)
    throws UnsupportedEncodingException
  {
  }

  public boolean isChunkedEncoding()
  {
    return false;
  }

  /**
   * Sets the buffer size.
   */
  abstract public void setBufferCapacity(int size);

  /**
   * Gets the buffer size.
   */
  abstract public int getBufferCapacity();

  /**
   * Sets the auto-flush
   */
  public final void setAutoFlush(boolean isAutoFlush)
  {
    _isAutoFlush = isAutoFlush;
  }

  /**
   * Return the auto-flush.
   */
  public final boolean isAutoFlush()
  {
    return _isAutoFlush;
  }

  /**
   * Returns the remaining buffer entries.
   */
  abstract public int getRemaining();

  /**
   * Returns the char buffer.
   */
  abstract public char []getCharBuffer()
    throws IOException;

  /**
   * Returns the char buffer offset.
   */
  abstract public int getCharOffset()
    throws IOException;

  /**
   * Sets the char buffer offset.
   */
  abstract public void setCharOffset(int offset)
    throws IOException;

  /**
   * Returns the next char buffer.
   */
  abstract public char []nextCharBuffer(int offset)
    throws IOException;

  /**
   * Sets a byte cache stream.
   */
  public void setByteCacheStream(OutputStream cacheStream)
  {
    if (cacheStream != null)
      throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Sets a byte cache stream.
   */
  protected OutputStream getByteCacheStream()
  {
    return null;
  }

  /**
   * Returns the written content length
   */
  public long getContentLength()
  {
    return 0;
  }
  
  /**
   * Writes a byte to the output.
   */
  abstract public void write(int v)
    throws IOException;

  /**
   * Writes a byte array to the output.
   */
  abstract public void write(byte []buffer, int offset, int length);

  public void write(Buffer data)
  {
    Objects.requireNonNull(data);
    
    int length = data.length();
    
    TempBuffer tBuf = TempBuffer.allocate();
    byte []buffer = tBuf.buffer();

    int pos = 0;
    while (pos < length) {
      int sublen = Math.min(length - pos, buffer.length);
      
      data.getBytes(pos, buffer, 0, sublen);
      
      write(buffer, 0, sublen);
      
      pos += sublen;
    }
    
    tBuf.freeSelf();
  }

  /**
   * Writes a character to the output.
   */
  abstract public void print(int ch)
    throws IOException;

  /**
   * Writes a char array to the output.
   */
  abstract public void print(char []buffer, int offset, int length)
    throws IOException;

  abstract public void print(String value)
    throws IOException;

  /**
   * Clears the output buffer, including headers if possible.
   */
  public void clear()
    throws IOException
  {
    clearBuffer();
  }
  
  /**
   * Clears the output buffer.
   */
  abstract public void clearBuffer();

  /**
   * Flushes the output buffer.
   */
  abstract public void flushBuffer()
    throws IOException;
  
  /**
   * Flushes the next buffer, leaving the current buffer alone
   */
  // server/1s04
  public void flushNext()
    throws IOException
  {
    flushBuffer();
  }

  /**
   * Flushes the output.
   */
  public void flushByte()
    throws IOException
  {
    flushBuffer();
  }

  /**
   * Flushes the output.
   */
  public void flushChar()
    throws IOException
  {
    flushBuffer();
  }

  /**
   * Sends a file.
   *
   * @param path the path to the file
   * @param length the length of the file (-1 if unknown)
   */
  public void sendFile(PathImpl path, long offset, long length)
    throws IOException
  {
    path.sendfile(this, offset, length);
  }

  /**
   * Flushes the output.
   */
  @Override
  public void flush()
    throws IOException
  {
    flushByte();
  }

  protected void killCaching()
  {
  }
  
  public void completeCache()
  {
  }
  
  //
  // lifecycle
  //
  
  /**
   * Starts the response stream.
   */
  public void start()
  {
    _state = _state.toStart();
    
    _isAutoFlush = true;
  }

  /**
   * Set true for HEAD requests.
   */
  public final void toHead()
  {
    _state = _state.toHead();
  }

  /**
   * Sets the committed state
   */
  public void toCommitted()
  {
    _state = _state.toCommitted();
  }

  public void upgrade()
  {
    //_state = _state.toUpgrade();
  }
  
  /**
   * Closes the response stream
   */
  @Override
  public final void close()
    throws IOException
  {
    State state = _state;
    
    if (state.isClosing()) {
      return;
    }
    
    _state = state.toClosing();
    
    try {
      closeImpl();
    } finally {
      try {
        _state = _state.toClose();
      } catch (RuntimeException e) {
        throw new RuntimeException(state + ": " + e, e);
      }
    }
  }
  
  /*
  protected final boolean toClosing()
  {
    State state = _state;
    
    if (state.isClosing()) {
      return false;
    }
    
    _state = state.toClosing();
    
    return true;
  }
  */

  protected void closeImpl()
    throws IOException
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _state + "]";
  }
  
  enum State {
    START {
      State toHead() { return HEAD; }
      State toCommitted() { return COMMITTED; }
      State toClosing() { return CLOSING; }
    },
    HEAD {
      boolean isHead() { return true; }
      
      State toHead() { return this; }
      State toCommitted() { return COMMITTED_HEAD; }
      State toClosing() { return CLOSING_HEAD; }
    },
    COMMITTED {
      boolean isCommitted() { return true; }
      
      State toHead() { return COMMITTED_HEAD; }
      State toCommitted() { return this; }
      State toClosing() { return CLOSING_COMMITTED; }
    },
    COMMITTED_HEAD {
      boolean isCommitted() { return true; }
      boolean isHead() { return true; }
      
      State toHead() { return this; }
      State toCommitted() { return this; }
      State toClosing() { return CLOSING_HEAD_COMMITTED; }
    },
    CLOSING {
      boolean isClosing() { return true; }
      
      State toHead() { return CLOSING_HEAD; }
      State toCommitted() { return CLOSING_COMMITTED; }
      State toClose() { return CLOSED; }
    },
    CLOSING_HEAD {
      boolean isHead() { return true; }
      boolean isClosing() { return true; }
      
      State toHead() { return this; }
      State toCommitted() { return CLOSING_HEAD_COMMITTED; }
      State toClose() { return CLOSED; }
    },
    CLOSING_COMMITTED {
      boolean isCommitted() { return true; }
      boolean isClosing() { return true; }
      
      State toHead() { return CLOSING_HEAD_COMMITTED; }
      State toCommitted() { return this; }
      // State toClosing() { Thread.dumpStack(); return CLOSED; }
      State toClose() { return CLOSED; }
    },
    CLOSING_HEAD_COMMITTED {
      boolean isHead() { return true; }
      boolean isCommitted() { return true; }
      boolean isClosing() { return true; }
      
      State toHead() { return this; }
      State toCommitted() { return this; }
      State toClose() { return CLOSED; }
    },
    CLOSED {
      boolean isCommitted() { return true; }
      boolean isClosed() { return true; }
      boolean isClosing() { return true; }
    };
    
    boolean isHead() { return false; }
    boolean isCommitted() { return false; }
    boolean isClosing() { return false; }
    boolean isClosed() { return false; }
   
    State toStart() { return START; }
    
    State toHead()
    { 
      throw new IllegalStateException(toString());
    }
    
    State toCommitted()
    {
      throw new IllegalStateException(toString());
    }
    
    State toClosing()
    {
      throw new IllegalStateException(toString());
    }
    
    State toClose()
    { 
      throw new IllegalStateException(toString());
    }
  }
}
