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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 */

package com.caucho.config;

import java.lang.ref.SoftReference;

import java.lang.reflect.*;

import java.io.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;
import com.caucho.util.RegistryNode;
import com.caucho.util.CompileException;
import com.caucho.util.LineCompileException;

import com.caucho.log.Log;

import com.caucho.make.Dependency;

import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentBean;

import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Depend;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.IOExceptionWrapper;

import com.caucho.config.types.ResinType;

/**
 * MapBuilder populates the bean based on a map.
 */
public class MapBuilder {
  private final static L10N L = new L10N(MapBuilder.class);
  private final static Logger log = Log.open(MapBuilder.class);
  
  /**
   * Configures the bean from a path
   */
  public static Object configure(Object bean, Map<String,Object> map)
    throws ConfigException
  {
    return configure(bean, map, true);
  }
  
  /**
   * Configures the bean from a path
   */
  public static Object configureNoInit(Object bean, Map<String,Object> map)
    throws ConfigException
  {
    return configure(bean, map, false);
  }
  
  /**
   * The first cut should just set attributes based
   * on the RegistryNode, i.e. basically cut and paste
   * from BeanUtil.
   * 
   * @param bean NOT NULL
   * 
   * @todo How to handle &lt;foo/&gt;
   */
  public static Object configure(Object bean,
				 Map<String,Object> map,
				 boolean doInit)
    throws ConfigException
  {
    /*
    NodeBuilder oldBuilder = NodeBuilder.getCurrentBuilder();
    ClassLoader oldConfigLoader = null;
    try {
      if (oldBuilder == null)
      TypeBuilderFactory factory = TypeBuilderFactory.createFactory();
      oldConfigLoader = factory.getConfigVariableResolver().getConfigLoader();

      TypeBuilder builder = factory.getTypeBuilder(bean.getClass());

      return configure(builder, bean, map, doInit);
    } catch (ConfigException e) {
      throw e;
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new ConfigException(e);
    } finally {
      TypeBuilderFactory.getConfigVariableResolver().setConfigLoader(oldConfigLoader);

      TypeBuilderFactory.setFactory(oldFactory);
    }
    */
    throw new UnsupportedOperationException();
  }

  public static Object configure(TypeStrategy builder,
				 Object bean,
				 Map<String,Object> map,
				 boolean doInit)
    throws Throwable
  {
    /*
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    TypeBuilderFactory oldFactory = TypeBuilderFactory.getFactory();

    ClassLoader oldConfigLoader = null;

    try {
      TypeBuilderFactory factory = TypeBuilderFactory.createFactory();
      oldConfigLoader = factory.getConfigVariableResolver().getConfigLoader();

      // XXX: make common
      if (bean instanceof EnvironmentBean) {
	EnvironmentBean envBean = (EnvironmentBean) bean;

        ClassLoader beanLoader = envBean.getClassLoader();

        if (beanLoader != null) {
          thread.setContextClassLoader(beanLoader);
	  factory.getConfigVariableResolver().setConfigLoader(beanLoader);
	}
      }

      for (String key : map.keySet()) {
	builder.setValue(bean, key, map.get(key));
      }

      if (doInit) {
        builder.init(bean);

        return builder.replaceObject(bean);
      }
      else
        return bean;
    } finally {
      thread.setContextClassLoader(oldLoader);

      TypeBuilderFactory.getConfigVariableResolver().setConfigLoader(oldConfigLoader);
      TypeBuilderFactory.setFactory(oldFactory);
    }
    */

    throw new UnsupportedOperationException();
  }
}
