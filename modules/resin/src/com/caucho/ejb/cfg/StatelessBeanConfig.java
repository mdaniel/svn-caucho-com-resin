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

package com.caucho.ejb.cfg;

import java.util.*;
import java.util.logging.*;

import javax.annotation.*;
import javax.inject.manager.Bean;
import javax.jms.*;

import com.caucho.config.*;
import com.caucho.config.cfg.AbstractBeanConfig;
import com.caucho.config.inject.ComponentImpl;
import com.caucho.config.inject.CauchoBean;
import com.caucho.config.types.*;
import com.caucho.ejb.manager.*;

import com.caucho.util.*;

/**
 * ejb-stateless-bean configuration
 */
public class StatelessBeanConfig extends AbstractBeanConfig
{
  private static final L10N L = new L10N(StatelessBeanConfig.class);
  private static final Logger log
    = Logger.getLogger(StatelessBeanConfig.class.getName());

  private CauchoBean _bean;
  private EjbStatelessBean _ejbBean;

  public StatelessBeanConfig()
  {
  }

  public StatelessBeanConfig(CauchoBean beanConfig)
  {
    _bean = beanConfig;
    
    ComponentImpl comp = (ComponentImpl) beanConfig;

    setClass((Class) comp.getTargetType());

    // XXX:
    //if (beanConfig.getComponentType() != null)
    //  setComponentType(beanConfig.getComponentType());
    
    if (beanConfig.getName() != null)
      setName(beanConfig.getName());
    
    // XXX:
    // setScope(beanConfig.getScope());

    if (comp.getInit() != null)
      setInit(comp.getInit());
  }

  @PostConstruct
  public void init()
  {
    if (getInstanceClass() == null)
      throw new ConfigException(L.l("ejb-stateless-bean requires a 'class' attribute"));
    
    EjbContainer ejbContainer = EjbContainer.create();
    EjbConfigManager configManager = ejbContainer.getConfigManager();

    EjbStatelessBean bean = new EjbStatelessBean(configManager, "config");
    bean.setEJBClass(getInstanceClass());

    String name = getName();
    
    if (name == null)
      name = getJndiName();

    if (name == null)
      name = getInstanceClass().getSimpleName();

    bean.setEJBName(name);

    if (getInit() != null)
      bean.setInit(getInit());

    _ejbBean = bean;

    configManager.setBeanConfig(name, bean);

    // XXX: timing?
    // configManager.start();
  }

  public Bean getInjectBean()
  {
    return _bean;
  }
}

