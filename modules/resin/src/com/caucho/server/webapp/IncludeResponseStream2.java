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

package com.caucho.server.webapp;

import com.caucho.server.connection.*;
import com.caucho.util.L10N;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class IncludeResponseStream2 extends ToByteResponseStream {
  private static final Logger log
    = Logger.getLogger(IncludeResponseStream2.class.getName());
  
  private static final L10N L = new L10N(IncludeResponseStream2.class);

  private final IncludeResponse _response;

  private AbstractResponseStream _stream;
  
  private ServletOutputStream _os;
  private PrintWriter _writer;
  
  private OutputStream _cacheStream;
  private Writer _cacheWriter;
  
  IncludeResponseStream2(IncludeResponse response)
  {
    if (response == null)
      throw new NullPointerException();
    
    _response = response;
  }

  void startRequest()
  {
    if (_os != null || _writer != null)
      throw new IllegalStateException();

    ServletResponse next = _response.getResponse();
    if (next == null)
      throw new NullPointerException();
    
    if (next instanceof CauchoResponse) {
      CauchoResponse cNext = (CauchoResponse) next;

      if (cNext.isCauchoResponseStream())
	_stream = cNext.getResponseStream();
    }

    start();
  }

  /**
   * Returns true for a caucho response stream.
   */
  public boolean isCauchoResponseStream()
  {
    return _stream != null;
  }

  /**
   * Set true for a caucho response stream.
   */
  /*
  public void setCauchoResponseStream(boolean isCaucho)
  {
    _isCauchoResponseStream = isCaucho;
  }
  */

  /**
   * Sets any cache stream.
   */
  public void setByteCacheStream(OutputStream cacheStream)
  {
    _cacheStream = cacheStream;
  }

  /**
   * Sets any cache stream.
   */
  public void setCharCacheStream(Writer cacheWriter)
  {
    _cacheWriter = cacheWriter;
  }

  /**
   * Converts the char buffer.
   */
  protected void flushCharBuffer()
    throws IOException
  {
    int charLength = getCharOffset();
    
    if (charLength == 0)
      return;
    
    if (_stream != null) {
      // jsp/18ek
      super.flushCharBuffer();
      
      return;
    }
    
    setCharOffset(0);
    char []buffer = getCharBuffer();

    // XXX: _response.startCaching(false);

    getWriter().write(buffer, 0, charLength);
    
    if (_cacheWriter != null) {
      _cacheWriter.write(buffer, 0, charLength);
    }
  }

  /**
   * Sets the byte buffer offset.
   */
  public void setBufferOffset(int offset)
    throws IOException
  {
    super.setBufferOffset(offset);

    if (_stream == null)
      flushByteBuffer();
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  public void write(int ch)
    throws IOException
  {
    flushCharBuffer();
    
    if (_stream != null) {
      super.write(ch);
    }
    else {
      getOutputStream().write(ch);
    }
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  public void write(byte []buf, int offset, int length)
    throws IOException
  {
    flushCharBuffer();
      
    // jsp/15dv
    // server/2h0m
      
    if (_cacheStream != null)
      _cacheStream.write(buf, offset, length);

    if (_cacheWriter != null) {
      // server/2h0m
      // XXX: _response.killCache();
    }

    if (_stream != null)
      super.write(buf, offset, length);
    else
      getOutputStream().write(buf, offset, length);
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  @Override
  protected void writeNext(byte []buf, int offset, int length, boolean isEnd)
    throws IOException
  {
    try {
      /* XXX:
      if (_response != null)
	_response.writeHeaders(null, -1);
      */
    
      if (length == 0)
	return;
    
      if (_cacheStream != null) {
	_cacheStream.write(buf, offset, length);
      }

      if (_stream != null)
        _stream.write(buf, offset, length);
      else
        getOutputStream().write(buf, offset, length);
    } catch (IOException e) {
      /*
      if (_next instanceof CauchoResponse)
	((CauchoResponse) _next).killCache();

      if (_response != null)
	_response.killCache();
      */

      throw e;
    }
  }

  /**
   * flushing
   */
  public void flushByte()
    throws IOException
  {
    flushBuffer();

    getOutputStream().flush();
  }

  /**
   * flushing
   */
  public void flushChar()
    throws IOException
  {
    flushBuffer();

    getWriter().flush();
  }

  private OutputStream getOutputStream()
    throws IOException
  {
    if (_os == null) {
      _os = _response.getResponse().getOutputStream();
    }

    return _os;
  }

  private Writer getWriter()
    throws IOException
  {
    if (_writer == null) {
      _writer = _response.getResponse().getWriter();
    }

    return _writer;
  }

  /**
   * Finish.
   */
  public void finish()
    throws IOException
  {
    flushBuffer();

    /*
    if (_writer != null)
      _writer.flush();
    
    if (_os != null)
      _os.flush();
    */

    _os = null;
    _writer = null;
    
    _cacheStream = null;
    _cacheWriter = null;
  }

  /**
   * Close.
   */
  public void close()
    throws IOException
  {
    super.close();

    /*
    if (_writer != null)
      _writer.close();
    
    if (_os != null)
      _os.close();
    */

    _stream = null;
    _os = null;
    _writer = null;

    _cacheStream = null;
    _cacheWriter = null;
  }
}
