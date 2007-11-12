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

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * The conversation scope value
 */
public class ConversationScope extends ScopeContext {
  private static final L10N L = new L10N(ConversationScope.class);
  
  public Object get(String name)
  {
    throw new IllegalStateException(L.l("@ConversationScoped is not available in this context"));
  }
  
  public void set(String name, Object value)
  {
    throw new IllegalStateException(L.l("@ConversationScoped is not available in this context"));
  }

  @Override
  public boolean canInject(ScopeContext scope)
  {
    return (scope instanceof ApplicationScope
	    || scope instanceof SessionScope
	    || scope instanceof ConversationScope);
  }
}
