/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Sam
 */


package com.caucho.quercus.lib.resin;

import java.util.Map;
import java.util.Hashtable;
import java.util.Set;

import javax.management.*;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.ReadOnly;
import com.caucho.quercus.module.Optional;
import com.caucho.server.webapp.Application;
import com.caucho.jmx.Jmx;

public class MBeanServer {
  public MBeanServer()
  {
  }

  public MBeanInfo info(String name)
  {
    javax.management.MBeanServer mbeanServer;

    if (name.contains(":"))
      mbeanServer = Jmx.getGlobalMBeanServer();
    else
      mbeanServer = Jmx.getMBeanServer();

    try {
      return mbeanServer.getMBeanInfo(new ObjectName(name));
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Perform a jmx lookup to retrieve an mbean object.
   *
   * If the optional name is not provided, the mbean for the current web-app
   * is returned.
   *
   * An unqualified name does not contain a `:' and is used to find an mbean
   * in the context of the current web-app.
   *
   * A fully qualified name contains a `:' and is used to find any mbean within the
   * server.
   *
   * @param name the name to lookup
   *
   * @return the mbean object, or null if it is not found.
   */
  public MBean lookup(Env env, @Optional String name)
  {
    try {
      if (name == null || name.length() == 0)
	return null;

      javax.management.MBeanServer server;
      server = Jmx.getGlobalMBeanServer();

      ObjectName objectName = Jmx.getObjectName(name);

      if (server.isRegistered(objectName))
	return new MBean(server, objectName);
      else
	return null;
    } catch (MalformedObjectNameException e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Returns an array of names that match a JMX pattern.
   * If the name contains a ":", it is a query in the global jmx namespace.
   * If the name does not contain a ":", it is a search in the JMX namespace
   * of the current web application.
   */
  public ArrayValue query(Env env, String pattern)
  {
    try {
      ArrayValueImpl values = new ArrayValueImpl();

      ObjectName patternObjectName;

      patternObjectName = new ObjectName(pattern);

      Set<ObjectName> objectNames;

      if (pattern.indexOf(':') > 0)
	objectNames = Jmx.getGlobalMBeanServer().queryNames(patternObjectName, null);
      else
	objectNames = Jmx.getMBeanServer().queryNames(patternObjectName, null);

      if (objectNames == null)
	return null;

      javax.management.MBeanServer server;
      server = Jmx.getGlobalMBeanServer();

      for (ObjectName objectName : objectNames)
	values.put(env.wrapJava(new MBean(server, objectName)));

      return values;
    } catch (MalformedObjectNameException e) {
      throw new QuercusModuleException(e);
    }
  }
}
