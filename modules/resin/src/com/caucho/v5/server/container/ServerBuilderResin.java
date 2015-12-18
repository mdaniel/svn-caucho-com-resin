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

package com.caucho.v5.server.container;

import java.io.IOException;

import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.cli.resin.BootConfigParserResin;
import com.caucho.v5.cli.server.BootConfigParser;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.http.container.HttpContainerBuilder;
import com.caucho.v5.http.container.HttpContainerBuilderResin;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.nautilus.impl.NautilusSystem;
import com.caucho.v5.server.cdi.ResinServerConfigLibrary;
import com.caucho.v5.server.config.RootConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.server.resin.Resin;
import com.caucho.v5.server.resin.ServerBaseConfigResin;

/**
 * The Resin class represents the top-level container for Resin.
 * It exactly matches the &lt;resin> tag in the resin.xml
 */
public class ServerBuilderResin extends ServerBuilder
{
  /**
   * Creates a new resin server.
   */
  public ServerBuilderResin(ArgsServerBase args, ServerConfigBoot serverConfig)
  {
    super(args, serverConfig);
  }
  
  /**
   * Creates a new resin server.
   */
  public ServerBuilderResin(String []argv)
  {
    super(argv);
  }
  
  /**
   * Creates a new resin server.
   */
  public ServerBuilderResin(ArgsServerBase args)
  {
    super(args);
  }

  /**
   * Creates a new Resin instance
   */
  public static ServerBuilder create(ArgsServerBase args,
                                     ServerConfigBoot serverConfig)
  {
    return new ServerBuilderResin(args, serverConfig);
  }
  
  @Override
  protected BootConfigParser createConfigParser()
  {
    return new BootConfigParserResin();
  }
 
  /**
   * Configures the selected server from the boot config.
   */
  @Override
  protected void initHttpSystem(SystemManager system,
                                         ServerBartender selfServer)
    throws IOException
  {
    ServerBaseConfigResin resinConfig = new ServerBaseConfigResin(this);

    RootConfigBoot rootConfig = getRootConfig();

    rootConfig.getProgram().configure(resinConfig);
    
    super.initHttpSystem(system, selfServer);
  }


  @Override
  public Resin build(SystemManager systemManager,
                     ServerBartender serverSelf)
    throws Exception
  {
    return new Resin(this, systemManager, serverSelf);
  }
  
  @Override
  protected ServerBartender initNetwork()
      throws Exception
  {
    ServerBartender serverSelf = super.initNetwork();

    // LoadBalanceSystem.createAndAddService(new LoadBalanceFactory());
    NautilusSystem.createAndAddSystem();
   
    return serverSelf;
  }

  @Override
  protected void initCdiEnvironment()
  {
    super.initCdiEnvironment();
    
    /*
    //CandiManager cdiManager = CandiManager.create();

    if (cdiManager.getBeans(CdiProducerResin.class).size() == 0) {
      Config.setProperty("fmt", new FmtFunctions());

      ResinConfigLibrary.configure();
      //ResinServerConfigLibrary.configure(cdiManager);

      try {
        Method method = JndiUtil.class.getMethod("lookup", new Class[] { String.class });
        Config.setProperty("jndi", method);
        Config.setProperty("jndi:lookup", method);
      } catch (Exception e) {
        throw ConfigException.create(e);
      }

      cdiManager.addManagedBeanDiscover(cdiManager.createManagedBean(CdiProducerResin.class));
      Class<?> resinValidatorClass = CdiProducerResin.createResinValidatorProducer();

      if (resinValidatorClass != null)
        cdiManager.addManagedBeanDiscover(cdiManager.createManagedBean(resinValidatorClass));

      cdiManager.update();
    }
      */

    ResinServerConfigLibrary.configure();
  }

  @Override
  protected HttpContainerBuilder createHttpBuilder(ServerBartender selfServer,
                                                   String serverHeader)
  {
    HttpContainerBuilderResin builder
      = new HttpContainerBuilderResin(selfServer, serverHeader);
    
    return builder;
  }
}
