/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.cloud.network.NetworkClusterSystem;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.Config;
import com.caucho.config.inject.InjectManager;
import com.caucho.env.service.ResinSystem;

/**
 * Library of static config functions.
 */
public class ResinServerConfigLibrary {
  private static final Logger log
    = Logger.getLogger(ResinServerConfigLibrary.class.getName());
  
  public static Object rvar(String name)
  {
    Object value = null;
    
    ResinSystem resinSystem = ResinSystem.getCurrent();
    
    if (resinSystem != null) {
      value = Config.getProperty(resinSystem.getId() + "." + name);

      if (value != null)
        return value;
    }
    
    String serverId = (String) Config.getProperty("server_id");
    if (serverId != null) {
      value = Config.getProperty(serverId + "." + name);
      
      if (value != null)
        return value;
    }
    
    NetworkClusterSystem networkSystem = NetworkClusterSystem.getCurrent();
    
    CloudServer selfServer = null;
    
    if (networkSystem != null)
      selfServer = networkSystem.getSelfServer();
    
    if (selfServer != null) {
      value = Config.getProperty(selfServer.getId() + "." + name);
      
      if (value != null)
        return value;
      
      value = Config.getProperty(selfServer.getCluster().getId() + "." + name);
      
      if (value != null)
        return value;
    }
    
    
    return Config.getProperty(name);
  }
  
  public static void configure(InjectManager webBeans)
  {
    try {
      ClassLoader loader = ClassLoader.getSystemClassLoader();
      
      for (Method m : ResinServerConfigLibrary.class.getMethods()) {
        if (! Modifier.isStatic(m.getModifiers()))
          continue;
        if (! Modifier.isPublic(m.getModifiers()))
          continue;
        if (m.getName().equals("configure"))
          continue;

        //BeanFactory factory = webBeans.createBeanFactory(m.getClass());

        // webBeans.addBean(factory.name(m.getName()).singleton(m));
        Config.setProperty(m.getName(), m, loader);
      }
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }
}
