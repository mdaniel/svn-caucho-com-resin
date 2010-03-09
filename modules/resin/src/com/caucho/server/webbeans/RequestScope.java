/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import java.lang.annotation.Annotation;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.servlet.ServletRequest;

import com.caucho.config.scope.ContextContainer;
import com.caucho.config.scope.ScopeContext;
import com.caucho.server.dispatch.ServletInvocation;

/**
 * Configuration for the xml web bean component.
 */
public class RequestScope extends ScopeContext
{
  private ScopeIdMap _idMap = new ScopeIdMap();

  /**
   * Returns true if the scope is currently active.
   */
  @Override
  public boolean isActive()
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    return request != null;
  }

  /**
   * Returns the scope annotation type.
   */
  @Override
  public Class<? extends Annotation> getScope()
  {
    return RequestScoped.class;
  }

  @Override
  public <T> T get(Contextual<T> bean)
  {
    if (! (bean instanceof PassivationCapable))
      return null;

    ServletRequest request = ServletInvocation.getContextRequest();

    if (request == null)
      return null;

    ContextContainer context
      = (ContextContainer) request.getAttribute("webbeans.resin");

    if (context != null) {
      String id = ((PassivationCapable) bean).getId();

      return (T) context.get(id);
    }

    return null;
  }

  @Override
  public <T> T get(Contextual<T> bean,
                   CreationalContext<T> creationalContext)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request == null)
      return null;

    Bean<T> comp = (Bean) bean;

    String id = ((PassivationCapable) bean).getId();

    ContextContainer context
      = (ContextContainer) request.getAttribute("webbeans.resin");

    if (context == null) {
      context = new ContextContainer();
      request.setAttribute("webbeans.resin", context);
    }

    T result = (T) context.get(id);

    if (result != null || creationalContext == null)
      return result;

    result = comp.create(creationalContext);

    context.put(comp, id, result, creationalContext);

    return result;
  }

  /*
  @Override
  public boolean canInject(ScopeContext scope)
  {
    return (scope instanceof ApplicationScope
            || scope instanceof SessionScope
            || scope instanceof ConversationScope
            || scope instanceof RequestScope);
  }

  @Override
  public boolean canInject(Class scopeType)
  {
    return (scopeType == ApplicationScoped.class
            || scopeType == SessionScoped.class
            || scopeType == ConversationScoped.class
            || scopeType == RequestScoped.class
            || scopeType == Dependent.class);
  }

  public void addDestructor(Bean comp, Object value)
  {
    ServletRequest request = ServletInvocation.getContextRequest();

    if (request != null) {
      DestructionListener listener
        = (DestructionListener) request.getAttribute("caucho.destroy");

      if (listener == null) {
        listener = new DestructionListener();
        request.setAttribute("caucho.destroy", listener);
      }

      // XXX:
      listener.addValue(comp, value);
    }
  }
  */
}
