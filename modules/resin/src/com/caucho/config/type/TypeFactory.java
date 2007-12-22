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

package com.caucho.config.type;

import com.caucho.config.*;
import com.caucho.config.attribute.*;
import com.caucho.config.types.InitProgram;
import com.caucho.config.types.RawString;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.xml.QName;

import org.w3c.dom.Node;

import java.io.InputStream;
import java.net.URL;
import java.lang.reflect.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Factory for returning type strategies.
 */
public class TypeFactory
{
  private static final Logger log
    = Logger.getLogger(TypeFactory.class.getName());
  private static L10N L = new L10N(TypeFactory.class);

  private static final HashMap<Class,ConfigType> _primitiveTypes
    = new HashMap<Class,ConfigType>();
  
  private static final EnvironmentLocal<TypeFactory> _localFactory
    = new EnvironmentLocal<TypeFactory>();
  
  private final HashMap<String,ConfigType> _typeMap
    = new HashMap<String,ConfigType>();

  private final HashMap<QName,Attribute> _flowMap
    = new HashMap<QName,Attribute>();

  private final HashMap<QName,Attribute> _envMap
    = new HashMap<QName,Attribute>();
  
  private TypeFactory()
  {
  }

  /**
   * Returns the appropriate strategy.
   */
  public static ConfigType getConfigType(Class type)
  {
    TypeFactory factory = getFactory(type.getClassLoader());

    return factory.getConfigTypeImpl(type);
  }
  
  public static Attribute getFlowAttribute(Class type, QName name)
    throws Exception
  {
    return getFactory(type.getClassLoader()).getFlowAttribute(name);
  }

  private Attribute getFlowAttribute(QName name)
  {
    return _flowMap.get(name);
  }

  public static Attribute getEnvironmentAttribute(Class type,
                                                          QName name) throws Exception
  {
    return getFactory(type.getClassLoader()).getEnvironmentAttribute(name);
  }

  private Attribute getEnvironmentAttribute(QName name)
  {
    Attribute strategy = _envMap.get(name);

    if (strategy != null)
      return strategy;

    if (name.getLocalName() != null && ! name.getLocalName().equals("")) {
      strategy = _envMap.get(new QName(name.getLocalName(), null));
      return strategy;
    }
    else
      return null;
  }

  private static TypeFactory getFactory(ClassLoader loader)
  {
    if (loader == null)
      loader = ClassLoader.getSystemClassLoader();

    TypeFactory factory = _localFactory.getLevel(loader);

    if (factory == null) {
      factory = new TypeFactory();
      _localFactory.set(factory, loader);
      factory.init(loader);
    }

    return factory;
  }

  /**
   * Initialize the type strategy factory with files in META-INF/caucho
   *
   * @param loader the owning class loader
   * @throws Exception
   */
  private void init(ClassLoader loader)
  {
    try {
      Enumeration<URL> urls = loader.getResources("META-INF/caucho/config-types.xml");

      while (urls.hasMoreElements()) {
	URL url = urls.nextElement();

	InputStream is = url.openStream();
	
	try {
	  new Config(loader).configure(this, is);
	} finally {
	  is.close();
	}
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Adds a new ConfigType
   */
  public void addConfigType(ConfigTypeConfig config)
  {
    _typeMap.put(config.getName(), config.getType());
  }

  /**
   * Adds an new environment attribute.
   */
  public FlowAttributeConfig createFlowAttribute()
  {
    return new FlowAttributeConfig();
  }

  /**
   * Adds an new environmnent attribute.
   */
  public EnvironmentAttributeConfig createEnvironmentAttribute()
  {
    return new EnvironmentAttributeConfig();
  }

  private ConfigType getConfigTypeImpl(Class type)
  {
    ConfigType strategy = _typeMap.get(type.getName());

    if (strategy == null) {
      strategy = _primitiveTypes.get(type.getName());

      if (strategy != null) {
      }
      else if (EnvironmentBean.class.isAssignableFrom(type))
        strategy = new EnvironmentBeanType(type);
      else
        strategy = new BeanType(type);

      _typeMap.put(type.getName(), strategy);
    }

    return strategy;
  }

  // configuration types
  public static class ConfigTypeConfig {
    private String _name;
    private ConfigType _type;

    public void setName(Class type)
    {
      _name = type.getName();
    }

    public String getName()
    {
      return _name;
    }

    public void setType(Class type)
            throws ConfigException, IllegalAccessException, InstantiationException
    {
      Config.validate(type, ConfigType.class);

      _type = (ConfigType) type.newInstance();
    }

    public ConfigType getType()
    {
      return _type;
    }
  }

  public class EnvironmentAttributeConfig {
    public void put(QName name, Class type)
    {
      ConfigType typeStrategy = getConfigTypeImpl(type);

      _envMap.put(name, new EnvironmentAttribute(typeStrategy));
    }
  }

  public class FlowAttributeConfig {
    public void put(QName name, Class type)
    {
      ConfigType typeStrategy = getConfigTypeImpl(type);

      _flowMap.put(name, new EnvironmentAttribute(typeStrategy));
    }
  }

  static {
    _primitiveTypes.put(boolean.class, BooleanType.TYPE);
    _primitiveTypes.put(byte.class, ByteType.TYPE);
    _primitiveTypes.put(short.class, ShortType.TYPE);
    _primitiveTypes.put(int.class, IntegerType.TYPE);
    _primitiveTypes.put(long.class, LongType.TYPE);
    _primitiveTypes.put(float.class, FloatType.TYPE);
    _primitiveTypes.put(double.class, DoubleType.TYPE);
    //_primitiveTypes.put(char.class, CharType.TYPE);

    _primitiveTypes.put(Boolean.class, BooleanType.TYPE);
    _primitiveTypes.put(Byte.class, ByteType.TYPE);
    _primitiveTypes.put(Short.class, ShortType.TYPE);
    _primitiveTypes.put(Integer.class, IntegerType.TYPE);
    _primitiveTypes.put(Long.class, LongType.TYPE);
    _primitiveTypes.put(Float.class, FloatType.TYPE);
    _primitiveTypes.put(Double.class, DoubleType.TYPE);
    //_primitiveTypes.put("java.lang.Character", new CharacterType());

    _primitiveTypes.put(Object.class, ObjectType.TYPE);
    _primitiveTypes.put(String.class, StringType.TYPE);
    /*
    _primitiveTypes.put("com.caucho.config.types.RawString",
                        new RawStringType());
    _primitiveTypes.put("org.w3c.dom.Node", new NodeType());
    _primitiveTypes.put("java.lang.Class", new ClassType());

    _primitiveTypes.put("com.caucho.config.types.InitProgram",
                        new InitProgramType());
    _primitiveTypes.put("com.caucho.config.BuilderProgram",
                        new BuilderProgramType());
    */
  }
}
