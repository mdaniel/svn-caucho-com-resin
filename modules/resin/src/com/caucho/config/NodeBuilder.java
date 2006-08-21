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
 *   Free SoftwareFoundation, Inc.
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

import javax.el.*;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Comment;
import org.w3c.dom.ProcessingInstruction;

import com.caucho.el.*;

import com.caucho.util.*;

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

import com.caucho.xml.QDocument;
import com.caucho.xml.QElement;
import com.caucho.xml.QAbstractNode;
import com.caucho.xml.QName;
import com.caucho.xml.QNode;
import com.caucho.xml.Xml;
import com.caucho.xml.XmlUtil;
import com.caucho.xml.DOMBuilder;

import com.caucho.config.types.ResinType;
import com.caucho.config.types.Validator;

import com.caucho.el.ELParser;
import com.caucho.el.Expr;
import com.caucho.el.EL;

/**
 * DOM builder is the interface for the
 * Node as input.
 * The other classes need to be independent of
 * Node because they might be moving to
 * something like XML schema.
 *
 * NodeBuilder will call routines in BeanBuilder.
 */
public class NodeBuilder {
  private final static L10N L = new L10N(NodeBuilder.class);
  private final static Logger log
    = Logger.getLogger(NodeBuilder.class.getName());

  private final static QName RESIN_TYPE = new QName("resin:type");
  private final static QName RESIN_TYPE_NS
    = new QName("resin:type", "http://caucho.com/ns/resin/core");

  private static ThreadLocal<NodeBuilder> _currentBuilder
    = new ThreadLocal<NodeBuilder>();

  private Config _config;

  private ArrayList<ValidatorEntry> _validators
    = new ArrayList<ValidatorEntry>();

  private ConfigELContext _elContext;
  private AbstractVariableResolver _varResolver;

  NodeBuilder()
  {
    _elContext = new ConfigELContext();
    _varResolver = (AbstractVariableResolver) _elContext.getELResolver();
  }
  
  NodeBuilder(Config config)
  {
    _config = config;
    _elContext = config.getELContext();

    if (_elContext == null)
      _elContext = new ConfigELContext();
    
    _varResolver = (AbstractVariableResolver) _elContext.getELResolver();
  }

  public static NodeBuilder getCurrentBuilder()
  {
    return _currentBuilder.get();
  }

  // s/b private?
  static void setCurrentBuilder(NodeBuilder builder)
  {
    _currentBuilder.set(builder);
  }

  /**
   * Returns true if EL expressions are used.
   */
  private boolean isEL()
  {
    // server/26b6
    return _config == null || _config.isEL();
  }

  /**
   * External call to configure a bean based on a top-level node, calling
   * init() and replaceObject() when done.
   *
   * @param bean the bean to be configured
   * @param top the top-level XML configuration node
   * @return the configured object, or the factory generated object
   */
  public Object configure(Object bean, Node top)
    throws LineConfigException
  {
    NodeBuilder oldBuilder = _currentBuilder.get();
    try {
      _currentBuilder.set(this);

      if (top instanceof QNode) {
	_varResolver.setValue(_elContext, "__FILE__", null,
			    ((QNode) top).getBaseURI());
      }

      TypeStrategy typeStrategy;
      typeStrategy = TypeStrategyFactory.getTypeStrategy(bean.getClass());

      return configureImpl(typeStrategy, bean, top);
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, top);
    } finally {
      _currentBuilder.set(oldBuilder);
    }
  }

  /**
   * External call to configure a bean based on a top-level node.
   * The init() and replaceObject() are not called.
   *
   * @param bean the object to be configured.
   */
  public void configureBean(Object bean, Node top)
    throws LineConfigException
  {
    NodeBuilder oldBuilder = _currentBuilder.get();
    try {
      _currentBuilder.set(this);

      if (top instanceof QNode) {
	_varResolver.setValue(_elContext, "__FILE__", null,
			    ((QNode) top).getBaseURI());
      }

      TypeStrategy typeStrategy;
      typeStrategy = TypeStrategyFactory.getTypeStrategy(bean.getClass());

      typeStrategy.configureBean(this, bean, top);
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, top);
    } finally {
      _currentBuilder.set(oldBuilder);
    }
  }

  /**
   * External call to configure a bean's attribute.
   *
   * @param bean the bean to be configured
   * @param attribute the node representing the configured attribute
   * @throws LineConfigException
   */
  public void configureAttribute(Object bean, Node attribute)
    throws LineConfigException
  {
    String attrName = attribute.getNodeName();

    if (attrName.equals("resin:type"))
      return;
    else if (attrName.startsWith("xmlns"))
      return;

    NodeBuilder oldBuilder = getCurrentBuilder();
    try {
      setCurrentBuilder(this);

      TypeStrategy typeStrategy = TypeStrategyFactory.getTypeStrategy(bean.getClass());

      typeStrategy.configureAttribute(this, bean, attribute);
    }
    catch (LineConfigException e) {
      throw e;
    }
    catch (Exception e) {
      throw error(e, attribute);
    } finally {
      setCurrentBuilder(oldBuilder);
    }
  }

  /**
   * Configures a bean, calling its init() and replaceObject() methods.
   *
   * @param typeStrategy the strategy for handling the bean's type
   * @param bean the bean instance
   * @param top the configuration top
   * @return the configured bean, possibly the replaced object
   * @throws LineConfigException
   */
  Object configureImpl(TypeStrategy typeStrategy,
                       Object bean,
		       Node top)
    throws LineConfigException
  {
    try {
      typeStrategy.configureBean(this, bean, top);

      typeStrategy.init(bean);

      return typeStrategy.replaceObject(bean);
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, top);
    }
  }

  /**
   * instantiates and configures a child bean
   *
   * @param typeStrategy the type strategy known to the parent
   * @param top the configuration top
   * @param parent the parent top
   *
   * @return the configured child
   *
   * @throws Exception
   */
  Object configureChildImpl(TypeStrategy typeStrategy, Node top, Object parent)
    throws Exception
  {
    Object bean = createResinType(top);

    if (bean == null && ! hasChildren(top)) {
      String value = textValue(top);

      if (isEL() && value != null &&
          value.startsWith("${") && value.endsWith("}")) {
        bean = evalObject(value);

	return bean;
      }
    }

    if (bean == null)
      bean = typeStrategy.create();

    typeStrategy = TypeStrategyFactory.getTypeStrategy(bean.getClass());

    typeStrategy.setParent(bean, parent);

    return configureImpl(typeStrategy, bean, top);
  }

  /**
   * Configures the bean with the values in the top.
   *
   * @param typeStrategy
   * @param bean
   * @param top top-level XML top
   * @throws Exception
   */
  void configureBeanImpl(TypeStrategy typeStrategy, Object bean, Node top)
    throws Exception
  {
    // XXX: need test for the CharacterData (<dependency-check-interval>)
    if (top instanceof Attr || top instanceof CharacterData) {
      QName qName = new QName("#text");

      AttributeStrategy attrStrategy = typeStrategy.getAttributeStrategy(qName);

      attrStrategy.configure(this, bean, qName, top);

      return;
    }

    NamedNodeMap attrList = top.getAttributes();
    if (attrList != null) {
      int length = attrList.getLength();
      for (int i = 0; i < length; i++) {
	Node child = attrList.item(i);

	configureAttributeImpl(typeStrategy, bean, child);
      }
    }

    Node child = top.getFirstChild();

    for (; child != null; child = child.getNextSibling()) {
      configureAttributeImpl(typeStrategy, bean, child);
    }
  }

  /**
   * ConfigureAttributeImpl is the main workhorse of the configuration.
   */
  void configureAttributeImpl(TypeStrategy typeStrategy,
		              Object bean,
			      Node node)
    throws Exception
  {
    try {
      QName qName = ((QAbstractNode) node).getQName();

      if (node instanceof Comment) {
        return;
      }
      else if (node instanceof DocumentType) {
        return;
      }
      else if (node instanceof ProcessingInstruction) {
        return;
      }
      else if (node instanceof CharacterData) {
        String data = ((CharacterData) node).getData();

        if (XmlUtil.isWhitespace(data))
	  return;
	
        qName = new QName("#text");
      }

      if (qName.getName().startsWith("xmlns"))
        return;
      else if (qName.getName().equals("resin:type"))
        return;

      AttributeStrategy attrStrategy = typeStrategy.getAttributeStrategy(qName);

      if (attrStrategy == null)
	throw error(L.l("{0} is an unknown property.", qName), node);

      attrStrategy.configure(this, bean, qName, node);
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, node);
    }
  }

  ArrayList<Dependency> getDependencyList(Node node)
  {
    ArrayList<Dependency> dependList = null;

    if (node instanceof QElement) {
      QElement qelt = (QElement) node;

      /* XXX: line #
      builder.setLocation(bean, qelt.getBaseURI(),
                          qelt.getFilename(), qelt.getLine());
      builder.setNode(bean, qelt);
      */

      ArrayList<Path> pathList;
      pathList = ((QDocument) qelt.getOwnerDocument()).getDependList();

      if (pathList != null) {
        dependList = new ArrayList<Dependency>();

        for (int i = 0; i < pathList.size(); i++) {
          dependList.add(new Depend(pathList.get(i)));
        }
      }
    }

    return dependList;
  }

  /** Configures a node, expecting an object in return.
   *
   * @param node the configuration node
   * @param parent
   * @return the configured object
   * @throws Exception
   */
  public Object configureObject(Node node, Object parent)
    throws Exception
  {
    Object resinTypeValue = createResinType(node);

    if (resinTypeValue != null) {
      Class type = resinTypeValue.getClass();
      TypeStrategy typeStrategy = TypeStrategyFactory.getTypeStrategy(type);

      typeStrategy.setParent(resinTypeValue, parent);

      return configureImpl(typeStrategy, resinTypeValue, node);
    }

    if (hasChildren(node))
      throw error(L.l("unexpected node {0}", node.getNodeName()), node); // XXX: qa

    String value = textValue(node);

    if (value == null)
      return null;
    else if (isEL() && value.indexOf("${") >= 0)
      return evalObject(value);
    else
      return value;
  }

  public String configureString(Node child)
    throws Exception
  {
    String value = configureRawString(child);

    if (value == null)
      return "";
    else if (isEL() && value.indexOf("${") >= 0)
      return evalString(value);
    else
      return value;
  }

  public String configureRawString(Node child)
    throws Exception
  {
    Object resinTypeValue = createResinType(child);

    if (resinTypeValue != null) {
      TypeStrategy typeStrategy =
              TypeStrategyFactory.getTypeStrategy(resinTypeValue.getClass());

      return String.valueOf(configureImpl(typeStrategy, resinTypeValue, child));
    }

    if (hasChildren(child))
      throw error(L.l("unexpected child nodes"), child); // XXX: qa

    String value = textValue(child);

    return value;
  }

  public String configureRawStringNoTrim(Node child)
    throws Exception
  {
    Object resinTypeValue = createResinType(child);

    if (resinTypeValue != null) {
      TypeStrategy typeStrategy =
              TypeStrategyFactory.getTypeStrategy(resinTypeValue.getClass());

      return String.valueOf(configureImpl(typeStrategy, resinTypeValue, child));
    }

    if (hasChildren(child))
      throw error(L.l("unexpected child nodes"), child); // XXX: qa

    String value = textValueNoTrim(child);

    return value;
  }

  /**
   * Create a custom resin:type value.
   */
  Object createResinType(Node child)
    throws Exception
  {
    String type = getValue(RESIN_TYPE, child, null);

    type = getValue(RESIN_TYPE_NS, child, type);

    if (type == null)
      return null;

    ResinType resinType = null;

    resinType = new ResinType();
    resinType.addText(type);
    resinType.init();

    return resinType.create(null);
  }

  /**
   * Configures a new object given the object's type.
   *
   * @param type the expected type of the object
   * @param node the configuration node
   * @return the configured object
   * @throws Exception
   */
  Object configureCreate(Class type, Node node)
    throws Exception
  {
    Object value = type.newInstance();

    return configure(value, node);
  }

  /**
   * Returns the variable resolver.
   */
  public ConfigELContext getELContext()
  {
    return _elContext;
  }

  /**
   * Returns the variable resolver.
   */
  public void setELContext(ConfigELContext elContext)
  {
    _elContext = elContext;
  }

  /**
   * Returns the variable resolver.
   */
  public Object putVar(String name, Object value)
  {
    ELResolver resolver = _elContext.getELResolver();
    Object oldValue = resolver.getValue(_elContext, name, null);

    resolver.setValue(_elContext, name, null, value);
    
    return oldValue;
  }

  /**
   * Returns the variable resolver.
   */
  public Object getVar(String name)
  {
    return _elContext.getELResolver().getValue(_elContext, name, null);
  }

  void addValidator(Validator validator)
  {
    _validators.add(new ValidatorEntry(validator));
  }

  static boolean hasChildren(Node node)
  {
    Node ptr;

    NamedNodeMap attrList = node.getAttributes();
    if (attrList != null) {
      for (int i = 0; i < attrList.getLength(); i++) {
	if (! attrList.item(i).getNodeName().startsWith("xml"))
	  return true;
      }
    }

    for (ptr = node.getFirstChild(); ptr != null; ptr = ptr.getNextSibling()) {
      if (ptr instanceof Element)
	return true;
    }

    return false;
  }

  static String getValue(QName name, Node node, String defaultValue)
  {
    NamedNodeMap attrList = node.getAttributes();
    if (attrList != null) {
      for (int i = 0; i < attrList.getLength(); i++) {
	if (attrList.item(i).getNodeName().equals(name.getName()))
	  return attrList.item(i).getNodeValue();
      }
    }

    Node ptr;

    for (ptr = node.getFirstChild(); ptr != null; ptr = ptr.getNextSibling()) {
      QName qName = ((QAbstractNode) ptr).getQName();

      if (name.equals(qName))
	return textValue(ptr);
    }

    return defaultValue;
  }

  /**
   * Returns the text value of the node.
   */
  static String textValue(Node node)
  {
    if (node instanceof Attr)
      return node.getNodeValue();
    else {
      String value = XmlUtil.textValue(node);

      if (value == null || value.equals(""))
	return "";
      else if (node instanceof Element) {
	String space = ((Element) node).getAttribute("xml:space");

	if (! space.equals(""))
	  return value;
      }

      return value.trim();
    }
  }

  /**
   * Returns the text value of the node.
   */
  static String textValueNoTrim(Node node)
  {
    if (node instanceof Attr)
      return node.getNodeValue();
    else {
      String value = XmlUtil.textValue(node);

      if (value == null || value.equals(""))
	return "";
      else if (node instanceof Element) {
	String space = ((Element) node).getAttribute("xml:space");

	if (! space.equals(""))
	  return value;
      }

      return value;
    }
  }

  /**
   * Evaluate as a string.
   */
  public String evalString(String exprString)
    throws ELException
  {
    if (exprString.indexOf("${") >= 0 && isEL()) {
      ELParser parser = new ELParser(getELContext(), exprString);
      parser.setCheckEscape(true);
      Expr expr = parser.parse();

      return expr.evalString(getELContext());
    }
    else
      return exprString;
  }

  /**
   * Evaluate as a string.
   */
  public boolean evalBoolean(String exprString)
    throws ELException
  {
    if (exprString.indexOf("${") >= 0 && isEL()) {
      ELParser parser = new ELParser(getELContext(), exprString);
      parser.setCheckEscape(true);
      Expr expr = parser.parse();
      
      return expr.evalBoolean(getELContext());
    }
    else
      return Expr.toBoolean(exprString, null);
  }

  /**
   * Evaluate as a long.
   */
  public long evalLong(String exprString)
    throws ELException
  {
    if (exprString.indexOf("${") >= 0 && isEL()) {
      ELParser parser = new ELParser(getELContext(), exprString);
      parser.setCheckEscape(true);
      Expr expr = parser.parse();
      
      return expr.evalLong(getELContext());
    }
    else
      return Expr.toLong(exprString, null);
  }

  /**
   * Evaluate as a double.
   */
  public double evalDouble(String exprString)
    throws ELException
  {
    if (exprString.indexOf("${") >= 0 && isEL()) {
      ELParser parser = new ELParser(getELContext(), exprString);
      parser.setCheckEscape(true);
      Expr expr = parser.parse();
      
      return expr.evalDouble(getELContext());
    }
    else
      return Expr.toDouble(exprString, null);
  }

  /**
   * Evaluate as an object
   */
  public Object evalObject(String exprString)
    throws ELException
  {
    if (exprString.indexOf("${") >= 0 && isEL()) {
      ELParser parser = new ELParser(getELContext(), exprString);
      parser.setCheckEscape(true);
      Expr expr = parser.parse();

      return expr.evalObject(getELContext());
    }
    else
      return exprString;
  }

  public static LineConfigException error(String msg, Node node)
  {
    String filename = null;
    int line = 0;

    if (node instanceof QAbstractNode) {
      QAbstractNode qnode = (QAbstractNode) node;
      
      filename = qnode.getFilename();
      line = qnode.getLine();
    }

    if (filename != null)
      return new LineConfigException(filename, line, msg);
    else
      return new LineConfigException(msg);
  }
  
  public static LineConfigException error(Throwable e, Node node)
  {
    String filename = null;
    int line = 0;

    if (node instanceof QAbstractNode) {
      QAbstractNode qnode = (QAbstractNode) node;
      
      filename = qnode.getFilename();
      line = qnode.getLine();
    }
    
    for (; e.getCause() != null; e = e.getCause()) {
      if (e instanceof LineCompileException)
        break;
      else if (e instanceof LineConfigRuntimeException)
        break;
      else if (e instanceof CompileException)
        break;
    }

    if (e instanceof LineConfigException)
      return (LineConfigException) e;
    else if (e instanceof LineConfigRuntimeException)
      throw (LineConfigRuntimeException) e;
    else if (e instanceof ConfigException &&
             e.getMessage() != null &&
             filename != null) {
      return new LineConfigException(filename, line, e);
    }
    else if (e instanceof LineCompileException) {
      return new LineConfigException(e.getMessage(), e);
    }
    else if (e instanceof CompileException && e.getMessage() != null) {
      return new LineConfigException(filename, line, e);
    }
    else {
      log.log(Level.CONFIG, e.toString(), e);
      
      String msg = filename + ":" + line + ": " + e;

      if (e instanceof RuntimeException) {
	throw new LineConfigRuntimeException(msg, e);
      }
      else
	return new LineConfigException(msg, e);
    }
  }

  static class ValidatorEntry {
    private Validator _validator;
    private ClassLoader _loader;

    ValidatorEntry(Validator validator)
    {
      _validator = validator;

      _loader = Thread.currentThread().getContextClassLoader();
    }

    void validate()
      throws ConfigException
    {
      Thread thread = Thread.currentThread();
      ClassLoader oldLoader = thread.getContextClassLoader();

      try {
	thread.setContextClassLoader(_loader);

	_validator.validate();
      } finally {
	thread.setContextClassLoader(oldLoader);
      }
    }
  }
}
