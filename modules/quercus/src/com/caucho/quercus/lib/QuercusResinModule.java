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

import com.caucho.Version;
import com.caucho.jmx.Jmx;
import com.caucho.naming.Jndi;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.ReadOnly;
import com.caucho.server.webapp.Application;
import com.caucho.util.L10N;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Hashtable;
import java.util.Map;
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
   * Explode an object name into an array with key value pairs that
   * correspond to the keys and values in the object name.
   * The domain is stored in the returned array under the key named ":".
   */
  public ArrayValue mbean_explode(@NotNull String name)
    throws MalformedObjectNameException
  {
    ObjectName objectName = new ObjectName(name);

    ArrayValueImpl exploded = new ArrayValueImpl();

    exploded.put(":", objectName.getDomain());

    Hashtable<String, String> entries = objectName.getKeyPropertyList();

    for (Map.Entry<String, String> entry : entries.entrySet()) {
      exploded.put(entry.getKey(), entry.getValue());
    }

    return exploded;
  }

  /**
   * Implode an array into an object name.  The array contains key value pairs
   * that become key vlaue pairs in the object name.  The key with the name
   * ":" becomes the domain of the object name.
   */
  public String mbean_implode(@NotNull @ReadOnly ArrayValue exploded)
    throws MalformedObjectNameException
  {
    if (exploded == null)
      return null;

    String domain;

    Value domainValue = exploded.get(StringValue.create(":"));

    if (domainValue.isNull())
      domain = "*";
    else
      domain = domainValue.toString();

    Hashtable<String, String> entries = new Hashtable<String, String>();

    for (Map.Entry<Value, Value> entry : exploded.entrySet()) {
      String key = entry.getKey().toString();
      String value = entry.getValue().toString();

      if (":".equals(key))
        continue;

      entries.put(key, value);
    }

    ObjectName objectName;

    if (entries.isEmpty())
      objectName = new ObjectName(domain + ":" + "*");
    else
      objectName = new ObjectName(domain, entries);

    return objectName.getCanonicalName();
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
    throws MalformedObjectNameException
  {
    if (name == null || name.length() == 0)
      return Application.getLocal().getAdmin();
    else if (name.contains(":"))
      return Jmx.findGlobal(name);
    else
      return Jmx.find(name);
  }

  /**
   * Returns an array of names that match a JMX pattern.
   * If the name contains a ":", it is a query in the global jmx namespace.
   * If the name does not contain a ":", it is a search in the JMX namespace
   * of the current web application.
   */
  public static ArrayValue mbean_query(Env env, String pattern)
    throws MalformedObjectNameException
  {
    ArrayValueImpl values = new ArrayValueImpl();

    ObjectName patternObjectName;

    patternObjectName = new ObjectName(pattern);

    Set<ObjectName> objectNames;

    if (pattern.indexOf(':') > 0)
      objectNames = Jmx.getGlobalMBeanServer().queryNames(patternObjectName, null);
    else
      objectNames = Jmx.getMBeanServer().queryNames(patternObjectName, null);

    for (ObjectName objectName : objectNames)
      values.put(objectName.toString());

    return values;
  }

  /**
   * Returns the version of the Resin server software.
   */
  public String resin_version()
  {
    return Version.FULL_VERSION;
  }

}
