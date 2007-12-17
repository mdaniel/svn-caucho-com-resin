/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.config;

import com.caucho.el.EL;
import com.caucho.el.EnvironmentContext;
import com.caucho.relaxng.CompactVerifierFactoryImpl;
import com.caucho.relaxng.Schema;
import com.caucho.relaxng.Verifier;
import com.caucho.relaxng.VerifierFilter;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.util.LruCache;
import com.caucho.vfs.*;
import com.caucho.xml.DOMBuilder;
import com.caucho.xml.QDocument;
import com.caucho.xml.QName;
import com.caucho.xml.QAttr;
import com.caucho.xml.Xml;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Facade for Resin's configuration builder.
 */
public class Config {
  private static final L10N L = new L10N(Config.class);
  private static final Logger log
    = Logger.getLogger(Config.class.getName());

  private static LruCache<Path,SoftReference<QDocument>> _parseCache
    = new LruCache<Path,SoftReference<QDocument>>(32);

  // Copied from parent for resin:import, server/13jk
  private ConfigVariableResolver _varResolver;
  
  private HashMap<String,Object> _vars
    = new HashMap<String,Object>();
  
  // the context class loader of the config
  private ClassLoader _classLoader;

  private ConfigLibrary _configLibrary;

  private boolean _isEL = true;
  private boolean _isIgnoreEnvironment;
  private boolean _allowResinInclude;

  public Config()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * @param loader the class loader environment to use.
   */
  public Config(ClassLoader loader)
  {
    _classLoader = loader;

    _configLibrary = ConfigLibrary.getLocal(_classLoader);
  }

  /**
   * Set true if resin:include should be allowed.
   */
  public void setResinInclude(boolean useResinInclude)
  {
    _allowResinInclude = useResinInclude;
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
   * Sets the variable resolver.
   */
  public void setConfigVariableResolver(ConfigVariableResolver varResolver)
  {
    _varResolver = varResolver;
  }

  /**
   * Gets the variable resolver.
   */
  public ConfigVariableResolver getConfigVariableResolver()
  {
    return _varResolver;
  }

  /**
   * Configures a bean with a configuration file.
   */
  public Object configure(Object obj, Path path)
    throws ConfigException
  {
    try {
      QDocument doc = parseDocument(path, null);

      return configure(obj, doc.getDocumentElement());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

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
  public Object configure(Object obj, Path path, String schemaLocation)
    throws ConfigException
  {
    try {
      Schema schema = findCompactSchema(schemaLocation);

      QDocument doc = parseDocument(path, schema);

      return configure(obj, doc.getDocumentElement());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw LineConfigException.create(e);
    }
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public Object configure(Object obj, Path path, Schema schema)
    throws ConfigException
  {
    try {
      QDocument doc = parseDocument(path, schema);

      return configure(obj, doc.getDocumentElement());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
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

    try {
      thread.setContextClassLoader(_classLoader);

      NodeBuilder builder = createBuilder();

      return builder.configure(obj, topNode);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public void configureBean(Object obj,
			    Path path,
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
  public void configureBean(Object obj, Path path)
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

    try {
      thread.setContextClassLoader(_classLoader);

      NodeBuilder builder = createBuilder();

      builder.configureBean(obj, topNode);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  private NodeBuilder createBuilder()
  {
    NodeBuilder builder = new NodeBuilder(this);

    for (String var : _vars.keySet())
      builder.putVar(var, _vars.get(var));

    ConfigLibrary lib = ConfigLibrary.getLocal();

    HashMap<String,Method> methodMap = lib.getMethodMap();

    for (Map.Entry<String,Method> entry : methodMap.entrySet()) {
      builder.putVar(entry.getKey(), entry.getValue());
    }

    return builder;
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public void configureBean(Object obj,
			    Path path,
			    Schema schema)
    throws Exception
  {
    QDocument doc = parseDocument(path, schema);

    configureBean(obj, doc.getDocumentElement());
  }
  
  /**
   * Configures the bean from a path
   */
  private QDocument parseDocument(Path path, Schema schema)
    throws LineConfigException, IOException, org.xml.sax.SAXException
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
    throws LineConfigException,
	   IOException,
	   org.xml.sax.SAXException
  {
    QDocument doc = new QDocument();
    DOMBuilder builder = new DOMBuilder();

    builder.init(doc);
    String systemId = null;
    String filename = null;
    if (is instanceof ReadStream) {
      systemId = ((ReadStream) is).getPath().getURL();
      filename = ((ReadStream) is).getPath().getUserPath();
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
    xml.setResinInclude(_allowResinInclude);
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
      
      Path path = Vfs.lookup(URLDecoder.decode(url.toString()));

      // VerifierFactory factory = VerifierFactory.newInstance("http://caucho.com/ns/compact-relax-ng/1.0");
          
      CompactVerifierFactoryImpl factory;
      factory = new CompactVerifierFactoryImpl();

      return factory.compileSchema(path);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Configures a bean with a configuration map.
   */
  public Object configureMap(Object obj, Map<String,Object> map)
    throws Exception
  {
    return new MapBuilder().configure(obj, map);
  }
  
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
   * Sets an attribute with a value.
   *
   * @param obj the bean to be set
   * @param attr the attribute name
   * @param value the attribute value
   */
  public static void setAttribute(Object obj, String attr, Object value)
    throws Exception
  {
    TypeStrategy strategy = TypeStrategyFactory.getTypeStrategy(obj.getClass());

    QName attrName = new QName(attr);
    AttributeStrategy attrStrategy = strategy.getAttributeStrategy(attrName);
    if (attrStrategy == null)
      throw new ConfigException(L.l("{0}: '{1}' is an unknown attribute.",
				    obj.getClass().getName(),
				    attrName.getName()));
    else if (value instanceof String)
      attrStrategy.setAttribute(obj, attrName, attrStrategy.convert((String) value));
    else
      attrStrategy.setAttribute(obj, attrName, value);
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
    NodeBuilder builder = new NodeBuilder();
    QAttr qAttr = new QAttr(attr);
    qAttr.setValue(value);

    builder.configureAttribute(obj, qAttr);
  }

  public static void init(Object bean)
    throws ConfigException
  {
    try {
      TypeStrategy strategy;

      strategy = TypeStrategyFactory.getTypeStrategy(bean.getClass());

      strategy.init(bean);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public static Object replaceObject(Object bean) throws Exception
  {
    TypeStrategy strategy = TypeStrategyFactory.getTypeStrategy(bean.getClass());

    return strategy.replaceObject(bean);
  }

  /**
   * Returns the variable resolver.
   */
  public static ELContext getEnvironment()
  {
    NodeBuilder builder = NodeBuilder.getCurrentBuilder();

    if (builder != null) {
      return builder.getELContext();
    }
    else
      return EL.getEnvironment();
  }

  /**
   * Returns the variable resolver.
   */
  public static ConfigELContext getELContext()
  {
    NodeBuilder builder = NodeBuilder.getCurrentBuilder();

    if (builder != null) {
      return builder.getELContext();
    }
    else
      return null;
  }

  /**
   * Returns the variable resolver.
   */
  public static void setELContext(ConfigELContext context)
  {
    NodeBuilder builder = NodeBuilder.getCurrentBuilder();

    if (builder != null) {
      builder.setELContext(context);
    }
  }

  /**
   * Sets an EL configuration variable.
   */
  public void setVar(String var, Object value)
  {
    setCurrentVar(var, value);

    _vars.put(var, value);
  }

  /**
   * Sets an EL configuration variable.
   */
  public static void setCurrentVar(String var, Object value)
  {
    NodeBuilder builder = NodeBuilder.getCurrentBuilder();

    if (builder != null)
      builder.putVar(var, value);
  }

  /**
   * Sets an EL configuration variable.
   */
  public static Object getCurrentVar(String var)
  {
    NodeBuilder builder = NodeBuilder.getCurrentBuilder();

    if (builder != null)
      return builder.getVar(var);
    else
      return null;
  }

  /**
   * Gets an EL configuration variable.
   */
  public static Object getVar(String var) throws ELException
  {
    return getEnvironment().getELResolver().getValue(getEnvironment(),
						     var, null);
  }

  /**
   * Evaluates an EL string in the context.
   */
  public static String evalString(String str)
    throws ELException
  {
    return AttributeStrategy.evalString(str);
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
    throws ELException
  {
    return AttributeStrategy.evalBoolean(str);
  }

  public static ELContext getEnvironment(HashMap<String,Object> varMap)
  {
    ELContext context = Config.getEnvironment();

    ELResolver parent = null;

    if (context != null)
      parent = context.getELResolver();

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
}

