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

package com.caucho.server.http;

import com.caucho.server.connection.*;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.logging.*;

public class HttpResponseStream extends ResponseStream {
  private static final Logger log
    = Logger.getLogger(HttpResponseStream.class.getName());
  
  private static final L10N L = new L10N(HttpResponseStream.class);

  private static final int _tailChunkedLength = 7;
  private static final byte []_tailChunked
    = new byte[] {'\r', '\n', '0', '\r', '\n', '\r', '\n'};
  
  private HttpResponse _response;
  
  private boolean _isChunkedEncoding;
  private int _bufferStartOffset;

  private byte []_singleByteBuffer = new byte[1];
  // True for the first chunk
  private boolean _isFirst;
  private WriteStream _next;
  
  private final byte []_buffer = new byte[16];

  HttpResponseStream(HttpResponse response, WriteStream next)
  {
    super(response);

    _response = response;
    _next = next;
  }
  
  /**
   * initializes the Response stream at the beginning of a request.
   */
  public void start()
  {
    super.start();

    _isChunkedEncoding = false;
    _bufferStartOffset = 0;
  }

  @Override
  protected void writeHeaders(int length)
    throws IOException
  {
    super.writeHeaders(length);

    _isChunkedEncoding = _response.isChunkedEncoding();
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

    if (! _isChunkedEncoding) {
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
  public byte []nextBuffer2(int offset)
    throws IOException
  {
    if (! _isCommitted) {
      // server/055b
      return super.nextBuffer(offset);
      // _bufferStartOffset = _next.getBufferOffset();
    }
    
    flushBuffer();
      
    int startOffset = _bufferStartOffset;
    _bufferStartOffset = 0;

    int length = offset - startOffset;
    long lengthHeader = _response.getContentLengthHeader();

    try {
      if (_isChunkedEncoding) {
	if (length == 0)
	  throw new IllegalStateException();
      
	byte []buffer = _next.getBuffer();

	writeChunkHeader(buffer, startOffset, length);

	buffer = _next.nextBuffer(offset);
	      
	if (log.isLoggable(Level.FINE))
	  log.fine(dbgId() + "write-chunk1(" + offset + ")");

	_bufferStartOffset = 8 + _next.getBufferOffset();
	_next.setBufferOffset(_bufferStartOffset);

	return buffer;
      }
      else {
	byte []buffer = _next.nextBuffer(offset);
	_bufferStartOffset = _next.getBufferOffset();
	      
	if (log.isLoggable(Level.FINE))
	  log.fine(dbgId() + "write-chunk2(" + offset + ")");

	return buffer;
      }
    } catch (ClientDisconnectException e) {
      _response.clientDisconnect();
      // XXX: _response.killCache();

      if (_response.isIgnoreClientDisconnect()) {
	return _next.getBuffer();
      }
      else
        throw e;
    } catch (IOException e) {
      // XXX: _response.killCache();
      
      throw e;
    }
  }

  /**
   * Sets the byte offset.
   */
  public void setBufferOffset(int offset)
    throws IOException
  {
    if (! _isCommitted) {
      super.setBufferOffset(offset);
      return;
    }

    flushBuffer();
    
    int startOffset = _bufferStartOffset;
    if (offset == startOffset)
      return;
    
    int length = 666;

    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() +  "write-chunk3(" + length + ")");

    _next.setBufferOffset(offset);
  }

  //
  // implementations
  //

  @Override
  protected byte []getNextBuffer()
  {
    return _next.getBuffer();
  }

  @Override
  protected int getNextStartOffset()
  {
    return _bufferStartOffset;
  }
  
  @Override
  protected void setNextBufferOffset(int offset)
  {
    _next.setBufferOffset(offset);
  }
      
  @Override
  protected int getNextBufferOffset()
    throws IOException
  {
    return _next.getBufferOffset();
  }

  @Override
  protected byte []writeNextBuffer(int offset)
    throws IOException
  {
    if (log.isLoggable(Level.FINE))
      log.fine(dbgId() + "write-chunk2(" + offset + ")");
    
    return _next.nextBuffer(offset);
  }

  protected void writeTail(int bufferStart)
    throws IOException
  {
    if (_isChunkedEncoding) {
      int bufferOffset = _next.getBufferOffset();

      if (bufferStart > 0 && bufferOffset != bufferStart) {
	byte []buffer = _next.getBuffer();

	writeChunkHeader(buffer, bufferStart, bufferOffset - bufferStart);
      }
      else {
	// server/05b3
	_next.setBufferOffset(0);
      }

      _isCommitted = true;
	
      ArrayList<String> footerKeys = _response.getFooterKeys();

      if (footerKeys.size() == 0)
	_next.write(_tailChunked, 0, _tailChunkedLength);
      else {
	ArrayList<String> footerValues = _response.getFooterValues();
	  
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
	log.fine(dbgId() + "write-chunk6(" + _tailChunkedLength + ")");
    }
  }

  /**
   * Fills the chunk header.
   */
  private void writeChunkHeader(byte []buffer, int start, int length)
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
}
