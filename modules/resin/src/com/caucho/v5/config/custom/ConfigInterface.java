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

package com.caucho.v5.config.custom;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.util.L10N;

/**
 * Configures an interface type.  Allows class and uri syntax
 */
public class ConfigInterface extends CustomBean
{
  private static final L10N L = new L10N(ConfigInterface.class);

  private boolean _isDeploy;
  private boolean _isFactory = true;

  private String _tagName = "bean";

  private String _valueName;
  private Object _value;
  
  public ConfigInterface()
  {
    super(null, null, null); // XXX:
  }
  
  public ConfigInterface(NameCfg name, Class<?> type, Object parent)
  {
    super(name, type, parent);
    
    System.out.println("CIT: " + type);
    Thread.dumpStack();
  }
  
  public ConfigInterface(Class<?> type)
  {
    super(null, type, null);
    
    // XXX: setBeanConfigClass(type);
  }
  
  public ConfigInterface(Class<?> type, String tagName)
  {
    this(type);

    //setTagName(tagName);
  }

  /*
  @Override
  protected String getDefaultScope()
  {
    return null;
  }
  */
  
  /**
   * Override init to handle value
   */
  @Override
  @PostConstruct
  public void init()
  {
    /*
    if (_valueName != null) {
      InjectManager cdiManager = InjectManager.create();

      Set<Bean<?>> beans = cdiManager.getBeans(_valueName);

      if (beans.size() > 0) {
        _bean = beans.iterator().next();
      }

      if (_bean == null) {
        _value = Jndi.lookup(_valueName);
      }

      if (_bean == null && _value == null)
        throw new ConfigException(L.l("'{0}' is an unknown bean",
                                      _valueName));
    }
    else if (getClassType() != null)
      super.init();
    else {
      // ioc/2130
    }
    */
  }
  

  /*
  @Override
  public void deploy()
  {
    if (_isDeploy)
      super.deploy();
  }

  public Object getObject()
  {
    if (_value != null)
      return _value;
    else if (getClassType() != null)
      return super.getObject();
    else if (getBeanConfigClass().isAssignableFrom(String.class))
      return _valueName;
    else
      return null;
  }

  public Object getObjectNoInit()
  {
    if (_value != null)
      return _value;
    else if (getClassType() != null)
      return super.createObjectNoInit();
    else if (getBeanConfigClass().isAssignableFrom(String.class))
      return _valueName;
    else
      return null;
  }
  */

  /**
   * Returns the configured object for configuration
   */
  /*
  public Object replaceObject()
  {
    if (_isFactory)
      return getObject();
    else
      return this;
  }
  */

  /**
   * Returns the configured object for configuration
   */
  /*
  public Object replaceObjectNoInit()
  {
    if (_isFactory)
      return getObjectNoInit();
    else
      return this;
  }
  */

  /*
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getBeanConfigClass().getName() + "]";
  }
  */
}

