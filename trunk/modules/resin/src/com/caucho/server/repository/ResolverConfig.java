/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.server.repository;

import com.caucho.config.ConfigException;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.cfg.BeanConfig;
import com.caucho.config.type.TypeFactory;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.ivy.IvyPattern;
import com.caucho.util.L10N;
import com.caucho.repository.Resolver;
import com.caucho.server.resin.Resin;
import com.caucho.vfs.Path;

import java.util.ArrayList;
import javax.annotation.PostConstruct;

/**
 * The configuration for a resolver
 */
public class ResolverConfig
{
  private static final L10N L = new L10N(ResolverConfig.class);

  private BeanConfig _beanConfig;
  private Resolver _resolver;
  
  private IvyPattern _artifactPattern;
  private IvyPattern _ivyPattern;

  public ResolverConfig()
  {
    _beanConfig = new BeanConfig();
    _beanConfig.setBeanConfigClass(Resolver.class);
  }

  public void setUri(String uri)
  {
    _beanConfig.setUri(uri);
    /*
    TypeFactory factory = TypeFactory.create();
    
    Class cl = factory.getDriverClassByUrl(Resolver.class, uri);

    try {
      _resolver = (Resolver) cl.newInstance();

      ContainerProgram program = factory.getUrlProgram(uri);

      if (program != null)
	program.configure(_resolver);
    } catch (Exception e) {
      throw new ConfigException(L.l("<resolver> can't instantiate '{0}'",
				    cl.getName()),
				e);
    }
    */
  }

  public void setArtifactPattern(String pattern)
  {
    _artifactPattern = new IvyPattern(pattern);
    _beanConfig.addProperty("artifact-pattern", _artifactPattern);
  }

  public void setIvyPattern(String pattern)
  {
    _ivyPattern = new IvyPattern(pattern);
    _beanConfig.addProperty("ivy-pattern", _ivyPattern);
  }
  
  @PostConstruct
  public void init()
  {
    _beanConfig.init();

    _resolver = (Resolver) _beanConfig.getObject();

    if (_resolver == null)
      throw new ConfigException(L.l("resolver is undefined"));
  }

  Resolver getResolver()
  {
    return _resolver;
  }
}
