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

package com.caucho.ejb.metadata;

import com.caucho.loader.EnvironmentBean;
import com.caucho.config.ConfigException;
import com.caucho.config.program.ContainerProgram;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Configuration for a new bean based on metadata.
 */
public class Bean implements EnvironmentBean {
  private static final L10N L = new L10N(Bean.class);
  private static final Logger log = Log.open(Bean.class);

  private EjbContainer _ejbContainer;

  private ClassLoader _tempClassLoader;

  private Class _type;
  private String _name;

  private ArrayList<ContainerProgram> _initList = new ArrayList<ContainerProgram>();

  public Bean(EjbContainer ejbContainer)
  {
    _ejbContainer = ejbContainer;
    
    _tempClassLoader = ejbContainer.getIntrospectionClassLoader();
  }

  public ClassLoader getClassLoader()
  {
    return _tempClassLoader;
  }

  protected String getEJBModuleName()
  {
    // XXX: s/b what?
    return "introspected";
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Sets the type.
   */
  public void setType(Class type)
    throws ConfigException
  {
    _type = type;
    _ejbContainer.getConfigManager().addIntrospectableClass(_type.getName());
  }

  /**
   * Adds an init.
   */
  public void addInit(ContainerProgram init)
  {
    _initList.add(init);
  }

  /**
   * Initializes the bean.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_type == null)
      throw new ConfigException(L.l("type is a required attribute of <bean>"));
  }
}

