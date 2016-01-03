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

package com.caucho.v5.config.xml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELException;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.DisplayableException;
import com.caucho.v5.config.ConfigExceptionLocation;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.core.ContextConfig;
import com.caucho.v5.config.expr.ExprCfg;
import com.caucho.v5.config.impl.RuntimeExceptionConfig;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.config.types.DirVar;
import com.caucho.v5.config.types.FileVar;
import com.caucho.v5.el.EL;
import com.caucho.v5.el.EnvironmentContext;
import com.caucho.v5.relaxng.CompactVerifierFactoryImpl;
import com.caucho.v5.relaxng.Schema;
import com.caucho.v5.relaxng.Verifier;
import com.caucho.v5.relaxng.VerifierFilter;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.Vfs;
import com.caucho.v5.xml.DOMBuilder;
import com.caucho.v5.xml.QDocument;
import com.caucho.v5.xml.Xml;

/**
 * The main configuration facade for XML configuration.
 */
public class ConfigXml extends ConfigContext
{
  private static final L10N L = new L10N(ConfigXml.class);
  private static final Logger log
    = Logger.getLogger(ConfigXml.class.getName());

  /*
  private static final EnvironmentLocal<ConfigProperties> _envProperties
    = new EnvironmentLocal<>();
    */
  
  // the context class loader of the config
  private ClassLoader _classLoader;

  private boolean _isEL = true;
  private boolean _isIgnoreEnvironment;

  public ConfigXml()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * @param loader the class loader environment to use.
   */
  public ConfigXml(ClassLoader loader)
  {
    _classLoader = loader;
 }

  @Override
  public ContextConfigXml currentOrCreateContext()
  {
    return (ContextConfigXml) super.currentOrCreateContext();
  }

  @Override
  protected ContextConfigXml createContext()
  {
    return new ContextConfigXml(this);
  }

  /**
   * True if EL expressions are allowed
   */
  @Override
  public boolean isEL()
  {
    return _isEL;
  }

  /**
   * True if EL expressions are allowed
   */
  @Override
  public void setEL(boolean isEL)
  {
    _isEL = isEL;
  }

  /**
   * True if environment tags are ignored
   */
  @Override
  public boolean isIgnoreEnvironment()
  {
    return _isIgnoreEnvironment;
  }

  /**
   * True if environment tags are ignored
   */
  @Override
  public void setIgnoreEnvironment(boolean isIgnore)
  {
    _isIgnoreEnvironment = isIgnore;
  }

  /**
   * Returns an environment property
   */
  /*
  public static Object getProperty(String key)
  {
    ConfigProperties props = _envProperties.get();

    if (props != null)
      return props.get(key);
    else
      return null;
  }
  */

  /*
  public static ConfigProperties getConfigProperties()
  {
    return _envProperties.get();
  }
  */

  /**
   * Sets a environment property
   */
  /*
  public static void setProperty(String key, Object value)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    setProperty(key, value, loader);
  }
  */

  /**
   * Sets a environment property
   */
  /*
  public static void setProperty(String key, Object value, ClassLoader loader)
  {
    ConfigProperties props = _envProperties.getLevel(loader);

    if (props == null) {
      props = createConfigProperties(loader);
    }

    props.put(key, value);
  }
  */

  /*
  private static ConfigProperties createConfigProperties(ClassLoader loader)
  {
    EnvironmentClassLoader envLoader
    = Environment.getEnvironmentClassLoader(loader);

    ConfigProperties props = _envProperties.getLevel(envLoader);

    if (props != null) {
      return props;
    }

    props = newConfigProperties(loader);

    _envProperties.set(props, envLoader);

    return props;
  }
  
  private static ConfigProperties newConfigProperties(ClassLoader loader)
  {
    EnvironmentClassLoader envLoader
      = Environment.getEnvironmentClassLoader(loader);
    
    ConfigProperties props;
    
    if (envLoader != null) {
      ConfigProperties parent = createConfigProperties(envLoader.getParent());

      props = new ConfigProperties(parent);
    }
    else {
      props = new ConfigProperties(null);
    }
    
    return props;
  }
  */

  /**
   * Configures a bean with a configuration file.
   */
  public Object configure(Object obj, PathImpl path)
    throws ConfigException, IOException
  {
    try {
      QDocument doc = parseDocument(path, null);

      return configure(obj, doc.getDocumentElement());
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  /**
   * Configures a bean with a configuration file.
   */
  /*
  public Object configure2(Object obj, Path path)
  {
    Objects.requireNonNull(obj);
    Objects.requireNonNull(path);
    
    ConfigProgram program = parseProgram(path);
    
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    
    ConfigProperties oldProperties = _envProperties.getLevel();

    try {
      ConfigProperties properties = newConfigProperties(loader);
      _envProperties.set(properties);
    
      program.configure(obj);
      
      return obj;
    } finally {
      _envProperties.set(oldProperties);
    }
  }
  */

  /**
   * Configures a bean with a configuration file.
   */
  public Object configure(Object obj, InputStream is)
    throws Exception
  {
    QDocument doc = parseDocument(is, null);

    return configure(obj, doc.getDocumentElement());
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public Object configure(Object obj, PathImpl path, String schemaLocation)
    throws ConfigException
  {
    try {
      Schema schema = findCompactSchema(schemaLocation);

      QDocument doc = parseDocument(path, schema);

      return configure(obj, doc.getDocumentElement());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigExceptionLocation.wrap(e);
    }
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public Object configure(Object obj, PathImpl path, Schema schema)
    throws ConfigException
  {
    try {
      QDocument doc = parseDocument(path, schema);

      return configure(obj, doc.getDocumentElement());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  /**
   * Configures a bean with a configuration file.
   */
  public Object configure(Object obj,
                          InputStream is,
                          String schemaLocation)
    throws Exception
  {
    Schema schema = findCompactSchema(schemaLocation);

    QDocument doc = parseDocument(is, schema);

    return configure(obj, doc.getDocumentElement());
  }

  /**
   * Configures a bean with a configuration file.
   */
  public Object configure(Object obj,
                          InputStream is,
                          Schema schema)
    throws Exception
  {
    QDocument doc = parseDocument(is, schema);

    return configure(obj, doc.getDocumentElement());
  }

  /**
   * Configures a bean with a DOM.
   */
  public Object configure(Object obj, Node topNode)
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    ContextConfig oldContext = ContextConfig.getCurrent();

    try {
      thread.setContextClassLoader(_classLoader);

      ContextConfigXml context = createContext();
      
      ContextConfig.setCurrent(context);

      setProperty("__FILE__", FileVar.__FILE__);
      setProperty("__DIR__", DirVar.__DIR__);
      setProperty("__PATH__", Vfs.lookup(topNode.getBaseURI()));
      // context.setBaseUri(topNode.getBaseURI());
      
      
      ConfigProgramBuilderXml builder = new ConfigProgramBuilderXml(this);
      ConfigProgram program = builder.buildTop(topNode);
      
      program.configure(obj);
      
      return ConfigContext.replaceObject(obj);
      
      // return context.configure(obj, topNode);
    } finally {
      thread.setContextClassLoader(oldLoader);
      
      ContextConfig.setCurrent(oldContext);
    }
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public void configureBean(Object obj,
                            PathImpl path,
                            String schemaLocation)
    throws Exception
  {
    Schema schema = findCompactSchema(schemaLocation);

    QDocument doc = parseDocument(path, schema);

    configureBean(obj, doc.getDocumentElement());
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public void configureBean(Object obj, PathImpl path)
    throws Exception
  {
    QDocument doc = parseDocument(path, null);

    configureBean(obj, doc.getDocumentElement());
  }

  /**
   * Configures a bean with a DOM.  configureBean does not
   * apply init() or replaceObject().
   */
  public void configureBean(Object obj, Node topNode)
    throws Exception
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    ContextConfig oldContext = ContextConfig.getCurrent();
    
    try {
      thread.setContextClassLoader(_classLoader);

      ContextConfigXml context = createContext();

      //InjectManager webBeans = InjectManager.create();

      ContextConfig.setCurrent(context);
      
      setProperty("__FILE__", FileVar.__FILE__);
      setProperty("__DIR__", DirVar.__DIR__);
      setProperty("__PATH__", Vfs.lookup(topNode.getBaseURI()));

      // context.configureBean(obj, topNode);
      
      ConfigProgramBuilderXml builder = new ConfigProgramBuilderXml(this);
      
      ConfigProgram program = builder.buildTop(topNode);
      
      program.configure(obj);
    } finally {
      thread.setContextClassLoader(oldLoader);
      
      ContextConfig.setCurrent(oldContext);
    }
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public void configureBean(Object obj,
                            PathImpl path,
                            Schema schema)
    throws Exception
  {
    QDocument doc = parseDocument(path, schema);

    configureBean(obj, doc.getDocumentElement());
  }

  /**
   * Configures the bean from a path
   */
  private QDocument parseDocument(PathImpl path, Schema schema)
    throws ConfigExceptionLocation, IOException, org.xml.sax.SAXException
  {
    // server/2d33
    SoftReference<QDocument> docRef = null;//_parseCache.get(path);
    QDocument doc;

    if (docRef != null) {
      doc = docRef.get();

      if (doc != null && ! doc.isModified())
        return doc;
    }

    ReadStream is = path.openRead();

    try {
      doc = parseDocument(is, schema);

      // _parseCache.put(path, new SoftReference<QDocument>(doc));

      return doc;
    } finally {
      is.close();
    }
  }

  /**
   * Configures the bean from an input stream.
   */
  private QDocument parseDocument(InputStream is, Schema schema)
    throws ConfigExceptionLocation,
           IOException,
           org.xml.sax.SAXException
  {
    QDocument doc = new QDocument();
    DOMBuilder builder = new DOMBuilder();

    builder.init(doc);
    String systemId = null;
    String filename = null;
    PathImpl path = null;
    if (is instanceof ReadStream) {
      path = ((ReadStream) is).getPath();
      systemId = path.getURL();
      filename = path.getUserPath();
    }

    doc.setSystemId(systemId);
    builder.setSystemId(systemId);
    doc.setRootFilename(filename);
    builder.setFilename(filename);
    builder.setSkipWhitespace(true);

    InputSource in = new InputSource();
    in.setByteStream(is);
    in.setSystemId(systemId);

    Xml xml = new Xml();
    xml.setOwner(doc);
    xml.setFilename(filename);

    if (schema != null) {
      Verifier verifier = schema.newVerifier();
      VerifierFilter filter = verifier.getVerifierFilter();

      filter.setParent(xml);
      filter.setContentHandler(builder);
      filter.setErrorHandler(builder);

      filter.parse(in);
    }
    else {
      xml.setContentHandler(builder);
      xml.parse(in);
    }
    
    if (path != null) {
      ConfigAdmin.registerPath(path);
    }

    return doc;
  }

  private Schema findCompactSchema(String location)
    throws IOException, ConfigException
  {
    try {
      if (location == null)
        return null;

      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();

      if (loader == null)
        loader = ClassLoader.getSystemClassLoader();

      URL url = loader.getResource(location);

      if (url == null)
        return null;

      PathImpl path = Vfs.lookup(URLDecoder.decode(url.toString()));

      // VerifierFactory factory = VerifierFactory.newInstance("http://caucho.com/ns/compact-relax-ng/1.0");

      CompactVerifierFactoryImpl factory;
      factory = new CompactVerifierFactoryImpl();

      return factory.compileSchema(path);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  /**
   * Configures the bean from an input stream.
   */
  /*
  private ConfigProgram parseProgram(Path path)
  {
    try {
      ConfigFileParser parser = new ConfigFileParser(this);
    
      return parser.parse(path);
    } catch (IOException e) {
      throw ConfigException.create(e);
    }
  }
  */

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

  /**
   * Sets an attribute with a value.
   *
   * @param obj the bean to be set
   * @param attr the attribute name
   * @param value the attribute value
   */
  /*
  public static void setStringAttribute(Object obj, String attr, String value)
    throws Exception
  {
    ContextConfigXml builder = ConfigXml...(this);
    QAttr qAttr = new QAttr(attr);
    qAttr.setValue(value);

    builder.configureAttribute(obj, qAttr);
  }
  */

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
   * Returns the variable resolver.
   */
  /*
  public static ELContext getELEnvironment()
  {
    ContextConfigXml builder = ContextConfigXml.getCurrent();

    if (builder != null) {
      return builder.getELContext();
    }
    else
      return EL.getEnvironment();
  }
  */

  /**
   * Returns the variable resolver.
   */
  /*
  public static ConfigELContext getELContext()
  {
    ContextConfigXml builder = ContextConfigXml.getCurrent();

    if (builder != null) {
      return builder.getELContext();
    }
    else
      return null;
  }
  */

  public static Object getElVar(String var)
  {
    /*
    ELContext context = getELEnvironment();
    
    if (context != null)
      return context.getELResolver().getValue(context, null, var);
      */
    
    return getProperty(var);
  }

  /**
   * Sets an EL configuration variable.
   */
  public static Object getCurrentVar(String var)
  {
    // return InjectManager.create().findByName(var);
    return getProperty(var);
  }

  /**
   * Evaluates an EL string in the context.
   */
  public static String evalString(String str)
    throws ELException
  {
    return ConfigContext.evalString(str);
  }

  /**
   * Evaluates an EL string in the context.
   */
  public static String evalString(String str, HashMap<String,Object> varMap)
         throws ELException
  {
    return EL.evalString(str, getEnvironment(varMap));
  }

  /**
   * Evaluates an EL boolean in the context.
   */
  public static boolean evalBoolean(String str)
  {
    return ExprCfg.newParser(str).parse().evalBoolean(ConfigContext.getEnvironment());
  }

  public static ELContext getEnvironment(HashMap<String,Object> varMap)
  {
    if (varMap != null)
      return new EnvironmentContext(varMap);
    else
      return new EnvironmentContext();
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
      is = Vfs.lookup().lookup(systemId).openRead();
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

      if (value != null)
        return value;
      else if (_parent != null)
        return _parent.get(key);
      else
        return null;
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

