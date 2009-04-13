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

import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.WriteStream;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class ResponseStream extends ToByteResponseStream {
  private static final Logger log
    = Logger.getLogger(ResponseStream.class.getName());
  
  static final L10N L = new L10N(ResponseStream.class);

  private static final int _tailChunkedLength = 7;
  private static final byte []_tailChunked =
    new byte[] {'\r', '\n', '0', '\r', '\n', '\r', '\n'};
  
  private final AbstractHttpResponse _response;
  
  private WriteStream _next;
  
  private OutputStream _cacheStream;
  private long _cacheMaxLength;
  // used for the direct copy and caching
  private int _bufferStartOffset;
  
  private boolean _chunkedEncoding;

  private byte []_singleByteBuffer = new byte[1];
  private int _bufferSize;
  private boolean _disableAutoFlush;
  
  // bytes actually written
  private int _contentLength;
  // True for the first chunk
  private boolean _isFirst;

  private boolean _allowFlush = true;
  private boolean _isHead = false;
  private boolean _isClosed = false;
  
  private final byte []_buffer = new byte[16];

  ResponseStream(AbstractHttpResponse response)
  {
    _response = response;
  }

  public void init(WriteStream next)
  {
    _next = next;
  }
  
  /**
   * initializes the Response stream at the beginning of a request.
   */
  public void start()
  {
    super.start();
    
    _chunkedEncoding = false;

    _contentLength = 0;
    _allowFlush = true;
    _disableAutoFlush = false;
    _isClosed = false;
    _isHead = false;
    _cacheStream = null;
    _isFirst = true;
    _bufferStartOffset = 0;
  }

  /**
   * Returns true for a Caucho response stream.
   */
  public boolean isCauchoResponseStream()
  {
    return true;
  }

  /**
   * Sets the underlying cache stream for a cached request.
   *
   * @param cacheStream the cache stream.
   */
  public void setByteCacheStream(OutputStream cacheStream)
  {
    _cacheStream = cacheStream;
    
    CauchoRequest req = _response.getRequest();
    WebApp app = req.getWebApp();
    _cacheMaxLength = app.getCacheMaxLength();
  }

  /**
   * Response stream is a writable stream.
   */
  public boolean canWrite()
  {
    return true;
  }

  void setFlush(boolean flush)
  {
    _allowFlush = flush;
  }

  public void setAutoFlush(boolean isAutoFlush)
  {
    setDisableAutoFlush(! isAutoFlush);
  }

  void setDisableAutoFlush(boolean disable)
  {
    _disableAutoFlush = disable;
  }

  public void setHead()
  {
    _isHead = true;
    _bufferSize = 0;
  }

  public final boolean isHead()
  {
    return _isHead;
  }

  @Override
  public int getContentLength()
  {
    // server/05e8
    try {
      flushCharBuffer();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (_isCommitted)
      return _contentLength;
    else
      return super.getContentLength();
  }

  public void setBufferSize(int size)
  {
    if (isCommitted())
      throw new IllegalStateException(L.l("Buffer size cannot be set after commit"));

    super.setBufferSize(size);
  }

  public boolean isCommitted()
  {
    // jsp/17ec
    return _isCommitted || _isClosed;
  }

  public boolean hasData()
  {
    return _isCommitted || _contentLength > 0;
  }
  
  public boolean isFlushed()
  {
    try {
      if (_isCommitted)
	return true;

      if (_contentLength > 0) {
	flushCharBuffer();
	int bufferOffset = getByteBufferOffset();

	// server/05e8
	if (_contentLength <= bufferOffset)
	  return true;
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
      
      return true;
    }
    
    return false;
  }

  public void clear()
    throws IOException
  {
    clearBuffer();
    
    if (_isCommitted)
      throw new IOException(L.l("can't clear response after writing headers"));
  }
  
  public void clearBuffer()
  {
    super.clearBuffer();

    if (! _isCommitted) {
      // jsp/15la
      _isFirst = true;
      _bufferStartOffset = 0;
      _response.setHeaderWritten(false);
    }

    _next.setBufferOffset(_bufferStartOffset);
  }

  /**
   * Clear the closed state, because of the NOT_MODIFIED
   */
  public void clearClosed()
  {
    _isClosed = false;
  }

  private void writeHeaders(int length)
    throws IOException
  {
    _isCommitted = true;
    _chunkedEncoding = _response.writeHeaders(_next, length);
  }

  @Override
  public void write(int ch)
    throws IOException
  {
    _singleByteBuffer[0] = (byte) ch;

    write(_singleByteBuffer, 0, 1);
  }

  /**
   * Returns the byte buffer.
   */
  @Override
  public void write(byte []buffer, int offset, int length)
    throws IOException
  {
    if (_isCommitted) {
      flushBuffer();
      
      writeNext(buffer, offset, length, false);
    }
    else
      super.write(buffer, offset, length);
  }

  /**
   * Returns the byte buffer.
   */
  @Override
  public byte []getBuffer()
    throws IOException
  {
    if (_isCommitted) {
      flushBuffer();
      
      return _next.getBuffer();
    }
    else
      return super.getBuffer();
  }

  /**
   * Returns the byte offset.
   */
  public int getBufferOffset()
    throws IOException
  {
    if (! _isCommitted)
      return super.getBufferOffset();
    
    flushBuffer();
      
    int offset;

    offset = _next.getBufferOffset();

    if (! _chunkedEncoding) {
      //_bufferStartOffset = offset;
      return offset;
    }
    else if (_bufferStartOffset > 0) {
      return offset;
    }

    byte []buffer;
    // chunked allocates 8 bytes for the chunk header
    buffer = _next.getBuffer();
    if (buffer.length - offset < 8) {
      _next.flushBuffer();
      
      buffer = _next.getBuffer();
      offset = _next.getBufferOffset();
    }

    _bufferStartOffset = offset + 8;
    _next.setBufferOffset(offset + 8);

    return _bufferStartOffset;
  }

  /**
   * Sets the next buffer
   */
  public byte []nextBuffer(int offset)
    throws IOException
  {
    if (! _isCommitted) {
      // server/055b
      return super.nextBuffer(offset);
      // _bufferStartOffset = _next.getBufferOffset();
    }
    
    if (_isClosed)
      return _next.getBuffer();
    
    flushBuffer();
      
    int startOffset = _bufferStartOffset;
    _bufferStartOffset = 0;

    int length = offset - startOffset;
    long lengthHeader = _response.getContentLengthHeader();

    if (lengthHeader > 0 && lengthHeader < _contentLength + length) {
      _isCommitted = true;
    
      lengthException(_next.getBuffer(), startOffset, length, lengthHeader);

      length = (int) (lengthHeader - _contentLength);
      offset = startOffset + length;
    }

    _contentLength += length;

    try {
      if (_isHead) {
	return _next.getBuffer();
      }
      else if (_chunkedEncoding) {
	if (length == 0)
	  throw new IllegalStateException();
      
	byte []buffer = _next.getBuffer();

	writeChunk(buffer, startOffset, length);

	buffer = _next.nextBuffer(offset);
	      
	if (log.isLoggable(Level.FINE))
	  log.fine(dbgId() + "write-chunk(" + offset + ")");

	_bufferStartOffset = 8 + _next.getBufferOffset();
	_next.setBufferOffset(_bufferStartOffset);

	return buffer;
      }
      else {
	if (_cacheStream != null)
	  writeCache(_next.getBuffer(), startOffset, length);
	
	byte []buffer = _next.nextBuffer(offset);
	_bufferStartOffset = _next.getBufferOffset();
	      
	if (log.isLoggable(Level.FINE))
	  log.fine(dbgId() + "write-chunk(" + offset + ")");

	return buffer;
      }
    } catch (ClientDisconnectException e) {
      _response.clientDisconnect();
      _response.killCache();

      if (_response.isIgnoreClientDisconnect()) {
	return _next.getBuffer();
      }
      else
        throw e;
    } catch (IOException e) {
      _response.killCache();
      
      throw e;
    }
  }

  /**
   * Sets the byte offset.
   */
  public void setBufferOffset(int offset)
    throws IOException
  {
    if (_isClosed)
      return;

    if (! _isCommitted) {
      super.setBufferOffset(offset);
      return;
    }

    flushBuffer();
    
    int startOffset = _bufferStartOffset;
    if (offset == startOffset)
      return;
    
    int length = offset - startOffset;
    long lengthHeader = _response.getContentLengthHeader();

    if (lengthHeader > 0 && lengthHeader < _contentLength + length) {
      lengthException(_next.getBuffer(), startOffset, length, lengthHeader);

      length = (int) (lengthHeader - _contentLength);
      offset = startOffset + length;
    }

    _contentLength += length;
    
    if (_cacheStream != null && ! _chunkedEncoding) {
      _bufferStartOffset = offset;
      writeCache(_next.getBuffer(), startOffset, length);
    }

    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() +  "write-chunk(" + length + ")");

    if (! _isHead) {
      _next.setBufferOffset(offset);
      _next.flush();
    }
  }
  
  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  protected void writeNext(byte []buf, int offset, int length,
			   boolean isFinished)
    throws IOException
  {
    try {
      if (_isClosed)
	return;

      if (_disableAutoFlush && ! isFinished)
	throw new IOException(L.l("auto-flushing has been disabled"));

      _isCommitted = true;

      boolean isFirst = _isFirst;
      _isFirst = false;

      if (! isFirst) {
      }
      else if (isFinished)
	writeHeaders(getBufferLength());
      else
	writeHeaders(-1);

      int bufferStart = _bufferStartOffset;
      int bufferOffset = _next.getBufferOffset();

      // server/05e2
      if (length == 0 && ! isFinished && bufferStart == bufferOffset)
        return;

      long contentLengthHeader = _response.getContentLengthHeader();
      // Can't write beyond the content length
      if (0 < contentLengthHeader
	  && contentLengthHeader < length + _contentLength) {
	if (lengthException(buf, offset, length, contentLengthHeader))
	  return;

	length = (int) (contentLengthHeader - _contentLength);
      }

      if (_next != null && ! _isHead) {
	if (length > 0 && log.isLoggable(Level.FINE)) {
	  log.fine(dbgId() +  "write-chunk(" + length + ")");
	}
	
	if (! _chunkedEncoding) {
	  byte []nextBuffer = _next.getBuffer();
	  int nextOffset = _next.getBufferOffset();

	  if (nextOffset + length < nextBuffer.length) {
	    System.arraycopy(buf, offset, nextBuffer, nextOffset, length);
	    _next.setBufferOffset(nextOffset + length);
	  }
	  else {
	    _isCommitted = true;

	    _next.write(buf, offset, length);

	    if (log.isLoggable(Level.FINE))
	      log.fine(dbgId() + "write-data(" + _tailChunkedLength + ")");
	  }

	  if (_cacheStream != null)
	    writeCache(buf, offset, length);

	  // server/1975
	  _bufferStartOffset = _next.getBufferOffset();
	}
	else {
	  byte []buffer = _next.getBuffer();
	  int writeLength = length;

	  if (bufferStart == 0 && writeLength > 0) {
	    bufferStart = bufferOffset + 8;
	    bufferOffset = bufferStart;
	  }

	  while (writeLength > 0) {
	    int sublen = buffer.length - bufferOffset;

	    if (writeLength < sublen)
	      sublen = writeLength;

	    System.arraycopy(buf, offset, buffer, bufferOffset, sublen);

	    writeLength -= sublen;
	    offset += sublen;
	    bufferOffset += sublen;

	    if (writeLength > 0) {
	      int delta = bufferOffset - bufferStart;
	      writeChunk(buffer, bufferStart, delta);
			   
	      _isCommitted = true;
	      buffer = _next.nextBuffer(bufferOffset);
	      
	      if (log.isLoggable(Level.FINE))
		log.fine(dbgId() + "write-chunk(" + bufferOffset + ")");
	      
	      bufferStart = _next.getBufferOffset() + 8;
	      bufferOffset = bufferStart;
	    }
	  }

	  _next.setBufferOffset(bufferOffset);
	  _bufferStartOffset = bufferStart;
	}
      }

      if (! _response.isClientDisconnect()) {
        _contentLength += length;
      }
    } catch (ClientDisconnectException e) {
      // server/183c
      _response.killCache();
      _response.clientDisconnect();

      if (! _response.isIgnoreClientDisconnect())
        throw e;
    }
  }
  
  private boolean lengthException(byte []buf, int offset, int length,
				  long contentLengthHeader)
  {
    if (_response.isClientDisconnect() || _isHead || _isClosed) {
    }
    else if (contentLengthHeader < _contentLength) {
      CauchoRequest request = _response.getRequest();
      ServletContext app = request.getWebApp();
      
      Exception exn =
	  new IllegalStateException(L.l("{0}: tried to write {1} bytes beyond the content-length header {2}.  Check that the Content-Length header correctly matches the expected bytes, and ensure that any filter which modifies the content also suppresses the content-length (to use chunked encoding).",
					request.getRequestURL(),
					"" + (length + _contentLength),
					"" + contentLengthHeader));

      if (app != null)
	app.log(exn.getMessage(), exn);
      else
	exn.printStackTrace();

      return false;
    }
    
    for (int i = (int) (offset + contentLengthHeader - _contentLength);
	 i < offset + length;
	 i++) {
      int ch = buf[i];

      if (ch != '\r' && ch != '\n' && ch != ' ' && ch != '\t') {
	CauchoRequest request = _response.getRequest();
	ServletContext app = request.getWebApp();
	String graph = "";
	    
	if (Character.isLetterOrDigit((char) ch))
	  graph = "'" + (char) ch + "', ";
	    
	Exception exn =
	  new IllegalStateException(L.l("{0}: tried to write {1} bytes with content-length {2} (At {3}char={4}).  Check that the Content-Length header correctly matches the expected bytes, and ensure that any filter which modifies the content also suppresses the content-length (to use chunked encoding).",
					request.getRequestURL(),
					"" + (length + _contentLength),
					"" + contentLengthHeader,
					graph,
					"" + ch));

	if (app != null)
	  app.log(exn.toString(), exn);
	else
	  exn.printStackTrace();
	break;
      }
    }
        
    length = (int) (contentLengthHeader - _contentLength);
    return (length <= 0);
  }

  /**
   * Flushes the buffered response to the output stream.
   */
  public void flush()
    throws IOException
  {
    try {
      _disableAutoFlush = false;

      if (_allowFlush && ! _isClosed) {
        flushBuffer();

	if (_chunkedEncoding) {
	  int bufferStart = _bufferStartOffset;
	  _bufferStartOffset = 0;

	  if (bufferStart > 0) {
	    int bufferOffset = _next.getBufferOffset();

	    if (bufferStart != bufferOffset) {
	      writeChunk(_next.getBuffer(), bufferStart,
			 bufferOffset - bufferStart);
	    }
	    else
	      _next.setBufferOffset(bufferStart - 8);
	  }
	}
	else {
	  // jsp/01cf
	  _bufferStartOffset = 0;
	}

        if (_next != null)
          _next.flush();
      }
    } catch (ClientDisconnectException e) {
      _response.clientDisconnect();
      
      if (! _response.isIgnoreClientDisconnect())
        throw e;
    }
  }

  /**
   * Flushes the buffered response to the output stream.
   */
  public void flushByte()
    throws IOException
  {
    flush();
  }

  /**
   * Flushes the buffered response to the writer.
   */
  public void flushChar()
    throws IOException
  {
    flush();
  }

  /**
   * Flushes the buffered response to the output stream.
   */
  /*
  public void flushBuffer()
    throws IOException
  {
    super.flushBuffer();

    // jsp/15la
    // _isCommitted = true;
  }
  */
  
  /**
   * Complete the request.
   */
  public void finish()
    throws IOException
  {
    boolean isClosed = _isClosed;

    if (_next == null || isClosed) {
      _isClosed = true;
      return;
    }

    _disableAutoFlush = false;

    flushCharBuffer();

    _isFinished = true;
    _allowFlush = true;
    
    flushBuffer();
    
    int bufferStart = _bufferStartOffset;
    _bufferStartOffset = 0;
    _isClosed = true;

    // flushBuffer can force 304 and then a cache write which would
    // complete the finish.
    if (isClosed || _next == null) {
      return;
    }
    
    try {
      if (_chunkedEncoding) {
	int bufferOffset = _next.getBufferOffset();

	if (bufferStart > 0 && bufferOffset != bufferStart) {
	  byte []buffer = _next.getBuffer();

	  writeChunk(buffer, bufferStart, bufferOffset - bufferStart);
	}
	else {
	  // server/05b3
	  _next.setBufferOffset(0);
	}

	_isCommitted = true;
	
	ArrayList<String> footerKeys = _response._footerKeys;

	if (footerKeys.size() == 0)
	  _next.write(_tailChunked, 0, _tailChunkedLength);
	else {
	  ArrayList<String> footerValues = _response._footerValues;
	  
	  _next.print("\r\n0\r\n");

	  for (int i = 0; i < footerKeys.size(); i++) {
	    _next.print(footerKeys.get(i));
	    _next.print(": ");
	    _next.print(footerValues.get(i));
	    _next.print("\r\n");
	  }
	  
	  _next.print("\r\n");
	}

	if (log.isLoggable(Level.FINE))
          log.fine(dbgId() + "write-chunk(" + _tailChunkedLength + ")");
      }

      CauchoRequest req = _response.getRequest();
      if (req.isComet() || req.isDuplex()) {
      }
      else if (! req.allowKeepalive()) {
	_isClosed = true;
        if (log.isLoggable(Level.FINE)) {
          log.fine(dbgId() + "close stream");
        }
      
        _next.close();
      }
      else {
	_isClosed = true;
	
        if (log.isLoggable(Level.FINE)) {
          log.fine(dbgId() + "finish/keepalive");
        }
      }
      
      /*
      else if (flush) {
        //_next.flush();
        _next.flushBuffer();
      }
      */
    } catch (ClientDisconnectException e) {
      _response.clientDisconnect();
      
      if (! _response.isIgnoreClientDisconnect()) {
        throw e;
      }
    }
  }

  /**
   * Fills the chunk header.
   */
  private void writeChunk(byte []buffer, int start, int length)
    throws IOException
  {
    buffer[start - 8] = (byte) '\r';
    buffer[start - 7] = (byte) '\n';
    buffer[start - 6] = hexDigit(length >> 12);
    buffer[start - 5] = hexDigit(length >> 8);
    buffer[start - 4] = hexDigit(length >> 4);
    buffer[start - 3] = hexDigit(length);
    buffer[start - 2] = (byte) '\r';
    buffer[start - 1] = (byte) '\n';

    if (_cacheStream != null)
      writeCache(buffer, start, length);
  }

  /**
   * Returns the hex digit for the value.
   */
  private static byte hexDigit(int value)
  {
    value &= 0xf;

    if (value <= 9)
      return (byte) ('0' + value);
    else
      return (byte) ('a' + value - 10);
  }

  protected void killCaching()
  {
    _cacheStream = null;
  }

  private void writeCache(byte []buf, int offset, int length)
    throws IOException
  {
    if (length == 0)
      return;
    
    if (_cacheMaxLength < _contentLength) {
      _cacheStream = null;
      _response.killCache();
    }
    else {
      _cacheStream.write(buf, offset, length);
    }
  }

  private String dbgId()
  {
    Object request = _response.getRequest();
    
    if (request instanceof AbstractHttpRequest) {
      AbstractHttpRequest req = (AbstractHttpRequest) request;

      return req.dbgId();
    }
    else
      return "inc ";
  }

  /**
   * Closes the stream.
   */
  public void close()
    throws IOException
  {
    finish();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _response + "]";
  }
}
