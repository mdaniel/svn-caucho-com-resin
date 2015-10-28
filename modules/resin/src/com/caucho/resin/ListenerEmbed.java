/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.resin;

import java.util.HashMap;
import java.util.Map;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.program.PropertyValueProgram;
import com.caucho.v5.http.dispatch.ServletBuilder;
import com.caucho.v5.util.L10N;

/**
 * Embeddable version of a servlet
 *
 * <code><pre>
 * ResinEmbed resin = new ResinEmbed();
 *
 * WebAppEmbed webApp = new WebAppEmbed("/foo", "/var/resin/foo");
 *
 * ServletEmbed myServlet = new ServletEmbed("my-servlet", "qa.MyServlet");
 * webApp.addServlet(myServlet);
 *
 * resin.addWebApp(webApp);
 * </pre></code>
 */
public class ListenerEmbed
{
  private Class<?> _listenerClass;

  /**
   * Creates a new embedded listener
   */
  public ListenerEmbed()
  {
  }

  /**
   * Creates a new embedded listener
   *
   * @param listenerClass the listener-class
   */
  public ListenerEmbed(Class<?> listenerClass)
  {
    setListenerClass(listenerClass);
  }

  /**
   * The listener-class
   */
  public ListenerEmbed setListenerClass(Class<?> listenerClass)
  {
    _listenerClass = listenerClass;
    
    return this;
  }

  /**
   * The listener-class
   */
  public Class<?> getListenerClass()
  {
    return _listenerClass;
  }
}
