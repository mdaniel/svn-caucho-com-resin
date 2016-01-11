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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.io.TempBuffer;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Encoding;
import com.caucho.v5.vfs.IOExceptionRuntime;
import com.caucho.v5.vfs.TempCharBuffer;
import com.caucho.v5.vfs.i18n.EncodingWriter;

/**
 * Handles the dual char/byte buffering for the response stream.
 */
public abstract class OutResponseToByte extends OutResponseBase2
{
  private static final L10N L = new L10N(OutResponseToByte.class);
  private static final Logger log
    = Logger.getLogger(OutResponseToByte.class.getName());
  protected static final int SIZE = TempBuffer.SIZE;
  //protected static final int DEFAULT_SIZE = 8 * SIZE;
  protected static final int DEFAULT_SIZE = SIZE;

  private TempCharBuffer _headChar;
  private char []_charBuffer = new char[SIZE];
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

  protected OutResponseToByte()
  {
  }

  /**
   * Initializes the Buffered Response stream at the beginning of a request.
   */
  @Override
  public void start()
  {
    super.start();
    
    _bufferCapacity = DEFAULT_SIZE;
    _contentLength = 0;

    clearBuffer();

    _isOutputStreamOnly = false;

    _toByte = Encoding.getLatin1Writer();
  }

  /**
   * Returns true for a caucho response stream.
   */
  @Override
  public boolean isCauchoResponseStream()
  {
    return true;
  }

  @Override
  public void setOutputStreamOnly(boolean isOutputStreamOnly)
  {
    _isOutputStreamOnly = isOutputStreamOnly;
  }

  protected boolean setFlush(boolean isAllowFlush)
  {
    return true;
  }

  /**
   * Sets the encoding.
   */
  @Override
  public void setEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    EncodingWriter toByte;

    if (encoding == null) {
      toByte = Encoding.getLatin1Writer();
    }
    else {
      toByte = Encoding.getWriteEncoding(encoding);
    }

    if (toByte != null) {
      _toByte = toByte;
    }
    else {
      _toByte = Encoding.getLatin1Writer();

      throw new UnsupportedEncodingException(encoding);
    }
  }

  /**
   * Sets the locale.
   */
  @Override
  public void setLocale(Locale locale)
    throws UnsupportedEncodingException
  {
  }
  
  //
  // byte buffer
  //

  /**
   * Returns the byte buffer.
   */
  @Override
  public byte []getBuffer()
    throws IOException
  {
    flushCharBuffer();

    return _tailByteBuffer;
  }
  
  protected byte []getBufferImpl()
  {
    return _tailByteBuffer;
  }

  /**
   * Returns the byte offset.
   */
  @Override
  public int getBufferOffset()
    throws IOException
  {
    flushCharBuffer();

    return _tailByteLength;
  }

  /**
   * Returns the byte offset.
   */
  public int getByteBufferOffset()
  {
    return _tailByteLength;
  }

  /**
   * Sets the byte offset.
   */
  @Override
  public void setBufferOffset(int offset)
    throws IOException
  {
    _tailByteLength = offset;
  }

  /**
   * Returns the buffer capacity.
   */
  @Override
  public int getBufferCapacity()
  {
    return _bufferCapacity;
  }

  /**
   * Sets the buffer capacity.
   */
  @Override
  public void setBufferCapacity(int size)
  {
    if (isCommitted()) {
      throw new IllegalStateException(L.l("Buffer size cannot be set after commit"));
    }

    _bufferCapacity = Math.max(0, SIZE * ((size + SIZE - 1) / SIZE));
  }

  /**
   * Returns the remaining value left.
   */
  @Override
  public int getRemaining()
  {
    return _bufferCapacity - getBufferLength();
  }

  /**
   * Returns the data in the buffer
   */
  protected int getBufferLength()
  {    
    return _bufferSize + (_tailByteLength - _tailByteStart) + _charLength;
  }

  @Override
  public long getContentLength()
  {
    // server/05e8
    flushCharBuffer();

    return _contentLength + _tailByteLength - _tailByteStart;
  }

  protected boolean isDisableAutoFlush()
  {
    return false;
  }
  
  /**
   * Clears the response buffer.
   */
  @Override
  public void clearBuffer()
  {
    TempBuffer next = _head.getNext();

    if (next != null) {
      _head.setNext(null);
      TempBuffer.freeAll(next);
    }

    _head.clear();
    _tail = _head;
    _tailByteBuffer = _tail.buffer();
    _tailByteStart = getBufferStart();
    _tailByteLength = _tailByteStart;

    _charLength = 0;

    _bufferSize = 0;
  }

  @Override
  public void clear()
    throws IOException
  {
    clearBuffer();

    if (isCommitted()) {
      throw new IOException(L.l("can't clear response after writing headers"));
    }
  }

  /**
   * Writes a byte to the output.
   */
  @Override
  public void write(int ch)
    throws IOException
  {
    _singleByteBuffer[0] = (byte) ch;
    
    write(_singleByteBuffer, 0, 1);
  }

  /**
   * Writes a chunk of bytes to the stream.
   */
  @Override
  public void write(byte []buffer, int offset, int length)
  {
    if (isClosed() || isHead()) {
      return;
    }

    flushCharBuffer();

    int byteLength = _tailByteLength;
    
    while (true) {
      int sublen = Math.min(length, SIZE - byteLength);

      System.arraycopy(buffer, offset, _tailByteBuffer, byteLength, sublen);
      offset += sublen;
      length -= sublen;
      byteLength += sublen;
      
      if (length <= 0) {
        break;
      }
      
      if (_bufferSize + byteLength < _bufferCapacity) {
        _tail.length(byteLength);
        TempBuffer tempBuf = TempBuffer.allocate();
        _tail.setNext(tempBuf);
        _tail = tempBuf;

        _bufferSize += SIZE;
        _tailByteBuffer = _tail.buffer();
        byteLength = _tailByteStart;
      }
      else {
        _tailByteLength = byteLength;
        flushByteBuffer();
        byteLength = _tailByteLength;
      }
    }

    _tailByteLength = byteLength;
  }

  /**
   * Returns the next byte buffer.
   */
  @Override
  public byte []nextBuffer(int offset)
    throws IOException
  {
    if (offset < 0 || SIZE < offset) {
      throw new IllegalStateException(L.l("Invalid offset: " + offset));
    }
    
    if (_bufferCapacity <= SIZE
        || _bufferCapacity <= offset + _bufferSize) {
      _tailByteLength = offset;
      flushByteBuffer();

      return getBuffer();
    }
    else {
      _tail.length(offset);
      _bufferSize += offset;

      TempBuffer tempBuf = TempBuffer.allocate();
      _tail.setNext(tempBuf);
      _tail = tempBuf;

      _tailByteBuffer = _tail.buffer();
      _tailByteLength = _tailByteStart;

      return _tailByteBuffer;
    }
  }

  protected final void flushByteBuffer()
  {
    flushByteBuffer(false);
  }
  
  protected int getBufferStart()
  {
    return 0;
  }
  
  //
  // char buffer
  //

  /**
   * Returns the char buffer.
   */
  @Override
  public final char []getCharBuffer()
  {
    return _charBuffer;
  }

  /**
   * Returns the char offset.
   */
  @Override
  public int getCharOffset()
  {
    return _charLength;
  }

  /**
   * Sets the char offset.
   */
  @Override
  public void setCharOffset(int offset)
  {
    _charLength = offset;

    if (_charLength == SIZE) {
      flushCharBuffer();
    }
  }

  /**
   * Writes a character to the output.
   */
  @Override
  public void print(int ch)
    throws IOException
  {
    if (isClosed() || isHead()) {
      return;
    }

    // server/13ww
    if (SIZE <= _charLength) {
      flushCharBuffer();
    }

    _charBuffer[_charLength++] = (char) ch;
  }

  /**
   * Writes a char array to the output.
   */
  @Override
  public void print(char []buffer, int offset, int length)
    throws IOException
  {
    if (isClosed() || isHead())
      return;

    int charLength = _charLength;

    while (length > 0) {
      int sublen = SIZE - charLength;

      if (length < sublen)
        sublen = length;

      System.arraycopy(buffer, offset, _charBuffer, charLength, sublen);

      offset += sublen;
      length -= sublen;
      charLength += sublen;

      if (charLength == SIZE && length > 0) {
        _charLength = charLength;
        flushCharBuffer();
        charLength = _charLength;
      }
    }

    _charLength = charLength;
  }

  /**
   * Writes a char array to the output.
   */
  @Override
  public void print(String value)
    throws IOException
  {
    
  }
  
  /**
   * Converts the char buffer.
   */
  @Override
  public char []nextCharBuffer(int offset)
    throws IOException
  {
    _charLength = offset;
    flushCharBuffer();

    return _charBuffer;
  }
  
  /**
   * True while the char buffer is being flushed, needed
   * for content-length vs chunked headers.
   */
  protected boolean isCharFlushing()
  {
    return _isCharFlushing;
  }

  /**
   * Converts the char buffer.
   */
  protected void flushCharBufferComplete()
    throws IOException
  {
    // double because the first flush might leave some characters because of
    // alignment. Not a loop because of utf-8 pairs which aren't flushed.
    if (_charLength > 0) {
      flushCharBuffer();
    }
    
    if (_charLength > 0) {
      flushCharBuffer();
    }
  }

  /**
   * Converts the char buffer.
   */
  protected void flushCharBuffer()
  {
    int charLength = _charLength;
    
    if (charLength <= 0) {
      return;
    }
    
    _charLength = 0;
    
    if (! _isOutputStreamOnly) {
      // server/05ef
      _isCharFlushing = true;

      try {
        boolean isFlush = setFlush(false);

        int writeLength = _toByte.write(this, _charBuffer, 0, charLength);

        if (writeLength < charLength) {
          System.arraycopy(_charBuffer, writeLength, _charBuffer, 0,
                           charLength - writeLength);
          charLength -= writeLength;
        }
        else {
          charLength = 0;
        }
        
        setFlush(isFlush);
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      } finally {
        _isCharFlushing = false;
      }

      int pad = 8;
      if (_bufferCapacity <= _tailByteLength + _bufferSize + pad) {
        _charLength = charLength;
        flushByteBuffer();
        charLength = _charLength;
      }

      // server/05e8, jsp/0182, jsp/0502, jsp/0503
      // _isCommitted = true;
    }
    
    _charLength = charLength;
  }

  /**
   * Flushes the buffered response to the output stream.
   */
  @Override
  public void flush()
    throws IOException
  {
    flushBuffer();
  }
  
  /**
   * Flushes the buffer.
   */
  @Override
  public void flushBuffer()
    throws IOException
  {
    if (isDisableAutoFlush()) {
      throw new IOException("auto-flush is disabled");
    }
    
    flushCharBuffer();

    flushByteBuffer(false);
  }
  
  /**
   * Flushes the buffered response to the output stream.
   */
  protected void flushByteBuffer(boolean isEnd)
  {
    // jsp/0182
    if (isDisableAutoFlush()) {
      throw new IOExceptionRuntime("auto-flush is disabled");
    }
    
    // jsp/0182 jsp/0502 jsp/0503
    // _isCommitted = true;

    if (_tailByteStart == _tailByteLength && _bufferSize == 0) {
      if (! isCommitted() || isEnd) {
        // server/0101
        flushData(null, null, isEnd);
        _tailByteStart = getBufferStart();
        _tailByteLength = _tailByteStart;
      }
      return;
    }

    _tail.length(_tailByteLength);
    _contentLength += _tailByteLength - _tailByteStart;
    _bufferSize = 0;
    _head = flushData(_head, _tail, isEnd);
    
    _tailByteStart = getBufferStart();
    _tailByteLength = _tailByteStart;

    _tail = _head;
    if (! isEnd) {
      _tail.length(_tailByteLength);
    }
    _tailByteBuffer = _tail.buffer();
    /*
    if (! isEnd) {
      flushNext();
    }
    */
  }
  
  abstract protected TempBuffer flushData(TempBuffer head,
                                          TempBuffer tail,
                                          boolean isEnd);
  
  /**
   * Writes data to the output. If the headers have not been written,
   * they should be written.
   */
  /*
  protected TempBuffer flushData(TempBuffer head,
                                 TempBuffer tail,
                                 boolean isEnd)
    throws IOException
  {
    // writeToCache(head);
    
    TempBuffer ptr = head;
    
    while (ptr != null) {
      TempBuffer next = ptr.getNext();
      ptr.setNext(null);

      boolean isWriteEnd = isEnd && next == null;
      
      int offsetStart = getBufferStart();
      int lengthBuffer = ptr.getLength() - offsetStart;
      
      flushDataBuffer(ptr.getBuffer(), offsetStart, lengthBuffer, isWriteEnd);

      if (ptr != head) {
        TempBuffer.free(ptr);
        ptr = null;
      }

      ptr = next;
    }
    
    if (head == null) {
      flushDataBuffer(null, 0, 0, isEnd);
    }

    return head;
  }
  */
  
  protected void writeToCache(TempBuffer head)
  {
    
  }
  
  /*
  protected void flushDataBuffer(byte []buffer, int offset, int length,
                                 boolean isEnd)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  /**
   * Closes the response stream.
   */
  @Override
  protected void closeImpl()
    throws IOException
  {
    flushCharBuffer();

    flushByteBuffer(true);
  }
}
