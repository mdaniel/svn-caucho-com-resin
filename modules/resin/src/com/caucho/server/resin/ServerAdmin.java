/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.server.resin;

import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import javax.management.ObjectName;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import org.iso_relax.verifier.Schema;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.log.Log;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;

import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.types.PathBuilder;

import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;

import com.caucho.relaxng.CompactVerifierFactoryImpl;

import com.caucho.loader.EnvironmentListener;

import com.caucho.jmx.Jmx;
import com.caucho.jmx.IntrospectionMBean;

import com.caucho.server.session.SessionManager;

import com.caucho.server.deploy.DeployControllerAdmin;
import com.caucho.server.deploy.ExpandDeployController;

import com.caucho.server.resin.mbean.ServletServerMBean;

public class ServerAdmin extends DeployControllerAdmin<ServerController>
  implements ServletServerMBean {

  ServerAdmin(ServerController controller)
  {
    super(controller);
  }
  
  /**
   * Returns the server directory.
   */
  public String getRootDirectory()
  {
    Path path = getController().getRootDirectory();

    if (path != null)
      return path.getNativePath();
    else
      return null;
  }

  /**
   * Returns the id
   */
  public String getId()
  {
    return getController().getId();
  }

  /**
   * Returns the array of ports.
   */
  public ObjectName []getPortObjectNames()
  {
    ServletServer server = getDeployInstance();
      
    if (server != null)
      return server.getPortObjectNames();
    else
      return new ObjectName[0];
  }
  
  /**
   * Returns the array of cluster.
   */
  public ObjectName []getClusterObjectNames()
  {
    ServletServer server = getDeployInstance();
      
    if (server != null)
      return server.getClusterObjectNames();
    else
      return new ObjectName[0];
  }
  
  /**
   * Clears the cache.
   */
  public void clearCache()
  {
    ServletServer server = getDeployInstance();
      
    if (server != null)
      server.clearCache();
  }
  
  /**
   * Clears the cache by regexp patterns.
   *
   * @param hostRegexp the regexp to match the host.  Null matches all.
   * @param urlRegexp the regexp to match the url. Null matches all.
   */
  public void clearCacheByPattern(String hostRegexp, String urlRegexp)
  {
    ServletServer server = getDeployInstance();
      
    if (server != null)
      server.clearCacheByPattern(hostRegexp, urlRegexp);
  }

  /**
   * Returns the invocation cache hit count.
   */
  public long getInvocationCacheHitCount()
  {
    ServletServer server = getDeployInstance();
      
    if (server != null)
      return server.getInvocationCacheHitCount();
    else
      return -1;
  }

  /**
   * Returns the invocation cache miss count.
   */
  public long getInvocationCacheMissCount()
  {
    ServletServer server = getDeployInstance();
      
    if (server != null)
      return server.getInvocationCacheMissCount();
    else
      return -1;
  }

  /**
   * Returns the proxy cache hit count.
   */
  public long getProxyCacheHitCount()
  {
    ServletServer server = getDeployInstance();
      
    if (server != null)
      return server.getProxyCacheHitCount();
    else
      return -1;
  }

  /**
   * Returns the proxy cache miss count.
   */
  public long getProxyCacheMissCount()
  {
    ServletServer server = getDeployInstance();
      
    if (server != null)
      return server.getProxyCacheMissCount();
    else
      return -1;
  }

  protected ServletServer getDeployInstance()
  {
    return getController().getDeployInstance();
  }
}
