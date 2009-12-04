/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
import com.caucho.loader.EnvironmentLocal;
import com.caucho.repository.ModuleRepository;
import com.caucho.repository.Resolver;
import com.caucho.server.resin.Resin;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;

/**
 * The module repository holds the module jars for osgi and ivy.
 */
public class ModuleRepositoryImpl extends ModuleRepository
{
  private static final Logger log
    = Logger.getLogger(ModuleRepositoryImpl.class.getName());
  private static final L10N L = new L10N(ModuleRepositoryImpl.class);
    
  private Path _root;

  private ArrayList<JarsDirectory> _jarsList
    = new ArrayList<JarsDirectory>();

  /**
   * The module repository is created once by the Management class.
   */
  public ModuleRepositoryImpl()
  {
  }

  public void setPath(Path root)
  {
    _root = root;
  }

  public Path getRoot()
  {
    if (_root == null)
      _root = Resin.getCurrent().getResinDataDirectory().lookup("ivy");

    return _root;
  }

  public void addResolver(ResolverConfig resolverConfig)
  {
    addResolverImpl(resolverConfig.getResolver());
  }

  public void add(Resolver resolver)
  {
    addResolverImpl(resolver);
  }

  public JarsDirectory createJars()
  {
    return new JarsDirectory(this);
  }

  public void addJars(JarsDirectory jars)
  {
    _jarsList.add(jars);
  }

  @PostConstruct
  public void init()
  {
    for (JarsDirectory jars : _jarsList) {
      jars.update();
    }
  }
}
