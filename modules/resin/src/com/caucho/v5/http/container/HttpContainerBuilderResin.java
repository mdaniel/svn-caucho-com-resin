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

package com.caucho.v5.http.container;

import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.http.rewrite.DispatchRuleBase2;
import com.caucho.v5.http.rewrite.RewriteDispatch;
import com.caucho.v5.util.L10N;

/**
 * Configuration for the <cluster> and <server> tags.
 */
@Configurable
public class HttpContainerBuilderResin extends HttpContainerBuilderServlet
{
  private static final L10N L = new L10N(HttpContainerBuilderResin.class);

  // private PersistentStoreConfig _persistentStoreConfig;

  /**
   * Creates a new servlet server.
   */
  public HttpContainerBuilderResin(ServerBartender serverSelf,
                                   String serverHeader)
  {
    super(serverSelf, serverHeader);
  }
  
  /*
  @Override
  protected HttpContainerResin createHttpContainer()
  {
    return new HttpContainerResin(this);
  }
  
  @Override
  protected HttpContainerResin getHttpContainer()
  {
    return (HttpContainerResin) super.getHttpContainer();
  }
  */

  /**
   * Creates a persistent store instance.
   */
  /*
  @Configurable
  public PersistentStoreConfig createPersistentStore()
  {
    if (_persistentStoreConfig == null) {
      _persistentStoreConfig = new PersistentStoreConfig();
      //getHttpContainer().setPersistentStore(_persistentStoreConfig);
    }

    return _persistentStoreConfig;
  }
  */
  
  public void setPersistentStore(ConfigProgram program)
  {
    // _persistentStoreConfig = config;
  }
  
  /*
  public PersistentStoreConfig getPersistentStore()
  {
    return _persistentStoreConfig;
  }
  */

  /**
   * Creates a persistent store instance.
   */
  /*
  public PersistentStoreConfig getPersistentStoreConfig()
  {
    return _persistentStoreConfig;
  }
  */

  public void startPersistentStore()
  {
  }

  /**
   * Adds rewrite-dispatch.
   */
  @Configurable
  public RewriteDispatch createRewriteDispatch()
  {
    //return _httpContainer.createRewriteDispatch();
    return null;
  }
  
  @Configurable
  public void add(DispatchRuleBase2 rewrite)
  {
    // _httpContainer.add(rewrite);
  }

  /*
  public RewriteDispatch createRewriteDispatch()
  {
    return _hostContainer.createRewriteDispatch();
  }
  
  public void add(DispatchRule rewriteRule)
  {
    createRewriteDispatch().addRule(rewriteRule);
  }
  */

  /**
   * Adds an EarDefault
   */
  /*
  public void addEarDefault(EarConfig config)
  {
    // _hostContainer.addEarDefault(config);
  }
  */

  /**
   * Adds an EarDefault
   */
  /*
  @Configurable
  public void addEarDefault(EarConfig earConfig)
  {
    getHttpContainer().getHostContainer().addEarDefault(earConfig);
  }
  */

  @Configurable
  public Object createJdbcStore()
    throws ConfigException
  {
    return null;
  }
  
  @Override
  public HttpContainerResin build()
  {
    init();
    
    return new HttpContainerResin(this);
  }
}
