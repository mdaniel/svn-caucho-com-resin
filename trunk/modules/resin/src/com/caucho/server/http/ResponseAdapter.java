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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.caucho.util.FreeList;
import com.caucho.vfs.FlushBuffer;

public class ResponseAdapter extends ResponseWrapper
  implements CauchoResponse
{
  private static final FreeList<ResponseAdapter> _freeList
    = new FreeList<ResponseAdapter>(32);

  private RequestAdapter _request;

  private FlushBuffer _flushBuffer;

  private AbstractResponseStream _originalResponseStream;
  private AbstractResponseStream _responseStream;

  private ServletOutputStreamImpl _os;
  private ResponseWriter _writer;

  private boolean _hasError;

  protected ResponseAdapter()
  {
    _originalResponseStream = createWrapperResponseStream();
    _responseStream = _originalResponseStream;

    _os = new ServletOutputStreamImpl();
    _writer = new ResponseWriter();
  }

  /**
   * Creates a new ResponseAdapter.
   */
  public static ResponseAdapter create(HttpServletResponse response)
  {
    ResponseAdapter resAdapt = _freeList.allocate();

    if (resAdapt == null) {
      resAdapt = new ResponseAdapter();
    }
    
    resAdapt.init(response);

    return resAdapt;
  }

  void setRequest(RequestAdapter request)
  {
    _request = request;
  }

  protected AbstractResponseStream createWrapperResponseStream()
  {
    return new WrapperResponseStream();
  }

  public void init(HttpServletResponse response)
  {
    setResponse(response);
    _hasError = false;

    _responseStream = _originalResponseStream;
    if (_originalResponseStream instanceof WrapperResponseStream) {
      WrapperResponseStream wrapper = (WrapperResponseStream) _originalResponseStream;
      wrapper.init(response);
    }

    _originalResponseStream.start();

    _os.init(_originalResponseStream);
    _writer.init(_originalResponseStream);
  }

  @Override
  public AbstractResponseStream getResponseStream()
  {
    return _responseStream;
  }

  @Override
  public boolean isCauchoResponseStream()
  {
    return false;
  }

  @Override
  public void setResponseStream(AbstractResponseStream responseStream)
  {
    _responseStream = responseStream;

    _os.init(responseStream);
    _writer.init(responseStream);
  }

  public boolean isTop()
  {
    return false;
  }

  @Override
  public void resetBuffer()
  {
    super.resetBuffer();

    _responseStream.clearBuffer();
  }

  @Override
  public void sendRedirect(String url)
    throws IOException
  {
    resetBuffer();

    super.sendRedirect(url);
  }

  @Override
  public int getBufferSize()
  {
    return _responseStream.getBufferSize();
  }

  @Override
  public void setBufferSize(int size)
  {
    _responseStream.setBufferSize(size);
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException
  {
    return _os;
  }

  /**
   * Sets the flush buffer
   */
  public void setFlushBuffer(FlushBuffer flushBuffer)
  {
    _flushBuffer = flushBuffer;
  }

  /**
   * Gets the flush buffer
   */
  public FlushBuffer getFlushBuffer()
  {
    return _flushBuffer;
  }

  @Override
  public PrintWriter getWriter() throws IOException
  {
    return _writer;
  }

  @Override
  public void setContentType(String value)
  {
    super.setContentType(value);

    try {
      _responseStream.setEncoding(getCharacterEncoding());
    } catch (UnsupportedEncodingException e) {
    }
  }

  @Override
  public void setContentLength(long length)
  {
    super.setContentLength((int) length);
  }

  public void addCookie(Cookie cookie)
  {
    if (_request != null)
      _request.setHasCookie();

    super.addCookie(cookie);
  }

  /*
   * caucho
   */

  @Override
  public String getHeader(String key)
  {
    return null;
  }

  public boolean disableHeaders(boolean disable)
  {
    return false;
  }

  @Override
  public void setFooter(String key, String value)
  {
  }

  @Override
  public void addFooter(String key, String value)
  {
  }

  public int getRemaining()
  {
    return _responseStream.getRemaining();
  }

  /**
   * When set to true, RequestDispatcher.forward() is disallowed on
   * this stream.
   */
  @Override
  public void setForbidForward(boolean forbid)
  {
  }

  /**
   * Returns true if RequestDispatcher.forward() is disallowed on
   * this stream.
   */
  @Override
  public boolean getForbidForward()
  {
    return false;
  }

  @Override
  public String getStatusMessage()
  {
    if (_response instanceof CauchoResponse)
      return ((CauchoResponse) _response).getStatusMessage();

    throw new UnsupportedOperationException();
  }

  /**
   * Set to true while processing an error.
   */
  @Override
  public void setHasError(boolean hasError)
  {
    _hasError = hasError;
  }

  /**
   * Returns true if we're processing an error.
   */
  @Override
  public boolean hasError()
  {
    return _hasError;
  }

  /**
   * Kills the cache for an error.
   */
  @Override
  public void killCache()
  {
    if (getResponse() instanceof CauchoResponse)
      ((CauchoResponse) getResponse()).killCache();
  }

  /**
   * Sets private caching
   */
  @Override
  public void setPrivateCache(boolean isPrivate)
  {
    if (getResponse() instanceof CauchoResponse)
      ((CauchoResponse) getResponse()).setPrivateCache(isPrivate);
  }

  /**
   * Sets no caching
   */
  @Override
  public void setNoCache(boolean isPrivate)
  {
    if (getResponse() instanceof CauchoResponse)
      ((CauchoResponse) getResponse()).setNoCache(isPrivate);
  }

  @Override
  public void setSessionId(String id)
  {
    if (getResponse() instanceof CauchoResponse)
      ((CauchoResponse) getResponse()).setSessionId(id);
  }

  @Override
  public boolean isNoCacheUnlessVary()
  {
    CauchoResponse cRes = getCauchoResponse();

    if (cRes != null)
      return cRes.isNoCacheUnlessVary();
    else
      return false;
  }

  public CauchoResponse getCauchoResponse()
  {
    ServletResponse response = getResponse();

    if (response instanceof CauchoResponse)
      return (CauchoResponse) response;
    else
      return null;
  }

  public void finish()
    throws IOException
  {
    if (_responseStream != null) {
      _responseStream.flushBuffer();
    }

    _responseStream = _originalResponseStream;
  }

  @Override
  public void completeCache()
  {
  }
  
  public void close()
    throws IOException
  {
    ServletResponse response = getResponse();

    AbstractResponseStream responseStream = _responseStream;
    _responseStream = _originalResponseStream;

    if (responseStream != null)
      responseStream.close();

    if (_originalResponseStream != responseStream)
      _originalResponseStream.close();

    if (response instanceof CauchoResponse) {
      ((CauchoResponse) response).close();
    }
    /*
    else {
      try {
        PrintWriter writer = response.getWriter();
        writer.close();
      } catch (Throwable e) {
      }

      try {
        OutputStream os = response.getOutputStream();
        os.close();
      } catch (Throwable e) {
      }
    }
    */
  }

  public int getStatus()
  {
    return _response.getStatus();
  }

  public Collection<String> getHeaders(String name)
  {
    return _response.getHeaders(name);
  }

  public Collection<String> getHeaderNames()
  {
    return _response.getHeaderNames();
  }

  public void setForwardEnclosed(boolean isForwardEnclosed)
  {
  }

  public boolean isForwardEnclosed()
  {
    return false;
  }
  
  @Override
  public void writeHeaders(int length)
  {
    
  }

  public static void free(ResponseAdapter resAdapt)
  {
    resAdapt.free();

    _freeList.free(resAdapt);
  }

  /**
   * Clears the adapter.
   */
  protected void free()
  {
    _request = null;
    _responseStream = null;

    setResponse(null);
  }

  /* (non-Javadoc)
   * @see com.caucho.server.http.CauchoResponse#getCharacterEncodingAssigned()
   */
  @Override
  public String getCharacterEncodingAssigned()
  {
    // TODO Auto-generated method stub
    return null;
  }
}
