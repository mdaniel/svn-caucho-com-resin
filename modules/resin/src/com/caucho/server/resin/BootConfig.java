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

package com.caucho.server.resin;

import java.lang.reflect.Method;

import com.caucho.cloud.topology.CloudSystem;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.core.ResinProperties;
import com.caucho.config.functions.FmtFunctions;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.lib.ResinConfigLibrary;
import com.caucho.env.service.ResinSystem;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.naming.Jndi;
import com.caucho.server.webbeans.ResinCdiProducer;
import com.caucho.server.webbeans.ResinServerConfigLibrary;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * The Resin class represents the top-level container for Resin.
 * It exactly matches the &lt;resin> tag in the resin.xml
 */
public class BootConfig
{
  private final ResinSystem _resinSystem;
  
  private final String _serverId;
  
  private final Path _resinHome;
  private final Path _rootDirectory;
  private final Path _logDirectory;
  private final Path _resinConf;
  private final boolean _isProfessional;
  
  private final BootResinConfig _bootResinConfig;
  
  private CloudSystem _cloudSystem;
  
  public BootConfig(ResinSystem resinSystem,
                    String serverId,
                    Path resinHome,
                    Path rootDirectory,
                    Path logDirectory,
                    Path resinConf,
                    boolean isProfessional,
                    BootType bootType)
  {
    _resinSystem = resinSystem;
    
    _serverId = serverId;
    
    _resinHome = resinHome;
    _rootDirectory = rootDirectory;
    _logDirectory = logDirectory;
    _resinConf = resinConf;
    _isProfessional = isProfessional;
    
    switch (bootType) {
    case RESIN:
      _bootResinConfig = new BootResinEnvConfig(_resinSystem);
      break;
      
    default:
      _bootResinConfig = new BootResinConfig(_resinSystem);
      break;
    }
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_resinSystem.getClassLoader());
      
      preConfigInit();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  public BootResinConfig getBootResin()
  {
    return _bootResinConfig;
  }
  
  public void configureFile(Path path)
  {
    if (path == null || ! path.canRead())
      return;
    
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      thread.setContextClassLoader(_resinSystem.getClassLoader());
      
      Vfs.setPwd(_rootDirectory);
      
      Config config = new Config();
      // server/10hc
      // config.setResinInclude(true);
      
      config.configure(_bootResinConfig,
                       path,
                       _bootResinConfig.getSchema());
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  private void preConfigInit()
  {
    ResinVar resinVar = new ResinVar(_serverId,
                                     _resinHome,
                                     _rootDirectory,
                                     _logDirectory,
                                     _resinConf,
                                     _isProfessional,
                                     null);

    Config.setProperty("resinHome", _resinHome);
    Config.setProperty("resin", resinVar);
    Config.setProperty("server", resinVar);
    Config.setProperty("java", new JavaVar());
    Config.setProperty("system", System.getProperties());
    Config.setProperty("getenv", System.getenv());
    // server/4342
    Config.setProperty("server_id", _serverId);
    Config.setProperty("serverId", _serverId);
    Config.setProperty("rvar0", _serverId);

    InjectManager cdiManager = InjectManager.create();
    
    if (cdiManager.getBeans(ResinCdiProducer.class).size() == 0) {
      Config.setProperty("fmt", new FmtFunctions());

      ResinConfigLibrary.configure(cdiManager);
      //ResinServerConfigLibrary.configure(cdiManager);

      try {
        Method method = Jndi.class.getMethod("lookup", new Class[] { String.class });
        Config.setProperty("jndi", method);
        Config.setProperty("jndi:lookup", method);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }

      cdiManager.addManagedBeanDiscover(cdiManager.createManagedBean(ResinCdiProducer.class));
      Class<?> resinValidatorClass = ResinCdiProducer.createResinValidatorProducer();
      
      if (resinValidatorClass != null)
        cdiManager.addManagedBeanDiscover(cdiManager.createManagedBean(resinValidatorClass));

      cdiManager.update();
    }
    
    ResinServerConfigLibrary.configure(null);
  }

  public CloudSystem initTopolopy()
  {
    synchronized (this) {
      if (_cloudSystem == null)
        _cloudSystem = _bootResinConfig.initTopology();
    }
    
    return _cloudSystem;
  }
  
  public enum BootType {
    RESIN,
    WATCHDOG;
  }
}
