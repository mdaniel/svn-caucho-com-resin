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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.types;

import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.el.Expr;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.enterprise.context.spi.CreationalContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;

/**
 * Configuration for the resource group
 */
abstract public class ResourceGroupConfig extends ConfigProgram {
  private static final L10N L = new L10N(ResourceGroupConfig.class);
  private static final Logger log
    = Logger.getLogger(ResourceGroupConfig.class.getName());

  private String _location = "";

  private String _description;

  private String _defaultInjectionClass;

  private ArrayList<InjectionTarget> _injectionTargets
    = new ArrayList<InjectionTarget>();

  public ResourceGroupConfig()
  {
  }

  public void setDefaultInjectionClass(String className)
  {
    _defaultInjectionClass = className;
  }

  public void setId(String id)
  {
  }

  /**
   * Sets the configuration location.
   */
  public void setConfigLocation(String filename, int line)
  {
    _location = filename + ":" + line + " ";
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Adds an injection-target
   */
  public void addInjectionTarget(InjectionTarget injectionTarget)
  {
    _injectionTargets.add(injectionTarget);
  }

  /**
   * Registers any injection targets
   */
  @PostConstruct
  public void init()
    throws Exception
  {
    for (InjectionTarget target : _injectionTargets) {
    }
  }
  
  /**
   * Configures the bean using the current program.
   * 
   * @param bean the bean to configure
   * @param env the Config environment
   */
  public <T> void inject(T bean, CreationalContext<T> env)
  {
  }

  protected ConfigException error(String msg)
  {
    if (_location != null)
      return new LineConfigException(_location + msg);
    else
      return new ConfigException(msg);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _location + "]";
  }
}

