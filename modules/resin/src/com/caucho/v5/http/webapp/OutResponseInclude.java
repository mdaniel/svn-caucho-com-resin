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

package com.caucho.v5.http.webapp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;

import com.caucho.v5.http.protocol.OutResponseBase2;
import com.caucho.v5.http.protocol.OutResponseToByte;
import com.caucho.v5.http.protocol.ResponseCaucho;
import com.caucho.v5.io.TempBuffer;
import com.caucho.v5.vfs.IOExceptionRuntime;

public class OutResponseInclude extends OutResponseToByte {
  private static final Logger log
    = Logger.getLogger(OutResponseInclude.class.getName());

  private final ResponseInclude _response;

  private OutResponseBase2 _stream;
  
  private ServletOutputStream _os;
  private PrintWriter _writer;
  
  private boolean _isCommitted;

  private ArrayList<String> _headerKeys = new ArrayList<String>(); 
  private ArrayList<String> _headerValues = new ArrayList<String>(); 
  
  OutResponseInclude(ResponseInclude response)
  {
    if (response == null)
      throw new NullPointerException();
    
    _response = response;
  }

  @Override
  public void start()
  {
    if (_os != null || _writer != null) {
      throw new IllegalStateException();
    }

    ServletResponse next = _response.getResponse();
    Objects.requireNonNull(next);
    
    if (next instanceof ResponseCaucho) {
      ResponseCaucho cNext = (ResponseCaucho) next;

      if (cNext.isCauchoResponseStream()) {
        _stream = cNext.getResponseStream();
      }
    }

    _isCommitted = false;
    _headerKeys.clear();
    _headerValues.clear();

    super.start();

    // server/053n
    try {
      setEncoding(next.getCharacterEncoding());
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
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

  void addHeader(String key, String value)
  {
    _headerKeys.add(key);
    _headerValues.add(value);
  }

  List<String> getHeaderKeys() {
    return _headerKeys;
  }

  List<String> getHeaderValues() {
    return _headerValues;
  }
    
  /**
   * Sets any cache stream.
   */
  @Override
  public void setByteCacheStream(OutputStream cacheStream)
  {
    Thread.dumpStack();
  }

  /**
   * Converts the char buffer.
   */
  @Override
  protected void flushCharBuffer()
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

    try {
      getWriter().write(buffer, 0, charLength);
    } catch (IOException e) {
      throw new IOExceptionRuntime(e);
    }
  }

  /**
   * Sets the byte buffer offset.
   */
  public void setBufferOffset(int offset)
    throws IOException
  {
    super.setBufferOffset(offset);

    if (_stream == null) {
      flushByteBuffer(false);
    }
  }

  /**
   * Sets the byte buffer offset.
   */
  @Override
  public byte []nextBuffer(int offset)
    throws IOException
  {
    super.nextBuffer(offset);

    if (_stream == null)
      flushByteBuffer();

    return getBuffer();
  }

  /**
   * Writes a byte
   * @param ch byte to write
   */
  @Override
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
  @Override
  public void write(byte []buf, int offset, int length)
  {
    flushCharBuffer();
      
    // jsp/15dv
    // server/2h0m

    // server/2h0m
    // XXX: _response.killCache();

    if (_stream != null) {
      super.write(buf, offset, length);
    }
    else {
      try {
        getOutputStream().write(buf, offset, length);
      } catch (IOException e) {
        throw new IOExceptionRuntime(e);
      }
    }
  }

  /*
  @Override
  protected void writeHeaders(int length)
  {
    startCaching(true);
  }
  */

  @Override
  protected TempBuffer flushData(TempBuffer head, TempBuffer tail, boolean isEnd)
  {
    for (TempBuffer ptr = head; ptr != null; ptr = ptr.getNext()) {
      boolean ptrEnd = isEnd && ptr.getNext() == null;
      
      flushDataBuffer(ptr.buffer(), 0, ptr.length(), ptrEnd);
    }
    
    TempBuffer next = head.getNext();
    
    if (next != null) {
      head.setNext(null);
      TempBuffer.freeAll(next);;
    }
    
    return head;
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  // @Override
  protected void flushDataBuffer(byte []buf, int offset, int length, boolean isEnd)
  {
    try {
      /* XXX:
      if (_response != null)
        _response.writeHeaders(null, -1);
      */

      startCaching();
      
      if (length == 0)
        return;

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

      throw new IOExceptionRuntime(e);
    }
  }

  protected void startCaching()
  {
  }

  /**
   * flushing
   */
  @Override
  public void flushByte()
    throws IOException
  {
    flushBuffer();

    getOutputStream().flush();
  }

  /**
   * flushing
   */
  @Override
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

  /*
  public void killCaching()
  {
    AbstractCacheEntry cacheEntry = _newCacheEntry;
    _newCacheEntry = null;

    if (cacheEntry != null) {
      _cacheInvocation.killCaching(cacheEntry);
      setByteCacheStream(null);
      setCharCacheStream(null);
    }
  }
  */

  /**
   * Finish.
   */
  @Override
  protected void closeImpl()
    throws IOException
  {
    super.closeImpl();
    
    flushBuffer();

    /*
    if (_writer != null)
      _writer.flush();
    
    if (_os != null)
      _os.flush();
    */

    _stream = null;
    _os = null;
    _writer = null;
  }

  @Override
  public void completeCache()
  {
    /*

    FilterChainHttpCacheBase cache = _response.getCacheInvocation();
      
    try {
      flushBuffer();

      if (cache != null)
        cache.finishCaching(_response);
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      // _response.setCacheInvocation(null);

      if (cache != null)
        cache.killCaching(_response);
    }
    */
  }
}
