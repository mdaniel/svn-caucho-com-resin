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
import com.caucho.widget.ExternalContext;
import com.caucho.widget.Widget;

import java.util.Enumeration;
import java.util.IdentityHashMap;

public class InitRootState
  extends InitState
{
  private static final L10N L = new L10N(InitRootState.class);

  private ExternalContext _externalContext;

  private IdentityHashMap<Widget, InitState> _initStateMap
    = new IdentityHashMap<Widget,  InitState>();

  public InitState getInitState(Widget widget)
  {
    return _initStateMap.get(widget);
  }

  public void putInitState(Widget widget, InitState state)
  {
    _initStateMap.put(widget, state);
  }

  public void removeInitState(Widget widget)
  {
    _initStateMap.remove(widget);
  }

  public void setExternalContext(ExternalContext externalContext)
  {
    _externalContext = externalContext;
  }

  protected ExternalContext getExternalContext()
  {
    return _externalContext;
  }

  public void init()
  {
    if (_externalContext == null)
      throw new IllegalStateException(L.l("`{0}' is required", "external-context"));

    super.init();
  }

  public void destroy()
  {
    try {
      super.destroy();
    }
    finally {
      _externalContext = null;
      _initStateMap = null;
    }
  }

  public void setApplicationAttribute(String name, Object value)
  {
    _externalContext.setApplicationAttribute(name, value);
  }

  public <T> T getApplicationAttribute(String name)
  {
    return (T) _externalContext.getApplicationAttribute(name);
  }

  public Enumeration<String> getApplicationAttributeNames()
  {
    return _externalContext.getApplicationAttributeNames();
  }

  public void removeApplicationAttribute(String name)
  {
    _externalContext.removeApplicationAttribute(name);
  }
}
