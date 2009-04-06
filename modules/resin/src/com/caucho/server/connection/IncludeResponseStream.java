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

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IncludeResponseStream extends ToByteResponseStream {
  private static final Logger log
    = Logger.getLogger(IncludeResponseStream.class.getName());
  
  static final L10N L = new L10N(IncludeResponseStream.class);

  private final AbstractHttpResponse _response;

  private boolean _isCauchoResponseStream;
  private AbstractResponseStream _nextResponseStream;
  
  private ServletResponse _next;
  private ServletOutputStream _os;
  private PrintWriter _writer;
  
  private OutputStream _cacheStream;
  private Writer _cacheWriter;
  
  public IncludeResponseStream(AbstractHttpResponse response)
  {
    _response = response;
  }

  public void init(ServletResponse next)
  {
    _next = next;

    if (_os != null || _writer != null)
      throw new IllegalStateException();

    if (_next instanceof CauchoResponse) {
      CauchoResponse response = (CauchoResponse) next;

      _isCauchoResponseStream = response.isCauchoResponseStream();

      if (_isCauchoResponseStream)
	_nextResponseStream = response.getResponseStream();
    }
    else {
      _nextResponseStream = null;
      _isCauchoResponseStream = false;
    }
  }

  public void start()
  {
    super.start();

    try {
      setEncoding(_next.getCharacterEncoding());
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Returns true for a caucho response stream.
   */
  public boolean isCauchoResponseStream()
  {
    return _isCauchoResponseStream;
  }

  /**
   * Set true for a caucho response stream.
   */
  public void setCauchoResponseStream(boolean isCaucho)
  {
    _isCauchoResponseStream = isCaucho;
  }

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
    if (_isCauchoResponseStream && _nextResponseStream == null) {
      // jsp/18ek
      super.flushCharBuffer();
      
      return;
    }
    
    try {
      if (_writer == null)
	_writer = _next.getWriter();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (_writer != null) {
      int charLength = getCharOffset();
      setCharOffset(0);
      char []buffer = getCharBuffer();

      _response.startCaching(false);

      _writer.write(buffer, 0, charLength);
    
      if (_cacheWriter != null)
	_cacheWriter.write(buffer, 0, charLength);
    }
    else
      super.flushCharBuffer();
  }

  /**
   * Sets the byte buffer offset.
   */
  public void setBufferOffset(int offset)
    throws IOException
  {
    super.setBufferOffset(offset);

    if (! _isCauchoResponseStream)
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
    
    if (_isCauchoResponseStream) {
      super.write(ch);
    }
    else {
      if (_os == null)
	_os = _next.getOutputStream();

      _os.write(ch);
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
      
    if (false && _isCauchoResponseStream) // jsp/15dv
      super.write(buf, offset, length);
    else {
      if (_os == null)
	_os = _next.getOutputStream();

      _os.write(buf, offset, length);
    }
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
      if (_response != null)
	_response.writeHeaders(null, -1);
    
      if (length == 0)
	return;
    
      if (_cacheStream != null)
	_cacheStream.write(buf, offset, length);

      if (_os == null)
	_os = _next.getOutputStream();

      _os.write(buf, offset, length);
    } catch (IOException e) {
      if (_next instanceof CauchoResponse)
	((CauchoResponse) _next).killCache();
      
      if (_response != null)
	_response.killCache();

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

    if (_os == null)
      _os = _next.getOutputStream();

    _os.flush();
  }

  /**
   * flushing
   */
  public void flushChar()
    throws IOException
  {
    flushBuffer();

    if (_writer == null)
      _writer = _next.getWriter();
    
    _writer.flush();
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
    _next = null;
    
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

    _os = null;
    _writer = null;
    _next = null;

    _cacheStream = null;
    _cacheWriter = null;
  }
}
