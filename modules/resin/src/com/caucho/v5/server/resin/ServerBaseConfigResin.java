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

package com.caucho.v5.server.resin;

import java.nio.file.Path;
import java.security.Provider;
import java.security.Security;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.v5.cloud.security.SecuritySystem;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.types.Bytes;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.jsp.cfg.JspPropertyGroup;
import com.caucho.v5.loader.EnvironmentBean;
import com.caucho.v5.loader.EnvironmentProperties;
import com.caucho.v5.server.container.ServerBuilderOld;
import com.caucho.v5.store.temp.TempFileManager;
import com.caucho.v5.subsystem.RootDirectorySystem;
import com.caucho.v5.subsystem.SystemManager;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;

/**
 * The ResinConfig class represents configuration for 
 * the top-level <resin> system.
 */
public class ServerBaseConfigResin extends ServerBaseConfig implements EnvironmentBean
{
  private static Logger log = Logger.getLogger(ServerBaseConfigResin.class.getName());
  private static L10N L = new L10N(ServerBaseConfigResin.class);

  private final ServerBuilderOld _builder;
  private final SystemManager _system;

  private long _minFreeMemory = 2 * 1024L * 1024L;

  private SecurityManager _securityManager;

  private TempFileManager _tempFileManager;

  /**
   * Creates a new resin server.
   */
  public ServerBaseConfigResin(ServerBuilderOld builder)
  {
    _builder = builder;
    _system = SystemManager.getCurrent();
  }

  /**
   * Returns the classLoader
   */
  @Override
  public ClassLoader getClassLoader()
  {
    return _system.getClassLoader();
  }
  
  /**
   * Sets the resin system key
   */
  @Configurable
  public void setClusterSystemKey(String key)
  {
    // _builder.setClusterSystemKey(key);
    
    SecuritySystem security = SecuritySystem.getCurrent();
    security.setSignatureSecret(key);
  }
  
  /**
   * Obsolete version of ClusterSystemKey
   */
  @Configurable
  public void setResinSystemAuthKey(String key)
  {
    setClusterSystemKey(key);
  }

  @Configurable
  public void setRootDirectory(PathImpl root)
  {
    // _resin.setRootDirectory(root);
  }

  /**
   * Set true if the server should enable environment-based
   * system properties.
   */
  @Configurable
  public void setEnvironmentSystemProperties(boolean isEnable)
  {
    EnvironmentProperties.enableEnvironmentSystemProperties(isEnable);
  }

  /**
   * Configures the thread pool
   */
  @Configurable
  public ThreadPoolConfig createThreadPool()
    throws Exception
  {
    return new ThreadPoolConfig();
  }

  /**
   * Sets the user name for setuid.
   */
  @Configurable
  public void setUserName(String userName)
  {
  }

  /**
   * Sets the group name for setuid.
   */
  @Configurable
  public void setGroupName(String groupName)
  {
  }

  /**
   * Sets the minimum free memory allowed.
   */
  @Configurable
  public void setMinFreeMemory(Bytes minFreeMemory)
  {
    _minFreeMemory = minFreeMemory.getBytes();
  }

  /**
   * Gets the minimum free memory allowed.
   */
  public long getMinFreeMemory()
  {
    return _minFreeMemory;
  }

  /**
   * Sets the shutdown time
   */
  @Configurable
  public void setShutdownWaitMax(Period shutdownWaitMax)
  {
    _builder.setShutdownWaitTime(shutdownWaitMax.getPeriod());
  }
  
  @Configurable
  public void setElasticServer(boolean isElasticServer)
  {
  }
  
  @Configurable
  public void setElasticDns(boolean isElasticServer)
  {
  }
  
  @Configurable
  public void setJoinCluster(String joinCluster)
  {
  }
  
  @Configurable
  public void setHomeCluster(String homeCluster)
  {
  }
  
  @Configurable
  public void setHomeServer(String homeServer)
  {
  }
  
  /**
   * Set true if system properties are global.
   */
  @Configurable
  public void setGlobalSystemProperties(boolean isGlobal)
  {
  }

  @Configurable
  public SecurityManagerConfig createSecurityManager()
  {
    return new SecurityManagerConfig();
  }

  @Configurable
  public void setWatchdogManager(ConfigProgram program)
  {
  }

  public TempFileManager getTempFileManager()
  {
    if (_tempFileManager == null) {
      Path path = RootDirectorySystem.getCurrent().dataDirectory();

      _tempFileManager = new TempFileManager(path, null);
    }

    return _tempFileManager;
  }

  /**
   * Adds a new security provider
   */
  public void addSecurityProvider(Class<?> providerClass)
    throws Exception
  {
    if (! Provider.class.isAssignableFrom(providerClass))
      throw new ConfigException(L.l("security-provider {0} must implement java.security.Provider",
                                    providerClass.getName()));

    Security.addProvider((Provider) providerClass.newInstance());
  }

  /**
   * Configures JSP (backwards compatibility).
   */
  public JspPropertyGroup createJsp()
  {
    return new JspPropertyGroup();
  }

  /**
   * Ignore the boot configuration
   */
  public void addBoot(ContainerProgram program)
    throws Exception
  {
  }

  /**
   * Sets the admin directory
   */
  @Configurable
  @Deprecated
  public void setAdminPath(PathImpl path)
  {
    // setResinDataDirectory(path);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  class SecurityManagerConfig {
    private boolean _isEnable = true;

    SecurityManagerConfig()
    {
      if (_securityManager == null)
        _securityManager = new SecurityManager();
    }

    public void setEnable(boolean enable)
    {
      _isEnable = enable;
    }

    public void setValue(boolean enable)
    {
      setEnable(enable);
    }

    public void setPolicyFile(PathImpl path)
      throws ConfigException
    {
      if (! path.canRead())
        throw new ConfigException(L.l("policy-file '{0}' must be readable.",
                                      path));

    }

    @PostConstruct
    public void init()
    {
      if (_isEnable)
        System.setSecurityManager(_securityManager);
    }
  }
}
