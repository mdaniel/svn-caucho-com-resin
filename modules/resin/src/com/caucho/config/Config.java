/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config;

import java.io.InputStream;

import java.util.Map;

import java.util.logging.Logger;

import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import com.caucho.vfs.Path;
import com.caucho.xml.QName;
import com.caucho.el.EL;

/**
 * Facade for Resin's configuration builder.
 */
public class Config {
  private static final L10N L = new L10N(Config.class);
  private static final Logger log = Log.open(Config.class);
  
  /**
   * Returns true if the class can be instantiated.
   */
  public static void checkCanInstantiate(Class beanClass)
    throws ConfigException
  {
    if (beanClass == null)
      throw new ConfigException(L.l("null classes can't be instantiated."));
    else if (beanClass.isInterface())
      throw new ConfigException(L.l("`{0}' must be a concrete class.  Interfaces cannot be instantiated.", beanClass.getName()));
    else if (! Modifier.isPublic(beanClass.getModifiers()))
      throw new ConfigException(L.l("Custom bean class `{0}' is not public.  Bean classes must be public, concrete, and have a zero-argument constructor.", beanClass.getName()));
    else if (Modifier.isAbstract(beanClass.getModifiers()))
      throw new ConfigException(L.l("Custom bean class `{0}' is abstract.  Bean classes must be public, concrete, and have a zero-argument constructor.", beanClass.getName()));

    Constructor []constructors = beanClass.getDeclaredConstructors();

    Constructor constructor = null;

    for (int i = 0; i < constructors.length; i++) {
      if (constructors[i].getParameterTypes().length == 0) {
        constructor = constructors[i];
        break;
      }
    }

    if (constructor == null)
      throw new ConfigException(L.l("Custom bean class `{0}' doesn't have a zero-arg constructor.  Bean classes must be have a zero-argument constructor.", beanClass.getName()));

    if (! Modifier.isPublic(constructor.getModifiers())) {
      throw new ConfigException(L.l("The zero-argument constructor for `{0}' isn't public.  Bean classes must have a public zero-argument constructor.", beanClass.getName()));
    }
  }
  
  /**
   * Returns true if the class can be instantiated.
   */
  public static void validate(Class cl, Class api)
    throws ConfigException
  {
    checkCanInstantiate(cl);

    if (! api.isAssignableFrom(cl)) {
      throw new ConfigException(L.l("{0} must implement {1}.",
				    cl.getName(), api.getName()));
    }
  }

  /**
   * Sets a  attribute with a value.
   */
  public static void setAttribute(Object obj, String attr, Object value)
    throws Exception
  {
    TypeStrategy strategy = TypeStrategyFactory.getTypeStrategy(obj.getClass());

    QName attrName = new QName(attr);
    AttributeStrategy attrStrategy = strategy.getAttributeStrategy(attrName);
    attrStrategy.setAttribute(obj, attrName, value);
  }

  public static void init(Object bean) throws Exception
  {
    TypeStrategy strategy = TypeStrategyFactory.getTypeStrategy(bean.getClass());

    strategy.init(bean);
  }

  public static Object replaceObject(Object bean) throws Exception
  {
    TypeStrategy strategy = TypeStrategyFactory.getTypeStrategy(bean.getClass());

    return strategy.replaceObject(bean);
  }

  /**
   * Configures a bean with a configuration file.
   */
  public static Object configure(Object obj, Path path)
    throws Exception
  {
    return new NodeBuilder().configure(obj, path);
  }

  /**
   * Configures a bean with a configuration file.
   */
  public static Object configure(Object obj, InputStream is)
    throws Exception
  {
    return new NodeBuilder().configure(obj, is);
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public static Object configure(Object obj, Path path, String schema)
    throws Exception
  {
    NodeBuilder builder = new NodeBuilder();

    builder.setCompactSchema(schema);

    return builder.configure(obj, path);
  }

  /**
   * Configures a bean with a configuration file.
   */
  public static Object configure(Object obj, InputStream is, String schema)
    throws Exception
  {
    NodeBuilder builder = new NodeBuilder();

    builder.setCompactSchema(schema);

    return builder.configure(obj, is);
  }

  /**
   * Configures a bean with a configuration map.
   */
  public static Object configure(Object obj, Map<String,Object> map)
    throws Exception
  {
    return new MapBuilder().configure(obj, map);
  }


  /**
   * Returns the variable resolver.
   */
  public static VariableResolver getEnvironment()
  {
    NodeBuilder builder = NodeBuilder.getCurrentBuilder();

    if (builder != null) {
      return builder.getConfigVariableResolver();
    }
    else
      return EL.getEnvironment();
  }

  /**
   * Returns the variable resolver.
   */
  public static VariableResolver getVariableResolver()
  {
    NodeBuilder builder = NodeBuilder.getCurrentBuilder();

    if (builder != null) {
      return builder.getConfigVariableResolver();
    }
    else
      return null;
  }

  /**
   * Sets an EL configuration variable.
   */
  public static void setVar(String var, Object value)
  {
    NodeBuilder builder = NodeBuilder.getCurrentBuilder();

    if (builder != null) {
      builder.putVar(var, value);
    }
  }

  /**
   * Gets an EL configuration variable.
   */
  public static Object getVar(String var) throws ELException
  {
    return getEnvironment().resolveVariable(var);
  }

  /**
   * Evaluates an EL string in the context.
   */
  public static String evalString(String str)
         throws ELException
  {
    return AttributeStrategy.evalString(str);
  }
}

