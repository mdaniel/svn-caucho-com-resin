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
 * @author Emil Ong
 */

package com.caucho.remote.client;

import com.caucho.config.*;
import com.caucho.util.*;
import com.caucho.webbeans.cfg.AbstractBeanConfig;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.manager.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Configuration class for a remote client
 */
public class RemoteClient extends AbstractBeanConfig
{
  private static final Logger log 
    = Logger.getLogger(RemoteClient.class.getName());
  private static final L10N L = new L10N(RemoteClient.class);

  private Class _type;
  
  private String _url;
  private Class _factoryClass;

  /**
   * Sets the proxy interface class.
   */
  public void setClass(Class type)
  {
    _type = type;

    if (! type.isInterface())
      throw new ConfigException(L.l("remote-client class '{0}' must be an interface",
				    type.getName()));
  }

  public void setInterface(Class type)
  {
    setClass(type);
  }

  /**
   * Sets the remote URL
   */
  public void setUrl(String url)
  {
    _url = url;
    
    int p = _url.indexOf(':');

    if (p < 0)
      throw new ConfigException(L.l("'{0}' is an invalid URL for <remote-client>.  <remote-client> requires a valid scheme.",
				    _url));

    String scheme = _url.substring(0, p);
    
    try {
      String name = ProtocolProxyFactory.class.getName() + "/" + scheme;
    
      ArrayList<String> drivers = Services.getServices(name);

      if (drivers.size() == 0)
	throw new ConfigException(L.l("'{0}' is an unknown remote-client protocol.",
				      _url));

      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      _factoryClass = Class.forName(drivers.get(0), false, loader);

      Config.validate(_factoryClass, ProtocolProxyFactory.class);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Called at initialization time
   */
  @PostConstruct
  public void init()
  {
    if (_type == null)
      throw new ConfigException(L.l("remote-client requires a 'type' attribute"));
    
    if (_url == null)
      throw new ConfigException(L.l("remote-client requires a 'url' attribute"));
    
    register(createProxy(), _type);
  }

  /**
   * Creates the object from the proxy.
   *
   * @param env the calling environment
   *
   * @return the object named by the proxy.
   */
  protected Object createProxy()
  {
    WebBeansContainer webBeans = WebBeansContainer.create();

    ComponentImpl comp
      = (ComponentImpl) webBeans.createTransient(_factoryClass);

    ProtocolProxyFactory factory = (ProtocolProxyFactory) comp.createNoInit();

    factory.setURL(_url);
    
    return factory.createProxy(_type);
  }
}

