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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.cache.EntryHttpCacheBase;
import com.caucho.v5.http.cache.FilterChainHttpCacheBase;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.ClientDisconnectException;

abstract public class OutResponseCache extends OutResponseToByte
{
  private static final Logger log
    = Logger.getLogger(OutResponseCache.class.getName());

  private static final L10N L = new L10N(OutResponseCache.class);

  private RequestHttpBase _request;
  private ResponseCaucho _proxyCacheResponse;

  private FilterChainHttpCacheBase _cacheInvocation;

  private OutputStream _cacheStream;
  private long _cacheMaxLength;

  // bytes actually written
  private long _contentLength;

  private boolean _isComplete;

  public OutResponseCache()
  {
  }

  protected OutResponseCache(RequestHttpBase response)
  {
    setResponse(response);
  }

  public void setResponse(RequestHttpBase response)
  {
    _request = response;
  }

  protected RequestHttpBase getResponse()
  {
    return _request;
  }

  protected RequestHttpBase getRequest()
  {
    return _request;
  }
  
  public void setProxyCacheResponse(ResponseCaucho response)
  {
    _proxyCacheResponse = response;
  }
  
  public ResponseCaucho getCauchoResponse()
  {
    return _proxyCacheResponse;
  }
  /**
   * initializes the Response stream at the beginning of a request.
   */
  @Override
  public void start()
  {
    super.start();

    _contentLength = 0;
    //_isAllowFlush = true;
    _cacheStream = null;
    _proxyCacheResponse = null;
    _isComplete = false;
  }

  /**
   * Returns true for a Caucho response stream.
   */
  @Override
  public boolean isCauchoResponseStream()
  {
    return true;
  }

  /**
   * Sets the underlying cache stream for a cached request.
   *
   * @param cacheStream the cache stream.
   */
  @Override
  public void setByteCacheStream(OutputStream cacheStream)
  {
    _cacheStream = cacheStream;

    if (cacheStream == null)
      return;

    RequestHttpBase req = getRequest();
    
    HttpContainerServlet http = (HttpContainerServlet) req.getHttp();
    
    _cacheMaxLength = http.getCacheMaxLength();
  }

  @Override
  protected OutputStream getByteCacheStream()
  {
    return _cacheStream;
  }

  /**
   * Response stream is a writable stream.
   */
  public boolean canWrite()
  {
    return true;
  }

  /*
  @Override
  public final long getContentLength()
  {
    // server/05e8
    try {
      flushCharBuffer();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (isCommitted())
      return _contentLength;
    else
      return super.getContentLength();
  }
  */

  /*
  @Override
  public void setBufferSize(int size)
  {
    if (isCommitted())
      throw new IllegalStateException(L.l("Buffer size cannot be set after commit"));

    super.setBufferSize(size);
  }
  */

  public boolean hasData()
  {
    return isCommitted() || _contentLength > 0;
  }

  /*
  @Override
  public boolean isCommitted()
  {
    if (super.isCommitted()) {
      return true;
    }

    // when the data hits the content-length, the request
    // is committed even if the data isn't actually flushed.
    try {
      if (_contentLength > 0) {
        flushCharBuffer();
        int bufferOffset = getByteBufferOffset();

        // server/05e8
        if (_contentLength <= bufferOffset) {
          toCommitted();
          return true;
        }
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }

    return false;
  }
  */

  /*
  @Override
  public void clear()
    throws IOException
  {
    clearBuffer();

    if (isCommitted())
      throw new IOException(L.l("can't clear response after writing headers"));
  }
  */

  /*
  @Override
  public void clearBuffer()
  {
    super.clearBuffer();

    if (! isCommitted()) {
      // jsp/15la
      _response.setHeaderWritten(false);
    }

    clearNext();
  }
  */

  @Override
  public boolean isCloseComplete()
  {
    return super.isCloseComplete() || _isComplete;
  }
  /**
   * Clear the closed state, because of the NOT_MODIFIED
   */
  public void clearClosed()
  {
    // _isClosed = false;
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  // @Override
  protected final void flushDataBuffer(byte []buf, int offset, int length,
                                       boolean isEnd)
    throws IOException
  {
    /*
    if (isClosed()) {
      return;
    }
    */

    if (! isAutoFlush() && ! isEnd)
      throw new IOException(L.l("auto-flushing has been disabled"));
    
    writeHeaders(isEnd ? length : -1);

    if (length == 0 && ! isEnd) {
      return;
    }

    long contentLengthHeader = _request.contentLengthOut();
    // Can't write beyond the content length
    if (0 < contentLengthHeader
        && contentLengthHeader < length + _contentLength) {
      if (lengthException(buf, offset, length, contentLengthHeader))
        return;

      length = (int) (contentLengthHeader - _contentLength);
    }

    if (isHead()) {
      if (isEnd) {
        writeNext(buf, 0, 0, true);
      }
      
      return;
    }

    writeCache(buf, offset, length);
    
    writeNext(buf, offset, length, isEnd);
  }

  //@Override
  protected void writeHeaders(int length)
    throws IOException
  {
    if (isCommitted()) {
      return;
    }

    // server/05ef
    if (! isCloseComplete() || isCharFlushing()) {
      length = -1;
    }
    
    ResponseCaucho proxyCacheResponse = _proxyCacheResponse;
    _proxyCacheResponse = null;
    
    if (proxyCacheResponse != null) {
      proxyCacheResponse.writeHeaders(length);
    }

    startCaching();

    _request.writeHeaders(length);

    // server/2hf3
    toCommitted();
  }

  private boolean lengthException(byte []buf, int offset, int length,
                                  long contentLengthHeader)
  {
    if (_request.isConnectionClosed() || isHead() || isClosed()) {
    }
    else if (contentLengthHeader < _contentLength) {
      RequestHttpBase request = getRequest();
      String msg = L.l("{0}: Can't write {1} extra bytes beyond the content-length header {2}.  Check that the Content-Length header correctly matches the expected bytes, and ensure that any filter which modifies the content also suppresses the content-length (to use chunked encoding).",
                       "uri", // XXX: request.getRequestURI(),
                       "" + (length + _contentLength),
                       "" + contentLengthHeader);

      log.fine(msg);

      return false;
    }

    for (int i = (int) (offset + contentLengthHeader - _contentLength);
         i < offset + length;
         i++) {
      int ch = buf[i];

      if (ch != '\r' && ch != '\n' && ch != ' ' && ch != '\t') {
        RequestHttpBase request = getRequest();
        String graph = "";

        if (Character.isLetterOrDigit((char) ch))
          graph = "'" + (char) ch + "', ";

        String msg
          = L.l("{0}: tried to write {1} bytes with content-length {2} (At {3}char={4}).  Check that the Content-Length header correctly matches the expected bytes, and ensure that any filter which modifies the content also suppresses the content-length (to use chunked encoding).",
                "uri", // XXX: request.get.uri(),
                "" + (length + _contentLength),
                "" + contentLengthHeader,
                graph,
                "" + ch);

        log.fine(msg);
        break;
      }
    }

    length = (int) (contentLengthHeader - _contentLength);
    return (length <= 0);
  }

  /**
   * Flushes the buffered response to the output stream.
   */
  @Override
  public void flushByte()
    throws IOException
  {
    flush();
  }

  /**
   * Flushes the buffered response to the writer.
   */
  @Override
  public void flushChar()
    throws IOException
  {
    flush();
  }

  /**
   * Complete the request.
   */
  @Override
  protected void closeImpl()
    throws IOException
  {
    setAutoFlush(true);

    flushCharBuffer();

    flushByteBuffer(true);

    //writeTail(true);

    // closeCache();

    //closeNext();
  }

  //
  // proxy caching
  //

  /**
   * Called to start caching.
   */
  protected void startCaching()
  {
    /*
    // server/1373 for getBufferSize()
    ResponseFacade resFacade = null;//getRequest().getResponseFacade();

    if (resFacade == null
        || resFacade.getStatus() != HttpServletResponse.SC_OK
        || resFacade.isDisableCache()) {
      return;
    }
    
    ResponseCache res = (ResponseCache) resFacade; 

    // server/13de
    if (_cacheInvocation != null)
      return;

    FilterChainHttpCacheBase cacheInvocation = res.getCacheInvocation();

    if (cacheInvocation == null)
      return;

    _cacheInvocation = cacheInvocation;

    RequestCache req = (RequestCache) _request.request();

    ArrayList<String> keys = res.getHeaderKeys();
    ArrayList<String> values = res.getHeaderValues();
    String contentType = res.getContentTypeImpl();
    String charEncoding = res.getCharacterEncodingImpl();

    int contentLength = -1;

    EntryHttpCacheBase newCacheEntry
      = cacheInvocation.startCaching(req, res,
                                     keys, values,
                                     contentType,
                                     charEncoding,
                                     contentLength);

    if (newCacheEntry != null) {
      setByteCacheStream(newCacheEntry.openOutputStream());
    }
    */
  }

  private void writeCache(byte []buf, int offset, int length)
    throws IOException
  {
    OutputStream cacheStream = _cacheStream;
    
    if (cacheStream == null) {
      return;
    }
    
    if (length == 0) {
      return;
    }

    if (_cacheMaxLength < _contentLength) {
      //server/2h0o
      killCaching();
      /*
      _cacheStream = null;
      _response.killCache();
      */
    }
    else {
      cacheStream.write(buf, offset, length);
    }
  }

  @Override
  public void killCaching()
  {
    FilterChainHttpCacheBase cacheInvocation = _cacheInvocation;
    
    if (cacheInvocation != null) {
      ResponseCache res = (ResponseCache) null;//_request.getRequest().getResponseFacade();
      
      cacheInvocation.killCaching(res);
      setByteCacheStream(null);
    }
  }

  @Override
  public void completeCache()
  {
    RequestCache req = (RequestCache) null;//_request.getRequest().getRequestFacade();
    ResponseCache res = (ResponseCache) null;//_request.getRequest().getResponseFacade();
    
    if (req == null) {
      return;
    }
    
    // server/1la7
    if (req.isAsyncStarted()) {
      return;
    }

    try {
      _isComplete = true;
      
      // flushBuffer();
      // closeBuffer();
      close();
      
      OutputStream cacheStream = getByteCacheStream();
      setByteCacheStream(null);

      if (cacheStream != null) {
        cacheStream.close();
      }
      
      FilterChainHttpCacheBase cache = _cacheInvocation;

      if (cache != null && res != null) {
        _cacheInvocation = null;

        cache.finishCaching(res);
        /*
        WebApp webApp = res.getRequest().getWebApp();
        if (webApp != null && webApp.isActive()) {
          cache.finishCaching(res);
        }
        */
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      FilterChainHttpCacheBase cache = _cacheInvocation;
      _cacheInvocation = null;

      if (cache != null) {
        cache.killCaching(res);
      }
    }
  }

  private void closeCache()
  {
    FilterChainHttpCacheBase cache = _cacheInvocation;
    _cacheInvocation = null;
    
    if (cache == null) {
      return;
    }

    ResponseCache res = (ResponseCache) null;//_request.getRequest().getResponseFacade();
    
    try {
      // flushBuffer();
      
      OutputStream cacheStream = getByteCacheStream();
      setByteCacheStream(null);

      if (cacheStream != null) {
        cacheStream.close();
        cache = null;
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      if (cache != null) {
        cache.killCaching(res);
      }
    }
  }

  //
  // implementations
  //

  protected void writeNext(byte []buffer, int offset, int length,
                           boolean isEnd)
    throws IOException
  {
    
  }

  @Override
  public final void flushNext()
    throws IOException
  {
    boolean isValid = false; 
    try {
      flushNextImpl();
      
      isValid = true;
    } catch (ClientDisconnectException e) {
      if (! _request.isIgnoreClientDisconnect())
        throw e;
    } finally {
      if (! isValid)
        _request.clientDisconnect();
    }
  }

  protected abstract void flushNextImpl()
    throws IOException;

  protected final void closeNext()
    throws IOException
  {
    boolean isValid = false; 
    try {
      closeNextImpl();
      
      isValid = true;
    } finally {
      if (! isValid) {
        _request.clientDisconnect();
      }
    }
  }

  abstract protected void closeNextImpl()
    throws IOException;

  /*
  protected final void writeTail(boolean isComplete)
    throws IOException
  {
    boolean isValid = false; 
    try {
      writeTailImpl(isComplete);
      
      isValid = true;
    } finally {
      if (! isValid)
        _response.clientDisconnect();
    }
  }

  protected void writeTailImpl(boolean isClosed)
    throws IOException
  {
  }
  */

  protected String dbgId()
  {
    Object request = _request;//_request.getRequest();

    if (request instanceof RequestHttpBase) {
      RequestHttpBase req = (RequestHttpBase) request;

      return req.dbgId();
    }
    else
      return "inc ";
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _request + "]";
  }
}
