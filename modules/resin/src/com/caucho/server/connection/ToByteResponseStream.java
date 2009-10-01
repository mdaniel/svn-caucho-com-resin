/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.connection;

import com.caucho.util.L10N;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.TempBuffer;
import com.caucho.vfs.i18n.EncodingWriter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Handles the dual char/byte buffering for the response stream.
 */
public abstract class ToByteResponseStream extends AbstractResponseStream {
  private static final Logger log
    = Logger.getLogger(ToByteResponseStream.class.getName());
  private static final L10N L = new L10N(ToByteResponseStream.class);

  protected static final int SIZE = TempBuffer.SIZE;

  private final char []_charBuffer = new char[SIZE];
  private int _charLength;

  // head of the expandable buffer
  private final TempBuffer _head = TempBuffer.allocate();
  private TempBuffer _tail;

  private byte []_tailByteBuffer;
  private int _tailByteLength;

  // total buffer length, from servlet response setter
  private int _bufferCapacity;
  // extended buffer length
  private int _bufferSize;

  // true if character data should be ignored
  private boolean _isOutputStreamOnly;

  private boolean _isHead;
  private boolean _isClosed;
  protected boolean _isCommitted;
  protected boolean _isFinished;

  private EncodingWriter _toByte = Encoding.getLatin1Writer();

  protected ToByteResponseStream()
  {
  }

  /**
   * Initializes the Buffered Response stream at the beginning of a request.
   */
  @Override
  public void start()
  {
    _bufferCapacity = SIZE;

    clearBuffer();

    _isHead = false;
    _isClosed = false;
    _isFinished = false;
    _isCommitted = false;
    _isOutputStreamOnly = false;

    _toByte = Encoding.getLatin1Writer();
  }

  /**
   * Returns true for a caucho response stream.
   */
  public boolean isCauchoResponseStream()
  {
    return true;
  }

  @Override
  public void setOutputStreamOnly(boolean isOutputStreamOnly)
  {
    _isOutputStreamOnly = isOutputStreamOnly;
  }

  /**
   * Sets the head.
   */
  @Override
  public void setHead()
  {
    _isHead = true;
  }

  /**
   * Sets the encoding.
   */
  @Override
  public void setEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    EncodingWriter toByte;

    if (encoding == null)
      toByte = Encoding.getLatin1Writer();
    else
      toByte = Encoding.getWriteEncoding(encoding);

    if (toByte != null)
      _toByte = toByte;
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

  /**
   * Returns the char buffer.
   */
  public final char []getCharBuffer()
  {
    return _charBuffer;
  }

  /**
   * Returns the char offset.
   */
  public int getCharOffset()
    throws IOException
  {
    return _charLength;
  }

  /**
   * Sets the char offset.
   */
  public void setCharOffset(int offset)
    throws IOException
  {
    _charLength = offset;

    if (_charLength == SIZE)
      flushCharBuffer();
  }

  /**
   * Returns the byte buffer.
   */
  public byte []getBuffer()
    throws IOException
  {
    if (! _isOutputStreamOnly)
      flushCharBuffer();

    return _tailByteBuffer;
  }

  /**
   * Returns the byte offset.
   */
  public int getBufferOffset()
    throws IOException
  {
    if (! _isOutputStreamOnly)
      flushCharBuffer();

    return _tailByteLength;
  }

  /**
   * Returns the byte offset.
   */
  public int getByteBufferOffset()
    throws IOException
  {
    return _tailByteLength;
  }

  /**
   * Sets the byte offset.
   */
  public void setBufferOffset(int offset)
    throws IOException
  {
    _tailByteLength = offset;
  }

  /**
   * Returns the buffer capacity.
   */
  public int getBufferSize()
  {
    return _bufferCapacity;
  }

  /**
   * Returns true for extended buffers.
   */
  /*
  public boolean isExtendedBuffer()
  {
    return _bufferCapacity < SIZE;
  }
  */

  /**
   * Sets the buffer capacity.
   */
  public void setBufferSize(int size)
  {
    _bufferCapacity = SIZE * ((size + SIZE - 1) / SIZE);

    if (_bufferCapacity <= 0)
      _bufferCapacity = 0;
  }

  /**
   * Returns the remaining value left.
   */
  public int getRemaining()
  {
    return _bufferCapacity - getBufferLength();
  }

  /**
   * Returns the data in the buffer
   */
  protected int getBufferLength()
  {
    return _bufferSize + _tailByteLength + _charLength;
  }

  /**
   * Clears the response buffer.
   */
  public void clearBuffer()
  {
    TempBuffer next = _head.getNext();

    if (next != null) {
      _head.setNext(null);
      TempBuffer.freeAll(next);
    }

    _head.clear();
    _tail = _head;
    _tailByteBuffer = _tail.getBuffer();
    _tailByteLength = 0;

    _charLength = 0;

    _bufferSize = 0;
  }

  /**
   * Writes a byte to the output.
   */
  public void write(int ch)
    throws IOException
  {
    if (_isClosed)
      return;
    else if (_isHead)
      return;

    if (_charLength > 0)
      flushCharBuffer();

    if (_bufferCapacity <= _bufferSize + _tailByteLength + 1) {
      flushByteBuffer();
    }
    else if (_tailByteLength == SIZE) {
      _tail.setLength(_tailByteLength);
      _bufferSize += _tailByteLength;

      TempBuffer tempBuf = TempBuffer.allocate();
      _tail.setNext(tempBuf);
      _tail = tempBuf;

      _tailByteBuffer = _tail.getBuffer();
      _tailByteLength = 0;
    }

    _tailByteBuffer[_tailByteLength++] = (byte) ch;
  }

  /**
   * Writes a chunk of bytes to the stream.
   */
  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    boolean isFinished = false;

    if (_isClosed)
      return;
    else if (_isHead)
      return;

    if (_charLength > 0)
      flushCharBuffer();

    if (_bufferCapacity <= _bufferSize + _tailByteLength + length) {
      if (_bufferSize + _tailByteLength > 0)
        flushByteBuffer();

      if (_bufferCapacity <= length) {
        // _bufferSize = length;
        writeNext(buffer, offset, length, isFinished);
        _bufferSize = 0;
        return;
      }
    }

    int byteLength = _tailByteLength;
    while (length > 0) {
      if (SIZE <= byteLength) {
        _tail.setLength(byteLength);
        _bufferSize += byteLength;

        TempBuffer tempBuf = TempBuffer.allocate();
        _tail.setNext(tempBuf);
        _tail = tempBuf;

        _tailByteBuffer = _tail.getBuffer();
        byteLength = 0;
      }

      int sublen = length;
      if (SIZE - byteLength < sublen)
        sublen = SIZE - byteLength;

      System.arraycopy(buffer, offset, _tailByteBuffer, byteLength, sublen);

      offset += sublen;
      length -= sublen;
      byteLength += sublen;
    }

    _tailByteLength = byteLength;
  }

  /**
   * Writes a character to the output.
   */
  public void print(int ch)
    throws IOException
  {
    if (_isClosed)
      return;
    else if (_isHead)
      return;

    _charBuffer[_charLength++] = (char) ch;

    if (_charLength == SIZE)
      flushCharBuffer();
  }

  /**
   * Writes a char array to the output.
   */
  public void print(char []buffer, int offset, int length)
    throws IOException
  {
    if (_isClosed)
      return;
    else if (_isHead)
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
        charLength = 0;
        flushCharBuffer();
      }
    }

    _charLength = charLength;
  }

  /**
   * Flushes the buffer.
   */
  public void flushBuffer()
    throws IOException
  {
    if (_charLength > 0)
      flushCharBuffer();

    flushByteBuffer();
  }

  /**
   * Flushes the buffered response to the output stream.
   */
  public void flush()
    throws IOException
  {
    flushBuffer();
  }

  /**
   * Closes the response stream.
   */
  public void close()
    throws IOException
  {
    flushBuffer();

    _isClosed = true;
  }

  /**
   * Converts the char buffer.
   */
  public char []nextCharBuffer(int offset)
    throws IOException
  {
    _charLength = offset;
    flushCharBuffer();

    return _charBuffer;
  }

  /**
   * Converts the char buffer.
   */
  protected void flushCharBuffer()
    throws IOException
  {
    int charLength = _charLength;
    _charLength = 0;

    if (charLength > 0 && ! _isOutputStreamOnly) {
      _toByte.write(this, _charBuffer, 0, charLength);

      if (_bufferCapacity <= _tailByteLength + _bufferSize) {
        flushByteBuffer();
      }

      // server/05e8, jsp/0182, jsp/0502, jsp/0503
      // _isCommitted = true;
    }
  }

  public int getContentLength()
  {
    try {
      // server/05e8
      flushCharBuffer();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return _bufferSize + _tailByteLength;
  }

  /**
   * Returns the next byte buffer.
   */
  public byte []nextBuffer(int offset)
    throws IOException
  {
    if (_bufferCapacity <= SIZE
        || _bufferCapacity <= offset + _bufferSize) {
      _tailByteLength = offset;
      flushByteBuffer();

      return getBuffer();
    }
    else {
      _tail.setLength(offset);
      _bufferSize += offset;

      TempBuffer tempBuf = TempBuffer.allocate();
      _tail.setNext(tempBuf);
      _tail = tempBuf;

      _tailByteBuffer = _tail.getBuffer();
      _tailByteLength = 0;

      return _tailByteBuffer;
    }
  }

  /**
   * Flushes the buffered response to the output stream.
   */
  protected void flushByteBuffer()
    throws IOException
  {
    // jsp/0182 jsp/0502 jsp/0503
    // _isCommitted = true;

    if (_tailByteLength == 0 && _bufferSize == 0) {
      if (! _isCommitted) {
        // server/0101
        writeNext(_head.getBuffer(), 0, 0, _isFinished);
      }
      return;
    }

    _tail.setLength(_tailByteLength);
    _bufferSize += _tailByteLength;
    _tailByteLength = 0;

    TempBuffer ptr = _head;
    do {
      TempBuffer next = ptr.getNext();
      ptr.setNext(null);

      writeNext(ptr.getBuffer(), 0, ptr.getLength(), _isFinished);

      if (ptr != _head) {
        TempBuffer.free(ptr);
        ptr = null;
      }

      ptr = next;
    } while (ptr != null);

    _tail = _head;
    _tail.setLength(0);
    _tailByteBuffer = _tail.getBuffer();
    _bufferSize = 0;
  }

  /**
   * Writes the chunk to the downward stream.
   */
  abstract protected void writeNext(byte []buffer, int offset,
                                    int length, boolean isEnd)
    throws IOException;

  /**
   * Clears the close.
   */
  public void clearClose()
  {
    _isClosed = false;
  }
}
