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

package com.caucho.webbeans.context;

import com.caucho.util.*;
import com.caucho.webbeans.component.*;
import com.caucho.server.dispatch.ServletInvocation;

import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.webbeans.*;

/**
 * The conversation scope value
 */
public class ConversationScope extends ScopeContext
  implements Conversation, java.io.Serializable
{
  private static final L10N L = new L10N(ConversationScope.class);

  private static final ThreadLocal<ConversationScope> _currentScope
    = new ThreadLocal<ConversationScope>();

  private final String _id;
  private final HashMap<String,Object> _map
    = new HashMap<String,Object>();

  private boolean _isExtended;

  public ConversationScope()
  {
    _id = "environment";
  }

  public ConversationScope(String id)
  {
    _id = id;
  }

  /**
   * Returns the current value of the component in the conversation scope.
   */
  public <T> T get(ComponentFactory<T> component, boolean create)
  {
    ConversationScope currentScope = _currentScope.get();

    if (currentScope == null)
      throw new IllegalStateException(L.l("@ConversationScoped is not available in this context"));

    ComponentImpl comp = (ComponentImpl) component;
    T value = (T) currentScope._map.get(comp.getScopeId());

    return value;
  }
  
  /**
   * Sets the current value of the component in the conversation scope.
   */
  public <T> void put(ComponentFactory<T> component, T value)
  {
    ConversationScope currentScope = _currentScope.get();

    if (currentScope == null)
      throw new IllegalStateException(L.l("@ConversationScoped is not available in this context"));

    ComponentImpl comp = (ComponentImpl) component;
    currentScope._map.put(comp.getScopeId(), value);
  }
  
  /**
   * Removes the current value of the component in the conversation scope.
   */
  public <T> void remove(ComponentFactory<T> component)
  {
    ConversationScope currentScope = _currentScope.get();

    if (currentScope == null)
      throw new IllegalStateException(L.l("@ConversationScoped is not available in this context"));

    ComponentImpl comp = (ComponentImpl) component;
    currentScope._map.remove(comp.getScopeId());
  }

  /**
   * returns true if the argument scope can be safely injected in a
   * scope-instance.
   */
  @Override
  public boolean canInject(ScopeContext scope)
  {
    return (scope instanceof ApplicationScope
	    || scope instanceof SessionScope
	    || scope instanceof ConversationScope);
  }

  //
  // Conversation API
  //

  /**
   * Begins an extended conversation
   */
  public void begin()
  {
    ConversationScope currentScope = _currentScope.get();

    if (currentScope == null)
      throw new IllegalStateException(L.l("@ConversationScoped is not available in this context"));
    
    HttpServletRequest request
      = (HttpServletRequest) ServletInvocation.getContextRequest();
    HttpSession session = request.getSession();

    session.setAttribute("caucho.conversation", currentScope);

    _isExtended = true;
  }

  /**
   * Ends an extended conversation
   */
  public void end()
  {
    HttpServletRequest request
      = (HttpServletRequest) ServletInvocation.getContextRequest();
    HttpSession session = request.getSession();

    session.removeAttribute("caucho.conversation");
    
    _isExtended = false;
  }

  //
  // JSF routines (LifecycleImpl)
  //

  /**
   * Activates the conversation scope.
   */
  public static ConversationScope beginRender(String id)
  {
    ConversationScope oldScope = _currentScope.get();
    ConversationScope scope;
    
    HttpServletRequest request
      = (HttpServletRequest) ServletInvocation.getContextRequest();
    HttpSession session = request.getSession();

    scope = (ConversationScope) session.getAttribute("caucho.conversation");
    if (scope == null)
      scope = new ConversationScope(id);

    _currentScope.set(scope);

    return oldScope;
  }

  /**
   * Restores the old scope
   */
  public static void endRender(ConversationScope oldScope)
  {
    _currentScope.set(oldScope);
  }
}
