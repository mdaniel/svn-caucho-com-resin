/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package javax.management;

import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.management.*;

/**
 * Resin implementation for an MBeanServer factory.
 */
public class MBeanServerFactory {
  private static final Logger log = Logger.getLogger("javax.management.MBeanServerFactory");
  
  private static final String BUILDER_PROPERTY =
    "javax.management.builder.initial";
  private static final String DEFAULT_BUILDER =
    "com.caucho.jmx.MBeanServerBuilderImpl";

  private static ObjectName SERVER_DELEGATE_NAME;

  private static ArrayList _servers = new ArrayList();
  
  // cached values
  private static String _builderClassName;
  private static MBeanServerBuilder _builder;
  
  /**
   * Returns a new MBeanServer with the given domain.
   */
  public static MBeanServer createMBeanServer()
    throws SecurityException
  {
    return createMBeanServer(null);
  }
  
  /**
   * Returns a new MBeanServer with the given domain.
   */
  public static MBeanServer createMBeanServer(String domain)
    throws SecurityException
  {
    if (domain == null)
      domain = "DefaultDomain";
    
    MBeanServer server = newMBeanServer(domain);

    _servers.add(server);

    return server;
  }

  /**
   * Returns a list of MBeanServers.
   */
  public static ArrayList findMBeanServer(String agentId)
  {
    ArrayList servers = new ArrayList(_servers);

    if (agentId == null)
      return servers;

    for (int i = servers.size() - 1; i >= 0; i--) {
      MBeanServer server = (MBeanServer) servers.get(i);

      boolean isMatch = false;
      try {
	Object id = server.getAttribute(SERVER_DELEGATE_NAME, "MBeanServerId");

	isMatch = agentId.equals(id);
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
      }

      if (! isMatch)
	servers.remove(i);
    }
    
    return servers;
  }

  /**
   * Returns a new MBeanServer with the given domain.
   */
  public static MBeanServer newMBeanServer()
    throws SecurityException
  {
    return newMBeanServer(null);
  }

  /**
   * Returns a new MBeanServer with the given domain.
   */
  public static MBeanServer newMBeanServer(String domain)
    throws SecurityException
  {
    if (domain == null)
      domain = "DefaultDomain";
    
    MBeanServerBuilder builder = getBuilder();

    MBeanServer outer = null;
    MBeanServerDelegate delegate = builder.newMBeanServerDelegate();

    return builder.newMBeanServer(domain, outer, delegate);
  }

  /**
   * Releases internal references to a created MBeanServer.
   */
  public static void releaseMBeanServer(MBeanServer mbeanServer)
  {
    _servers.remove(mbeanServer);
  }
  
  /**
   * Returns the local server.
   */
  private static synchronized MBeanServerBuilder getBuilder()
  {
    String className = System.getProperty(BUILDER_PROPERTY);

    if (className == null)
      className = DEFAULT_BUILDER;

    if (! className.equals(_builderClassName)) {
      try {
	Thread thread = Thread.currentThread();
	ClassLoader loader = thread.getContextClassLoader();

	Class cl = Class.forName(className, false, loader);
	_builder = (MBeanServerBuilder) cl.newInstance();
	_builderClassName = className;
      } catch (Exception e) {
	throw new JMRuntimeException(e);
      }
    }

    return _builder;
  }

  static {
    try {
      SERVER_DELEGATE_NAME = new ObjectName("JMImplementation:type=MBeanServerDelegate");
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
