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

package com.caucho.v5.resin;

import javax.enterprise.context.ApplicationScoped;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.config.program.PropertyValueProgram;
import com.caucho.v5.inject.InjectManager;
import com.caucho.v5.util.L10N;

/**
 * Embeddable version of a singleton bean
 *
 * <code><pre>
 * ResinEmbed resin = new ResinEmbed();
 *
 * WebAppEmbed webApp = new WebAppEmbed("/foo", "/var/resin/foo");
 *
 * MyBean myBean = new MyBean();
 *
 * BeanEmbed bean = new BeanEmbed(myBean);
 *
 * webApp.addBean(bean);
 *
 * resin.addWebApp(webApp);
 * </pre></code>
 */
public class BeanEmbed
{
  private static final L10N L = new L10N(BeanEmbed.class);
  
  private Object _value;

  private String _className;
  private String _name;
  private ContainerProgram _init = new ContainerProgram();

  /**
   * Creates a new embedded bean
   */
  public BeanEmbed()
  {
  }

  /**
   * Creates a new embedded bean with a singleton value
   */
  public BeanEmbed(Object value)
  {
    setValue(value);
  }

  /**
   * Creates a new embedded bean with a singleton value
   */
  public BeanEmbed(Object value, String name)
  {
    setValue(value);
    setName(name);
  }

  /**
   * Creates a new embedded bean with a given classname
   */
  public BeanEmbed(String className)
  {
    setClass(className);
  }

  /**
   * Creates a new embedded bean with a given classname and name
   */
  public BeanEmbed(String className, String name)
  {
    setClass(className);
    setName(name);
  }

  /**
   * Sets the bean's classname
   */
  public void setClass(String className)
  {
    _className = className;
  }

  /**
   * Sets the bean's name
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Sets the bean value.
   */
  public void setValue(Object value)
  {
    _value = value;
  }

  /**
   * Adds a property.
   */
  public void addProperty(String name, Object value)
  {
    _init.addProgram(new PropertyValueProgram(name, value));
  }

  /**
   * validates the bean
   */
  public void validate()
  {
    if (_value != null)
      return;
  }

  /**
   * Configures the bean (for internal use)
   */
  protected void configure()
  {
    try {
      InjectManager cdiManager = InjectManager.create();
      
      /*
      if (_value != null) {
        BeanBuilder<?> factory = cdiManager.createBeanBuilder(_value.getClass());

        if (_name != null)
          factory.name(_name);

        cdiManager.addBeanDiscover(factory.singleton(_value));
      }
      else if (_className == null)
        throw new ConfigException(L.l("BeanEmbed must either have a value or a class"));
      else {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class<?> cl = Class.forName(_className, false, loader);

        BeanBuilder<?> factory = cdiManager.createBeanBuilder(cl);

        factory.scope(ApplicationScoped.class);

        if (_name != null)
          factory.name(_name);

        if (_init != null)
          factory.init(_init);

        cdiManager.addBeanDiscover(factory.bean());
      }
      */
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
