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

package com.caucho.v5.config.core;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.type.EnvBean;
import com.caucho.v5.config.type.FlowBean;
import com.caucho.v5.naming.JndiUtil;

/**
 * Sets an EL value.
 */
public class ResinSet implements EnvBean, FlowBean {
  private String _jndiName;
  private String _var;
  
  private Object _value;
  
  private Object _default;
  

  /**
   * The EL name to be set.
   */
  public void setVar(String name)
  {
    _var = name;
  }

  /**
   * The JNDI name to be set.
   */
  public void setJndiName(String name)
  {
    _jndiName = name;
  }

  /**
   * The EL value to be set.
   */
  public void setValue(Object value)
  {
    _value = value;
  }

  /**
   * The EL default value to be set.
   */
  public void setDefault(Object value)
  {
    _default = value;
  }

  /**
   * The EL value to be set.
   */
  public void setProperty(String name, Object value)
  {
    //BeanFactory factory = webBeans.createBeanFactory(value.getClass());
    //factory.name(name);
    //factory.type();

    // webBeans.addBean(factory.singleton(value));

    ConfigContext.setProperty(name, value);
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    if (_jndiName != null)
      JndiUtil.rebindDeepShort(_jndiName, _value);
    
    if (_var != null) {
      // InjectManager webBeans = InjectManager.create();

      if (_value != null) {
        ConfigContext.setProperty(_var, _value);
      }
      else if (_default != null) {
        if (ConfigContext.getProperty(_var) == null)
          ConfigContext.setProperty(_var, _default);
      }
      
      /*
      if (_value != null) {
        BeanFactory factory = webBeans.createBeanFactory(_value.getClass());
        factory.name(_var);
        factory.type();

        webBeans.addBean(factory.singleton(_value));
      }
      else if (_default != null && webBeans.findByName(_var) == null) {
        BeanFactory factory = webBeans.createBeanFactory(_default.getClass());
        factory.name(_var);
        factory.type();

        webBeans.addBean(factory.singleton(_default));
      }
      */
    }
  }
  
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append(getClass().getSimpleName()).append("[");
    
    if (_var != null)
      sb.append(_var);
    
    if (_jndiName != null)
      sb.append(",jndi=").append(_jndiName);
    
    sb.append("]");
    
    return sb.toString();
  }
}

