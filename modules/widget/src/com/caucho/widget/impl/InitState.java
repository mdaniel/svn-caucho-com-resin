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
import com.caucho.widget.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.ArrayList;

public class InitState
  implements WidgetInit, WidgetDestroy
{
  private static final L10N L = new L10N(InitState.class);

  private static final VarDefinition UNKNOWN_VAR_DEFINITION =  new VarDefinition("<unknown>", Object.class);

  private Widget _widget;
  private InitWalker _initWalker;

  private HashMap<String, VarDefinition> _varDefinitionMap;
  private HashMap<String, InitState> _namespaceMap;

  private VarHolder _varHolder = new VarHolder();

  private InitState _namespaceState;
  private InitState _parentState;
  private ArrayList<WidgetInterceptor> _interceptorList;

  public void setWidget(Widget widget)
  {
    _widget = widget;
  }

  protected Widget getWidget()
  {
    return _widget;
  }

  public void setInitWalker(InitWalker initWalker)
  {
    _initWalker = initWalker;
  }

  public void setParentState(InitState parent)
  {
    _parentState = parent;
  }

  protected InitState getParentState()
  {
    return _parentState;
  }

  public VarHolder getVarHolder()
  {
    return _varHolder;
  }

  public void init()
  {
    if (_widget == null)
      throw new IllegalStateException(L.l("`{0}' is required", "widget"));

    _varHolder.setWidgetInit(this);
    _varHolder.setIgnoreReadOnly(true);

    if (_parentState != null)
      _varHolder.setParent(_parentState.getVarHolder());

    _varHolder.init();
  }

  public void destroy()
  {
    _varHolder.destroy();
  }

  public String getId()
  {
    return _widget.getId();
  }

  public boolean isNamespace()
  {
    return getParentState() == null ? true : _widget.isNamespace();
  }

  public WidgetInitChain getInitChain()
  {
    return _initWalker;
  }

  public WidgetDestroyChain getDestroyChain()
  {
    return _initWalker;
  }

  public void addVarDefinition(VarDefinition varDefinition)
  {
    String name = varDefinition.getName();

    if (varDefinition.isValue())
      checkVarValue(varDefinition, varDefinition.getValue());

    if (_varDefinitionMap == null)
      _varDefinitionMap =  new HashMap<String, VarDefinition>();

    _varDefinitionMap.put(name, varDefinition);
  }

  public void addInterceptor(WidgetInterceptor interceptor)
  {
    if (_interceptorList == null)
      _interceptorList = new ArrayList<WidgetInterceptor>();

    _interceptorList.add(interceptor);
  }

  public ArrayList<WidgetInterceptor> getInterceptorList()
  {
    return _interceptorList;
  }

  /**
   * Never returns null.
   */
  public VarDefinition getVarDefinition(String name)
  {
    VarDefinition varDefinition;

    if (_varDefinitionMap != null)
      varDefinition = _varDefinitionMap.get(name);
    else
      varDefinition = null;

    return (varDefinition == null) ? UNKNOWN_VAR_DEFINITION : varDefinition;
  }

  public InitState getInitState(Widget widget)
  {
    return _parentState.getInitState(widget);
  }

  public void removeVar(Widget widget, String name)
  {
    InitState initState = getInitState(widget);

    initState.getVarHolder().removeVar(name);
  }

  public void removeVar(Widget widget, VarDefinition varDefinition)
  {
    removeVar(widget, varDefinition);
  }

  private void checkVarValue(VarDefinition varDefinition, Object value)
    throws ClassCastException, IllegalArgumentException
  {
    VarHolder.checkVarValue(varDefinition, value);
  }

  public void setVar(Widget widget, String name, Object value)
    throws UnsupportedOperationException
  {
    InitState initState = getInitState(widget);

    initState.getVarHolder().setVar(name, value);
  }

  public void setVar(Widget widget, VarDefinition varDefinition, Object value)
    throws UnsupportedOperationException
  {
    InitState initState = getInitState(widget);

    initState.getVarHolder().setVar(varDefinition, value);
  }

  public <T> T getVar(Widget widget, String name)
  {
    InitState initState = getInitState(widget);

    return (T) initState.getVarHolder().getVar(name);
  }

  public <T> T getVar(Widget widget, VarDefinition varDefinition)
  {
    InitState initState = getInitState(widget);

    return (T) initState.getVarHolder().getVar(varDefinition);
  }

  public <T> T getVar(Widget widget, String name, T deflt)
  {
    InitState initState = getInitState(widget);

    return (T) initState.getVarHolder().getVar(name, deflt);
  }

  public <T> T getVar(Widget widget, VarDefinition varDefinition, T deflt)
  {
    InitState initState = getInitState(widget);

    return (T) initState.getVarHolder().getVar(varDefinition, deflt);
  }

  public <T> T find(String name)
  {
    InitState initState = getNamespaceState().getFromLocalNamespace(name);

    return (T) (initState == null ? null : initState.getWidget());
  }

  private InitState getFromLocalNamespace(String name)
  {
    return _namespaceMap == null ? null : _namespaceMap.get(name);
  }

  public void setApplicationAttribute(String name, Object value)
  {
    getParentState().setApplicationAttribute(name, value);
  }

  public <T> T getApplicationAttribute(String name)
  {
    return (T) getParentState().getApplicationAttribute(name);
  }

  public Enumeration<String> getApplicationAttributeNames()
  {
    return getParentState().getApplicationAttributeNames();
  }

  public void removeApplicationAttribute(String name)
  {
    getParentState().removeApplicationAttribute(name);
  }

  public void finish()
  {
  }

  public InitState getNamespaceState()
  {
    if (_namespaceState == null) {
      InitState namespaceState = this;

      while (!namespaceState.isNamespace() && namespaceState.getParentState() != null) {
        namespaceState = namespaceState.getParentState();
      }

     _namespaceState = namespaceState;
    }

    return _namespaceState;
  }

  /**
   * Register a child state in this namespace.
   */
  public void register(InitState childState)
  {
    assert isNamespace();


    String id = childState.getId();

    int len = id.length();

    for (int i = 0; i < len; i++) {
      char ch = id.charAt(i);

      boolean isValidChar;

      if (i == 0)
        isValidChar = Character.isLetter(ch);
      else
        isValidChar = Character.isLetterOrDigit(ch);

      if (!isValidChar)
        throw new IllegalArgumentException(L.l("invalid id `{0}' character `{1}' at position `{2}' is not allowed", id, Character.valueOf(ch), Integer.valueOf(i)));
    }

    if (_namespaceMap == null)
      _namespaceMap = new HashMap<String, InitState>();

    if (_namespaceMap.containsKey(id))
      throw new IllegalStateException(L.l("duplicate id `{0}' in namespace `{1}'", id, getId()));

    _namespaceMap.put(id, childState);
  }

  public String toString()
  {
    return "WidgetInit[id=" + getId() + "]";
  }

}
