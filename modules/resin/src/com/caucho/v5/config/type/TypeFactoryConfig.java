/*
< * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
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

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.attribute.EnvironmentAttribute;
import com.caucho.v5.config.attribute.FlowAttribute;
import com.caucho.v5.config.attribute.ListValueAttribute;
import com.caucho.v5.config.attribute.SetValueAttribute;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.custom.CustomBean;
import com.caucho.v5.config.custom.TypeCustomBean;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.types.AnnotationConfig;
import com.caucho.v5.config.types.RawString;
import com.caucho.v5.config.util.ClassLoadUtil;
import com.caucho.v5.io.IoUtil;
import com.caucho.v5.loader.AddLoaderListener;
import com.caucho.v5.loader.DynamicClassLoader;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.loader.EnvironmentBean;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.loader.RootDynamicClassLoader;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.QDate;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.VfsOld;

/**
 * Factory for returning type strategies.
 */
public class TypeFactoryConfig // implements AddLoaderListener
{
  private static final Logger log
    = Logger.getLogger(TypeFactoryConfig.class.getName());
  private static L10N L = new L10N(TypeFactoryConfig.class);

  private static final HashMap<Class<?>,ConfigType<?>> _primitiveTypes
    = new HashMap<Class<?>,ConfigType<?>>();

  private static final EnvironmentLocal<TypeFactoryConfig> _localFactory
    = new EnvironmentLocal<>();

  private static final ClassLoader _systemClassLoader;

  private final ClassLoader _classLoader;
  private final EnvironmentClassLoader _envLoader;
  private final TypeFactoryConfig _parent;

  private final HashSet<URL> _configSet
    = new HashSet<URL>();

  private final HashMap<String,ArrayList<String>> _packageImportMap
    = new HashMap<String,ArrayList<String>>();

  private final ConcurrentHashMap<String,ConfigType<?>> _typeMap
    = new ConcurrentHashMap<String,ConfigType<?>>();

  private final ConcurrentHashMap<NameCfg,ConfigType<?>> _attrMap
    = new ConcurrentHashMap<NameCfg,ConfigType<?>>();
  
  private final ConcurrentHashMap<NameCfg,Class<?>> _customClassMap
    = new ConcurrentHashMap<NameCfg,Class<?>>();
  
  private final ConcurrentHashMap<String,Class<?>> _urnClassMap
    = new ConcurrentHashMap<String,Class<?>>();

  private final HashMap<NameCfg,AttributeConfig> _listAttrMap
    = new HashMap<NameCfg,AttributeConfig>();

  private final HashMap<NameCfg,AttributeConfig> _setAttrMap
    = new HashMap<NameCfg,AttributeConfig>();

  private final ConcurrentHashMap<NameCfg,AttributeConfig> _envAttrMap
    = new ConcurrentHashMap<NameCfg,AttributeConfig>();

  private final HashMap<String,NamespaceConfig> _nsMap
    = new HashMap<>();

  private final HashSet<URL> _driverTypeSet
    = new HashSet<URL>();

  private final HashMap<String,HashMap<String,String>> _driverTypeMap
    = new HashMap<>();
  
  private final ConcurrentHashMap<String,TypeCustomBean<?>> _customBeanMap
    = new ConcurrentHashMap<>();

  private final AtomicBoolean _isInInit = new AtomicBoolean();

  protected TypeFactoryConfig(ClassLoader loader)
  {
    if (loader == null) {
      loader = _systemClassLoader;
    }
    
    _classLoader = loader;
    _envLoader = EnvLoader.getEnvironmentClassLoader(loader);
    
    _localFactory.set(this, loader);

    if (_envLoader != null) {
      _parent = getFactory(_envLoader.getParent());

      //_envLoader.addLoaderListener(this);
    }
    else {
      _parent = null;
    }
    
    init(loader);
  }

  /**
   * Returns the appropriate strategy.
   */
  public static <T> ConfigType<T> getType(T bean)
  {
    if (bean instanceof CustomBean) {
      return (ConfigType<T>) ((CustomBean) bean).getConfigType();
    }
    else if (bean instanceof AnnotationConfig) {
      return (ConfigType<T>) ((AnnotationConfig) bean).getConfigType();
    }
    else {
      return (ConfigType<T>) getType(bean.getClass());
    }
  }

  /**
   * Returns the appropriate strategy.
   */
  public static <T> ConfigType<T> getType(Class<T> type)
  {
    TypeFactoryConfig factory = getFactory(type.getClassLoader());

    return factory.getConfigTypeImpl(type);
  }

  /**
   * Returns the appropriate strategy.
   */
  public static ConfigType<?> getType(Type type)
  {
    return getType((Class) type);
  }

  /**
   * Returns the appropriate strategy.
   */
  public static Class<?> loadClass(NameCfg qName)
  {
    return getFactory().loadClassImpl(qName);
  }

  /**
   * Returns the appropriate strategy.
   */
  public static Class<?> loadClass(String pkg, String name)
  {
    return getFactory().loadClassImpl(pkg, name);
  }

  public static TypeFactoryConfig create()
  {
    return getFactory();
  }

  public static TypeFactoryConfig getFactory()
  {
    return getFactory(Thread.currentThread().getContextClassLoader());
  }

  public static TypeFactoryConfig getFactorySystem()
  {
    return getFactory(_systemClassLoader);
  }

  public static TypeFactoryConfig getFactory(ClassLoader loader)
  {
    if (loader == null) {
      loader = _systemClassLoader;
    }

    TypeFactoryConfig factory = _localFactory.getLevel(loader);

    if (factory == null) {
      factory = createTypeFactory(loader);
      _localFactory.set(factory, loader);
      factory.init(loader);
    }

    return factory;
  }
  
  private static TypeFactoryConfig createTypeFactory(ClassLoader loader)
  {
    try {
      Class<?> cl = Class.forName("com.caucho.v5.config.type.TypeFactoryResin");
      
      Constructor<?> ctor = cl.getConstructor(ClassLoader.class);
      
      return (TypeFactoryConfig) ctor.newInstance(loader);
    } catch (Exception e) {
      log.log(Level.ALL, e.toString(), e);
    }
    
    return new TypeFactoryConfig(loader);
  }

  public static CustomBean createCustomBean(NameCfg qName, Class<?> cl,
                                                  Object parent)
  {
    return getFactory().createCustomBeanImpl(qName, cl, parent);
  }
  
  protected CustomBean createCustomBeanImpl(NameCfg qName,
                                                  Class<?> cl,
                                                  Object parent)
  {
    return new CustomBean(qName, cl, parent);
  }
  
  ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Returns an environment type.
   */
  public ConfigType<?> getEnvironmentType(NameCfg name)
  {
    ConfigType<?> type = _attrMap.get(name);

    if (type != null)
      return type == NotFoundConfigType.NULL ? null : type;

    type = getEnvironmentTypeRec(name);

    if (type != null) {
      return type;
    }

    if (! "".equals(name.getNamespaceURI())) {
      type = getEnvironmentType(new NameCfg(name.getLocalName()));

      if (type != null) {
        _attrMap.put(name, type);

        return type;
      }
    }

    _attrMap.put(name, NotFoundConfigType.NULL);

    return null;
  }

  /**
   * Returns an environment type.
   */
  protected ConfigType<?> getEnvironmentTypeRec(NameCfg name)
  {
    ConfigType<?> type = _attrMap.get(name);

    if (type != null) {
      return type == NotFoundConfigType.NULL ? null : type;
    }

    if (_parent != null)
      type = _parent.getEnvironmentTypeRec(name);

    if (type != null) {
      _attrMap.put(name, type);

      return type;
    }

    String uri = name.getNamespaceURI();

    NamespaceConfig ns = _nsMap.get(uri);

    if (ns != null) {
      type = ns.getBean(name.getLocalName());

      if (type != null) {
        _attrMap.put(name, type);

        return type;
      }
    }

    if (uri != null && uri.startsWith("urn:java:")) {
      String pkg = uri.substring("urn:java:".length());
      String className = name.getLocalName();

      Class<?> cl = loadClassImpl(pkg, className);

      if (cl != null) {
        type = getType(cl);
        
        _attrMap.put(name, type);
        
        return type;
      }
    }

    _attrMap.put(name, NotFoundConfigType.NULL);

    return null;
  }

  /**
   * Returns an environment type.
   */
  public AttributeConfig getListAttribute(NameCfg name)
  {
    synchronized (_listAttrMap) {
      AttributeConfig attr = _listAttrMap.get(name);

      if (attr != null)
        return attr;

      ConfigType<?> type = getEnvironmentType(name);

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
  public AttributeConfig getSetAttribute(NameCfg name)
  {
    synchronized (_setAttrMap) {
      AttributeConfig attr = _setAttrMap.get(name);

      if (attr != null)
        return attr;

      ConfigType<?> type = getEnvironmentType(name);

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
  public AttributeConfig getEnvironmentAttribute(NameCfg name)
  {
    AttributeConfig attr = _envAttrMap.get(name);

    if (attr != null)
      return attr;

    ConfigType<?> type = getEnvironmentType(name);

    if (type == null)
      return null;

    if (type instanceof FlowBeanType<?>)
      attr = new FlowAttribute(type);
    else if (type.isEnvBean()) {
      attr = new EnvironmentAttribute(type);
    }
    else {
      // attr = new XmlBeanAttribute(null, type);
      //Thread.dumpStack();
      return null;
    }

    _envAttrMap.put(name, attr);

    return attr;
  }

  private Class<?> loadClassImpl(NameCfg qName)
  {
    Class<?> cl = _customClassMap.get(qName);
    
    if (cl != null)
      return cl == void.class ? null : cl;
    
    String uri = qName.getNamespaceURI();
    String localName = qName.getLocalName();

    if (! uri.startsWith("urn:java:"))
      throw new IllegalStateException(L.l("'{0}' is an unexpected namespace, expected 'urn:java:...'", uri));

    String packageName = uri.substring("uri:java:".length());
    
    cl = loadClassImpl(packageName, localName);
    
    if (cl != null)
      _customClassMap.put(qName, cl);
    else
      _customClassMap.put(qName, void.class);
    
    return cl;
  }
  
  private Class<?> loadClassImpl(String pkg, String name)
  {
    ClassLoader loader = _envLoader;

    if (_envLoader == null)
      loader = _systemClassLoader;
    
    TypeFactoryConfig rootFactory = TypeFactoryConfig.getFactory(_systemClassLoader);
    
    NamespaceConfig nsConfig = rootFactory._nsMap.get("urn:java:" + pkg);

    if (nsConfig != null) {
      ConfigType<?> configType = nsConfig.getBean(name);
      
      if (configType != null)
        return configType.getType();
    }
    
    String urnName = "urn:" + pkg + '.' + name;
    
    Class<?> cl = findUrnClass(urnName, loader);

    if (cl == null) {
      cl = loadClassImpl(pkg, name, loader);
      
      // save negative lookups
      if (cl == null) {
        cl = void.class;
      }
      
      putUrnClass(urnName, cl, cl.getClassLoader());
    }
    
    if (cl == void.class)
      return null;
    else
      return cl;
  }
  
  private Class<?> findUrnClass(String urnName, ClassLoader loader)
  {
    for (; loader != null; loader = loader.getParent()) {
      TypeFactoryConfig factory = TypeFactoryConfig.getFactory(loader);
      
      Class<?> cl = factory._urnClassMap.get(urnName);
      
      if (cl != null && cl != void.class)
        return cl;
    }
    
    return null;
  }
  
  private void putUrnClass(String urnName, Class<?> cl, ClassLoader loader)
  {
    TypeFactoryConfig factory = TypeFactoryConfig.getFactory(loader);
    
    factory._urnClassMap.put(urnName, cl);
  }

  private Class<?> loadClassImpl(String pkg, String name, ClassLoader loader)
  {
    ArrayList<String> pkgList = loadPackageList(pkg);
    
    DynamicClassLoader dynLoader = null;
    
    if (loader instanceof DynamicClassLoader)
      dynLoader = (DynamicClassLoader) loader;

    for (String pkgName : pkgList) {
      try {
        Class<?> cl;
        
        String className = pkgName + '.' + name;

        if (dynLoader != null) {
          dynLoader.updateScan();

          cl = dynLoader.loadClassImpl(className, false);
        }
        else {
          cl = ClassLoadUtil.load(className, loader);
        }

        if (cl != null)
          return cl;
      } catch (ClassNotFoundException e) {
        log.log(Level.ALL, e.toString(), e);
      }
    }

    return null;
  }

  private ArrayList<String> loadPackageList(String pkg)
  {
    synchronized (_packageImportMap) {
      ArrayList<String> pkgList = _packageImportMap.get(pkg);

      if (pkgList != null)
        return pkgList;

      pkgList = new ArrayList<String>();
      pkgList.add(pkg);

      InputStream is = null;
      try {
        ClassLoader loader = _envLoader;

        if (loader == null)
          loader = _systemClassLoader;

        is = loader.getResourceAsStream(pkg.replace('.', '/') + "/namespace");

        if (is != null) {
          ReadStream in = VfsOld.openRead(is);
          String line;
          while ((line = in.readLine()) != null) {
            for (String name : line.split("[ \t\r\n]+")) {
              if (! "".equals(name)) {
                if (! pkgList.contains(name))
                  pkgList.add(name);
              }
            }
          }
        }
      } catch (IOException e) {
        log.log(Level.FINE, e.toString(), e);
      } finally {
        IoUtil.close(is);
      }

      _packageImportMap.put(pkg, pkgList);

      return pkgList;
    }
  }

  private ConfigType getConfigTypeImpl(Class<?> type)
  {
    ConfigType strategy = _typeMap.get(type.getName());
    
    if (strategy == null) {
      strategy = _primitiveTypes.get(type);

      if (strategy == null)
        strategy = createType(type);

      _typeMap.putIfAbsent(type.getName(), strategy);

      strategy = _typeMap.get(type.getName());
    }
    
    strategy.carefulIntrospect();

    return strategy;
  }

  ConfigType createType(Class<?> type)
  {
    PropertyEditor editor = null;

    if (ConfigType.class.isAssignableFrom(type)) {
      try {
        return (ConfigType) type.newInstance();
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }
    }
    else if (Enum.class.isAssignableFrom(type)) {
      return new EnumType(type);
    }
    else if ((editor = findEditor(type)) != null)
      return new PropertyEditorType(type, editor);
    else if (Set.class.isAssignableFrom(type))
      return new SetType(type);
    else if (Collection.class.isAssignableFrom(type)
             && ! Queue.class.isAssignableFrom(type)) {
      // jms/2300
      return new ListType(type);
    }
    else if (Map.class.isAssignableFrom(type)
             && type.getName().startsWith("java.util")) {
      return new MapType(type);
    }
    else if (EnvironmentBean.class.isAssignableFrom(type))
      return new EnvironmentBeanType(this, type);
    else if (FlowBean.class.isAssignableFrom(type))
      return new FlowBeanType(this, type);
    else if (type.isArray()) {
      Class<?> compType = type.getComponentType();

      return new ArrayType(getType(compType), compType);
    }
    else if (Annotation.class.isAssignableFrom(type)) {
      return new AnnotationInterfaceType(type);
    }
    else if (type == ConfigProgram.class) {
      return new ProgramConfigType(this, type);
    }
    else if (type.isInterface()) {
      return new InterfaceType(this, type);
    }
    else if (Modifier.isAbstract(type.getModifiers())) {
      return new BeanTypeBase(this, type);
    }
    else {
      return createInlineBean(type);
    }
  }
  
  protected InlineBeanType createInlineBean(Class<?> type)
  {
    return new InlineBeanType(this, type);
  }

  /**
   * Returns the Java bean property editor
   */
  private static PropertyEditor findEditor(Class<?> type)
  {
    // none of the caucho classes has a ProperyEditorManager

    if (type.getName().startsWith("com.caucho"))
      return null;
    else
      return PropertyEditorManager.findEditor(type);
  }

  /**
   * Returns the appropriate strategy.
   */
  public static <T> TypeCustomBean<T> getCustomBeanType(Class<T> type)
  {
    TypeFactoryConfig factory = getFactory(type.getClassLoader());

    return factory.getCustomBeanTypeImpl(type);
  }

  private <T> TypeCustomBean<T> getCustomBeanTypeImpl(Class<T> type)
  {
    synchronized (_customBeanMap) {
      TypeCustomBean<?> beanType = _customBeanMap.get(type.getName());

      if (beanType == null) {
        ConfigType<T> configType = getType(type);
        
        //beanType = new TypeCustomBeanXml<>(type, configType);
        beanType = new TypeCustomBean<>(type, configType);
        
        _customBeanMap.put(type.getName(), beanType);
      }

      return (TypeCustomBean) beanType;
    }
  }

  /**
   * Initialize the type strategy factory with files in META-INF/caucho
   *
   * @param loader the owning class loader
   * @throws Exception
   */
  protected void init(ClassLoader loader)
  {
    if (! _isInInit.getAndSet(true)) {
      return;
    }

    try {
      _nsMap.clear();
      _driverTypeSet.clear();
      _driverTypeMap.clear();

      if (_parent == null) {
        initNamespaces();
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    } finally {
      _isInInit.set(false);
    }
  }
  
  protected void initNamespaces()
  {
    addNamespace(NamespaceConfig.NS_DEFAULT);
    addNamespace(NamespaceConfig.URN_BARATINE);
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

  /**
   * Returns a driver by the url
   */
  public Class<?> getDriverClassByUrl(Class<?> api, String url)
  {
    String scheme;

    int p = url.indexOf(':');
    if (p >= 0)
      scheme = url.substring(0, p);
    else
      scheme = url;

    String typeName = getDriverType(api.getName(), scheme);

    if (typeName == null) {
      ArrayList<String> schemes = new ArrayList<String>();

      getDriverSchemes(schemes, api.getName());

      Collections.sort(schemes);

      throw new ConfigException(L.l("'{0}' is an unknown scheme for driver '{1}'.  The available schemes are '{2}'",
                                    scheme, api.getName(), schemes));
    }

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class<?> cl = ClassLoadUtil.load(typeName, loader);

      if (! api.isAssignableFrom(cl))
        throw new ConfigException(L.l("'{0}' is not assignable to '{1}' for scheme '{2}'",
                                      cl.getName(), api.getName(),
                                      scheme));

      return cl;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(L.l("'{0}' is an undefined class for scheme '{1}'",
                                    typeName, scheme), e);
    }
  }

  /**
   * Returns a driver by the scheme
   */
  public Class<?> getDriverClassByScheme(Class<?> api, String scheme)
  {
    String typeName = getDriverType(api.getName(), scheme);

    if (typeName == null) {
      ArrayList<String> schemes = new ArrayList<String>();

      getDriverSchemes(schemes, api.getName());

      Collections.sort(schemes);

      throw new ConfigException(L.l("'{0}' is an unknown scheme for driver '{1}'.  The available schemes are '{2}'",
                                    scheme, api.getName(), schemes));
    }

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      Class<?> cl = ClassLoadUtil.load(typeName, loader);

      if (! api.isAssignableFrom(cl))
        throw new ConfigException(L.l("'{0}' is not assignable to '{1}' for scheme '{2}'",
                                      cl.getName(), api.getName(),
                                      scheme));

      return cl;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(L.l("'{0}' is an undefined class for scheme '{1}'",
                                    typeName, scheme), e);
    }
  }

  /**
   * Returns the classname of the given driver.
   *
   * @param apiType the driver API
   * @param scheme the configuration scheme
   */
  public String getDriverType(String apiType, String scheme)
  {
    HashMap<String,String> driverMap = getDriverTypeMap(apiType);

    return driverMap.get(scheme);
  }

  /**
   * Returns a list of schemes supported by the api type.
   *
   * @param schemes the return list of schemes scheme
   * @param apiType the driver API
   */
  public void getDriverSchemes(ArrayList<String> schemes, String apiType)
  {
    HashMap<String,String> driverMap = getDriverTypeMap(apiType);

    ClassLoader loader = _envLoader;
    if (_envLoader == null)
      loader = _systemClassLoader;

    for (Map.Entry<String,String> entry : driverMap.entrySet()) {
      String scheme = entry.getKey();
      String type = entry.getValue();

      try {
        Class<?> cl = ClassLoadUtil.load(type, loader);

        if (cl != null)
          schemes.add(scheme);
      } catch (Exception e) {
        log.finest(apiType + " schemes: " + e.toString());
      }
    }
  }

  /**
   * Loads the map for a driver.
   */
  private HashMap<String,String> getDriverTypeMap(String apiType)
  {
    synchronized (_driverTypeMap) {
      HashMap<String,String> driverMap = _driverTypeMap.get(apiType);

      if (driverMap == null) {
        driverMap = new HashMap<String,String>();

        if (_parent != null)
          driverMap.putAll(_parent.getDriverTypeMap(apiType));

        loadDriverTypeMap(driverMap, apiType);

        _driverTypeMap.put(apiType, driverMap);
      }

      return driverMap;
    }
  }

  /**
   * Reads the drivers from the META-INF/caucho
   */
  private void loadDriverTypeMap(HashMap<String,String> driverMap,
                                 String apiType)
  {
    try {
      ClassLoader loader = _envLoader;

      if (loader == null)
        loader = _systemClassLoader;

      Enumeration<URL> urls
        = loader.getResources("META-INF/caucho/com.caucho.v5.config.uri/"
                              + apiType);

      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();

        if (hasDriver(url))
          continue;

        _driverTypeSet.add(url);

        InputStream is = url.openStream();

        try {
          Properties props = new Properties();

          props.load(is);

          for (Map.Entry entry : props.entrySet()) {
            driverMap.put((String) entry.getKey(), (String) entry.getValue());
          }
        } finally {
          is.close();
        }
      }
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  protected boolean hasDriver(URL url)
  {
    synchronized (_driverTypeSet) {
      if (_driverTypeSet.contains(url))
        return true;
      else if (_parent != null)
        return _parent.hasDriver(url);
      else
        return false;
    }
  }

  //
  // AddLoaderListener
  //

  public boolean isEnhancer()
  {
    return false;
  }

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
  protected void addNamespace(NamespaceConfig ns)
  {
    _nsMap.put(ns.getName(), ns);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _envLoader + "]";
  }
  
  protected static ClassLoader getSystemClassLoader()
  {
    return _systemClassLoader;
  }
  
  protected static void addPrimitiveType(Class<?> cl, ConfigType<?> type)
  {
    _primitiveTypes.put(cl, type);
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
    _primitiveTypes.put(PathImpl.class, PathType.TYPE);
    _primitiveTypes.put(File.class, FileType.TYPE);
    _primitiveTypes.put(URL.class, UrlType.TYPE);
    _primitiveTypes.put(Pattern.class, PatternType.TYPE);
    _primitiveTypes.put(Level.class, LevelBuilder.TYPE);
    _primitiveTypes.put(Locale.class, LocaleType.TYPE);
    // _primitiveTypes.put(Node.class, NodeType.TYPE);
    _primitiveTypes.put(QDate.class, QDateType.TYPE);
    _primitiveTypes.put(Date.class, DateType.TYPE);
    _primitiveTypes.put(Properties.class, PropertiesType.TYPE);
    
    // _primitiveTypes.put(Expr.class, ExprType.TYPE);

    // _primitiveTypes.put(DataSource.class, DataSourceType.TYPE);

    //_primitiveTypes.put(MethodExpression.class, MethodExpressionType.TYPE);
    // _primitiveTypes.put(Bean.class, CdiBeanType.TYPE);

    /*
    ClassLoader systemClassLoader = null;

    try {
      systemClassLoader = ClassLoader.getSystemClassLoader();
    } catch (Exception e) {
    }
    */

    _systemClassLoader = RootDynamicClassLoader.getSystemRootClassLoader();
  }
}
