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
import com.caucho.server.dispatch.ServletInvocation;

import java.lang.annotation.Annotation;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.context.*;
import javax.inject.manager.Bean;
import javax.inject.manager.InjectionTarget;

/**
 * The session scope value
 */
public class SessionScope extends ScopeContext {
  
  /**
   * Returns true if the scope is currently active.
   */
  public boolean isActive()
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request != null) {
      HttpSession session = ((HttpServletRequest) request).getSession();

      return session != null;
    }

    return false;
  }
  
  /**
   * Returns the scope annotation type.
   */
  public Class<? extends Annotation> getScopeType()
  {
    return SessionScoped.class;
  }
  
  public <T> T get(Contextual<T> bean)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request == null)
      return null;

    HttpSession session = ((HttpServletRequest) request).getSession();
    ComponentImpl comp = (ComponentImpl) bean;

    Object result = session.getAttribute(comp.getScopeId());

    return (T) result;
  }
  /*  
  public <T> T get(Contextual<T> bean,
		   CreationalContext<T> creationalContext)
  {
  */
  
  public <T> T get(InjectionTarget<T> bean)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request == null)
      return null;

    HttpSession session = ((HttpServletRequest) request).getSession();
    ComponentImpl comp = (ComponentImpl) bean;

    Object result = session.getAttribute(comp.getScopeId());

    if (result != null)
      return (T) result;

    result = comp.instantiate();

    session.setAttribute(comp.getScopeId(), result);
    boolean isValid = false;

    try {
      comp.inject(result);
      comp.postConstruct(result);
      
      isValid = true;
    } finally {
      if (! isValid)
	session.removeAttribute(comp.getScopeId());
    }
    
    return (T) result;
  }

  @Override
  public boolean canInject(Class scopeType)
  {
    return (scopeType == ApplicationScoped.class
	    || scopeType == SessionScoped.class
	    || scopeType == ConversationScoped.class
	    || scopeType == Dependent.class);
  }

  @Override
  public boolean canInject(ScopeContext scope)
  {
    return (scope instanceof ApplicationScope
	    || scope instanceof SessionScope);
  }

  public void addDestructor(ComponentImpl comp, Object value)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request != null) {
      HttpSession session = ((HttpServletRequest) request).getSession();
      DestructionListener listener
	= (DestructionListener) session.getAttribute("caucho.destroy");

      if (listener == null) {
	listener = new DestructionListener();
	session.setAttribute("caucho.destroy", listener);
      }

      // XXX:
      //listener.addValue(comp, value);
    }
  }
}
