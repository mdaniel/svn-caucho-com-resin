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

package com.caucho.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.caucho.hessian.io.AbstractHessianOutput;

/**
 * Abstract output stream for JSON requests.
 *
 * <pre>
 * OutputStream os = ...; // from http connection
 * AbstractOutput out = new HessianSerializerOutput(os);
 * String value;
 *
 * out.startCall("hello");  // start hello call
 * out.writeString("arg1"); // write a string argument
 * out.completeCall();      // complete the call
 * </pre>
 */
public class JsonOutput extends AbstractHessianOutput {
  private OutputStream _os;
  
  private byte []_buffer = new byte[1024];
  private int _offset;

  public JsonOutput()
  {
  }

  public JsonOutput(OutputStream os)
  {
    init(os);
  }
  
  /**
   * Initialize the output with a new underlying stream.
   */
  public void init(OutputStream os)
  {
    _os = os;
  }

  /**
   * Starts the method call:
   *
   * @param method the method name to call.
   */
  public void startCall()
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().toString());
  }

  /**
   * Starts the method call:
   *
   * <code><pre>
   * C string int
   * </pre></code>
   *
   * @param method the method name to call.
   */
  public void startCall(String method, int length)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().toString());
  }

  /**
   * Writes the method tag.
   *
   * <code><pre>
   * string
   * </pre></code>
   *
   * @param method the method name to call.
   */
  public void writeMethod(String method)
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().toString());
  }

  /**
   * Completes the method call:
   *
   * <code><pre>
   * </pre></code>
   */
  public void completeCall()
    throws IOException
  {
    throw new UnsupportedOperationException(getClass().toString());
  }

  /**
   * Writes a boolean value to the stream.  The boolean will be written
   * with the following syntax:
   *
   * <code><pre>
   * T
   * F
   * </pre></code>
   *
   * @param value the boolean value to write.
   */
  public void writeBoolean(boolean value)
    throws IOException
  {
    if (value)
      write("true");
    else
      write("false");
  }

  /**
   * Writes an integer value to the stream.  The integer will be written
   * with the following syntax:
   *
   * @param value the integer value to write.
   */
  public void writeInt(int value)
    throws IOException
  {
    writeLong(value);
  }

  /**
   * Writes a long value to the stream.  The long will be written
   * with the following syntax:
   *
   * <code><pre>
   * L b64 b56 b48 b40 b32 b24 b16 b8
   * </pre></code>
   *
   * @param value the long value to write.
   */
  public void writeLong(long value)
    throws IOException
  {
    if (value == 0) {
      write('0');
      return;
    }
    
    byte []buffer = _buffer;
    int offset = _offset;
    
    if (buffer.length <= offset + 32) {
      flushBuffer();
      _offset = offset;
    }

    if (value < 0) {
      value = -value;
      buffer[offset++] = '-';
    }

    int startOffset = offset;

    for (; value > 0; value /= 10) {
      int digit = (int) value % 10;

      buffer[offset++] = (byte) ('0' + digit);
    }

    int pivot = (offset - startOffset) / 2;

    while (pivot-- > 0) {
      byte tmp = buffer[startOffset + pivot];
      buffer[startOffset + pivot] = buffer[offset - pivot - 1];
      buffer[offset - pivot - 1] = tmp;
    }

    _offset = offset;
  }

  /**
   * Writes a double value to the stream.  The double will be written
   * with the following syntax:
   *
   * <code><pre>
   * D b64 b56 b48 b40 b32 b24 b16 b8
   * </pre></code>
   *
   * @param value the double value to write.
   */
  public void writeDouble(double value)
    throws IOException
  {
  }

  /**
   * Writes a date to the stream.
   *
   * <code><pre>
   * T  b64 b56 b48 b40 b32 b24 b16 b8
   * </pre></code>
   *
   * @param time the date in milliseconds from the epoch in UTC
   */
  public void writeUTCDate(long time)
    throws IOException
  {
  }

  /**
   * Writes a null value to the stream.
   * The null will be written with the following syntax
   *
   * <code><pre>
   * N
   * </pre></code>
   *
   * @param value the string value to write.
   */
  public void writeNull()
    throws IOException
  {
    write("null");
  }

  /**
   * Writes a string value to the stream using UTF-8 encoding.
   * The string will be written with the following syntax:
   *
   * If the value is null, it will be written as
   *
   * <code><pre>
   * N
   * </pre></code>
   *
   * @param value the string value to write.
   */
  public void writeString(String value)
    throws IOException
  {
    if (value == null)
      write("null");
    else {
      write('"');
      writeUTF(value);
      write('"');
    }
  }

  /**
   * Writes a string value to the stream using UTF-8 encoding.
   * The string will be written with the following syntax:
   *
   * @param value the string value to write.
   */
  public void writeString(char []buffer, int offset, int length)
    throws IOException
  {
    write('"');
    writeUTF(buffer, offset, length);
    write('"');
  }

  /**
   * Writes a byte array to the stream.
   * The array will be written with the following syntax:
   *
   * <code><pre>
   * B b16 b18 bytes
   * </pre></code>
   *
   * If the value is null, it will be written as
   *
   * <code><pre>
   * N
   * </pre></code>
   *
   * @param value the string value to write.
   */
  public void writeBytes(byte []buffer)
    throws IOException
  {
  }
  
  /**
   * Writes a byte array to the stream.
   * The array will be written with the following syntax:
   *
   * <code><pre>
   * B b16 b18 bytes
   * </pre></code>
   *
   * If the value is null, it will be written as
   *
   * <code><pre>
   * N
   * </pre></code>
   *
   * @param value the string value to write.
   */
  public void writeBytes(byte []buffer, int offset, int length)
    throws IOException
  {
  }
  
  /**
   * Writes a byte buffer to the stream.
   */
  public void writeByteBufferStart()
    throws IOException
  {
  }
  
  /**
   * Writes a byte buffer to the stream.
   *
   * <code><pre>
   * b b16 b18 bytes
   * </pre></code>
   *
   * @param value the string value to write.
   */
  public void writeByteBufferPart(byte []buffer,
					   int offset,
					   int length)
    throws IOException
  {
  }
  
  /**
   * Writes the last chunk of a byte buffer to the stream.
   *
   * <code><pre>
   * b b16 b18 bytes
   * </pre></code>
   *
   * @param value the string value to write.
   */
  public void writeByteBufferEnd(byte []buffer,
				 int offset,
				 int length)
    throws IOException
  {
  }

  /**
   * Writes a reference.
   *
   * @param value the integer value to write.
   */
  protected void writeRef(int value)
    throws IOException
  {
  }

  /**
   * Removes a reference.
   */
  public boolean removeRef(Object obj)
    throws IOException
  {
    return false;
  }

  /**
   * Replaces a reference from one object to another.
   */
  public boolean replaceRef(Object oldRef, Object newRef)
    throws IOException
  {
    return false;
  }

  /**
   * Adds an object to the reference list.  If the object already exists,
   * writes the reference, otherwise, the caller is responsible for
   * the serialization.
   *
   * @param object the object to add as a reference.
   *
   * @return true if the object has already been written.
   */
  public boolean addRef(Object object)
    throws IOException
  {
    return false;
  }

  /**
   * Resets the references for streaming.
   */
  public void resetReferences()
  {
  }

  /**
   * Writes a generic object to the output stream.
   */
  public void writeObject(Object object)
    throws IOException
  {
  }

  /**
   * Writes the list header to the stream.  List writers will call
   * <code>writeListBegin</code> followed by the list contents and then
   * call <code>writeListEnd</code>.
   */
  public boolean writeListBegin(int length, String type)
    throws IOException
  {
    write('[');

    return false;
  }

  /**
   * Writes the tail of the list to the stream.
   */
  public void writeListEnd()
    throws IOException
  {
    write(']');
  }

  /**
   * Writes the map header to the stream.  Map writers will call
   * <code>writeMapBegin</code> followed by the map contents and then
   * call <code>writeMapEnd</code>.
   *
   * <code><pre>
   * M type (<key> <value>)* Z
   * </pre></code>
   */
  public void writeMapBegin(String type)
    throws IOException
  {
    write("{class:\"");
    write(type);
    write('\"');
  }

  /**
   * Writes the tail of the map to the stream.
   */
  public void writeMapEnd()
    throws IOException
  {
    write('}');
  }

  protected void write(int ch)
    throws IOException
  {
    byte []buffer = _buffer;

    if (buffer.length <= _offset)
      flushBuffer();

    buffer[_offset++] = (byte) ch;
  }

  protected void write(String s)
    throws IOException
  {
    int len = s.length();
    
    byte []buffer = _buffer;
    int offset = _offset;

    int sOff = 0;
    while (len-- > 0) {
      if (buffer.length <= offset) {
	_offset = offset;
	flushBuffer();
	offset = _offset;
      }
      
      buffer[offset++] = (byte) s.charAt(sOff++);
    }

    _offset = offset;
  }

  protected void writeUTF(String s)
    throws IOException
  {
    int len = s.length();
    
    byte []buffer = _buffer;
    int offset = _offset;

    int sOff = 0;
    while (len > 0) {
      if (buffer.length <= offset + 2) {
	_offset = offset;
	flushBuffer();
	offset = _offset;
      }

      int ch = s.charAt(sOff++);

      if (ch < 0x80)
	buffer[offset++] = (byte) ch;
      else if (ch < 0x800) {
	buffer[offset++] = (byte) (0xc0 + (ch >> 6));
	buffer[offset++] = (byte) (0x80 + (ch & 0x3f));
      }
      else {
	buffer[offset++] = (byte) (0xe0 + (ch >> 12));
	buffer[offset++] = (byte) (0x80 + ((ch >> 6) & 0x3f));
	buffer[offset++] = (byte) (0x80 + (ch & 0x3f));
      }
    }

    _offset = offset;
  }

  protected void writeUTF(char []s, int sOff, int len)
    throws IOException
  {
    byte []buffer = _buffer;
    int offset = _offset;

    while (len > 0) {
      if (buffer.length <= offset + 2) {
	_offset = offset;
	flushBuffer();
	offset = _offset;
      }

      int ch = s[sOff++];

      if (ch < 0x80)
	buffer[offset++] = (byte) ch;
      else if (ch < 0x800) {
	buffer[offset++] = (byte) (0xc0 + (ch >> 6));
	buffer[offset++] = (byte) (0x80 + (ch & 0x3f));
      }
      else {
	buffer[offset++] = (byte) (0xe0 + (ch >> 12));
	buffer[offset++] = (byte) (0x80 + ((ch >> 6) & 0x3f));
	buffer[offset++] = (byte) (0x80 + (ch & 0x3f));
      }
    }

    _offset = offset;
  }

  protected void flushBuffer()
    throws IOException
  {
    if (_offset > 0)
      _os.write(_buffer, 0 , _offset);
    
    _offset = 0;
  }

  public void flush()
    throws IOException
  {
    flushBuffer();
  }

  public void close()
    throws IOException
  {
    flush();
  }
}
