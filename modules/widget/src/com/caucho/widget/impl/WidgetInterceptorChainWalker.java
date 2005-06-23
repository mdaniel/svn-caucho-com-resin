/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.widget.impl;

import com.caucho.widget.*;

import java.util.ArrayList;
import java.io.IOException;

public class WidgetInterceptorChainWalker
  implements WidgetInterceptorChain
{
  private ArrayList<WidgetInterceptor> _interceptorList;

  private Widget _widget;

  private WidgetCallback _callback;
  private int _currentIndex = -1;

  private final WidgetInterceptor _last = new WidgetInterceptor() {
    public void invocation(WidgetInvocation invocation,
                             WidgetInterceptorChain next)
      throws WidgetException
    {
      if (_callback != null)
        _callback.invocation(invocation);
      else
        _widget.invocation(invocation);
    }

    public void request(WidgetRequest request, WidgetInterceptorChain next)
      throws WidgetException
    {
      if (_callback != null)
        _callback.request(request);
      else
        _widget.request(request);
    }

    public void response(WidgetResponse response, WidgetInterceptorChain next)
      throws WidgetException, IOException
    {
      if (_callback != null)
        _callback.response(response);
      else
        _widget.response(response);
    }

    public void url(WidgetURL url, WidgetInterceptorChain next)
      throws WidgetException
    {
      if (_callback != null)
        _callback.url(url);
      else
        _widget.url(url);
    }

    public void destroy(WidgetDestroy destroy, WidgetInterceptorChain next)
    {
      if (_callback != null)
        _callback.destroy(destroy);
      else
        _widget.destroy(destroy);
    }
  };

  public void setInterceptorList(ArrayList<WidgetInterceptor> interceptorList)
  {
    _interceptorList = interceptorList;
  }

  /**
   * The widget becomes the last in the chain
   */
  public void setWidget(Widget widget)
  {
    _widget = widget;
  }

  public void init()
  {
  }

  public void destroy()
  {
    _widget = null;
    _interceptorList = null;
    _currentIndex = -1;
  }

  private WidgetInterceptor advance()
  {
    _currentIndex++;

    int size = _interceptorList == null ? 0 : _interceptorList.size();

    if (_currentIndex == size)
      return _last;
    else if (_currentIndex > size)
      return null;
    else
      return _interceptorList.get(_currentIndex);
  }

  public void startInvocation(WidgetInvocation invocation, WidgetCallback callback)
    throws WidgetException
  {
    _currentIndex = -1;
    _callback = callback;
    invocation(invocation);
  }

  /** callback */
  public void invocation(WidgetInvocation invocation)
    throws WidgetException
  {
    WidgetInterceptor interceptor = advance();

    if (interceptor == null)
      return;

    interceptor.invocation(invocation, this);
  }

  public void startRequest(WidgetRequest request, WidgetCallback callback)
    throws WidgetException
  {
    _currentIndex = -1;
    _callback = callback;
    request(request);
  }

  /** callback */
  public void request(WidgetRequest request)
    throws WidgetException
  {
    WidgetInterceptor interceptor = advance();

    if (interceptor == null)
      return;

    interceptor.request(request, this);
  }

  public void startResponse(WidgetResponse response, WidgetCallback callback)
    throws WidgetException, IOException
  {
    _currentIndex = -1;
    _callback = callback;
    response(response);
  }

  /** callback */
  public void response(WidgetResponse response)
    throws WidgetException, IOException
  {
    WidgetInterceptor interceptor = advance();

    if (interceptor == null)
      return;

    interceptor.response(response, this);
  }

  public void startURL(WidgetURL url, WidgetCallback callback)
    throws WidgetException
  {
    _currentIndex = -1;
    _callback = callback;
    url(url);
  }

  /** callback */
  public void url(WidgetURL url)
    throws WidgetException
  {
    WidgetInterceptor interceptor = advance();

    if (interceptor == null)
      return;

    interceptor.url(url, this);
  }

  public void startDestroy(WidgetDestroy destroy, WidgetCallback callback)
    throws WidgetException
  {
    _currentIndex = -1;
    _callback = callback;
    destroy(destroy);
  }

  /** callback */
  public void destroy(WidgetDestroy destroy)
  {
    WidgetInterceptor interceptor = advance();

    if (interceptor == null)
      return;

    interceptor.destroy(destroy, this);
  }
}
