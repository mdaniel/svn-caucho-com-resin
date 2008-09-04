/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import java.io.*;
import java.net.*;
import java.util.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

/**
 * A fast bufferered input stream supporting both character
 * and byte data.  The underlying stream sources are provided by StreamImpl
 * classes, so all streams have the same API regardless of the underlying
 * implementation.
 *
 * <p>Dynamic streams, like tcp and http 
 * will properly flush writes before reading input.  And random access
 * streams, like RandomAccessFile, can use the same API as normal streams.
 *
 * <p>Most applications will use the Path routines to create their own streams.
 * Specialized applications, like servers, need the capability of recycling
 * streams.
 */
public final class ReadStream extends InputStream {
  public static int ZERO_COPY_SIZE = 1024;
  
  private TempBuffer _tempRead;
  private byte []_readBuffer;
  private int _readOffset;
  private int _readLength;

  private WriteStream _sibling;

  private StreamImpl _source;
  private long _position;

  private Reader _readEncoding;
  private String _readEncodingName;
  private int _specialEncoding;

  private boolean _disableClose;
  private boolean _isDisableCloseSource;
  private boolean _reuseBuffer;
  private Reader _reader;

  /**
   * Creates an uninitialized stream. Use <code>init</code> to initialize.
   */ 
  public ReadStream()
  {
  }

  /**
   * Creates a stream and initializes with a specified source.
   *
   * @param source Underlying source for the stream.
   */
  public ReadStream(StreamImpl source)
  {
    init(source, null);
  }

  /**
   * Creates a stream and initializes with a specified source.
   *
   * @param source Underlying source for the stream.
   * @param sibling Sibling write stream.
   */
  public ReadStream(StreamImpl source, WriteStream sibling)
  {
    init(source, sibling);
  }

  /**
   * Initializes the stream with a given source.
   *
   * @param source Underlying source for the stream.
   * @param sibling Sibling write stream
   */
  public void init(StreamImpl source, WriteStream sibling)
  {
    _disableClose = false;
    _isDisableCloseSource = false;

    if (_source != null && _source != source) {
      try {
	close();
      } catch (IOException e) {
	e.printStackTrace();
      }
    }
    if (source == null)
      throw new IllegalArgumentException();

    _source = source;
    _sibling = sibling;

    if (source.canRead()) {
      if (_tempRead == null) {
        _tempRead = TempBuffer.allocate();
        _readBuffer = _tempRead._buf;
      }
    }
    _readOffset = 0;
    _readLength = 0;

    _readEncoding = null;
    _readEncodingName = "ISO-8859-1";
  }

  public void setSibling(WriteStream sibling)
  {
    _sibling = sibling;
  }

  public WriteStream getSibling()
  {
    return _sibling;
  }

  /**
   * Returns the underlying source for the stream.
   *
   * @return the source
   */
  public StreamImpl getSource()
  {
    return _source;
  }

  public void setReuseBuffer(boolean reuse)
  {
    _reuseBuffer = reuse;
  }

  /**
   * Pushes a filter on the top of the stream stack.
   *
   * @param filter the filter to be added.
   */
  public void pushFilter(StreamFilter filter)
  {
    filter.init(_source);
    _source = filter;
  }

  public byte []getBuffer()
  {
    return _readBuffer;
  }

  public int getOffset()
  {
    return _readOffset;
  }

  public int getLength()
  {
    return _readLength;
  }

  public void setOffset(int offset)
  {
    _readOffset = offset;
  }

  /**
   * Returns the read position.
   */
  public long getPosition()
  {
    return _position - (_readLength - _readOffset);
  }
  
  /**
   * Returns the sets current read position.
   */
  public boolean setPosition(long pos)
    throws IOException
  {
    if (pos < _position) {
      _position = pos;
      _readLength = _readOffset = 0;

      if (_source != null) {
	_source.seekStart(pos);

	return true;
      }
      else
	return false;
    }
    else if (pos < _position + _readLength) {
      _readOffset = (int) (pos - _position);

      return true;
    }
    else {
      long n = pos - _position - _readOffset;
      
      return skip(n) == n;
    }
  }

  /**
   * Returns true if the stream allows reading.
   */
  public boolean canRead()
  {
    return _source.canRead();
  }

  /**
   * Clears the read buffer.
   */
  public void clearRead()
  {
    _readOffset = 0;
    _readLength = 0;
  }

  /**
   * Returns an estimate of the available bytes.  If a read would not block,
   * it will always return greater than 0.
   */
  public int getAvailable() throws IOException
  {
    if (_readOffset < _readLength) {
      return _readLength - _readOffset;
    }

    if (_sibling != null)
      _sibling.flush();
    
    return _source.getAvailable();
  }

  /**
   * Returns true if data in the buffer is available.
   */
  public int getBufferAvailable() throws IOException
  {
    return _readLength - _readOffset;
  }

  /**
   * Compatibility with InputStream.
   */
  public int available() throws IOException
  {
    return getAvailable();
  }

  /**
   * Returns the next byte or -1 if at the end of file.
   */
  public final int read() throws IOException
  {
    if (_readLength <= _readOffset) {
      if (! readBuffer())
	return -1;
    }

    return _readBuffer[_readOffset++] & 0xff;
  }

  /**
   * Unreads the last byte.
   */
  public final void unread()
  {
    if (_readOffset <= 0)
      throw new RuntimeException();

    _readOffset--;
  }

  /**
   * Waits for data to be available.
   */
  public final boolean waitForRead() throws IOException
  {
    if (_readLength <= _readOffset) {
      if (! readBuffer())
	return false;
    }

    return true;
  }

  /**
   * Skips the next <code>n</code> bytes.
   *
   * @param n bytes to skip.
   *
   * @return number of bytes skipped.
   */
  public long skip(long n)
    throws IOException
  {
    long count = _readLength - _readOffset;
    
    if (n < count) {
      _readOffset += n;
      return n;
    }

    _readLength = 0;
    _readOffset = 0;

    if (_source.hasSkip()) {
      long skipped = _source.skip(n - count);
      
      if (skipped < 0)
        return count;
      else
        return skipped + count;
    }
    
    while (_readLength < _readOffset + n - count) {
      count += _readLength - _readOffset;
      _readOffset = 0;
      _readLength = 0;

      if (! readBuffer())
	return count;
    }
    
    _readOffset += (int) (n - count);

    return n;
  }

  /**
   * Reads into a byte array.  <code>read</code> may return less than
   * the maximum bytes even if more bytes are available to read.
   *
   * @param buf byte array
   * @param offset offset into the byte array to start reading
   * @param length maximum byte allowed to read.
   *
   * @return number of bytes read or -1 on end of file.
   */
  public final int read(byte []buf, int offset, int length)
    throws IOException
  {
    int readOffset = _readOffset;
    int readLength = _readLength;

    if (readLength <= readOffset) {
      if (ZERO_COPY_SIZE <= length) {
        if (_sibling != null)
          _sibling.flush();

        int len = _source.read(buf, offset, length);

	if (len > 0)
	  _position += len;

	return len;
      }
        
      if (! readBuffer())
	return -1;

      readOffset = _readOffset;
      readLength = _readLength;
    }

    int sublen = readLength - readOffset;
    if (length < sublen)
      sublen = length;

    System.arraycopy(_readBuffer, readOffset, buf, offset, sublen);

    _readOffset = readOffset + sublen;
    
    return sublen;
  }

  /**
   * Reads into a byte array.  <code>readAll</code> will always read
   * <code>length</code> bytes, blocking if necessary, until the end of
   * file is reached.
   *
   * @param buf byte array
   * @param offset offset into the byte array to start reading
   * @param length maximum byte allowed to read.
   *
   * @return number of bytes read or -1 on end of file.
   */
  public int readAll(byte []buf, int offset, int length) throws IOException
  {
    int readLength = 0;

    while (length > 0) {
      int sublen = read(buf, offset, length);

      if (sublen < 0)
	return readLength == 0 ? -1 : readLength;

      offset += sublen;
      readLength += sublen;
      length -= sublen;
    }

    return readLength == 0 ? -1 : readLength;
  }

  /*
   * Reader methods
   */

  /**
   * Sets the current read encoding.  The encoding can either be a
   * Java encoding name or a mime encoding.
   *
   * @param encoding name of the read encoding
   */
  public void setEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    String mimeName = Encoding.getMimeName(encoding);
    
    if (mimeName != null && mimeName.equals(_readEncodingName))
      return;
    
    _readEncoding = Encoding.getReadEncoding(this, encoding);
    _readEncodingName = mimeName;
  }

  /**
   * Returns the mime-encoding currently read.
   */
  public String getEncoding()
  {
    return _readEncodingName;
  }

  /**
   * Reads a character from the stream, returning -1 on end of file.
   */
  public final int readChar() throws IOException
  {
    if (_readEncoding != null) {
      int ch = _readEncoding.read();
      return ch;
    }

    if (_readLength <= _readOffset) {
      if (! readBuffer())
	return -1;
    }

    return _readBuffer[_readOffset++] & 0xff;
  }

  /**
   * Reads into a character buffer from the stream.  Like the byte
   * array version, read may return less characters even though more
   * characters are available.
   *
   * @param buf character buffer to fill
   * @param offset starting offset into the character buffer
   * @param length maximum number of characters to read
   * @return number of characters read or -1 on end of file.
   */
  public final int read(char []buf, int offset, int length) throws IOException
  {
    if (_readEncoding != null)
      return _readEncoding.read(buf, offset, length);
    
    byte []readBuffer = _readBuffer;
    if (readBuffer == null)
      return -1;

    int readOffset = _readOffset;
    int readLength = _readLength;

    int sublen = readLength - readOffset;

    if (sublen <= 0) {
      if (! readBuffer()) {
	return -1;
      }
      readLength = _readLength;
      readOffset = _readOffset;
      sublen = readLength - readOffset;
    }

    if (length < sublen)
      sublen = length;

    for (int i = 0; i < sublen; i++)
      buf[offset + i] = (char) (readBuffer[readOffset + i] & 0xff);

    _readOffset = readOffset + sublen;

    return sublen;
  }

  /**
   * Reads into a character buffer from the stream.  <code>length</code>
   * characters will always be read until the end of file is reached.
   *
   * @param buf character buffer to fill
   * @param offset starting offset into the character buffer
   * @param length maximum number of characters to read
   * @return number of characters read or -1 on end of file.
   */
  public int readAll(char []buf, int offset, int length) throws IOException
  {
    int readLength = 0;

    while (length > 0) {
      int sublen = read(buf, offset, length);

      if (sublen <= 0)
	return readLength > 0 ? readLength : -1;

      offset += sublen;
      readLength += sublen;
      length -= sublen;
    }

    return readLength;
  }

  /**
   * Reads characters from the stream, appending to the character buffer.
   *
   * @param buf character buffer to fill
   * @param length maximum number of characters to read
   * @return number of characters read or -1 on end of file.
   */
  public int read(CharBuffer buf, int length) throws IOException
  {
    int len = buf.getLength();

    buf.setLength(len + length);
    int readLength = read(buf.getBuffer(), len, length);
    if (readLength < 0)
      buf.setLength(len);
    else if (readLength < length)
      buf.setLength(len + readLength);

    return length;
  }

  /**
   * Reads characters from the stream, appending to the character buffer.
   * <code>length</code> characters will always be read until the end of
   * file.
   *
   * @param buf character buffer to fill
   * @param length maximum number of characters to read
   * @return number of characters read or -1 on end of file.
   */
  public int readAll(CharBuffer buf, int length) throws IOException
  {
    int len = buf.getLength();

    buf.setLength(len + length);
    int readLength = readAll(buf.getBuffer(), len, length);
    if (readLength < 0)
      buf.setLength(len);
    else if (readLength < length)
      buf.setLength(len + readLength);

    return length;
  }

  /**
   * Reads a line from the stream, returning a string.
   */
  public final String readln() throws IOException
  {
    return readLine();
  }

  /**
   * Reads a line, returning a string.
   */
  public String readLine() throws IOException
  {
    CharBuffer cb = new CharBuffer();

    if (readLine(cb, true))
      return cb.toString();
    else if (cb.length() == 0)
      return null;
    else
      return cb.toString();
  }

  /**
   * Reads a line, returning a string.
   */
  public String readLineNoChop() throws IOException
  {
    CharBuffer cb = new CharBuffer();

    if (readLine(cb, false))
      return cb.toString();
    else if (cb.length() == 0)
      return null;
    else
      return cb.toString();
  }

  /**
   * Fills the buffer with the next line from the input stream.
   *
   * @return true on success, false on end of file.
   */
  public final boolean readln(CharBuffer cb) throws IOException
  {
    return readLine(cb, true);
  }
  
  /**
   * Reads a line into the character buffer.  \r\n is converted to \n.
   *
   * @param buf character buffer to fill
   * @return false on end of file
   */
  public final boolean readLine(CharBuffer cb)
    throws IOException
  {
    return readLine(cb, true);
  }
  
  /**
   * Reads a line into the character buffer.  \r\n is converted to \n.
   *
   * @param buf character buffer to fill
   * @return false on end of file
   */
  public final boolean readLine(CharBuffer cb, boolean isChop)
    throws IOException
  {
    if (_readEncoding != null)
      return readlnEncoded(cb, isChop);

    int capacity = cb.getCapacity();
    int offset = cb.getLength();
    char []buf = cb.getBuffer();

    byte []readBuffer = _readBuffer;
    int startOffset = offset;

    while (true) {
      int readOffset = _readOffset;
      
      int sublen = _readLength - readOffset;
      if (capacity - offset < sublen)
        sublen = capacity - offset;

      for (; sublen > 0; sublen--) {
        int ch = readBuffer[readOffset++] & 0xff;

	if (ch != '\n') {
	  buf[offset++] = (char) ch;
	}
        else if (isChop) {
          if (offset > 0 && buf[offset - 1] == '\r')
            cb.setLength(offset - 1);
          else
            cb.setLength(offset);
          
          _readOffset = readOffset;

          return true;
        }
	else {
	  buf[offset++] = (char) '\n';

	  cb.setLength(offset);

	  _readOffset = readOffset;

	  return true;
	}
      }

      _readOffset = readOffset;

      if (_readLength <= readOffset) {
	if (! readBuffer()) {
	  cb.setLength(offset);
	  return startOffset < offset;
	}
      }

      if (capacity <= offset) {
	cb.setLength(offset + 1);
	capacity = cb.getCapacity();
	buf = cb.getBuffer();
      }
    }
  }
  
  /**
   * Reads a line into the character buffer.  \r\n is converted to \n.
   *
   * @param buf character buffer to fill.
   * @param length number of characters to fill.
   *
   * @return -1 on end of file or the number of characters read.
   */
  public final int readLine(char []buf, int length)
    throws IOException
  {
    return readLine(buf, length, true);
  }
  
  /**
   * Reads a line into the character buffer.  \r\n is converted to \n.
   *
   * @param buf character buffer to fill.
   * @param length number of characters to fill.
   *
   * @return -1 on end of file or the number of characters read.
   */
  public final int readLine(char []buf, int length, boolean isChop)
    throws IOException
  {
    byte []readBuffer = _readBuffer;

    int offset = 0;
    
    while (true) {
      int readOffset = _readOffset;

      int sublen = _readLength - readOffset;
      if (sublen < length)
        sublen = length;

      for (; sublen > 0; sublen--) {
        int ch = readBuffer[readOffset++] & 0xff;

	if (ch != '\n') {
	}
        else if (isChop) {
          _readOffset = readOffset;
          
          if (offset > 0 && buf[offset - 1] == '\r')
            return offset - 1;
          else
            return offset;
        }
        else {
	  buf[offset++] = (char) ch;
	  
          _readOffset = readOffset;
	  
	  return offset + 1;
        }

        buf[offset++] = (char) ch;
      }
      _readOffset = readOffset;

      if (readOffset <= _readLength) {
	if (! readBuffer()) {
	  return offset;
	}
      }

      if (length <= offset)
        return length + 1;
    }
  }
  
  private boolean readlnEncoded(CharBuffer cb, boolean isChop)
    throws IOException
  {
    while (true) {
      int ch = readChar();

      if (ch < 0)
	return cb.length() > 0;

      if (ch != '\n') {
      }
      else if (isChop) {
	if (cb.length() > 0 && cb.getLastChar() == '\r')
	  cb.setLength(cb.getLength() - 1);

	return true;
      }
      else {
	cb.append('\n');

	return true;
      }

      cb.append((char) ch);
    }
  }

  /**
   * Copies this stream to the output stream.
   *
   * @param os destination stream.
   */
  public void writeToStream(OutputStream os) throws IOException
  {
    if (_readLength <= _readOffset) {
      readBuffer();
    }

    while (_readOffset < _readLength) {
      os.write(_readBuffer, _readOffset, _readLength - _readOffset);

      readBuffer();
    }
  }

  /**
   * Writes <code>len<code> bytes to the output stream from this stream.
   *
   * @param os destination stream.
   * @param len bytes to write.
   */
  public void writeToStream(OutputStream os, int len) throws IOException
  {
    while (len > 0) {
      if (_readLength <= _readOffset) {
	if (! readBuffer())
	  return;
      }

      int sublen = _readLength - _readOffset;
      if (len < sublen)
	sublen = len;

      os.write(_readBuffer, _readOffset, sublen);
      _readOffset += sublen;
      len -= sublen;
    }
  }

  /**
   * Copies this stream to the output stream.
   *
   * @param out destination writer
   */
  public void writeToWriter(Writer out) throws IOException
  {
    int ch;
    while ((ch = readChar()) >= 0)
      out.write((char) ch);
  }

  /**
   * Fills the buffer from the underlying stream.
   */
  public int fillBuffer()
    throws IOException
  {
    if (! readBuffer())
      return -1;
    else
      return _readLength;
  }

  /**
   * Fills the buffer with a non-blocking read.
   */
  public boolean readNonBlock()
    throws IOException
  {
    if (_readOffset < _readLength)
      return true;
    
    if (_readBuffer == null) {
      _readOffset = 0;
      _readLength = 0;
      return false;
    }

    if (_sibling != null)
      _sibling.flush();

    _readOffset = 0;
    _readLength = _source.readNonBlock(_readBuffer, 0, _readBuffer.length);
    
    // Setting to 0 is needed to avoid int to long conversion errors with AIX
    if (_readLength > 0) {
      _position += _readLength;
      return true;
    }
    else {
      _readLength = 0;
      return false;
    }
  }

  /**
   * Fills the read buffer, flushing the write buffer.
   *
   * @return false on end of file and true if there's more data.
   */
  private boolean readBuffer()
    throws IOException
  {
    if (_readBuffer == null) {
      _readOffset = 0;
      _readLength = 0;
      return false;
    }

    if (_sibling != null)
      _sibling.flush();

    _readOffset = 0;
    _readLength = _source.read(_readBuffer, 0, _readBuffer.length);
    
    // Setting to 0 is needed to avoid int to long conversion errors with AIX
    if (_readLength > 0) {
      _position += _readLength;
      return true;
    }
    else {
      _readLength = 0;
      return false;
    }
  }

  private boolean readBuffer(int off)
    throws IOException
  {
    if (_readBuffer == null)
      return false;

    if (_sibling != null)
      _sibling.flush();

    _readOffset = 0;
    _readLength = _source.read(_readBuffer, off, _readBuffer.length - off);

    // Setting to 0 is needed to avoid int to long conversion errors with AIX
    if (_readLength > 0) {
      _position += _readLength;
      return true;
    }
    else {
      _readLength = 0;
      return false;
    }
  }

  /**
   * Disables close.  Sometimes an application will pass a stream
   * to a client that may close the stream at an inappropriate time.
   * Setting disable close gives the calling routine control over closing
   * the stream.
   */
  public void setDisableClose(boolean disableClose)
  {
    _disableClose = disableClose;
  }

  /**
   * Disables closing of the underlying source.
   */
  public void setDisableCloseSource(boolean disableClose)
  {
    _isDisableCloseSource = disableClose;
  }

  /**
   * Close the stream.
   */
  public final void close() throws IOException
  {
    if (_disableClose)
      return;

    if (! _reuseBuffer) {
      if (_tempRead != null) {
        TempBuffer.free(_tempRead);
      }
      _tempRead = null;
      _readBuffer = null;
    }

    if (_readEncoding != null) {
      Reader reader = _readEncoding;
      _readEncoding = null;
      reader.close();
    }
    
    if (_source != null && ! _isDisableCloseSource) {
      StreamImpl s = _source;
      _source = null;
      s.close();
    }
  }

  /**
   * Returns a named attribute.  For example, an HTTP stream
   * may use this to return header values.
   */
  public Object getAttribute(String name)
    throws IOException
  {
    if (_sibling != null)
      _sibling.flush();
    
    return _source.getAttribute(name);
  }

  /**
   * Lists all named attributes.
   */
  public Iterator getAttributeNames()
    throws IOException
  {
    if (_sibling != null)
      _sibling.flush();
    
    return _source.getAttributeNames();
  }

  /**
   * Sets a named attribute.  For example, an HTTP stream
   * may use this to set header values.
   */
  public void setAttribute(String name, Object value)
    throws IOException
  {
    _source.setAttribute(name, value);
  }

  /**
   * Removes a named attribute.
   */
  public void removeAttribute(String name)
    throws IOException
  {
    _source.removeAttribute(name);
  }

  /**
   * Returns the Path which opened this stream.
   */
  public Path getPath()
  {
    return _source == null ? null : _source.getPath();
  }

  /**
   * Returns the user path which opened this stream.
   *
   * <p>Parsing routines typically use this for error reporting.
   */
  public String getUserPath()
  {
    if (_source == null || _source.getPath() == null)
      return "stream";
    else
      return _source.getPath().getUserPath();
  }

  /**
   * Returns the user path which opened this stream.
   *
   * <p>Parsing routines typically use this for error reporting.
   */
  public String getURL()
  {
    if (_source == null || _source.getPath() == null)
      return "stream:";
    else
      return _source.getPath().getURL();
  }

  /**
   * Sets a path name associated with the stream.
   */
  public void setPath(Path path)
  {
    _source.setPath(path);
  }

  /**
   * Returns a Reader reading to this stream.
   */
  public Reader getReader()
  {
    if (_reader == null)
      _reader = new StreamReader();

    return _reader;
  }

  /**
   * Returns a printable representation of the read stream.
   */
  public String toString()
  {
    return "ReadStream[" + _source + "]";
  }

  class StreamReader extends Reader {
    public final int read()
      throws IOException
    {
      return ReadStream.this.readChar();
    }

    public final int read(char []cbuf, int off, int len)
      throws IOException
    {
      return ReadStream.this.read(cbuf, off, len);
    }

    public boolean ready()
      throws IOException
    {
      return ReadStream.this.available() > 0;
    }
  
    public final void close()
      throws IOException
    {
    }

    ReadStream getStream()
    {
      return ReadStream.this;
    }
  }
}
