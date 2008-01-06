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
import com.caucho.loader.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.QName;

import org.w3c.dom.Node;

import java.beans.*;
import java.io.InputStream;
import java.net.URL;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.logging.Logger;

import javax.annotation.*;
import javax.sql.*;

/**
 * Factory for returning type strategies.
 */
public class TypeFactory implements AddLoaderListener
{
  private static final Logger log
    = Logger.getLogger(TypeFactory.class.getName());
  private static L10N L = new L10N(TypeFactory.class);

  private static final HashMap<Class,ConfigType> _primitiveTypes
    = new HashMap<Class,ConfigType>();
  
  private static final EnvironmentLocal<TypeFactory> _localFactory
    = new EnvironmentLocal<TypeFactory>();

  private final EnvironmentClassLoader _loader;
  private final TypeFactory _parent;

  private final HashSet<URL> _configSet
    = new HashSet<URL>();
  
  private final HashMap<String,ConfigType> _typeMap
    = new HashMap<String,ConfigType>();

  private final HashMap<QName,ConfigType> _attrMap
    = new HashMap<QName,ConfigType>();

  private final HashMap<QName,Attribute> _listAttrMap
    = new HashMap<QName,Attribute>();

  private final HashMap<QName,Attribute> _setAttrMap
    = new HashMap<QName,Attribute>();

  private final HashMap<QName,Attribute> _envAttrMap
    = new HashMap<QName,Attribute>();
  
  private TypeFactory(ClassLoader loader)
  {
    _loader = Environment.getEnvironmentClassLoader(loader);

    if (_loader != null) {
      _parent = getFactory(_loader.getParent());

      _loader.addLoaderListener(this);
    }
    else
      _parent = null;


  }

  /**
   * Returns the appropriate strategy.
   */
  public static ConfigType getType(Class type)
  {
    TypeFactory factory = getFactory(type.getClassLoader());

    return factory.getConfigTypeImpl(type);
  }

  public static TypeFactory getFactory()
  {
    return getFactory(Thread.currentThread().getContextClassLoader());
  }

  public static TypeFactory getFactory(ClassLoader loader)
  {
    if (loader == null)
      loader = ClassLoader.getSystemClassLoader();

    TypeFactory factory = _localFactory.getLevel(loader);

    if (factory == null) {
      factory = new TypeFactory(loader);
      _localFactory.set(factory, loader);
      factory.init(loader);
    }

    return factory;
  }

  /**
   * Returns an environment type.
   */
  public ConfigType getEnvironmentType(QName name)
  {
    synchronized (_attrMap) {
      ConfigType type = _attrMap.get(name);

      if (type != null)
	return type;

      if (_parent != null)
	type = _parent.getEnvironmentType(name);

      if (type != null) {
	_attrMap.put(name, type);
	
	return type;
      }

      QName baseName = new QName(name.getLocalName());

      type = _attrMap.get(baseName);

      if (type != null) {
	_attrMap.put(name, type);
	
	return type;
      }

      return null;
    }
  }

  /**
   * Returns an environment type.
   */
  public Attribute getListAttribute(QName name)
  {
    synchronized (_listAttrMap) {
      Attribute attr = _listAttrMap.get(name);

      if (attr != null)
	return attr;

      ConfigType type = getEnvironmentType(name);

      if (type == null)
	return null;

      attr = new ListValueAttribute(type);

      _listAttrMap.put(name, attr);

      return attr;
    }
  }

  /**
   * Returns an environment type.
   */
  public Attribute getSetAttribute(QName name)
  {
    synchronized (_setAttrMap) {
      Attribute attr = _setAttrMap.get(name);

      if (attr != null)
	return attr;

      ConfigType type = getEnvironmentType(name);

      if (type == null)
	return null;

      attr = new SetValueAttribute(type);

      _setAttrMap.put(name, attr);

      return attr;
    }
  }

  /**
   * Returns an environment type.
   */
  public Attribute getEnvironmentAttribute(QName name)
  {
    synchronized (_envAttrMap) {
      Attribute attr = _envAttrMap.get(name);

      if (attr != null)
	return attr;

      ConfigType type = getEnvironmentType(name);

      if (type == null)
	return null;

      attr = new EnvironmentAttribute(type);

      _envAttrMap.put(name, attr);

      return attr;
    }
  }

  private ConfigType getConfigTypeImpl(Class type)
  {
    ConfigType strategy = _typeMap.get(type.getName());

    if (strategy == null) {
      strategy = _primitiveTypes.get(type);

      if (strategy == null)
	strategy = createType(type);

      _typeMap.put(type.getName(), strategy);

      strategy.introspect();
    }

    return strategy;
  }

  private ConfigType createType(Class type)
  {
    PropertyEditor editor = null;

    if (ConfigType.class.isAssignableFrom(type)) {
      try {
	return (ConfigType) type.newInstance();
      } catch (Exception e) {
	throw ConfigException.create(e);
      }
    }
    else if ((editor = PropertyEditorManager.findEditor(type)) != null)
      return new PropertyEditorType(type, editor);
    else if (type.getEnumConstants() != null)
      return new EnumType(type);
    else if (Set.class.isAssignableFrom(type))
      return new SetType(type);
    else if (Collection.class.isAssignableFrom(type))
      return new ListType(type);
    else if (Map.class.isAssignableFrom(type))
      return new MapType(type);
    else if (EnvironmentBean.class.isAssignableFrom(type))
      return new EnvironmentBeanType(type);
    else if (type.isArray()) {
      Class compType = type.getComponentType();
      
      return new ArrayType(getType(compType), compType);
    }
    else if (type.isInterface())
      return new InterfaceType(type);
    else
      return new BeanType(type);
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
      Enumeration<URL> urls
	= loader.getResources("META-INF/services/com.caucho.config/default.xml");

      while (urls.hasMoreElements()) {
	URL url = urls.nextElement();

	if (hasConfig(url))
	  continue;

	_configSet.add(url);

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

  protected boolean hasConfig(URL url)
  {
    if (_configSet.contains(url))
      return true;
    else if (_parent != null)
      return _parent.hasConfig(url);
    else
      return false;
  }

  //
  // AddLoaderListener
  //
  
  /**
   * Called with the loader config changes.
   */
  public void addLoader(EnvironmentClassLoader loader)
  {
    init(loader);
  }

  //
  // Configuration methods
  //

  /**
   * Adds an new environment attribute.
   */
  public NamespaceConfig createNamespace()
  {
    return new NamespaceConfig();
  }

  // configuration types
  public class NamespaceConfig {
    private String _ns = "";
    private boolean _isDefault;

    public void setName(String ns)
    {
      _ns = ns;
    }

    public String getName()
    {
      return _ns;
    }

    public void setDefault(boolean isDefault)
    {
      _isDefault = isDefault;
    }

    public boolean isDefault()
    {
      return _isDefault;
    }

    public BeanConfig createBean()
    {
      return new BeanConfig(_ns, _isDefault);
    }
  }

  public class BeanConfig {
    private String _ns;
    private boolean _isDefault;

    private String _name;
    private Class _type;

    BeanConfig(String ns, boolean isDefault)
    {
      _ns = ns;
      _isDefault = isDefault;
    }

    public void setName(String name)
    {
      _name = name;
    }

    public void setClass(Class type)
    {
      _type = type;
      Config.checkCanInstantiate(type);
    }

    @PostConstruct
    public void init()
    {
      if (_name == null)
	throw new ConfigException(L.l("bean requires a 'name' attribute"));
      
      if (_type == null)
	throw new ConfigException(L.l("bean requires a 'class' attribute"));

      QName qName = new QName(null, _name, _ns);

      ConfigType type = createType(_type);

      type.introspect();

      _attrMap.put(qName, type);

      if (_isDefault) {
	qName = new QName(_name);
	_attrMap.put(qName, type);
      }
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _loader + "]";
  }

  static {
    _primitiveTypes.put(boolean.class, BooleanPrimitiveType.TYPE);
    _primitiveTypes.put(byte.class, BytePrimitiveType.TYPE);
    _primitiveTypes.put(short.class, ShortPrimitiveType.TYPE);
    _primitiveTypes.put(int.class, IntegerPrimitiveType.TYPE);
    _primitiveTypes.put(long.class, LongPrimitiveType.TYPE);
    _primitiveTypes.put(float.class, FloatPrimitiveType.TYPE);
    _primitiveTypes.put(double.class, DoublePrimitiveType.TYPE);
    _primitiveTypes.put(char.class, CharacterPrimitiveType.TYPE);

    _primitiveTypes.put(Boolean.class, BooleanType.TYPE);
    _primitiveTypes.put(Byte.class, ByteType.TYPE);
    _primitiveTypes.put(Short.class, ShortType.TYPE);
    _primitiveTypes.put(Integer.class, IntegerType.TYPE);
    _primitiveTypes.put(Long.class, LongType.TYPE);
    _primitiveTypes.put(Float.class, FloatType.TYPE);
    _primitiveTypes.put(Double.class, DoubleType.TYPE);
    _primitiveTypes.put(Character.class, CharacterType.TYPE);

    _primitiveTypes.put(Object.class, ObjectType.TYPE);
    
    _primitiveTypes.put(String.class, StringType.TYPE);
    _primitiveTypes.put(RawString.class, RawStringType.TYPE);
    
    _primitiveTypes.put(String[].class, StringArrayType.TYPE);
    
    _primitiveTypes.put(Class.class, ClassType.TYPE);
    _primitiveTypes.put(Path.class, PathType.TYPE);
    _primitiveTypes.put(Pattern.class, PatternType.TYPE);
    _primitiveTypes.put(Locale.class, LocaleType.TYPE);
    _primitiveTypes.put(QDate.class, QDateType.TYPE);
    _primitiveTypes.put(Date.class, DateType.TYPE);
    _primitiveTypes.put(Properties.class, PropertiesType.TYPE);
    
    _primitiveTypes.put(DataSource.class, DataSourceType.TYPE);
    
    /*
    _primitiveTypes.put("org.w3c.dom.Node", new NodeType());

    _primitiveTypes.put("com.caucho.config.types.InitProgram",
                        new InitProgramType());
    _primitiveTypes.put("com.caucho.config.BuilderProgram",
                        new BuilderProgramType());
    */
  }
}
