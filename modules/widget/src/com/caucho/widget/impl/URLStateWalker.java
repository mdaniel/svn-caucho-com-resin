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
import com.caucho.util.Log;
import com.caucho.widget.*;

import java.util.IdentityHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class URLStateWalker
  implements WidgetURLChain
{
  private static final L10N L = new L10N(URLStateWalker.class);
  private static final Logger log = Log.open(URLStateWalker.class);

  private URLRootState _rootState = new URLRootState();

  private Widget _widget;

  private Lifecycle _lifecycle = new Lifecycle();

  private ActiveStateWalker _activeStateWalker;

  private IdentityHashMap<Widget, URLState> _urlStateMap
    = new IdentityHashMap<Widget,URLState>();

  private URLState _currentState;

  private Widget _topWidget;

  private final WidgetInterceptorChainWalker _interceptorChainWalker
    = new WidgetInterceptorChainWalker();

  public void setActiveStateWalker(ActiveStateWalker activeStateWalker)
  {
    _activeStateWalker = activeStateWalker;
  }

  public void setExternalConnection(ExternalConnection externalConnection)
  {
    _rootState.setExternalConnection(externalConnection);
  }

  /**
   * @param widget the widget that is creating the url
   */
  public void setWidget(Widget widget)
  {
    _widget = widget;
  }

  public void setSubmitPrefix(String submitPrefix)
  {
    _rootState.setSubmitPrefix(submitPrefix);
  }

  public void init()
  {
    if (!_lifecycle.toStarting())
      throw new IllegalStateException(L.l("already init"));

    if (_widget == null)
      throw new IllegalStateException(L.l("`{0}' is required", "widget"));

    _rootState.setActiveState(_activeStateWalker.getRootState());
    _rootState.setStateWalker(this);
    _rootState.init();

    _currentState = _rootState;

    _lifecycle.toActive();
  }

  public boolean isInit()
  {
    return _lifecycle.isActive();
  }

  public void destroy()
  {
    _lifecycle.toStopping();

    _widget = null;

    _rootState.destroy();

    _currentState = null;

    // XXX: pool widgetState

    RuntimeException exn = null;

    // XXX: iteration of Map is slow
    for (URLState URLState : _urlStateMap.values()) {
      try {
        URLState.destroy();
      }
      catch (RuntimeException ex) {
        if (exn != null)
          exn = ex;
        else
          log.log(Level.WARNING, ex.toString(), ex);
      }
    }

    try {
      _urlStateMap.clear();
    }
    catch (RuntimeException ex) {
      if (exn != null)
        exn = ex;
      else
        log.log(Level.WARNING, ex.toString(), ex);
    }

    try {
      if (_rootState != null)
        _rootState.destroy();
    }
    catch (RuntimeException ex) {
      if (exn != null)
        exn = ex;
      else
        log.log(Level.WARNING, ex.toString(), ex);
    }

    _lifecycle.toStop();

    if (exn != null)
      throw exn;
  }

  public WidgetURL walk()
    throws WidgetException
  {
    URLState state = createState(_widget);

    // figure out what the _topWidget is
    ActiveState widgetState = state.getActiveState();

    if (widgetState == null)
      throw new IllegalStateException(L.l("{0} not known", _widget));

    while (widgetState.getParentState() != null && widgetState.getParentState().getWidget() != null) {
      widgetState = widgetState.getParentState();
    }

    _topWidget = widgetState.getWidget();

    return state.getWidgetURL();
  }

  private URLState createState(Widget next)
    throws IllegalStateException
  {
    URLState nextState = _urlStateMap.get(next);

    if (nextState == null) {
      // XXX: pool
      nextState = new URLState();

      ActiveState widgetState = _activeStateWalker.getState(next);

      if (widgetState == null)
        throw new IllegalStateException(L.l("{0} not known", next));

      nextState.setStateWalker(this);
      nextState.setActiveState(widgetState);

      ActiveState parentWidgetState = widgetState.getParentState();
      Widget parentWidget = parentWidgetState == null ? null : parentWidgetState.getWidget();

      if (parentWidget != null) {
        URLState parentState = createState(parentWidget);

        nextState.setParentState(parentState);
      }
      else {
        nextState.setParentState(_rootState);
      }

      nextState.init();

      _urlStateMap.put(next, nextState);
    }

    return nextState;
  }

  public URLState getState(Widget widget)
  {
    return _urlStateMap.get(widget);
  }

  private WidgetInterceptorChainWalker createInterceptorChainWalker(URLState state)
  {
    ActiveState widgetState = state.getActiveState();
    InitState initState = widgetState.getInitState();
    Widget widget = widgetState.getWidget();

    _interceptorChainWalker.setInterceptorList(initState.getInterceptorList());
    _interceptorChainWalker.setWidget(widget);
    _interceptorChainWalker.init();

    return _interceptorChainWalker;
  }

  public void doURL()
    throws WidgetException
  {
    url(_topWidget);
  }

  // callback
  public void url(Widget next)
    throws WidgetException
  {
    url(next, null);
  }

  // callback
  public void url(Widget next, WidgetCallback callback)
    throws WidgetException
  {
    assert next != null;

    URLState nextState = createState(next);

    URLState parent = nextState.getParentState();

    if (parent != _currentState)
      throw new IllegalStateException(L.l("bad state, {0} has parent {1}, cannot be child of {2}", nextState, parent, _currentState));

    URLState lastState = _currentState;

    try {
      _currentState = nextState;

      WidgetURL nextURL = _currentState.getWidgetURL();

      WidgetInterceptorChainWalker interceptorChainWalker
        = createInterceptorChainWalker(nextState);

      try {
        interceptorChainWalker.startURL(nextURL, callback);
      }
      finally {
        interceptorChainWalker.destroy();
      }
    }
    finally {
      _currentState = lastState;
    }
  }

  public boolean isFormingURL()
  {
    return _currentState != _rootState;
  }
}
