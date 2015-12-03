/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Alex Rojkov
 */

package com.caucho.tomcat;

import com.caucho.util.L10N;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.logging.Logger;

/**
 * Installs Resin implementation of <code></code>org.apache.tomcat.InstanceManager</code>
 * into <code>ServletContext</code>
 */
public class ContextListener implements ServletContextListener
{
  private static L10N L = new L10N(ContextListener.class);
  private static Logger log = Logger.getLogger(ContextListener.class.getName());

  private InstanceManager _manager;

  public void contextInitialized(ServletContextEvent event)
  {
    ServletContext context = event.getServletContext();

    installContextInstanceManager(context);
  }

  private void installContextInstanceManager(final ServletContext context)
  {
    final String key = org.apache.tomcat.InstanceManager.class.getName();

    _manager = new InstanceManager();

    context.setAttribute(key, _manager);

    log.finer(L.l("installing {0} into {1}@{2}", _manager, context, key));
  }

  public void contextDestroyed(ServletContextEvent event)
  {
  }
}
