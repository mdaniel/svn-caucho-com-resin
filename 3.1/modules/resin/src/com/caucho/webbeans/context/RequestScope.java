/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.webbeans.context;

import com.caucho.server.dispatch.ServletInvocation;
import com.caucho.webbeans.component.ComponentImpl;

import javax.servlet.*;
import javax.webbeans.*;

/**
 * Configuration for the xml web bean component.
 */
public class RequestScope extends ScopeContext {
  public <T> T get(ComponentFactory<T> component, boolean create)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request != null) {
      ComponentImpl comp = (ComponentImpl) component;

      Object value = request.getAttribute(comp.getScopeId());

      if (value == null && create) {
	value = component.create();
	
	request.setAttribute(comp.getScopeId(), value);
      }
      
      return (T) value;
    }
    else
      return null;
  }
  
  public <T> void put(ComponentFactory<T> component, T value)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request != null) {
      ComponentImpl comp = (ComponentImpl) component;
      
      request.setAttribute(comp.getScopeId(), value);
    }
  }

  /**
   * Removes the scope value for the given component.
   */
  public <T> void remove(ComponentFactory<T> component)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request != null) {
      ComponentImpl comp = (ComponentImpl) component;
      
      request.removeAttribute(comp.getScopeId());
    }
  }


  @Override
  public boolean canInject(ScopeContext scope)
  {
    return (scope instanceof SingletonScope
	    || scope instanceof ApplicationScope
	    || scope instanceof SessionScope
	    || scope instanceof ConversationScope
	    || scope instanceof RequestScope);
  }

  public void addDestructor(ComponentImpl comp, Object value)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request != null) {
      DestructionListener listener
	= (DestructionListener) request.getAttribute("caucho.destroy");

      if (listener == null) {
	listener = new DestructionListener();
	request.setAttribute("caucho.destroy", listener);
      }
      
      listener.addValue(comp, value);
    }
  }
}
