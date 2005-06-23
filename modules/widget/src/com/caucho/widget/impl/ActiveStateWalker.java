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
import com.caucho.log.Log;
import com.caucho.util.L10N;
import com.caucho.widget.*;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActiveStateWalker
  implements WidgetInvocationChain,
             WidgetRequestChain,
             WidgetResponseChain
{
  private static final L10N L = new L10N(ActiveStateWalker.class);
  private static final Logger log = Log.open(ActiveStateWalker.class);

  private ActiveRootState _rootState = new ActiveRootState();

  private InitRootState _initRootState;
  private Widget _widget;

  private Lifecycle _lifecycle = new Lifecycle();

  private IdentityHashMap<Widget,ActiveState> _activeStateMap
    = new IdentityHashMap<Widget,ActiveState>();

  private ActiveState _currentState;
  private WidgetInterceptorChainWalker _interceptorChainWalker
    = new WidgetInterceptorChainWalker();

  public void setInitRootState(InitRootState initRootState)
  {
    _initRootState = initRootState;
  }

  public void setWidget(Widget widget)
  {
    _widget = widget;
  }

  public void setExternalConnection(ExternalConnection externalConnection)
  {
    _rootState.setExternalConnection(externalConnection);
  }

  public void init()
  {
    if (!_lifecycle.toStarting())
      throw new IllegalStateException(L.l("already init"));

    if (_initRootState == null)
      throw new IllegalStateException(L.l("`{0}' is required", "context-root-state"));

    if (_widget == null)
      throw new IllegalStateException(L.l("`{0}' is required", "widget"));

    _rootState.setActiveStateWalker(this);
    _rootState.setInitState(_initRootState);

    _rootState.init();

    _lifecycle.toActive();
  }

  public void destroy()
  {
    _lifecycle.toStopping();

    _initRootState = null;

    // XXX: pool activeState

    RuntimeException exn = null;

    // XXX: iteration of Map is slow
    for (ActiveState activeState : _activeStateMap.values()) {
      try {
        activeState.destroy();
      }
      catch (RuntimeException ex) {
        if (exn != null)
          exn = ex;
        else
          log.log(Level.WARNING, ex.toString(), ex);
      }
    }

    try {
      _activeStateMap.clear();
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

  public void walk()
    throws WidgetException, IOException
  {
    _currentState = _rootState;

    try {
      invocation(_widget);
      request(_widget);
      response(_widget);
    }
    finally {
      _currentState = null;
    }
  }

  private ActiveState createState(Widget next)
    throws IllegalStateException
  {
    ActiveState nextState = _activeStateMap.get(next);

    if (nextState == null) {
      nextState = new ActiveState();

      InitState initState = _initRootState.getInitState(next);

      if (initState == null)
        throw new IllegalStateException(L.l("{0} not initialized", next));

      nextState.setInitState(initState);
      nextState.setActiveStateWalker(this);
      nextState.setWidget(next);
      nextState.setParentState(_currentState);

      nextState.init();

      _activeStateMap.put(next, nextState);
    }
    else {
      ActiveState parent = nextState.getParentState();

      if (parent != _currentState)
        throw new IllegalStateException(L.l("bad state, {0} has parent {1}, cannot be child of {2}", nextState, parent, _currentState));
    }

    return nextState;
  }

  public ActiveState getState(Widget widget)
  {
    return _activeStateMap.get(widget);
  }

  private WidgetInterceptorChainWalker createInterceptorChainWalker(ActiveState state)
  {
    InitState initState = state.getInitState();
    Widget widget = state.getWidget();

    _interceptorChainWalker.setInterceptorList(initState.getInterceptorList());
    _interceptorChainWalker.setWidget(widget);
    _interceptorChainWalker.init();

    return _interceptorChainWalker;
  }

  // callback
  public void invocation(Widget next)
    throws WidgetException
  {
    invocation(next, null);
  }

  // callback
  public void invocation(Widget next, WidgetCallback callback)
    throws WidgetException
  {
    ActiveState nextState = createState(next);

    ActiveState currentActiveState = _currentState;

    try {
      _currentState = nextState;

      WidgetInvocation nextInvocation = nextState.getWidgetInvocation();

      WidgetInterceptorChainWalker interceptorChainWalker
        = createInterceptorChainWalker(nextState);

      try {
        interceptorChainWalker.startInvocation(nextInvocation, callback);
      }
      finally {
        interceptorChainWalker.destroy();
      }

      nextInvocation.finish();
    }
    finally {
      _currentState = currentActiveState;
    }
  }

  // callback
  public void request(Widget next)
    throws WidgetException
  {
    request(next, null);
  }

  // callback
  public void request(Widget next, WidgetCallback callback)
    throws WidgetException
  {
    ActiveState nextState = createState(next);

    ActiveState currentActiveState = _currentState;

    try {
      _currentState = nextState;

      WidgetRequest nextRequest = nextState.getWidgetRequest();

      WidgetInterceptorChainWalker interceptorChainWalker
        = createInterceptorChainWalker(nextState);

      try {
        interceptorChainWalker.startRequest(nextRequest, callback);
      }
      finally {
        interceptorChainWalker.destroy();
      }

      nextRequest.finish();
    }
    finally {
      _currentState = currentActiveState;
    }
  }

  // callback
  public void response(Widget next)
    throws WidgetException, IOException
  {
    response(next, null);
  }

  // callback
  public void response(Widget next, WidgetCallback callback)
    throws WidgetException, IOException
  {
    ActiveState nextState = createState(next);

    ActiveState currentActiveState = _currentState;

    try {
      _currentState = nextState;

      WidgetResponse nextResponse = nextState.getWidgetResponse();

      WidgetInterceptorChainWalker interceptorChainWalker
        = createInterceptorChainWalker(nextState);

      try {
        interceptorChainWalker.startResponse(nextResponse, callback);
      }
      finally {
        interceptorChainWalker.destroy();
      }


      nextResponse.finish();
    }
    finally {
      _currentState = currentActiveState;
    }
  }

  public ActiveRootState getRootState()
  {
    return _rootState;
  }
}
