/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
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

package com.caucho.v5.config;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.ConfigFileParser;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.core.ContextConfig;
import com.caucho.v5.config.expr.ExprCfg;
import com.caucho.v5.config.impl.RuntimeExceptionConfig;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.PropertyStringProgram;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.config.types.DirVar;
import com.caucho.v5.config.types.FileVar;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.VfsOld;

/**
 * Facade for the config parser.
 */
public class ConfigContext
{
  private static final L10N L = new L10N(ConfigContext.class);
  private static final Logger log
    = Logger.getLogger(ConfigContext.class.getName());

  public static final String []PROPERTIES = new String[] {
    "rvar0", "rvar1", "rvar2", "rvar3", "rvar4"
  };
                                                        
  private static final EnvironmentLocal<ConfigProperties> _envProperties
    = new EnvironmentLocal<>();
  
  private static final ThreadLocal<ConfigProperties> _threadProperties
    = new ThreadLocal<>();
    
  private static final ConfigContext _defaultConfig = new ConfigContext();
  
  private boolean _isEL = true;
  private boolean _isIgnoreEnvironment;

  public ConfigContext()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * @param loader the class loader environment to use.
   */
  public ConfigContext(ClassLoader loader)
  {
  }
  
  public static ConfigContext getDefaultConfig()
  {
    return _defaultConfig;
  }
  
  public static ConfigContext getCurrent()
  {
    ContextConfig context = ContextConfig.getCurrent();
    
    if (context != null) {
      return context.getConfig();
    }
    else {
      return _defaultConfig;
    }
  }
  
  /**
   * Returns the current thread-local context or creates a new one.
   */
  public ContextConfig currentOrCreateContext()
  {
    ContextConfig env = ContextConfig.getCurrent();

    if (env != null) {
      return env;
    }
    else {
      return createContext();
    }
  }

  protected ContextConfig createContext()
  {
    return new ContextConfig(this);
  }

  /**
   * True if EL expressions are allowed
   */
  public boolean isEL()
  {
    return _isEL;
  }

  /**
   * True if EL expressions are allowed
   */
  public void setEL(boolean isEL)
  {
    _isEL = isEL;
  }

  /**
   * True if environment tags are ignored
   */
  public boolean isIgnoreEnvironment()
  {
    return _isIgnoreEnvironment;
  }

  /**
   * True if environment tags are ignored
   */
  public void setIgnoreEnvironment(boolean isIgnore)
  {
    _isIgnoreEnvironment = isIgnore;
  }

  /**
   * Returns an environment property
   */
  public static Object getProperty(String key)
  {
    ConfigProperties props = _threadProperties.get();
    
    if (props != null) {
      Object value = props.get(key);

      if (value != null) {
        return value;
      }
    }
    
    props = _envProperties.get();
      
    
    if (props != null) {
      return props.get(key);
    }
    
    return null;
  }

  /**
   * Sets a environment property
   */
  public static void setProperty(String key, Object value)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    setProperty(key, value, loader);
  }

  /**
   * Sets a environment property
   */
  public static void setProperty(String key, Object value, ClassLoader loader)
  {
    ConfigProperties props = getOrCreateConfigProperties(loader);

    props.put(key, value);
  }

  private static ConfigProperties getOrCreateConfigProperties(ClassLoader loader)
  {
    EnvironmentClassLoader envLoader
      = EnvLoader.getEnvironmentClassLoader(loader);

    ConfigProperties props = _envProperties.getLevel(envLoader);

    if (props != null) {
      return props;
    }

    if (envLoader != null) {
      ConfigProperties parent = getOrCreateConfigProperties(envLoader.getParent());

      props = new ConfigProperties(parent);
    }
    else {
      props = new ConfigProperties(null);
    }

    _envProperties.set(props, envLoader);

    return props;
  }

  /**
   * Configures a bean with a configuration file.
   */
  public Object configure2(Object obj, PathImpl path)
  {
    Objects.requireNonNull(obj);
    Objects.requireNonNull(path);
    
    ConfigProgram program = parseProgram(path);
    
    Thread thread = Thread.currentThread();

    // ClassLoader loader = thread.getContextClassLoader();
    
    ConfigProperties oldProperties = _threadProperties.get();

    try {
      // ConfigProperties parentProps = getOrCreateConfigProperties(loader); 
      ConfigProperties threadProperties = new ConfigProperties(null);
      
      _threadProperties.set(threadProperties);
      
      threadProperties.put("__FILE__", FileVar.__FILE__);
      threadProperties.put("__DIR__", DirVar.__DIR__);
      
      threadProperties.put("__PATH__", path);

      program.configure(obj);
      
      return obj;
    } finally {
      _threadProperties.set(oldProperties);
    }
  }

  /**
   * Configures the bean from an input stream.
   */
  private ConfigProgram parseProgram(PathImpl path)
  {
    try {
      ConfigFileParser parser = new ConfigFileParser(this);
    
      return parser.parse(path);
    } catch (IOException e) {
      throw ConfigException.wrap(e);
    }
  }

  /**
   * Returns true if the class can be instantiated.
   */
  public static void checkCanInstantiate(Class<?> beanClass)
    throws ConfigException
  {
    if (beanClass == null)
      throw new ConfigException(L.l("null classes can't be instantiated."));
    else if (beanClass.isInterface())
      throw new ConfigException(L.l("'{0}' must be a concrete class.  Interfaces cannot be instantiated.", beanClass.getName()));
    else if (! Modifier.isPublic(beanClass.getModifiers()))
      throw new ConfigException(L.l("Custom bean class '{0}' is not public.  Bean classes must be public, concrete, and have a zero-argument constructor.", beanClass.getName()));
    else if (Modifier.isAbstract(beanClass.getModifiers()))
      throw new ConfigException(L.l("Custom bean class '{0}' is abstract.  Bean classes must be public, concrete, and have a zero-argument constructor.", beanClass.getName()));

    Constructor<?> []constructors = beanClass.getDeclaredConstructors();

    Constructor<?> constructor = null;

    for (int i = 0; i < constructors.length; i++) {
      if (constructors[i].getParameterTypes().length == 0
          || constructors[i].isAnnotationPresent(javax.inject.Inject.class)) {
        constructor = constructors[i];
        break;
      }
    }

    if (constructor == null)
      throw new ConfigException(L.l("Custom bean class '{0}' doesn't have a zero-arg constructor.  Bean classes must be have a zero-argument constructor.", beanClass.getName()));

    if (! Modifier.isPublic(constructor.getModifiers())) {
      throw new ConfigException(L.l("The zero-argument constructor for '{0}' isn't public.  Bean classes must have a public zero-argument constructor.", beanClass.getName()));
    }
  }

  /**
   * Returns true if the class can be instantiated.
   */
  public static void validate(Class<?> cl, Class<?> api)
    throws ConfigException
  {
    checkCanInstantiate(cl);

    if (! api.isAssignableFrom(cl)) {
      throw new ConfigException(L.l("{0} must implement {1}.",
                                    cl.getName(), api.getName()));
    }
  }

  /**
   * Returns true if the class can be instantiated using zero args constructor
   * or constructor that accepts an instance of class passed in type argument
   */
  public static void checkCanInstantiate(Class<?> beanClass,
                                         Class<?> type)
    throws ConfigException
  {
    if (beanClass == null)
      throw new ConfigException(L.l("null classes can't be instantiated."));
    else if (beanClass.isInterface())
      throw new ConfigException(L.l(
        "'{0}' must be a concrete class.  Interfaces cannot be instantiated.",
        beanClass.getName()));
    else if (! Modifier.isPublic(beanClass.getModifiers()))
      throw new ConfigException(L.l(
        "Custom bean class '{0}' is not public.  Bean classes must be public, concrete, and have a zero-argument constructor.",
        beanClass.getName()));
    else if (Modifier.isAbstract(beanClass.getModifiers()))
      throw new ConfigException(L.l(
        "Custom bean class '{0}' is abstract.  Bean classes must be public, concrete, and have a zero-argument constructor.",
        beanClass.getName()));

    Constructor<?> [] constructors = beanClass.getDeclaredConstructors();

    Constructor<?> zeroArgsConstructor = null;

    Constructor<?> singleArgConstructor = null;

    for (int i = 0; i < constructors.length; i++) {
           if (constructors [i].getParameterTypes().length == 0) {
             zeroArgsConstructor = constructors [i];

             if (singleArgConstructor != null)
               break;
           }
           else if (type != null
                    && constructors [i].getParameterTypes().length == 1 &&
                    type.isAssignableFrom(constructors[i].getParameterTypes()[0])) {
             singleArgConstructor = constructors [i];

             if (zeroArgsConstructor != null)
               break;
           }
         }

    if (zeroArgsConstructor == null
        && singleArgConstructor == null)
      if (type != null)
        throw new ConfigException(L.l(
                                      "Custom bean class '{0}' doesn't have a zero-arg constructor, or a constructor accepting parameter of type '{1}'.  Bean class '{0}' must have a zero-argument constructor, or a constructor accepting parameter of type '{1}'",
                                      beanClass.getName(),
                                      type.getName()));
      else
        throw new ConfigException(L.l(
                                      "Custom bean class '{0}' doesn't have a zero-arg constructor.  Bean classes must have a zero-argument constructor.",
                                      beanClass.getName()));


    if (singleArgConstructor != null) {
      if (! Modifier.isPublic(singleArgConstructor.getModifiers()) &&
          (zeroArgsConstructor == null ||
           ! Modifier.isPublic(zeroArgsConstructor.getModifiers()))) {
        throw new ConfigException(L.l(
          "The constructor for bean '{0}' accepting parameter of type '{1}' is not public.  Constructor accepting parameter of type '{1}' must be public.",
          beanClass.getName(),
          type.getName()));
      }
    }
    else if (zeroArgsConstructor != null) {
      if (! Modifier.isPublic(zeroArgsConstructor.getModifiers()))
        throw new ConfigException(L.l(
          "The zero-argument constructor for '{0}' isn't public.  Bean classes must have a public zero-argument constructor.",
          beanClass.getName()));
    }
  }

  public static void validate(Class<?> cl, 
                              Class<?> api, 
                              Class<?> type)
    throws ConfigException
  {
    checkCanInstantiate(cl, type);

    if (! api.isAssignableFrom(cl)) {
      throw new ConfigException(L.l("{0} must implement {1}.",
                                    cl.getName(), api.getName()));
    }
  }

  /**
   * Sets an attribute with a value.
   *
   * @param obj the bean to be set
   * @param attr the attribute name
   * @param value the attribute value
   */
  public static void setAttribute(Object obj, String attr, Object value)
  {
    ConfigType<?> type = TypeFactoryConfig.getType(obj.getClass());

    NameCfg attrName = new NameCfg(attr);
    AttributeConfig attrStrategy = type.getAttribute(attrName);
    if (attrStrategy == null)
      throw new ConfigException(L.l("{0}: '{1}' is an unknown attribute.",
                                    obj.getClass().getName(),
                                    attrName.getName()));

    value = attrStrategy.getConfigType().valueOf(value);

    attrStrategy.setValue(obj, attrName, value);
  }

  public static Function<String, Object> getEnvironment()
  {
    return x->null;
  }

  /**
   * Sets an attribute with a value.
   *
   * @param obj the bean to be set
   * @param attr the attribute name
   * @param value the attribute value
   */
  public static void setStringAttribute(Object obj, String attr, String value)
    throws Exception
  {
    ConfigContext config = ConfigContext.getDefaultConfig();
    
    PropertyStringProgram program
      = new PropertyStringProgram(config, attr, value);
    
    program.configure(obj);
  }

  public static void init(Object bean)
    throws ConfigException
  {
    try {
      ConfigType type = TypeFactoryConfig.getType(bean.getClass());

      type.init(bean);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  public static String evalString(String value)
  {
    return value;
  }

  public static String evalString(String value, Function<String,Object> map)
  {
    Objects.requireNonNull(map);
    
    return value;
  }

  public static String evalString(String value, Map<String,Object> map)
  {
    Objects.requireNonNull(map);
    
    return evalString(value, x->map.get(x));
  }

  public static Object eval(String expr)
  {
    return ExprCfg.newParser(expr).parse().eval(getEnvironment());
  }

  public static void inject(Object bean)
    throws ConfigException
  {
    try {
      ConfigType type = TypeFactoryConfig.getType(bean.getClass());

      type.inject(bean);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  public static Object replaceObject(Object bean) throws Exception
  {
    ConfigType type = TypeFactoryConfig.getType(bean.getClass());

    return type.replaceObject(bean);
  }

  /**
   * Sets an EL configuration variable.
   */
  public static Object getCurrentVar(String var)
  {
    // return InjectManager.create().findByName(var);
    return getProperty(var);
  }
 
  public static ConfigException error(Field field, String msg)
  {
    return new ConfigException(location(field) + msg);
  }

  public static ConfigException error(Method method, String msg)
  {
    return new ConfigException(location(method) + msg);
  }

  public static RuntimeException createLine(String systemId, int line,
                                            Throwable e)
  {
    while (e.getCause() != null
           && (e instanceof InstantiationException
               || e instanceof InvocationTargetException
               || e.getClass().equals(RuntimeExceptionConfig.class))) {
      e = e.getCause();
    }

    if (e instanceof ConfigExceptionLocation)
      throw (ConfigExceptionLocation) e;

    String lines = getSourceLines(systemId, line);
    String loc = systemId + ":" + line + ": ";

    if (e instanceof DisplayableException) {
      return new ConfigExceptionLocation(loc + e.getMessage() + "\n" + lines, e);
    }
    else
      return new ConfigExceptionLocation(loc + e + "\n" + lines, e);
  }

  public static String location(Field field)
  {
    String className = field.getDeclaringClass().getName();

    return className + "." + field.getName() + ": ";
  }

  public static String location(Method method)
  {
    String className = method.getDeclaringClass().getName();

    return className + "." + method.getName() + ": ";
  }

  private static String getSourceLines(String systemId, int errorLine)
  {
    if (systemId == null)
      return "";

    ReadStream is = null;
    try {
      is = VfsOld.lookup().lookup(systemId).openRead();
      int line = 0;
      StringBuilder sb = new StringBuilder("\n\n");
      String text;
      while ((text = is.readLine()) != null) {
        line++;

        if (errorLine - 2 <= line && line <= errorLine + 2) {
          sb.append(line);
          sb.append(": ");
          sb.append(text);
          sb.append("\n");
        }
      }

      return sb.toString();
    } catch (IOException e) {
      log.log(Level.FINEST, e.toString(), e);

      return "";
    } finally {
      if (is != null)
        is.close();
    }
  }
  
  static class ConfigProperties {
    private ConfigProperties _parent;
    private HashMap<String,Object> _properties = new HashMap<String,Object>(8);

    ConfigProperties(ConfigProperties parent)
    {
      _parent = parent;
    }

    public Object get(String key)
    {
      Object value = _properties.get(key);

      if (value != null) {
        return value;
      }
      else if (_parent != null) {
        return _parent.get(key);
      }
      else {
        return null;
      }
    }

    public void put(String key, Object value)
    {
      _properties.put(key, value);
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[]";
    }
  }
}

