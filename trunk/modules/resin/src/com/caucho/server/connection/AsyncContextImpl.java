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

import javax.servlet.*;

import com.caucho.server.http.HttpServletRequestImpl;

/**
 * Implementation of the Servlet 3.0 AsyncContext
 */
public class AsyncContextImpl implements AsyncContext
{
  private HttpServletRequestImpl _topRequest;
  
  private ServletRequest _request;
  private ServletResponse _response;

  public AsyncContextImpl(HttpServletRequestImpl topRequest,
			  ServletRequest request,
			  ServletResponse response)
  {
    _topRequest = topRequest;
    _request = request;
    _response = response;
  }
  
  public ServletRequest getRequest()
  {
    return _request;
  }
  
  public ServletResponse getResponse()
  {
    return _response;
  }

  public boolean hasOriginalRequestAndResponse()
  {
    return true;
  }

  public void dispatch()
  {
  }
  
  public void dispatch(String path)
  {
  }
  
  public void dispatch(ServletContext context, String path)
  {
  }

  public void complete()
  {
  }

  public void start(Runnable task)
  {
  }

  public void addListener(AsyncListener listener)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void addListener(AsyncListener listener,
                          ServletRequest request,
                          ServletResponse response)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public <T extends AsyncListener> T createListener(Class<T> cl)
    throws ServletException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void setTimeout(long timeout)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public long getTimeout()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _topRequest + "]";
  }
}
