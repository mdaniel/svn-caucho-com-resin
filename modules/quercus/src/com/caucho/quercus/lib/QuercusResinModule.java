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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.quercus.lib;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.JavaClassDefinition;
import com.caucho.quercus.env.ObjectNameValue;
import com.caucho.naming.Jndi;
import com.caucho.jmx.Jmx;
import com.caucho.util.L10N;
import com.caucho.server.webapp.Application;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Set;

public class QuercusResinModule
  extends AbstractQuercusModule
{
  private static final L10N L = new L10N(QuercusResinModule.class);

  /**
   * Perform a jndi lookup to retrieve an object.
   *
   * @param name a fully qualified name "java:comp/env/foo", or a short-form "foo".
   *
   * @return the object, or null if it is not found.
   */
  public static Object jndi_lookup(String name)
  {
    return Jndi.lookup(name);
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
  public static Object mbean_lookup(Env env, @Optional String name)
  {
    try {
      if (name == null || name.length() == 0)
        return Application.getLocal().getAdmin();

      return Jmx.find(name);
    }
    catch (MalformedObjectNameException e) {
      env.warning(L.l("Malformed object name `{0}'", name), e);

      return null;
    }
  }

  // XXX: need test, doc
  public static ArrayValue mbean_query(Env env, String pattern)
  {
    ArrayValueImpl values = new ArrayValueImpl();

    ObjectName patternObjectName;

    try {
      patternObjectName = new ObjectName(pattern);
    }
    catch (MalformedObjectNameException e) {
      env.warning(L.l("Malformed object name `{0}'", pattern), e);

      return null;
    }

    Set<ObjectName> objectNames;

    if (pattern.indexOf(':') > 0)
      objectNames = Jmx.getGlobalMBeanServer().queryNames(patternObjectName, null);
    else
      objectNames = Jmx.getMBeanServer().queryNames(patternObjectName, null);

    for (ObjectName objectName : objectNames)
      values.put(ObjectNameValue.create(objectName));

    return values;
  }
}
