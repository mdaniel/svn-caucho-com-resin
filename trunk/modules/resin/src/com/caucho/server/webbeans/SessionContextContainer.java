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

import com.caucho.config.scope.ApplicationScope;
import com.caucho.config.scope.DestructionListener;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.scope.ContextContainer;
import com.caucho.server.dispatch.ServletInvocation;

import java.lang.annotation.Annotation;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.enterprise.context.*;
import javax.enterprise.context.spi.*;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

/**
 * Configuration for the xml web bean component.
 */
public class SessionContextContainer extends ContextContainer
  implements HttpSessionBindingListener
{
  public void valueBound(HttpSessionBindingEvent event)
  {
  }
  
  public void valueUnbound(HttpSessionBindingEvent event)
  {
    close();
  }
}
