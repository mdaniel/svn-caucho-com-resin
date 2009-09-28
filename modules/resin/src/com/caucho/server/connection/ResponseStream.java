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

import com.caucho.server.cache.AbstractCacheEntry;
import com.caucho.server.cache.AbstractCacheFilterChain;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.WriteStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract public class ResponseStream extends ToByteResponseStream {
  private static final Logger log
    = Logger.getLogger(ResponseStream.class.getName());

  private static final L10N L = new L10N(ResponseStream.class);

  private final byte []_buffer = new byte[16];
  private final byte []_singleByteBuffer = new byte[1];

  private AbstractHttpResponse _response;

  private AbstractCacheFilterChain _cacheInvocation;
  private AbstractCacheEntry _newCacheEntry;
  private OutputStream _cacheStream;
  private long _cacheMaxLength;

  private int _bufferSize;
  private boolean _disableAutoFlush;

  // bytes actually written
  private int _contentLength;
  // True for the first chunk
  private boolean _isHeaderWritten;

  private boolean _allowFlush = true;
  private boolean _isHead = false;
  private boolean _isClosed = false;

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

  /**
   * initializes the Response stream at the beginning of a request.
   */
  public void start()
  {
    super.start();

    _contentLength = 0;
    _allowFlush = true;
    _disableAutoFlush = false;
    _isClosed = false;
    _isHead = false;
    _cacheStream = null;
    _isHeaderWritten = false;
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

    if (cacheStream == null)
      return;

    AbstractHttpRequest req = _response.getRequest();
    WebApp app = req.getWebApp();
    _cacheMaxLength = app.getCacheMaxLength();
  }

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
      _isHeaderWritten = false;
      _response.setHeaderWritten(false);
    }

    clearNext();
  }

  /**
   * Clear the closed state, because of the NOT_MODIFIED
   */
  public void clearClosed()
  {
    // _isClosed = false;
  }

  protected void writeHeaders(int length)
    throws IOException
  {
    if (_isCommitted)
      return;

    _isCommitted = true;

    startCaching(true);

    _response.writeHeaders(length);
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
  /* hessian/3544
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
  */

  /**
   * Returns the byte buffer.
   */
  @Override
  public byte []getBuffer()
    throws IOException
  {
    if (_isCommitted) {
      flushBuffer();

      return getNextBuffer();
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

    return getNextBufferOffset();
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

    int startOffset = getNextStartOffset();
    if (offset == startOffset)
      return;

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

    if (! _isHead) {
      // server/051e
      setNextBufferOffset(offset);
    }
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
    }

    if (_isClosed)
      return getNextBuffer();

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

    try {
      if (_isHead) {
        return nextBuffer;
      }

      if (_cacheStream != null)
        writeCache(nextBuffer, oldOffset, offset - oldOffset);

      return writeNextBuffer(offset);
    } catch (ClientDisconnectException e) {
      _response.clientDisconnect();
      // XXX: _response.killCache();

      if (_response.isIgnoreClientDisconnect()) {
        return nextBuffer;
      }
      else
        throw e;
    } catch (IOException e) {
      // XXX: _response.killCache();

      throw e;
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

      boolean isHeaderWritten = _isHeaderWritten;
      _isHeaderWritten = true;

      if (! isHeaderWritten) {
        if (isFinished)
          writeHeaders(getBufferLength());
        else
          writeHeaders(-1);
      }

      int bufferStart = getNextStartOffset();
      int bufferOffset = getNextBufferOffset();

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

      if (_isHead) {
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
    } catch (ClientDisconnectException e) {
      // server/183c
      // XXX: _response.killCache();
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
      AbstractHttpRequest request = _response.getRequest();
      ServletContext app = request.getWebApp();

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
        ServletContext app = request.getWebApp();
        String graph = "";

        if (Character.isLetterOrDigit((char) ch))
          graph = "'" + (char) ch + "', ";

        String msg =
          L.l("{0}: tried to write {1} bytes with content-length {2} (At {3}char={4}).  Check that the Content-Length header correctly matches the expected bytes, and ensure that any filter which modifies the content also suppresses the content-length (to use chunked encoding).",
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
  public void flush()
    throws IOException
  {
    try {
      _disableAutoFlush = false;

      if (_allowFlush && ! _isClosed) {
        flushBuffer();

        int bufferStart = getNextStartOffset();
        int bufferOffset = getNextBufferOffset();

        if (bufferStart != bufferOffset) {
          _contentLength += (bufferOffset - bufferStart);

          writeNextBuffer(bufferOffset);
        }

        flushNext();
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

    if (isClosed)
      return;

    _disableAutoFlush = false;

    flushCharBuffer();

    _isFinished = true;
    _allowFlush = true;

    flushBuffer();

    // flushBuffer can force 304 and then a cache write which would
    // complete the finish.
    if (isClosed) {
      return;
    }

    _isClosed = true;

    try {
      writeTail();

      AbstractHttpRequest req = _response.getRequest();
      if (req.isComet() || req.isDuplex()) {
      }
      else if (! req.allowKeepalive()) {
        _isClosed = true;

        if (log.isLoggable(Level.FINE)) {
          log.fine(dbgId() + "close stream");
        }

        closeNext();
      }
      else {
        _isClosed = true;

        if (log.isLoggable(Level.FINE)) {
          log.fine(dbgId() + "finish/keepalive");
        }
      }
    } catch (ClientDisconnectException e) {
      _response.clientDisconnect();

      if (! _response.isIgnoreClientDisconnect()) {
        throw e;
      }
    }
  }

  //
  // implementations
  //

  abstract protected byte []getNextBuffer();

  protected int getNextStartOffset()
  {
    return 0;
  }

  abstract protected void setNextBufferOffset(int offset)
    throws IOException;

  abstract protected int getNextBufferOffset()
    throws IOException;

  abstract protected byte []writeNextBuffer(int offset)
    throws IOException;

  abstract protected void flushNext()
    throws IOException;

  abstract protected void closeNext()
    throws IOException;

  protected void clearNext()
  {
  }

  protected void writeTail()
    throws IOException
  {
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
      _newCacheEntry = newCacheEntry;

      setByteCacheStream(newCacheEntry.openOutputStream());
    }
    else {
      _newCacheEntry = newCacheEntry;

      setCharCacheStream(newCacheEntry.openWriter());
    }
  }

  private void writeCache(byte []buf, int offset, int length)
    throws IOException
  {
    if (length == 0)
      return;

    if (_cacheMaxLength < _contentLength) {
      _cacheStream = null;
      // XXX: _response.killCache();
    }
    else {
      _cacheStream.write(buf, offset, length);
    }
  }

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

  /**
   * Closes the stream.
   */
  public void close()
    throws IOException
  {
    if (_isClosed)
      return;

    finish();
    finishCache();

    _isClosed = true;
  }

  public void finishCache()
    throws IOException
  {
    try {
      OutputStream cacheStream = getByteCacheStream();
      setByteCacheStream(null);

      Writer cacheWriter = getCharCacheStream();
      setCharCacheStream(null);

      if (cacheStream != null)
        cacheStream.close();

      if (cacheWriter != null)
        cacheWriter.close();

      if (_newCacheEntry != null) {
        HttpServletRequestImpl request
          = _response.getRequest().getRequestFacade();

        if (request == null)
          return;

        WebApp webApp = request.getWebApp();
        if (webApp != null && webApp.isActive()) {
          AbstractCacheEntry cacheEntry = _newCacheEntry;

          _cacheInvocation.finishCaching(cacheEntry);

          _newCacheEntry = null;
        }
      }
    } finally {
      AbstractCacheFilterChain cache = _cacheInvocation;
      _cacheInvocation = null;

      AbstractCacheEntry cacheEntry = _newCacheEntry;
      _newCacheEntry = null;

      if (cache != null && cacheEntry != null)
        cache.killCaching(cacheEntry);
    }
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

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _response + "]";
  }
}
