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

/**
 * In the following scenario:
 *
 * Widget[a]
 *   Widget[b]
 *
 * ask for b.foo:
 *
 * does b.foo have a runtime value?
 * does b.foo have an init value?
 *
 * does a.foo have a runtime value?
 * does a.foo have an init value?
 *
 * does b.foo have a default value?
 * does a.foo have a default value?
 *
 * b[runtime] has a sibling of b[init]
 * b[init] has a sibling of null
 * b[runtime] has a parent of a[runtime]
 * a[runtime] has a sibling of a[init]
 * a[init] has a sibling of a[null]
 */

package com.caucho.widget.impl;

import com.caucho.util.L10N;
import com.caucho.widget.VarDefinition;
import com.caucho.widget.WidgetInit;

import java.util.HashMap;


public class VarHolder
{
  private static final L10N L = new L10N(VarHolder.class);

  private WidgetInit _widgetInit;

  private VarHolder _parent;
  private VarHolder _sibling;

  private boolean _isIgnoreReadOnly;

  private HashMap<String, Object> _varMap;

  public void setWidgetInit(WidgetInit widgetInit)
  {
    _widgetInit = widgetInit;
  }

  final protected WidgetInit getWidgetInit()
  {
    return _widgetInit;
  }

  public void setParent(VarHolder parent)
  {
    _parent = parent;
  }

  private VarHolder getParent()
  {
    return _parent;
  }

  public void setSibling(VarHolder sibling)
  {
    _sibling = sibling;
  }

  private VarHolder getSibling()
  {
    return _sibling;
  }

  public void setIgnoreReadOnly(boolean isIgnoreReadOnly)
  {
    _isIgnoreReadOnly = isIgnoreReadOnly;
  }

  public boolean isIgnoreReadOnly()
  {
    return _isIgnoreReadOnly;
  }

  public void init()
  {
    if (_widgetInit == null)
      throw new IllegalStateException(L.l("`{0}' is required", "widget-init"));
  }

  public void destroy()
  {
    _widgetInit = null;
    _parent = null;

    if (_varMap != null)
      _varMap.clear();
  }

  static void checkVarValue(VarDefinition varDefinition, Object value)
    throws ClassCastException, IllegalArgumentException
  {
    if (value == null) {
      if (!varDefinition.isAllowNull())
        throw new IllegalArgumentException(L.l("var `{0}' cannot be null", varDefinition.getName()));
    }
    else {
      Class type = value.getClass();
      Class varType = varDefinition.getType();

      if (!varType.isAssignableFrom(type))
        throw new ClassCastException(L.l("var `{0}' value with type `{1}' is incompatible with `{2}'", varDefinition.getName(), type.getName(), varType.getName()));
    }
  }

  public VarDefinition getVarDefinition(String name)
  {
    return getWidgetInit().getVarDefinition(name);
  }

  final public void setVar(String name, Object value)
    throws UnsupportedOperationException
  {
    VarDefinition varDefinition = getVarDefinition(name);

    if ( (!isIgnoreReadOnly()) && varDefinition.isReadOnly())
      throw new UnsupportedOperationException(L.l("var `{0}' is read-only", name));

    checkVarValue(varDefinition, value);

    if (_varMap == null)
      _varMap = new HashMap<String, Object>();

    _varMap.put(name, value);
  }

  final public void setVar(VarDefinition varDefinition, Object value)
    throws UnsupportedOperationException
  {
    setVar(varDefinition.getName(), value);
  }

  protected Object getVarImpl(String name, boolean isDefaultValue, Object defaultValue)
  {
    // this method does not use recursive calls

    VarHolder current = this;
    VarHolder parent = getParent();

    do {
      HashMap<String, Object> varMap = current._varMap;

      if (varMap != null && varMap.size() > 0) {
        Object value = null;

        value = varMap.get(name);

        if (value != null || varMap.containsKey(name))
          return value;
      }

      VarHolder next = current.getSibling();

      if (next == null) {
        VarDefinition varDefinition = current.getVarDefinition(name);

        if (!varDefinition.isInherited())
          return isDefaultValue ? defaultValue : varDefinition.getValue();

        // The default value is established by the first widget that has a default value
        // If the recursion get's to the top without encountering a value, the established default
        // is returned

        if (!isDefaultValue && varDefinition.isValue()) {
          isDefaultValue = true;
          defaultValue = varDefinition.getValue();
        }

        current = parent;
        parent = current == null ? null : current.getParent();
      }
      else
        current = next;

    } while (current != null);

    return defaultValue;
  }

  final public <T> T getVar(String name)
  {
    return (T) getVarImpl(name, false, null);
  }

  final public <T> T getVar(VarDefinition varDefinition)
  {
    return (T) getVarImpl(varDefinition.getName(), false, null);
  }

  final public <T> T getVar(String name, T deflt)
  {
    T value = (T) getVarImpl(name, false, deflt);

    return value == null ? deflt : value;
  }

  final public <T> T getVar(VarDefinition varDefinition, T deflt)
  {
    T value = (T) getVarImpl(varDefinition.getName(), false, deflt);

    return value == null ? deflt : value;
  }

  final public void removeVar(String name)
  {
    if (_varMap != null)
      _varMap.remove(name);
  }

  final public void removeVar(VarDefinition varDefinition)
  {
    removeVar(varDefinition.getName());
  }
}
