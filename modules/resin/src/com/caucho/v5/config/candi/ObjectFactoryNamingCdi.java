/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.candi;

import sun.reflect.Reflection;

import java.util.Hashtable;

import javax.naming.NamingException;

/**
 * The JNDI proxy for Manager
 */
@SuppressWarnings("serial")
public class ObjectFactoryNamingCdi implements ObjectFactoryNaming, java.io.Serializable
{
  /**
   * Creates the object from the proxy.
   *
   * @param env the calling environment
   *
   * @return the object named by the proxy.
   */
  @Override
  public Object createObject(Hashtable<?,?> env)
    throws NamingException
  {
    CandiManager manager = CandiManager.create();

    Class<?> applicationClass = findApplicationClass();

    return manager.getBeanManager(applicationClass);
  }

  public Class<?> findApplicationClass()
  {
    final ClassLoader classLoader
      = Thread.currentThread().getContextClassLoader();

    Class<?> caller = null;

    for (int i = 1; i < 50; i++) {
      caller = Reflection.getCallerClass(i++);
      
      if (caller == null) {
        return null;
      }
      else if (caller.getClassLoader() == classLoader) {
        return caller;
      }
    }

    return caller;
  }
}
