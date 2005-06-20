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
 *
 * @author Scott Ferguson
 */

package com.caucho.config;

import java.io.InputStream;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

import java.util.logging.Logger;

import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;

import java.lang.ref.SoftReference;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import org.iso_relax.verifier.VerifierFactory;
import org.iso_relax.verifier.Schema;
import org.iso_relax.verifier.Verifier;
import org.iso_relax.verifier.VerifierFilter;
import org.iso_relax.verifier.VerifierConfigurationException;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;

import org.w3c.dom.Node;

import com.caucho.relaxng.CompactVerifierFactoryImpl;

import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.util.LruCache;

import com.caucho.vfs.Path;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.IOExceptionWrapper;

import com.caucho.xml.QName;
import com.caucho.xml.DOMBuilder;
import com.caucho.xml.QDocument;
import com.caucho.xml.Xml;

import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;

/**
 * Facade for Resin's configuration builder.
 */
public class Config {
  private static final L10N L = new L10N(Config.class);
  private static final Logger log = Log.open(Config.class);

  private static LruCache<Path,SoftReference<QDocument>> _parseCache =
    new LruCache<Path,SoftReference<QDocument>>(32);

  // the context class loader of the config
  private ClassLoader _classLoader;

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
  }

  /**
   * Set true if resin:include should be allowed.
   */
  public void setResinInclude(boolean useResinInclude)
  {
    _allowResinInclude = useResinInclude;
  }

  /**
   * Configures a bean with a configuration file.
   */
  public Object configure(Object obj, Path path)
    throws Exception
  {
    QDocument doc = parseDocument(path, null);

    return configure(obj, doc.getDocumentElement());
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
    throws Exception
  {
    Schema schema = findCompactSchema(schemaLocation);

    QDocument doc = parseDocument(path, schema);

    return configure(obj, doc.getDocumentElement());
  }

  /**
   * Configures a bean with a configuration file and schema.
   */
  public Object configure(Object obj, Path path, Schema schema)
    throws Exception
  {
    QDocument doc = parseDocument(path, schema);

    return configure(obj, doc.getDocumentElement());
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

      return new NodeBuilder().configure(obj, topNode);
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

      new NodeBuilder().configureBean(obj, topNode);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
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
    try {
      QDocument doc = new QDocument();
      DOMBuilder builder = new DOMBuilder();

      builder.init(doc);
      String systemId = null;
      if (is instanceof ReadStream) {
	systemId = ((ReadStream) is).getPath().getUserPath();
      }

      doc.setSystemId(systemId);
      builder.setSystemId(systemId);
      builder.setSkipWhitespace(true);

      InputSource in = new InputSource();
      in.setByteStream(is);
      in.setSystemId(systemId);

      Xml xml = new Xml();
      xml.setOwner(doc);
      xml.setResinInclude(_allowResinInclude);

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
    } catch (VerifierConfigurationException e) {
      throw new IOExceptionWrapper(e);
    }
  }
  
  private Schema findCompactSchema(String location)
    throws IOException, ConfigException
  {
    try {
      if (location == null)
	return null;
      
      MergePath schemaPath = new MergePath();
      schemaPath.addClassPath();
      
      Path path = schemaPath.lookup(location);
      if (path.canRead()) {
	// VerifierFactory factory = VerifierFactory.newInstance("http://caucho.com/ns/compact-relax-ng/1.0");
          
	CompactVerifierFactoryImpl factory;
	factory = new CompactVerifierFactoryImpl();

	return factory.compileSchema(path);
      }
      else
	return null;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  /**
   * Configures a bean with a configuration map.
   */
  public Object configure(Object obj, Map<String,Object> map)
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

  /**
   * Evaluates an EL string in the context.
   */
  public static String evalString(String str, HashMap<String,Object> varMap)
         throws ELException
  {
    return EL.evalString(str, getEnvironment(varMap));
  }

  public static VariableResolver getEnvironment(HashMap<String,Object> varMap)
  {
    VariableResolver parent = Config.getEnvironment();

    if (varMap != null)
      return new MapVariableResolver(varMap, parent);
    else
      return parent;
  }
}

