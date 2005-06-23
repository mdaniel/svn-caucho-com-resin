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

import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.widget.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;

public class InitWalker
  implements WidgetInitChain, WidgetDestroyChain
{
  private static final L10N L =  new L10N(InitWalker.class);
  private static final Logger log = Log.open(InitWalker.class);

  private ExternalContext _externalContext;
  private Widget _widget;

  private InitRootState _initRootState;
  private InitState _currentState;

  public void setExternalContext(ExternalContext externalContext)
  {
    _externalContext = externalContext;
  }

  public void setWidget(Widget widget)
  {
    _widget = widget;
  }

  public void init()
    throws WidgetException
  {
    if (_widget == null)
      throw new IllegalStateException(L.l("`{0}' is required", "widget"));

    if (_externalContext == null)
      throw new IllegalStateException(L.l("`{0}' is required", "external-context"));

    _initRootState = new InitRootState();
    _initRootState.setExternalContext(_externalContext);
    _initRootState.setWidget(_widget);

    doInit(_widget, null, _initRootState);
  }

  public InitRootState getInitRootState()
  {
    return _initRootState;
  }

  public void destroy()
  {
    try {
      destroy(_initRootState.getWidget());
    }
    finally {
      try {
        _initRootState.destroy();
      }
      finally {
        _initRootState = null;
        _currentState = null;
        _widget = null;
        _externalContext = null;
      }
    }
  }

  // callback
  public void init(Widget next)
    throws WidgetException
  {
    InitState nextState = new InitState();

    doInit(next, null, nextState);
  }

  // callback
  public void init(Widget next, WidgetCallback callback)
    throws WidgetException
  {
    InitState nextState = new InitState();

    doInit(next, callback, nextState);
  }

  private void doInit(Widget next, WidgetCallback callback, InitState nextState)
    throws WidgetException
  {
    InitState existingState = _initRootState.getInitState(next);

    if (existingState != null)
      throw new IllegalStateException(L.l("{0} already initialized", next));

    nextState.setWidget(next);
    nextState.setInitWalker(this);
    nextState.setParentState(_currentState);

    nextState.init();

    _initRootState.putInitState(next, nextState);

    InitState currentState = _currentState;

    try {
      _currentState = nextState;

      InitState namespaceState = _currentState.getNamespaceState();

      String preInitId = nextState.getId();

      if (preInitId != null)
        register(namespaceState);

      if (callback != null)
        callback.init(nextState);
      else
        next.init(nextState);

      String postInitId = nextState.getId();

      if (postInitId == null) {
        if (preInitId != null)
          throw new IllegalStateException(L.l("`{0}' cannot change from `{1}' to `{2}' in init", "id", preInitId, postInitId));
      }
      else {
        if (preInitId != null) {
          if (!preInitId.equals(postInitId))
            throw new IllegalStateException(L.l("`{0}' cannot change from `{1}' to `{2}' in init", "id", preInitId, postInitId));
        }
        else {
          register(namespaceState);
        }
      }
    }
    finally {
      _currentState = currentState;
    }
  }

  /**
   * Register the _currentState with the passed namespaceState, and if
   * they are the same, also register with the parent's namespace state
   */
  private void register(InitState namespaceState)
  {
    namespaceState.register(_currentState);

    if (namespaceState == _currentState) {
      InitState parent =_currentState.getParentState();

      if (parent != null) {
        InitState parentNamespaceState = parent.getNamespaceState();
        parentNamespaceState.register(_currentState);
      }
    }
  }

  // callback
  public void destroy(Widget next)
  {
    destroy(next, null);
  }

  // callback
  public void destroy(Widget next, WidgetCallback callback)
  {
    InitState nextState = _initRootState.getInitState(next);

    if (nextState == null)
      throw new IllegalStateException(L.l("{0} is not initialized, missing super.init(init) in {1}?", next, "parent"));

    InitState currentState = _currentState;

    try {
      _currentState = nextState;

      // XXX: dead code
      // next.doDestroy(nextState);

      ArrayList<WidgetInterceptor> interceptorList = _currentState.getInterceptorList();

      WidgetInterceptorChainWalker chainWalker = new WidgetInterceptorChainWalker();
      chainWalker.setInterceptorList(interceptorList);
      chainWalker.setWidget(next);
      chainWalker.startDestroy(_currentState, callback);
    }
    catch (Throwable t) {
      log.log(Level.WARNING, t.toString(), t);
    }
    finally {
      _currentState = currentState;

      _initRootState.removeInitState(next);
    }
  }

  public void removeVar(Widget widget, String name)
  {
    _initRootState.removeVar(widget, name);
  }

  public void removeVar(Widget widget, VarDefinition varDefinition)
  {
    _initRootState.removeVar(widget, varDefinition);
  }

  public void setVar(Widget widget, String name, Object value)
    throws UnsupportedOperationException
  {
    _initRootState.setVar(widget, name, value);
  }

  public void setVar(Widget widget, VarDefinition varDefinition, Object value)
    throws UnsupportedOperationException
  {
    _initRootState.setVar(widget, varDefinition, value);
  }

  public <T> T getVar(Widget widget, String name)
  {
    return (T) _initRootState.getVar(widget, name);
  }

  public <T> T getVar(Widget widget, VarDefinition varDefinition)
  {
    return (T) _initRootState.getVar(widget, varDefinition);
  }

  public <T> T getVar(Widget widget, String name, T deflt)
  {
    return (T) _initRootState.getVar(widget, name, deflt);
  }

  public <T> T getVar(Widget widget, VarDefinition varDefinition, T deflt)
  {
    return (T) _initRootState.getVar(widget, varDefinition, deflt);
  }
}
