/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.cli.server;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.RootConfigBoot;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.Version;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

public class BootConfigParser
{
  private static final L10N L = new L10N(BootConfigParser.class);
  private static final Logger log
    = Logger.getLogger(BootConfigParser.class.getName());
  
  public BootConfigParser()
  {
  }
  
  public ConfigBoot parseBoot(ArgsDaemon args, SystemManager system)
  {
    Objects.requireNonNull(args);

    PathImpl homeDir = args.getHomeDirectory();

    // required for license check
    System.setProperty(args.getProgramName() + ".home", 
                       homeDir.getNativePath());

    EnvLoader.init();

    VfsOld.initJNI();

    homeDir = VfsOld.lookup(homeDir.getFullPath());

    // watchdog/0210
    // Vfs.setPwd(_rootDirectory);
    
    PathImpl configPath = args.getConfigPath();
    
    // baratine/1650 vs hudson/3000
    if (configPath == null) { //&& ! CurrentTime.isTest()) {
      configPath = args.getConfigPathDefault();
    }

    if (configPath != null && ! configPath.canRead()) {
      throw new ConfigException(L.l("{0}/{1} can't open configuration file '{2}'",
                                    args.getCommandName(),
                                    Version.getVersion(),
                                    configPath.getNativePath()));
    }
    
    Thread thread = Thread.currentThread();
    thread.setContextClassLoader(system.getClassLoader());
    
    RootConfigBoot rootConfig = new RootConfigBoot();
    
    rootConfig.setSkipLog(args.isSkipLog());
    
    rootConfig.setConfigTemplateImpl(args.getConfigPathTemplate());

    ServerELContext<?> elContext = null;//args.getELContext();

    /**
     * XXX: the following setVar calls should not be necessary, but the
     * EL.setEnviornment() call above is not effective:
     */
    //InjectManager cdiManager = InjectManager.create();

    ConfigContext config = createConfig();
    elContext.setProperties(config);
    
    ConfigContext.setProperty("rvar0", args.getServerId());
    
    ConfigContext.setProperty("server", new VarServerBoot(args));

    for (PathImpl path : args.getConfigPaths()) {
      configure(config, rootConfig, path);
    }

    //args.getProgram().configure(rootConfig);
    
    // read $HOME/.baratine.cf
    if (args.getUserProperties() != null && args.getUserProperties().canRead()) {
      configure(config, rootConfig, args.getUserProperties());
    }
    
    if (configPath != null) {
      configure(config, rootConfig, configPath);
    }

    PathImpl configTemplate = rootConfig.getConfigTemplate();

    if (configTemplate != null && ! configTemplate.getTail().equals("none")) {
      configure(config, rootConfig, configTemplate);
    }
    
    rootConfig.init();
    
    //args.postConfig(rootConfig);

    return new ConfigBoot(rootConfig);
  }
  
  protected ConfigContext createConfig()
  {
    //ConfigXml config = new ConfigXml();
    ConfigContext config = new ConfigContext();

    // CandiManager cdiManager = CandiManager.create();

    ConfigLibraryBaratine.configure();
      
    return config;
  }
  
  protected void configure(ConfigContext config, RootConfigBoot bean, PathImpl path)
  {
    if (path == null) {
      return;
    }

    if (log.isLoggable(Level.FINER)) {
      log.fine("CLI parsing " + path.getNativePath());
    }
    
    if (path.getTail().endsWith(".cf")) {
      config.configure2(bean, path);
    }
    else {
      throw new UnsupportedOperationException(path.getURL());
      /*
      bean.setConfigTemplate(null);
      
      config.configure(bean, path, 
                       "com/caucho/v5/server/resin/resin.rnc");
                       */
    }
  }
  
  public static class VarServerBoot {
    private ArgsDaemon _args;
    
    VarServerBoot(ArgsDaemon args)
    {
      _args = args;
    }
    
    public String getProgram()
    {
      return _args.getProgramName();
    }
  }
}
