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
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import com.caucho.config.scope.AbstractScopeContext;
import com.caucho.config.scope.ContextContainer;
import com.caucho.util.L10N;

/**
 * The conversation scope value
 */
public class ConversationScope extends AbstractScopeContext
  implements Conversation, java.io.Serializable
{
  private static final L10N L = new L10N(ConversationScope.class);

  public ConversationScope()
  {
  }

  /**
   * Returns true if the scope is currently active.
   */
  public boolean isActive()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();

    return facesContext != null;
  }

  public boolean isTransient()
  {
    return false;
  }

  /**
   * Returns the scope annotation type.
   */
  public Class<? extends Annotation> getScope()
  {
    return ConversationScoped.class;
  }

  /**
   * Returns the current value of the component in the conversation scope.
   */
  @Override
  protected ContextContainer getContextContainer()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();

    if (facesContext == null)
      return null;

    ExternalContext extContext = facesContext.getExternalContext();
    Map<String,Object> sessionMap = extContext.getSessionMap();

    ContextContainer scope = (ContextContainer) sessionMap.get("caucho.conversation");

    return scope;
  }

  /**
   * Returns the current value of the component in the conversation scope.
   */
  @Override
  protected ContextContainer createContextContainer()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();

    if (facesContext == null)
      return null;

    ExternalContext extContext = facesContext.getExternalContext();
    Map<String,Object> sessionMap = extContext.getSessionMap();

    ContextContainer scope = (ContextContainer) sessionMap.get("caucho.conversation");
    
    if (scope == null) {
      scope = new ContextContainer();
      sessionMap.put("caucho.conversation", scope);
    }

    return scope;
  }

  //
  // Conversation API
  //

  /**
   * Begins an extended conversation
   */
  public void begin(String name)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Begins an extended conversation
   */
  public void begin()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();

    if (facesContext == null)
      throw new IllegalStateException(L.l("@ConversationScoped is not available because JSF is not active"));

    ExternalContext extContext = facesContext.getExternalContext();
    Map<String,Object> sessionMap = extContext.getSessionMap();

    Scope scope = (Scope) sessionMap.get("caucho.conversation");

    if (scope == null) {
      scope = new Scope();
      sessionMap.put("caucho.conversation", scope);
    }

    UIViewRoot root = facesContext.getViewRoot();
    String id = root.getViewId();

    HashMap map = scope._conversationMap.get(id);

    if (map == null) {
      map = new HashMap(8);
      scope._conversationMap.put(id, map);
    }

    scope._extendedConversation = map;
  }

  /**
   * Ends an extended conversation
   */
  public void end()
  {
    FacesContext facesContext = FacesContext.getCurrentInstance();

    if (facesContext == null)
      throw new IllegalStateException(L.l("@ConversationScoped is not available because JSF is not active"));

    ExternalContext extContext = facesContext.getExternalContext();
    Map<String,Object> sessionMap = extContext.getSessionMap();

    Scope scope = (Scope) sessionMap.get("caucho.conversation");

    if (scope == null)
      return;

    scope._extendedConversation = null;
  }

  public boolean isLongRunning()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String getId()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public long getTimeout()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public void setTimeout(long timeout)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  static class Scope implements java.io.Serializable {
    final HashMap<String,HashMap> _conversationMap
      = new HashMap<String,HashMap>();

    HashMap _extendedConversation;
  }
}
