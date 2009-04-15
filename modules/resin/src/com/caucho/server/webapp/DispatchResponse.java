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

import com.caucho.server.connection.AbstractHttpResponse;
import com.caucho.server.connection.AbstractResponseStream;
import com.caucho.server.connection.CauchoResponse;
import com.caucho.server.connection.IncludeResponseStream;
import com.caucho.server.connection.HttpBufferStore;
import com.caucho.util.FreeList;
import com.caucho.vfs.WriteStream;

import javax.servlet.ServletResponse;
import javax.servlet.ServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Internal response for an include() or forward()
 */
class DispatchResponse extends AbstractHttpResponse
{
  private static final FreeList<DispatchResponse> _freeList
    = new FreeList<DispatchResponse>(32);

  private IncludeResponseStream _stream;
  
  private HttpServletResponse _next;
  
  protected DispatchResponse()
  {
    _stream = new IncludeResponseStream(this);
  }

  /**
   * Creates a dispatch request.
   */
  public static DispatchResponse createDispatch()
  {
    DispatchResponse res = _freeList.allocate();
    if (res == null)
      res = new DispatchResponse();

    return res;
  }

  /**
   * Sets the next response.
   */
  public void setNextResponse(HttpServletResponse next)
  {
    _next = next;

    _stream.init(next);
  }

  /**
   * Gets the next response.
   */
  public ServletResponse getResponse()
  {
    return _next;
  }

  /**
   * Starts the response.
   */
  @Override
  public void startRequest(HttpBufferStore httpBuffer)
    throws IOException
  {
    super.startRequest(httpBuffer);

    setResponseStream(_stream);

    _stream.start();
  }

  /**
   * included response can't set the content type.
   */
  public void setContentType(String type)
  {
  }

  /**
   * Wrapped calls.
   */
  public String encodeURL(String url)
  {
    return _next.encodeURL(url);
  }

  /**
   * Wrapped calls.
   */
  public String encodeRedirectURL(String url)
  {
    return _next.encodeRedirectURL(url);
  }

  /**
   * included() responses don't print the headers.
   */
  protected boolean writeHeadersInt(WriteStream os, int length, boolean isHead)
    throws IOException
  {
    return false;
  }

  /**
   * This is not a top response.
   */
  public boolean isTop()
  {
    return false;
  }

  @Override
  public String getCharacterEncoding()
  {
    // jsp/17e1
    return _next.getCharacterEncoding();
  }
  
  @Override
  public boolean isCommitted()
  {
    /**
     * JSP TCK requires this to return true if the request is committed,
     * making bug #2481 not possible to resolve (unless we add a config
     * option)
     */
    
    // jsp/15m2
    // #2481, server/10y5
    // jsp/15lg (tck)
    
    return _next.isCommitted();
  }

  /**
   * Returns true for a caucho response.
   */
  public boolean isCauchoResponse()
  {
    return _next instanceof CauchoResponse;
  }

  /**
   * Set true for a caucho response stream.
   */
  public void setCauchoResponseStream(boolean isCaucho)
  {
    _stream.setCauchoResponseStream(isCaucho);
  }

  /**
   * Kills the cache.
   */
  public void killCache()
  {
    super.killCache();

    ServletResponse next = _next;
    while (next != null && next != this) {
      if (next instanceof CauchoResponse) {
	((CauchoResponse) next).killCache();
	break;
      }

      if (next instanceof ServletResponseWrapper)
	next = ((ServletResponseWrapper) next).getResponse();
      else if (next instanceof DispatchResponse)
	next = ((DispatchResponse) next).getResponse();
      else
	break;
    }
  }

  /**
   * Frees the response.
   */
  public void free()
  {
    super.free();

    if (_stream != null)
      _stream.init(null);
    
    _next = null;
  }

  /**
   * Frees the request.
   */
  public static void free(DispatchResponse res)
  {
    res.free();

    _freeList.free(res);
  }
}
