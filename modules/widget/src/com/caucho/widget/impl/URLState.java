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
import com.caucho.util.L10N;
import com.caucho.widget.*;

import java.util.Map;

public class URLState
{
  private static final L10N L = new L10N(URLState.class);

  private WidgetURLImpl _widgetURL = new WidgetURLImpl();

  private URLStateWalker _stateWalker;
  private ActiveState _activeState;
  private URLState _parentState;

  private Lifecycle _lifecycle = new Lifecycle();

  private boolean _isParameterAllowed;

  private PrefixMap _localParameterMap = new PrefixMap();
  private PrefixMap _namedParameterMap = new PrefixMap();

  private UrlVarHolder _urlVarHolder = new UrlVarHolder();

  public URLState()
  {
  }

  public void setStateWalker(URLStateWalker URLStateWalker)
  {
    _stateWalker = URLStateWalker;
  }

  protected URLStateWalker getStateWalker()
  {
    return _stateWalker;
  }

  public void setActiveState(ActiveState widgetState)
  {
    _activeState = widgetState;
  }

  public ActiveState getActiveState()
  {
    return _activeState;
  }

  public void setParentState(URLState parentState)
  {
    _parentState = parentState;
  }

  public URLState getParentState()
  {
    return _parentState;
  }

  public void init()
  {
    if (!_lifecycle.toStarting())
      throw new IllegalStateException(L.l("already init"));

    if (_stateWalker == null)
      throw new IllegalStateException(L.l("`{0}' is required", "state-walker"));

    if (_activeState == null)
      throw new IllegalStateException(L.l("`{0}' is required", "widget-state"));

    _isParameterAllowed = _activeState.getId() != null;

    if (_isParameterAllowed) {
      Map<String, String[]> backingMap = getBackingMap();

      _localParameterMap.setBackingMap(backingMap);
      _localParameterMap.setPrefix(_activeState.getLocalParameterMapPrefix());
      _localParameterMap.init();

      _namedParameterMap.setBackingMap(backingMap);
      _namedParameterMap.setPrefix(_activeState.getNamedParameterMapPrefix());
      _namedParameterMap.init();
    }

    _urlVarHolder.setWidgetInit(_activeState.getInitState());
    _urlVarHolder.setVarHolder(_activeState.getVarHolder());

    if (_parentState != null)
      _urlVarHolder.setParent(_parentState.getVarHolder());

    _lifecycle.toActive();
  }

  protected Map<String, String[]> getBackingMap()
  {
    return getParentState().getBackingMap();
  }

  public void destroy()
  {
    _lifecycle.toStopping();

    try {
      _urlVarHolder.destroy();

      if (_isParameterAllowed) {
        _localParameterMap.destroy();
        _namedParameterMap.destroy();
      }
    }
    finally {
      _activeState = null;
      _parentState = null;
      _stateWalker = null;

      _lifecycle.toStop();
    }

  }

  public WidgetURL getWidgetURL()
  {
    return _widgetURL;
  }

  private WidgetURL getParentWidgetURL()
  {
    return getParentState().getWidgetURL();
  }

  private void checkParameterAllowed()
  {
    if (!_isParameterAllowed)
      throw new UnsupportedOperationException(L.l("`{0}' is required for widget `{1}' if parameter values are set", "id", _activeState));

    if (!_stateWalker.isFormingURL())
      throw new UnsupportedOperationException(L.l("cannot set parameter except in recursive formation of url"));
  }

  public void setParameter(String[] values)
  {
    checkParameterAllowed();

    _localParameterMap.put("", values);
  }

  public void setParameter(String value)
  {
    setParameter(new String[] { value });
  }

  public void setParameter(String name, String[] values)
  {
    checkParameterAllowed();

    _namedParameterMap.put(name, values);
  }

  public void setParameter(String name, String value)
  {
    checkParameterAllowed();

    _namedParameterMap.put(name, new String[] { value });
  }

  public WidgetURLChain getURLChain()
  {
    return _stateWalker;
  }

  private VarHolder getVarHolder(Widget widget)
  {
    if (widget == _activeState.getWidget())
      return _urlVarHolder;

    URLState URLState = _stateWalker.getState(widget);

    if (URLState == null)
      throw new IllegalArgumentException(L.l("unknown widget `{0}'", widget));

    return URLState.getVarHolder();
  }

  VarHolder getVarHolder()
  {
    return _urlVarHolder;
  }

  public void setVar(Widget widget, VarDefinition varDefinition, Object value)
    throws UnsupportedOperationException
  {
    getVarHolder(widget).setVar(varDefinition, value);
  }

  public void setVar(Widget widget, String name, Object value)
    throws UnsupportedOperationException
  {
    getVarHolder(widget).setVar(name, value);
  }

  public <T> T getVar(Widget widget, VarDefinition varDefinition)
  {
    return (T) getVarHolder(widget).getVar(varDefinition);
  }

  public <T> T getVar(Widget widget, String name)
  {
    return (T) getVarHolder(widget).getVar(name);
  }

  public <T> T getVar(Widget widget, VarDefinition varDefinition, T deflt)
  {
    return (T) getVarHolder(widget).getVar(varDefinition, deflt);
  }

  public <T> T getVar(Widget widget, String name, T deflt)
  {
    return (T) getVarHolder(widget).getVar(name, deflt);
  }

  public void removeVar(Widget widget, String name)
  {
     getVarHolder(widget).removeVar(name);
  }

  public void removeVar(Widget widget, VarDefinition varDefinition)
  {
     getVarHolder(widget).removeVar(varDefinition);
  }

  public InitWalker getContextChain()
  {
    throw new UnimplementedException();
  }

  public String createURLString()
    throws WidgetException
  {
    return getParentWidgetURL().toString();
  }

  public boolean isSecure()
  {
    return getParentWidgetURL().isSecure();
  }

  public void setSecure(boolean isSecure)
    throws WidgetSecurityException
  {
    getParentWidgetURL().setSecure(isSecure);
  }

  public void setPathInfo(String pathInfo)
  {
    getParentWidgetURL().setPathInfo(pathInfo);
  }

  private URLState getState()
  {
    return this;
  }

  private class WidgetURLImpl
    implements WidgetURL
  {
    public boolean isSecure()
    {
      return getState().isSecure();
    }

    public void setSecure(boolean isSecure)
      throws WidgetSecurityException
    {
      getState().setSecure(isSecure);
    }

    public void setParameter(String[] values)
    {
      getState().setParameter(values);
    }

    public void setParameter(String value)
    {
      getState().setParameter(value);
    }

    public void setParameter(String name, String[] values)
    {
      getState().setParameter(name, values);
    }

    public void setParameter(String name, String value)
    {
      getState().setParameter(name, value);
    }

    public void setPathInfo(String pathInfo)
    {
      getState().setPathInfo(pathInfo);
    }

    public void setVar(Widget widget, VarDefinition varDefinition, Object value)
      throws UnsupportedOperationException
    {
      getState().setVar(widget, varDefinition, value);
    }

    public void setVar(Widget widget, String name, Object value)
      throws UnsupportedOperationException
    {
      getState().setVar(widget, name, value);
    }

    public <T> T getVar(Widget widget, VarDefinition varDefinition)
    {
      return (T) getState().getVar(widget, varDefinition);
    }

    public <T> T getVar(Widget widget, String name)
    {
      return (T) getState().getVar(widget, name);
    }

    public <T> T getVar(Widget widget, VarDefinition varDefinition, T deflt)
    {
      return (T) getState().getVar(widget, varDefinition, deflt);
    }

    public <T> T getVar(Widget widget, String name, T deflt)
    {
      return (T) getState().getVar(widget, name, deflt);
    }

    public void removeVar(Widget widget, String name)
    {
      getState().removeVar(widget, name);
    }

    public void removeVar(Widget widget, VarDefinition varDefinition)
    {
      getState().removeVar(widget, varDefinition);
    }

    public String toString()
    {
      try {
        return getState().createURLString();
      }
      catch (WidgetException e) {
        throw new RuntimeException(e);
      }
    }

    public WidgetURLChain getURLChain()
    {
      return getState().getURLChain();
    }
  }
}
