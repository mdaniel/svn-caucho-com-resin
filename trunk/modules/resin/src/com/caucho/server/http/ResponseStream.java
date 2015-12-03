/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import com.caucho.server.httpcache.AbstractCacheEntry;
import com.caucho.server.httpcache.AbstractCacheFilterChain;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.ClientDisconnectException;

abstract public class ResponseStream extends ToByteResponseStream {
  private static final Logger log
    = Logger.getLogger(ResponseStream.class.getName());

  private static final L10N L = new L10N(ResponseStream.class);

  private final byte []_singleByteBuffer = new byte[1];

  private AbstractHttpResponse _response;
  private CauchoResponse _proxyCacheResponse;

  private AbstractCacheFilterChain _cacheInvocation;

  private OutputStream _cacheStream;
  private long _cacheMaxLength;

  private boolean _isDisableAutoFlush;

  // bytes actually written
  private int _contentLength;

  private boolean _isAllowFlush = true;
  private boolean _isComplete;

  public ResponseStream()
  {
  }

  protected ResponseStream(AbstractHttpResponse response)
  {
    setResponse(response);
  }

  public void setResponse(AbstractHttpResponse response)
  {
    _response = response;
  }

  protected AbstractHttpResponse getResponse()
  {
    return _response;
  }
  
  public void setProxyCacheResponse(CauchoResponse response)
  {
    _proxyCacheResponse = response;
  }
  
  public CauchoResponse getCauchoResponse()
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
    _isAllowFlush = true;
    _isDisableAutoFlush = false;
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

    AbstractHttpRequest req = _response.getRequest();
    WebApp webApp = req.getWebApp();
    _cacheMaxLength = webApp.getCacheMaxLength();
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

  @Override
  protected boolean setFlush(boolean flush)
  {
    boolean isFlush = _isAllowFlush;

    _isAllowFlush = flush;

    return isFlush;
  }

  @Override
  public void setAutoFlush(boolean isAutoFlush)
  {
    setDisableAutoFlush(! isAutoFlush);
  }

  void setDisableAutoFlush(boolean disable)
  {
    _isDisableAutoFlush = disable;
  }
  
  @Override
  protected boolean isDisableAutoFlush()
  {
    return _isDisableAutoFlush;
  }

  @Override
  public final int getContentLength()
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

  @Override
  public void setBufferSize(int size)
  {
    if (isCommitted())
      throw new IllegalStateException(L.l("Buffer size cannot be set after commit"));

    super.setBufferSize(size);
  }

  public boolean hasData()
  {
    return isCommitted() || _contentLength > 0;
  }

  @Override
  public boolean isCommitted()
  {
    if (super.isCommitted())
      return true;

    // when the data hits the content-length, the request
    // is committed even if the data isn't actually flushed.
    try {
      if (_contentLength > 0) {
        flushCharBuffer();
        int bufferOffset = getByteBufferOffset();

        // server/05e8
        if (_contentLength <= bufferOffset) {
          setCommitted();
          return true;
        }
      }
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }

    return false;
  }

  @Override
  public void clear()
    throws IOException
  {
    clearBuffer();

    if (isCommitted())
      throw new IOException(L.l("can't clear response after writing headers"));
  }

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

  @Override
  protected void writeHeaders(int length)
    throws IOException
  {
    if (isCommitted())
      return;

    // server/05ef
    if (! isCloseComplete() || isCharFlushing())
      length = -1;
    
    CauchoResponse proxyCacheResponse = _proxyCacheResponse;
    _proxyCacheResponse = null;
    
    if (proxyCacheResponse != null) {
      proxyCacheResponse.writeHeaders(length);
    }

    startCaching(true);

    _response.writeHeaders(length);

    // server/2hf3
    setCommitted();
  }

  @Override
  public final void write(int ch)
    throws IOException
  {
    _singleByteBuffer[0] = (byte) ch;

    write(_singleByteBuffer, 0, 1);
  }

  /**
   * Returns the byte buffer.
   */
  @Override
  public final byte []getBuffer()
    throws IOException
  {
    if (isCommitted()) {
      flushBuffer();

      return getNextBuffer();
    }
    else {
      return super.getBuffer();
    }
  }

  /**
   * Returns the byte offset.
   */
  @Override
  public final int getBufferOffset()
    throws IOException
  {
    if (! isCommitted()) {
      return super.getBufferOffset();
    }

    flushBuffer();

    return getNextBufferOffset();
  }

  /**
   * Sets the byte offset.
   */
  @Override
  public final void setBufferOffset(int offset)
    throws IOException
  {
    if (isClosed()) {
      // server/2630
      setNextBufferOffset(getNextStartOffset());
      return;
    }

    if (! isCommitted()) {
      super.setBufferOffset(offset);
      return;
    }

    flushBuffer();

    int startOffset = getNextStartOffset();
    if (offset == startOffset) {
      setNextBufferOffset(offset);
      return;
    }

    int oldOffset = getNextBufferOffset();
    int sublen = (offset - oldOffset);
    long lengthHeader = _response.getContentLengthHeader();

    if (lengthHeader > 0 && lengthHeader < _contentLength + sublen) {
      byte []nextBuffer = getNextBuffer();

      lengthException(nextBuffer, oldOffset, sublen, lengthHeader);

      sublen = (int) (lengthHeader - _contentLength);
      offset = oldOffset + sublen;
    }

    _contentLength += sublen;

    if (_cacheStream != null) {
      byte []nextBuffer = getNextBuffer();

      writeCache(nextBuffer, oldOffset, sublen);
    }

    if (! isHead()) {
      // server/051e
      setNextBufferOffset(offset);
    }
  }

  /**
   * Sets the next buffer
   */
  @Override
  public final byte []nextBuffer(int offset)
    throws IOException
  {
    if (! isCommitted()) {
      // server/055b
      return super.nextBuffer(offset);
    }

    if (isClosed()) {
      return getNextBuffer();
    }

    flushBuffer();

    byte []nextBuffer = getNextBuffer();
    int startOffset = getNextStartOffset();
    int oldOffset = getNextBufferOffset();

    int sublen = offset - oldOffset;
    long lengthHeader = _response.getContentLengthHeader();

    if (lengthHeader > 0 && lengthHeader < _contentLength + sublen) {
      lengthException(nextBuffer, startOffset, sublen, lengthHeader);

      sublen = (int) (lengthHeader - _contentLength);
    }

    _contentLength += sublen;
    // server/1213
    offset = oldOffset + sublen;

    if (isHead()) {
      return nextBuffer;
    }

    if (_cacheStream != null)
      writeCache(nextBuffer, oldOffset, sublen);

    return writeNextBuffer(offset);
  }

  /**
   * Writes the next chunk of data to the response stream.
   *
   * @param buf the buffer containing the data
   * @param offset start offset into the buffer
   * @param length length of the data in the buffer
   */
  @Override
  protected final void writeNext(byte []buf, int offset, int length,
                                 boolean isFinished)
    throws IOException
  {
    if (isClosed()) {
      return;
    }

    if (_isDisableAutoFlush && ! isFinished)
      throw new IOException(L.l("auto-flushing has been disabled"));

    int bufferOffset = getNextBufferOffset();
    if (length == 0 && bufferOffset == 0) {
      return;
    }

    int bufferStart = getNextStartOffset();

    // server/05e2
    if (length == 0 && bufferStart == bufferOffset) {
      // server/26a5
      // writeNextBuffer(bufferOffset);
      return;
    }

    long contentLengthHeader = _response.getContentLengthHeader();
    // Can't write beyond the content length
    if (0 < contentLengthHeader
        && contentLengthHeader < length + _contentLength) {
      if (lengthException(buf, offset, length, contentLengthHeader))
        return;

      length = (int) (contentLengthHeader - _contentLength);
    }

    if (isHead()) {
      return;
    }

    if (_cacheStream != null)
      writeCache(buf, offset, length);

    byte []buffer = getNextBuffer();
    int writeLength = length;

    while (writeLength > 0) {
      int sublen = buffer.length - bufferOffset;

      if (writeLength < sublen)
        sublen = writeLength;

      System.arraycopy(buf, offset, buffer, bufferOffset, sublen);

      writeLength -= sublen;
      offset += sublen;
      bufferOffset += sublen;
      _contentLength += sublen;

      if (writeLength > 0) {
        buffer = writeNextBuffer(bufferOffset);

        bufferStart = getNextStartOffset();
        bufferOffset = bufferStart;
      }
    }

    // server/051c
    if (bufferOffset < buffer.length)
      setNextBufferOffset(bufferOffset);
    else {
      writeNextBuffer(bufferOffset);
    }
  }

  private boolean lengthException(byte []buf, int offset, int length,
                                  long contentLengthHeader)
  {
    if (_response.isConnectionClosed() || isHead() || isClosed()) {
    }
    else if (contentLengthHeader < _contentLength) {
      AbstractHttpRequest request = _response.getRequest();
      String msg = L.l("{0}: Can't write {1} extra bytes beyond the content-length header {2}.  Check that the Content-Length header correctly matches the expected bytes, and ensure that any filter which modifies the content also suppresses the content-length (to use chunked encoding).",
                       request.getRequestURL(),
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
        AbstractHttpRequest request = _response.getRequest();
        String graph = "";

        if (Character.isLetterOrDigit((char) ch))
          graph = "'" + (char) ch + "', ";

        String msg
          = L.l("{0}: tried to write {1} bytes with content-length {2} (At {3}char={4}).  Check that the Content-Length header correctly matches the expected bytes, and ensure that any filter which modifies the content also suppresses the content-length (to use chunked encoding).",
                request.getRequestURL(),
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
  public final void flush()
    throws IOException
  {
    _isDisableAutoFlush = false;

    if (_isAllowFlush && ! isClosed()) {
      flushBuffer();

      int bufferOffset = getNextBufferOffset();

      if (bufferOffset > 0) {
        int bufferStart = getNextStartOffset();

        if (bufferStart != bufferOffset) {
          // server/10c9
          // _contentLength += (bufferOffset - bufferStart);

          writeNextBuffer(bufferOffset);
        }
      }

      flushNext();
    }
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
    try {
      closeBuffer();

      writeTail(true);

      closeCache();

      closeNext();
    } catch (ClientDisconnectException e) {
      _response.clientDisconnect();

      if (! _response.isIgnoreClientDisconnect()) {
        throw e;
      }
    }
  }
  
  private void closeBuffer()
    throws IOException
  {
    _isDisableAutoFlush = false;

    flushCharBuffer();

    _isAllowFlush = true;

    flushBuffer();
    // flushBuffer can force 304 and then a cache write which would
    // complete the finish.
    /*
  if (isClosed()) {
    return;
  }
     */

    // XXX: this needs to be cleaned up with the above
    // use of writeHeaders
    if (! _response.isHeaderWritten()) {
      writeHeaders(-1);
    }
  }

  //
  // proxy caching
  //

  /**
   * Called to start caching.
   */
  protected void startCaching(boolean isByte)
  {
    // server/1373 for getBufferSize()
    HttpServletResponseImpl res = _response.getRequest().getResponseFacade();

    if (res == null
        || res.getStatus() != HttpServletResponse.SC_OK
        || res.isDisableCache()) {
      return;
    }

    // server/13de
    if (_cacheInvocation != null)
      return;

    AbstractCacheFilterChain cacheInvocation = res.getCacheInvocation();

    if (cacheInvocation == null)
      return;

    _cacheInvocation = cacheInvocation;

    HttpServletRequestImpl req = _response.getRequest().getRequestFacade();

    ArrayList<String> keys = res.getHeaderKeys();
    ArrayList<String> values = res.getHeaderValues();
    String contentType = res.getContentTypeImpl();
    String charEncoding = res.getCharacterEncodingImpl();

    int contentLength = -1;

    AbstractCacheEntry newCacheEntry
      = cacheInvocation.startCaching(req, res,
                                     keys, values,
                                     contentType,
                                     charEncoding,
                                     contentLength);

    if (newCacheEntry == null) {
    }
    else if (isByte) {
      setByteCacheStream(newCacheEntry.openOutputStream());
    }
    else {
      setCharCacheStream(newCacheEntry.openWriter());
    }
  }

  private void writeCache(byte []buf, int offset, int length)
    throws IOException
  {
    if (length == 0)
      return;

    if (_cacheMaxLength < _contentLength) {
      //server/2h0o
      killCaching();
      /*
      _cacheStream = null;
      _response.killCache();
      */
    }
    else {
      _cacheStream.write(buf, offset, length);
    }
  }

  @Override
  public void killCaching()
  {
    AbstractCacheFilterChain cacheInvocation = _cacheInvocation;
    
    if (cacheInvocation != null) {
      HttpServletResponseImpl res = _response.getRequest().getResponseFacade();
      
      cacheInvocation.killCaching(res);
      setByteCacheStream(null);
      setCharCacheStream(null);
    }
  }

  @Override
  public void completeCache()
  {
    HttpServletResponseImpl res = _response.getRequest().getResponseFacade();
    HttpServletRequestImpl req = _response.getRequest().getRequestFacade();
    
    if (req == null) {
      return;
    }
    
    // server/1la7
    if (req.isAsyncStarted()) {
      return;
    }

    try {
      _isComplete = true;
      closeBuffer();
      
      if (! isNextValid()) {
        killCaching();
      }
      
      OutputStream cacheStream = getByteCacheStream();
      setByteCacheStream(null);

      Writer cacheWriter = getCharCacheStream();
      setCharCacheStream(null);

      if (cacheStream != null)
        cacheStream.close();

      if (cacheWriter != null)
        cacheWriter.close();
      
      AbstractCacheFilterChain cache = _cacheInvocation;

      if (cache != null && res != null) {
        _cacheInvocation = null;

        WebApp webApp = res.getRequest().getWebApp();
        if (webApp != null && webApp.isActive()) {
          cache.finishCaching(res);
        }
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      AbstractCacheFilterChain cache = _cacheInvocation;
      _cacheInvocation = null;

      if (cache != null)
        cache.killCaching(res);
    }
  }

  private void closeCache()
  {
    AbstractCacheFilterChain cache = _cacheInvocation;
    _cacheInvocation = null;

    HttpServletResponseImpl res = _response.getRequest().getResponseFacade();
    
    try {
      OutputStream cacheStream = getByteCacheStream();
      setByteCacheStream(null);

      Writer cacheWriter = getCharCacheStream();
      setCharCacheStream(null);

      if (cacheStream != null)
        cacheStream.close();

      if (cacheWriter != null)
        cacheWriter.close();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      if (cache != null)
        cache.killCaching(res);
    }
  }

  //
  // implementations
  //

  protected final boolean isNextValid()
  {
    return ! _response.isConnectionClosed();
  }
  
  protected void clearNext()
  {
  }

  abstract protected byte []getNextBuffer();

  protected int getNextStartOffset()
  {
    return 0;
  }

  abstract protected int getNextBufferOffset()
    throws IOException;

  protected final void setNextBufferOffset(int offset)
    throws IOException
  {
    boolean isValid = false; 
    try {
      setNextBufferOffsetImpl(offset);
      
      isValid = true;
    } catch (ClientDisconnectException e) {
      if (! _response.isIgnoreClientDisconnect())
        throw e;
    } finally {
      if (! isValid)
        _response.clientDisconnect();
    }
  }

  abstract protected void setNextBufferOffsetImpl(int offset)
    throws IOException;

  protected final byte []writeNextBuffer(int offset)
    throws IOException
  {
    boolean isValid = false; 
    try {
      byte []buffer = writeNextBufferImpl(offset);
      
      isValid = true;
      
      return buffer;
    } catch (ClientDisconnectException e) {
      if (! _response.isIgnoreClientDisconnect())
        throw e;
      else {
        log.log(Level.FINER, e.toString(), e);
      }
      
      return getNextBuffer();
    } finally {
      if (! isValid)
        _response.clientDisconnect();
    }
  }

  abstract protected byte []writeNextBufferImpl(int offset)
    throws IOException;

  @Override
  public final void flushNext()
    throws IOException
  {
    boolean isValid = false; 
    try {
      flushNextImpl();
      
      isValid = true;
    } catch (ClientDisconnectException e) {
      if (! _response.isIgnoreClientDisconnect())
        throw e;
    } finally {
      if (! isValid)
        _response.clientDisconnect();
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
        _response.clientDisconnect();
      }
    }
  }

  abstract protected void closeNextImpl()
    throws IOException;

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

  protected String dbgId()
  {
    Object request = _response.getRequest();

    if (request instanceof AbstractHttpRequest) {
      AbstractHttpRequest req = (AbstractHttpRequest) request;

      return req.dbgId();
    }
    else
      return "inc ";
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _response + "]";
  }
}
