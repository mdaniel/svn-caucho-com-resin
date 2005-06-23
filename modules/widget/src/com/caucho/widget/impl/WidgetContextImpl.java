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

import com.caucho.lifecycle.Lifecycle;
import com.caucho.widget.*;
import com.caucho.util.L10N;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class WidgetContextImpl
  implements WidgetContext
{
  private static final L10N L = new L10N(WidgetContextImpl.class);

  private Lifecycle _lifecycle = new Lifecycle();

  private InitWalker _initWalker;
  private Widget _widget;
  private ExternalContext _externalContext;

  private ServletContext _servletContext;

  public WidgetContextImpl()
    throws WidgetException
  {
  }

  public Widget getWidget()
  {
    return _widget;
  }

  public void setWidget(Widget widget)
  {
    _widget = widget;
  }

  public ExternalContext getExternalContext()
  {
    return _externalContext;
  }

  public void setExternalContext(ExternalContext externalContext)
  {
    _externalContext = externalContext;
  }

  public void setServletContext(ServletContext servletContext)
  {
    _servletContext = servletContext;
  }

  public ServletContext getServletContext()
  {
    return _servletContext;
  }

  public void init()
    throws WidgetException
  {
    if (!_lifecycle.toInitializing())
      return;

    if (_widget == null)
      throw new IllegalStateException(L.l("`{0}' is required", "widget"));

    if (_externalContext == null)
      throw new IllegalStateException(L.l("`{0}' is required", "external-context"));

    _initWalker = new InitWalker();

    _initWalker.setExternalContext(_externalContext);
    _initWalker.setWidget(_widget);

    _initWalker.init();

    _lifecycle.toInit();
  }

  public void render(ExternalConnection externalConnection)
    throws IOException, WidgetException
  {
    ActiveStateWalker activeStateWalker = new ActiveStateWalker();

    activeStateWalker.setInitRootState(_initWalker.getInitRootState());
    activeStateWalker.setExternalConnection(externalConnection);
    activeStateWalker.setWidget(_widget);

    activeStateWalker.init();

    try {
      activeStateWalker.walk();
    }
    finally {
      activeStateWalker.destroy();
    }
  }

  public void render(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    if (_servletContext == null)
      throw new IllegalStateException(L.l("context not initialized for servlet"));

    // XXX: pool
    HttpExternalConnection connection = new HttpExternalConnection();
    connection.start(_servletContext, request, response);

    try {
      render(connection);
    }
    catch (WidgetException e) {
      throw new ServletException(e);
    }

    connection.finish();
  }

  public void destroy()
  {
    _lifecycle.toDestroying();

    try {
      if (_initWalker != null)
        _initWalker.destroy();
    }
    finally {
      _initWalker = null;
      _externalContext = null;
      _widget = null;
      _servletContext = null;

      _lifecycle.toDestroy();
    }
  }

  public void removeVar(Widget widget, String name)
  {
    _initWalker.removeVar(widget, name);
  }

  public void removeVar(Widget widget, VarDefinition varDefinition)
  {
    _initWalker.removeVar(widget, varDefinition);
  }

  public void setVar(Widget widget, String name, Object value)
    throws UnsupportedOperationException
  {
    _initWalker.setVar(widget, name, value);
  }

  public void setVar(Widget widget, VarDefinition varDefinition, Object value)
    throws UnsupportedOperationException
  {
    _initWalker.setVar(widget, varDefinition, value);
  }

  public <T> T getVar(Widget widget, String name)
  {
    return (T) _initWalker.getVar(widget, name);
  }

  public <T> T getVar(Widget widget, VarDefinition varDefinition)
  {
    return (T) _initWalker.getVar(widget, varDefinition);
  }

  public <T> T getVar(Widget widget, String name, T deflt)
  {
    return (T) _initWalker.getVar(widget, name, deflt);
  }

  public <T> T getVar(Widget widget, VarDefinition varDefinition, T deflt)
  {
    return (T) _initWalker.getVar(widget, varDefinition, deflt);
  }
}
