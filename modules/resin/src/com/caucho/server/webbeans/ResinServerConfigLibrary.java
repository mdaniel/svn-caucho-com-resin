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
 * @author Scott Ferguson
 */

package com.caucho.server.webbeans;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.Config;
import com.caucho.config.ConfigPropertiesResolver;
import com.caucho.config.inject.InjectManager;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;


/**
 * Library of static config functions.
 */
public class ResinServerConfigLibrary {
  private static final Logger log
    = Logger.getLogger(ResinServerConfigLibrary.class.getName());
  
  public static String lookupResource(String resource, String parent) {
	  if (log.isLoggable(Level.FINEST)) {
		  log.finest(String.format("ResinServerConfigLibrary:lookupResource TRACE resource = %s, parent = %s", resource, parent));
	  }
	  
	  if (resource == null || resource.trim().isEmpty()) 
	  {
		  log.finer("ResinServerConfigLibrary:lookupResource: relative resource is not set, this is a normal situation.");
		  return null;
	  }
	  if (parent == null || parent.isEmpty()) {
		  log.warning("ResinServerConfigLibrary:lookupResource: parent argument (2nd argument) should be set, and it is not.");
		  return null;		  
	  }
	  Path parentPath = Vfs.lookup(parent);
	  Path resourcePath = parentPath.lookup(resource);
	  if (!resourcePath.exists() || !resourcePath.canRead()) 
	  {
		  log.warning("ResinServerConfigLibrary:lookupResource: resource does not exist or is not readable.");
		  return null;
	  }
	  if (log.isLoggable(Level.FINEST)) {
		  log.finest("ResinServerConfigLibrary:lookupResource: resource found and equal to " + resourcePath.getFullPath());
	  }
	  return resourcePath.getFullPath();
  }
  
  public static Object rvar(String var)
  {
    Object value = null;
    
    for (String resinProp: ConfigPropertiesResolver.RESIN_PROPERTIES) {
      String resinKey = (String) getProperty(resinProp);
      
      if (resinKey == null)
        break;
      
      value = getProperty(resinKey + '.' + var);
      
      if (value != null)
        return value;
    }
    
    return getProperty(var);
  }
  
  private static Object getProperty(String name)
  {
    return Config.getElVar(name);
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
