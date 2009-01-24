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

package com.caucho.server.webbeans;

import com.caucho.config.inject.ComponentImpl;
import com.caucho.config.scope.ApplicationScope;
import com.caucho.config.scope.DestructionListener;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.scope.SingletonScope;
import com.caucho.server.dispatch.ServletInvocation;

import java.lang.annotation.Annotation;
import javax.servlet.*;
import javax.context.RequestScoped;
import javax.inject.manager.Bean;

/**
 * Configuration for the xml web bean component.
 */
public class RequestScope extends ScopeContext
{
  /**
   * Returns true if the scope is currently active.
   */
  public boolean isActive()
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    return request != null;
  }
  
  /**
   * Returns the scope annotation type.
   */
  public Class<? extends Annotation> getScopeType()
  {
    return RequestScoped.class;
  }

  /**
   * Returns the value in the request scope
   *
   * @param bean the component to retrieve
   * @param create if true, create a new instance if it doesn't already exist
   */
  public <T> T get(Bean<T> bean, boolean create)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request != null) {
      ComponentImpl comp = (ComponentImpl) bean;

      Object value = request.getAttribute(comp.getScopeId());

      if (value == null && create) {
	value = bean.create();
	
	request.setAttribute(comp.getScopeId(), value);
      }
      
      return (T) value;
    }
    else
      return null;
  }
  
  public <T> void put(Bean<T> bean, T value)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request != null) {
      ComponentImpl comp = (ComponentImpl) bean;
      
      request.setAttribute(comp.getScopeId(), value);
    }
  }

  /**
   * Removes the scope value for the given component.
   */
  public <T> void remove(Bean<T> bean)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request != null) {
      ComponentImpl comp = (ComponentImpl) bean;
      
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
