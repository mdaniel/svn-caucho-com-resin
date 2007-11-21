/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.webbeans;

import java.lang.annotation.*;
import javax.webbeans.*;

import com.caucho.ejb.AbstractServer;

import com.caucho.webbeans.component.ComponentImpl;
import com.caucho.webbeans.context.*;
import com.caucho.webbeans.manager.WebBeansContainer;

/**
 * Component for session beans
 */
public class StatefulComponent extends ComponentImpl {
  private static final Object []NULL_ARGS = new Object[0];

  private AbstractServer _server;

  public StatefulComponent(AbstractServer server)
  {
    super(WebBeansContainer.create().getWbWebBeans());
    
    _server = server;
  }

  @Override
  public void setScope(ScopeContext scope)
  {
  }

  @Override
  public Object get()
  {
    return _server.getLocalObject();
  }

  @Override
  public Object get(DependentScope scope)
  {
    if (scope == null)
      return _server.getLocalObject();
    
    Object obj = scope.get(this);
    if (obj != null)
      return obj;

    obj = _server.getLocalObject(scope);
    scope.put(this, obj);

    return obj;
  }

  @Override
  public Object create()
  {
    return _server.getLocalObject();
  }
}
