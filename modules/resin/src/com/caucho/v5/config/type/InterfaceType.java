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

package com.caucho.v5.config.type;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Names;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.custom.ConfigInterface;
import com.caucho.v5.config.util.JndiUtil;
import com.caucho.v5.inject.InjectManagerAmp;
import com.caucho.v5.util.L10N;

/**
 * Represents an interface.  The interface will try to lookup the
 * value in cdi.
 */
public class InterfaceType<T> extends ConfigType<T>
{
  private static final L10N L = new L10N(InterfaceType.class);

  private final Class<T> _type;

  /**
   * Create the interface type
   */
  public InterfaceType(TypeFactoryConfig typeFactory, Class<T> type)
  {
    super(typeFactory);
    
    _type = type;
  }

  /**
   * Returns the Java type.
   */
  @Override
  public Class<T> getType()
  {
    return _type;
  }

  /**
   * Returns an InterfaceConfig object
   */
  @Override
  public Object create(Object parent, NameCfg name)
  {
    ConfigInterface cfg = new ConfigInterface(_type, _type.getSimpleName());

    return cfg;
  }

  /**
   * Replace the type with the generated object
   */
  public void init(Object bean)
  {
    if (bean instanceof ConfigInterface)
      ((ConfigInterface) bean).init();
    else
      super.init(bean);
  }

  /**
   * Replace the type with the generated object
   */
  public Object replaceObject(Object bean)
  {
    /*
    if (bean instanceof ConfigInterface)
      return ((ConfigInterface) bean).replaceObject();
    else
      return bean;
      */
    
    return bean;
  }

  /**
   * Converts the string to a value of the type.
   */
  @Override
  public Object valueOf(String text)
  {
    if (text == null) {
      return null;
    }
    
    Object value;

    InjectManagerAmp inject = InjectManagerAmp.current();
    
    if (! text.equals("")) {
      value = inject.lookup(_type, Names.create(text));
    }
    else {
      value = inject.lookup(_type);
    }
    
    /*
    CandiManager beanManager = CandiManager.create();


    Set<Bean<?>> beans;

    if (! text.equals(""))
      beans = beanManager.getBeans(_type, Names.create(text));
    else
      beans = beanManager.getBeans(_type);

    if (beans.iterator().hasNext()) {
      Bean bean = beanManager.resolve(beans);

      CreationalContext<?> env = beanManager.createCreationalContext(bean);

      value = beanManager.getReference(bean, _type, env);

      return value;
    }
    */

    value = lookupJndi(text);

    if (value != null) {
      return value;
    }

    throw new ConfigException(L.l("{0}: '{1}' is an unknown bean.",
                                  _type.getName(), text));
  }
  
  protected Object lookupJndi(String name)
  {
    return JndiUtil.lookup(name);
  }

  /**
   * Converts the value to a value of the type.
   */
  public Object valueOf(Object value)
  {
    if (value == null)
      return null;
    else if (value instanceof String)
      return valueOf((String) value);
    else if (_type.isAssignableFrom(value.getClass()))
      return value;
    else
      throw new ConfigException(L.l("{0}: '{1}' is an invalid value.",
                                    _type.getName(), value));
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _type.getName() + "]";
  }
}
