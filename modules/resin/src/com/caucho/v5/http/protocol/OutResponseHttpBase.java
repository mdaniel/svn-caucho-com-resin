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
import java.util.logging.Logger;

import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.ClientDisconnectException;

abstract public class OutResponseHttpBase extends OutResponseToByte
{
  private static final Logger log
    = Logger.getLogger(OutResponseHttpBase.class.getName());

  private static final L10N L = new L10N(OutResponseHttpBase.class);

  private RequestHttpBase _response;

  // bytes actually written
  private long _contentLength;

  private boolean _isComplete;

  public OutResponseHttpBase()
  {
  }

  protected OutResponseHttpBase(RequestHttpBase response)
  {
    setResponse(response);
  }

  public void setResponse(RequestHttpBase response)
  {
    _response = response;
  }

  protected RequestHttpBase getResponse()
  {
    return _response;
  }
  
  /**
   * initializes the Response stream at the beginning of a request.
   */
  @Override
  public void start()
  {
    super.start();

    _contentLength = 0;
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
   * Response stream is a writable stream.
   */
  public boolean canWrite()
  {
    return true;
  }

  public boolean hasData()
  {
    return isCommitted() || _contentLength > 0;
  }

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
  /*
  protected final void flushDataBuffer(byte []buf, int offset, int length,
                                       boolean isEnd)
    throws IOException
  {
    if (! isAutoFlush() && ! isEnd) {
      throw new IOException(L.l("auto-flushing has been disabled"));
    }

    long contentLengthHeader = _response.getContentLengthHeader();
    // Can't write beyond the content length
    if (0 < contentLengthHeader
        && contentLengthHeader < length + _contentLength) {
      if (lengthWarning(buf, offset, length, contentLengthHeader))
        return;

      length = (int) (contentLengthHeader - _contentLength);
    }
    
    writeHeaders(isEnd ? length : -1);

    if (length == 0 && ! isEnd) {
      return;
    }

    if (isHead()) {
      if (isEnd) {
        writeNext(buf, 0, 0, true);
      }
      
      return;
    }

    writeNext(buf, offset, length, isEnd);
  }
  */

  private boolean lengthWarning(byte []buf, int offset, int length,
                                long contentLengthHeader)
  {
    if (_response.isConnectionClosed() || isHead() || isClosed()) {
    }
    else if (contentLengthHeader < _contentLength) {
      RequestHttpBase request = _response;//.getRequest();
      String msg = L.l("{0}: Can't write {1} extra bytes beyond the content-length header {2}.  Check that the Content-Length header correctly matches the expected bytes, and ensure that any filter which modifies the content also suppresses the content-length (to use chunked encoding).",
                       "uri", // request.getRequestURI(),
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
        RequestHttpBase request = _response;//.getRequest();
        String graph = "";

        if (Character.isLetterOrDigit((char) ch))
          graph = "'" + (char) ch + "', ";

        String msg
          = L.l("{0}: tried to write {1} bytes with content-length {2} (At {3}char={4}).  Check that the Content-Length header correctly matches the expected bytes, and ensure that any filter which modifies the content also suppresses the content-length (to use chunked encoding).",
                "uri", // request.getRequestURI(),
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
  }

  /*
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

    _response.writeHeaders(length);

    // server/2hf3
    toCommitted();
  }
  */

  //
  // implementations
  //

  /*
  abstract protected void writeNext(byte []buffer, int offset, int length,
                                    boolean isEnd)
    throws IOException;
    */

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

  protected String dbgId()
  {
    Object request = _response;

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
    return getClass().getSimpleName() + "[" + _response + "]";
  }
}
