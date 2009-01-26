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
 */

package com.caucho.config;

import com.caucho.config.inject.ComponentImpl;
import com.caucho.config.program.NodeBuilderChildProgram;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.scope.DependentScope;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.types.Validator;
import com.caucho.config.type.*;
import com.caucho.config.types.*;
import com.caucho.config.attribute.*;
import com.caucho.el.ELParser;
import com.caucho.el.Expr;
import com.caucho.loader.*;
import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.*;

import org.w3c.dom.*;

import javax.el.*;
import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.context.CreationalContext;

/**
 * The ConfigContext contains the state of the current configuration.
 */
public class ConfigContext implements CreationalContext {
  private final static L10N L = new L10N(ConfigContext.class);
  private final static Logger log
    = Logger.getLogger(ConfigContext.class.getName());

  private final static QName RESIN_TYPE = new QName("resin:type", null);
  private final static QName RESIN_TYPE_NS
    = new QName("resin:type", "http://caucho.com/ns/resin/core");

  private final static QName RESIN_CLASS = new QName("resin:class", null);
  private final static QName RESIN_CLASS_NS
    = new QName("resin:class", "http://caucho.com/ns/resin/core");

  private final static QName RESIN_PARAM = new QName("resin:param", null);
  private final static QName RESIN_PARAM_NS
    = new QName("resin:param", "http://caucho.com/ns/resin/core");
  
  private final static QName TEXT = new QName("#text");
  private final static QName VALUE = new QName("value");

  private final static Object NULL = new Object();

  private final static HashSet<QName> _resinClassSet = new HashSet<QName>();

  private static ThreadLocal<ConfigContext> _currentBuilder
    = new ThreadLocal<ConfigContext>();

  private Config _config;

  private ConfigELContext _elContext = new ConfigELContext();
  
  private DependentScope _dependentScope;

  private ArrayList<Dependency> _dependList;
  private Document _dependDocument;

  private String _baseUri;

  public ConfigContext()
  {
  }

  public ConfigContext(ComponentImpl component,
		       Object value,
		       ScopeContext scope)
  {
    this();

    _dependentScope = new DependentScope(component, value, scope);
  }

  public ConfigContext(ScopeContext scope)
  {
    this();

    _dependentScope = new DependentScope(scope);
  }
  
  ConfigContext(Config config)
  {
    _config = config;
  }
  
  public static ConfigContext create()
  {
    ConfigContext env = _currentBuilder.get();
    
    if (env != null)
      return env;
    else
      return new ConfigContext();
  }

  public static ConfigContext createForProgram()
  {
    return new ConfigContext();
  }

  public static ConfigContext getCurrentBuilder()
  {
    return _currentBuilder.get();
  }

  public static ConfigContext getCurrent()
  {
    return _currentBuilder.get();
  }

  // s/b private?
  static void setCurrentBuilder(ConfigContext builder)
  {
    _currentBuilder.set(builder);
  }

  /**
   * Returns the file var
   */
  public String getBaseUri()
  {
    return Vfs.decode(_baseUri);
  }

  public static String decode(String uri)
  {
    StringBuilder sb = new StringBuilder();

    int len = uri.length();

    for (int i = 0; i < len; i++) {
      char ch = uri.charAt(i);

      if (ch != '%' || len <= i + 2) {
	sb.append(ch);
	continue;
      }

      int d1 = uri.charAt(i + 1);
      int d2 = uri.charAt(i + 2);
      int v = 0;

      if ('0' <= d1 && d1 <= '9')
	v = 16 * v + d1 - '0';
      else if ('a' <= d1 && d1 <= 'f')
	v = 16 * v + d1 - 'a' + 10;
      else if ('A' <= d1 && d1 <= 'F')
	v = 16 * v + d1 - 'A' + 10;
      else {
	sb.append('%');
	continue;
      }

      if ('0' <= d2 && d2 <= '9')
	v = 16 * v + d2 - '0';
      else if ('a' <= d2 && d2 <= 'f')
	v = 16 * v + d2 - 'a' + 10;
      else if ('A' <= d2 && d2 <= 'F')
	v = 16 * v + d2 - 'A' + 10;
      else {
	sb.append('%');
	continue;
      }

      sb.append((char) v);
      i += 2;
    }

    return sb.toString();
  }

  /**
   * WebBeans method
   * 
   * @param aThis
   * @param value
   */
  public void addDestructor(ComponentImpl comp, Object value)
  {
    if (_dependentScope != null)
      _dependentScope.addDestructor(comp, value);
    else if (comp instanceof Closeable)
      Environment.addCloseListener((Closeable) comp);
  }

  public boolean canInject(ScopeContext scope)
  {
    return _dependentScope == null || _dependentScope.canInject(scope);
  }

  public boolean canInject(Class scopeType)
  {
    return _dependentScope == null || _dependentScope.canInject(scopeType);
  }

  /**
   * Returns the component value for the dependent scope
   * 
   * @param aThis
   * @return
   */
  public Object get(ComponentImpl comp)
  {
    if (_dependentScope != null)
      return _dependentScope.get(comp);
    else
      return null;
  }

  public Config getConfig()
  {
    return _config;
  }

  /**
   * WebBeans dependent scope setting
   * 
   * @param aThis
   * @param obj
   */
  public void put(ComponentImpl comp, Object obj)
  {
    if (_dependentScope == null)
      _dependentScope = new DependentScope();

    _dependentScope.put(comp, obj);
  }

  /**
   * Returns true if EL expressions are used.
   */
  private boolean isEL()
  {
    // server/26b6
    return _config == null || _config.isEL();
  }

  public boolean isIgnoreEnvironment()
  {
    return _config != null && _config.isIgnoreEnvironment();
  }

  /**
   * External call to configure a bean based on a top-level node.
   * The init() and replaceObject() are not called.
   *
   * @param bean the object to be configured.
   */
  public Object configure(Object bean, Node top)
    throws LineConfigException
  {
    if (bean == null)
      throw new NullPointerException(L.l("unexpected null bean at node '{0}'", top));
    
    ConfigContext oldBuilder = _currentBuilder.get();
    try {
      _currentBuilder.set(this);

      ConfigType type = TypeFactory.getType(bean);

      configureBean(bean, top);

      type.init(bean);

      return type.replaceObject(bean);
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, top);
    } finally {
      _currentBuilder.set(oldBuilder);
    }
  }

  /**
   * External call to configure a bean based on a top-level node, calling
   * init() and replaceObject() when done.
   *
   * @param bean the bean to be configured
   * @param top the top-level XML configuration node
   * @return the configured object, or the factory generated object
   */
  public void configureBean(Object bean, Node top)
    throws LineConfigException
  {
    ConfigContext oldBuilder = _currentBuilder.get();
    String oldFile = _baseUri;
    ArrayList<Dependency> oldDependList = _dependList;

    try {
      _currentBuilder.set(this);

      if (top instanceof QNode) {
        QNode qNode = (QNode) top;
        
	_baseUri = qNode.getBaseURI();
      }

      _dependList = getDependencyList(top);

      ConfigType type = TypeFactory.getType(bean);

      configureNode(top, bean, type);
    } finally {
      _currentBuilder.set(oldBuilder);

      _dependList = oldDependList;
      _baseUri = oldFile;
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

    String oldFile = _baseUri;
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    ConfigContext oldBuilder = getCurrentBuilder();
    try {
      setCurrentBuilder(this);

      _baseUri = attribute.getBaseURI();
      
      ConfigType type = TypeFactory.getType(bean);

      QName qName = ((QAbstractNode) attribute).getQName();
      
      type.beforeConfigure(this, bean, attribute);

      configureChildNode(attribute, qName, bean, type, false);
      
      type.afterConfigure(this, bean);
    }
    catch (LineConfigException e) {
      throw e;
    }
    catch (Exception e) {
      throw error(e, attribute);
    } finally {
      _baseUri = oldFile;
      setCurrentBuilder(oldBuilder);
      thread.setContextClassLoader(oldLoader);
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
  public Object configureNode(Node node,
			      Object bean,
			      ConfigType type)
    throws LineConfigException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      type.beforeConfigure(this, bean, node);
      type.beforeConfigureBean(this, bean, node);
      
      configureNodeAttributes(node, bean, type);

      for (Node childNode = node.getFirstChild();
           childNode != null;
           childNode = childNode.getNextSibling()) {
        QName qName = ((QAbstractNode) childNode).getQName();
        
        configureChildNode(childNode, qName, bean, type, false);
      }

      type.afterConfigure(this, bean);
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, node);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    return bean;
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
  private void configureNodeAttributes(Node node,
                                       Object bean,
                                       ConfigType type)
    throws Exception
  {
    if (node instanceof QAttributedNode) {
      Node child = ((QAttributedNode) node).getFirstAttribute();

      for (; child != null; child = child.getNextSibling()) {
        Attr attr = (Attr) child;
        QName qName = ((QNode) attr).getQName();
        
        configureChildNode(attr, qName, bean, type, false);
      }
    }
    else {
      NamedNodeMap attrList = node.getAttributes();
      if (attrList != null) {
        int length = attrList.getLength();
        for (int i = 0; i < length; i++) {
          Attr attr = (Attr) attrList.item(i);
          QName qName = ((QNode) attr).getQName();

          configureChildNode(attr, qName, bean, type, false);
        }
      }
    }
  }
  
  private void configureChildNode(Node childNode,
                                  QName qName,
                                  Object bean,
                                  ConfigType type,
				  boolean allowParam)
    throws Exception
  {
    if (qName.getName().startsWith("xmlns")
	|| ! allowParam && _resinClassSet.contains(qName)) {
      return;
    }

    Attribute attrStrategy;

    try {
      attrStrategy = type.getAttribute(qName);

      if (attrStrategy == null)
	attrStrategy = type.getProgramAttribute();

      if (attrStrategy == null) {
	// ioc/2252 - flow attributes are not captured by ContentProgram
	
	attrStrategy = type.getContentProgramAttribute();

	Attribute envStrategy
	  = TypeFactory.getFactory().getEnvironmentAttribute(qName);

	if (envStrategy instanceof FlowAttribute)
	  attrStrategy = null;
      }

      if (attrStrategy == null)
	attrStrategy = TypeFactory.getFactory().getEnvironmentAttribute(qName);

      if (attrStrategy == null) {
	if (childNode instanceof Element || childNode instanceof Attr) {
	  String localName = qName.getLocalName();
	  
	  if (localName.indexOf(':') >= 0) {
	    // XXX: need ioc QA
	    throw error(L.l("'{0}' does not have a defined namespace for 'xmlns:{1}'.  Tags with prefixes need defined namespaces.",
			    qName.getName(),
			    localName.substring(0, localName.indexOf(':'))),
			childNode);
	  }
	  
	  throw error(L.l("'{0}' is an unknown property of '{1}'.",
			  qName.getName(), type.getTypeName()),
		      childNode);
	}

	return;
      }

      if (attrStrategy.isProgram()) {
	attrStrategy.setValue(bean, qName,
			      buildProgram(attrStrategy, childNode));
	return;
      }
      else if (attrStrategy.isNode()) {
	attrStrategy.setValue(bean, qName, childNode);
	return;
      }

      ConfigType childType = null;
      Object childBean;

      if (childNode instanceof Element) {
	childBean = createResinType(attrStrategy.getConfigType(),
				    (Element) childNode);

	if (childBean != null) {
	  ConfigType childBeanType = TypeFactory.getType(childBean);

	  // server/02e3
	  // childBeanType.init(childBean);

	  childBean = childBeanType.replaceObject(childBean);
	  
	  attrStrategy.setValue(bean, qName, childBean);
	  return;
	}
      }

      String text;

      if (attrStrategy.isAllowText()
	  && (text = getTextValue(childNode)) != null) {
	boolean isTrim = isTrim(childNode);
	  
	if (isEL() && attrStrategy.isEL()
	    && (text.indexOf("#{") >= 0 || text.indexOf("${") >= 0)) {
	  if (isTrim)
	    text = text.trim();
	  
	  Object elValue = eval(attrStrategy.getConfigType(), text);

	  // ioc/2410
	  if (elValue != NULL)
	    attrStrategy.setValue(bean, qName, elValue);
	  else
	    attrStrategy.setValue(bean, qName, null);
	}
	else {
	  setText(bean, qName, text, attrStrategy, isTrim);
	}

	return;
      }
      else
	childBean = attrStrategy.create(bean, qName);

      if (childBean != null) {
	ConfigType childBeanType = TypeFactory.getType(childBean);
	
	if (childNode instanceof Element)
	  configureNode(childNode, childBean, childBeanType);
	else
	  configureChildNode(childNode, TEXT, childBean, childBeanType, false);

	childBeanType.init(childBean);

	Object newBean = attrStrategy.replaceObject(childBean);
	if (newBean != childBean)
	  childBean = newBean;
	else
	  childBean = childBeanType.replaceObject(childBean);

	attrStrategy.setValue(bean, qName, childBean);
      }
      else if ((childBean = getElementValue(attrStrategy, childNode)) != null) {
	if (childBean != NULL)
	  attrStrategy.setValue(bean, qName, childBean);
	else
	  attrStrategy.setValue(bean, qName, null);
      }
      else {
	ConfigType attrType = attrStrategy.getConfigType();
	
	String textValue;

	if (attrType.isNoTrim())
	  textValue = textValueNoTrim(childNode);
	else
	  textValue = textValue(childNode);

	if (isEL() && attrType.isEL() && textValue.indexOf("${") >= 0) {
	  childType = attrStrategy.getConfigType();
	  
	  Object value = childType.valueOf(evalObject(textValue));
	  
	  attrStrategy.setValue(bean, qName, value);
	}
	else {
	  // needed for boolean
	  attrStrategy.setText(bean, qName, textValue);
	}
      }
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, childNode);
    }
  }
  
  public Object create(Node childNode, ConfigType type)
    throws ConfigException
  {
    if (childNode instanceof Element) {
      Object childBean = createResinType(type, (Element) childNode);

      if (childBean != null) {
	ConfigType childBeanType = TypeFactory.getType(childBean);
	
	childBeanType.init(childBean);

	return childBeanType.replaceObject(childBean);
      }
    }

    try {
      Object childBean;
      String text;

      if ((text = getTextValue(childNode)) != null) {
	boolean isTrim = isTrim(childNode);

	if (isEL() && type.isEL()
	    && (text.indexOf("#{") >= 0 || text.indexOf("${") >= 0)) {
	  if (isTrim)
	    text = text.trim();
	  
	  Object elValue = eval(type, text);

	  // ioc/2410
	  if (elValue != NULL)
	    return elValue;
	  else
	    return null;
	}
	else {
	  return text;
	}
      }
      else
	childBean = type.create(null, TEXT);

      if (childBean != null) {
	ConfigType childBeanType = TypeFactory.getType(childBean);
	
	if (childNode instanceof Element)
	  configureNode(childNode, childBean, childBeanType);
	else
	  configureChildNode(childNode, TEXT, childBean, childBeanType, false);

	childBeanType.init(childBean);

	return childBeanType.replaceObject(childBean);
      }
      /*
      else if ((childBean = getElementValue(attrStrategy, childNode)) != null) {
	if (childBean != NULL)
	  attrStrategy.setValue(bean, qName, childBean);
	else
	  attrStrategy.setValue(bean, qName, null);
      }
      */
      else {
	String textValue;

	if (type.isNoTrim())
	  textValue = textValueNoTrim(childNode);
	else
	  textValue = textValue(childNode);

	if (isEL() && type.isEL() && textValue.indexOf("${") >= 0) {
	  Object value = type.valueOf(evalObject(textValue));
	  
	  return value;
	}
	else
	  return type.valueOf(textValue);
      }
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, childNode);
    }
  }
  
  private void setText(Object bean,
		       QName qName,
		       String text,
		       Attribute attrStrategy,
		       boolean isTrim)
    throws Exception
  {
    ConfigType attrType = attrStrategy.getConfigType();
	
    if (isTrim && ! attrType.isNoTrim())
      text = text.trim();

    if (isEL() && attrType.isEL() && text.indexOf("${") >= 0) {
      ConfigType childType = attrStrategy.getConfigType();
	  
      Object value = childType.valueOf(evalObject(text));
	  
      attrStrategy.setValue(bean, qName, value);
    }
    else
      attrStrategy.setText(bean, qName, text);
  }

  private boolean isTrim(Node node)
  {
    if (node instanceof Attr)
      return false;
    else if (node instanceof Element) {
      Element elt = (Element) node;

      if (! "".equals(elt.getAttribute("xml:space")))
	return false;
    }

    return true;
  }

  private ConfigProgram buildProgram(Attribute attr, Node node)
  {
    return new NodeBuilderChildProgram(node);
  }
  
  private void configureChildAttribute(Attr childNode,
				       QName qName,
				       Object bean,
				       ConfigType type)
    throws Exception
  {
    if (qName.getName().startsWith("xmlns")
	|| _resinClassSet.contains(qName)) {
      return;
    }

    Attribute attrStrategy;

    try {
      attrStrategy = type.getAttribute(qName);

      if (attrStrategy == null) {
	throw error(L.l("'{0}' is an unknown property of '{1}'.",
			qName.getName(), type.getTypeName()),
		    childNode);
      }

      if (attrStrategy.isProgram()) {
	attrStrategy.setValue(bean, qName,
			      buildProgram(attrStrategy, childNode));
	return;
      }
      else if (attrStrategy.isNode()) {
	attrStrategy.setValue(bean, qName, childNode);
	return;
      }

      String textValue = childNode.getValue();

      attrStrategy.setText(bean, qName, textValue);
    } catch (LineConfigException e) {
      throw e;
    } catch (Exception e) {
      throw error(e, childNode);
    }
  }

  Object configureValue(Node node)
  {
    String value = textValue(node);

    if (isEL() && value != null
        && value.startsWith("${") && value.endsWith("}")) {
      return evalObject(value);
    }
    else
      return value;
  }

  public void setDependentScope(DependentScope scope)
  {
    _dependentScope = scope;
  }

  public DependentScope getDependentScope()
  {
    if (_dependentScope == null)
      _dependentScope = new DependentScope();

    return _dependentScope;
  }

  public ArrayList<Dependency> getDependencyList()
  {
    return _dependList;
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

      QDocument doc = (QDocument) qelt.getOwnerDocument();

      if (doc == null)
	return null;
      else if (doc == _dependDocument)
        return _dependList;

      _dependDocument = doc;

      ArrayList<Path> pathList;
      pathList = doc.getDependList();

      if (pathList != null) {
        dependList = new ArrayList<Dependency>();

        for (int i = 0; i < pathList.size(); i++) {
          dependList.add(new Depend(pathList.get(i)));
        }
      }

      _dependList = dependList;
    }

    return dependList;
  }

  /**
   * Create a custom resin:type value.
   */
  Object createResinType(ConfigType childType, Element node)
  {
    String typeName = null;

    if (node instanceof QAttributedNode) {
      Node child = ((QAttributedNode) node).getFirstAttribute();

      for (; child != null; child = child.getNextSibling()) {
        Attr attr = (Attr) child;
        QName qName = ((QNode) attr).getQName();

	if (_resinClassSet.contains(qName)) {
	  typeName = attr.getValue();
	  break;
	}
      }
    }
    else {
      NamedNodeMap attrList = node.getAttributes();
      if (attrList != null) {
        int length = attrList.getLength();
        for (int i = 0; i < length; i++) {
          Attr attr = (Attr) attrList.item(i);
          QName qName = ((QNode) attr).getQName();

	  if (_resinClassSet.contains(qName)) {
	    typeName = attr.getValue();
	    break;
	  }
        }
      }
    }

    if (typeName != null) {
      try {
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	
	Class cl = Class.forName(typeName, false, loader);

	ConfigType beanConfigType = TypeFactory.getType(BeanConfig.class);
	InterfaceConfig cfg = new InterfaceConfig(cl);
	cfg.setClass(cl);

	for (Node childNode = node.getFirstChild();
	     childNode != null;
	     childNode = childNode.getNextSibling()) {
	  QName qName = ((QAbstractNode) childNode).getQName();
        
	  if (qName.equals(RESIN_PARAM)
	      || qName.equals(RESIN_PARAM_NS)) {
	    configureChildNode(childNode, qName, cfg, beanConfigType, true);
	  }
	}

	cfg.init();

	Object bean = cfg.replaceObjectNoInit();

	configureBean(bean, node);

	Config.init(bean);
	
	return bean;
      } catch (Exception e) {
	throw ConfigException.create(e);
      }
    }
    
    return null;
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

  static boolean hasChildren(Node node)
  {
    Node ptr;

    if (node instanceof QAttributedNode) {
      Node attr = ((QAttributedNode) node).getFirstAttribute();

      for (; attr != null; attr = attr.getNextSibling()) {
        if (! attr.getNodeName().startsWith("xml"))
          return true;
      }
    }
    else if (node instanceof Element) {
      NamedNodeMap attrList = node.getAttributes();
      if (attrList != null) {
        for (int i = 0; i < attrList.getLength(); i++) {
          if (! attrList.item(i).getNodeName().startsWith("xml"))
            return true;
        }
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
    /*
    NamedNodeMap attrList = node.getAttributes();
    if (attrList != null) {
      for (int i = 0; i < attrList.getLength(); i++) {
	if (attrList.item(i).getNodeName().equals(name.getName()))
	  return attrList.item(i).getNodeValue();
      }
    }
    */

    if (node instanceof Element) {
      String value = ((Element) node).getAttribute(name.getName());

      if (! "".equals(value))
        return value;
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
  Object getELValue(Attribute attr, Node node)
  {
    if (node instanceof Attr) {
      Attr attrNode = (Attr) node;
      String data = attrNode.getNodeValue();

      // server/12h6
      if (data != null && isEL() && attr.isEL()
	  && (data.indexOf("#{") >= 0 || data.indexOf("${") >= 0)) {
	return eval(attr.getConfigType(), data);
      }

      return null;
    }

    if (! (node instanceof Element))
      return null;

    Element elt = (Element) node;
    Element childElt = null;

    for (Node child = elt.getFirstChild();
	 child != null;
	 child = child.getNextSibling()) {
      if (child instanceof Element) {
	if (childElt != null)
	  return null;
	
	childElt = (Element) child;
      }
      
      else if (child instanceof CharacterData
	       && ! XmlUtil.isWhitespace(((CharacterData) child).getData())) {
	String data = ((CharacterData) child).getData();

	if (isEL() && attr.isEL() && childElt == null
	    && child.getNextSibling() == null
	    && (data.indexOf("#{") >= 0 || data.indexOf("${") >= 0)) {

	  String exprString = data.trim();

	  ELContext elContext = getELContext();

	  ELParser parser = new ELParser(elContext, exprString);

	  Expr expr = parser.parse();

	  Object value = expr.getValue(elContext);

	  // ioc/2403
	  return attr.getConfigType().valueOf(value);
	}
	
	return null;
      }
    }

    return null;
  }

  /**
   * Returns the text value of the node.
   */
  String getTextValue(Node node)
  {
    if (node instanceof Attr) {
      Attr attrNode = (Attr) node;
      String data = attrNode.getNodeValue();

      return data;
    }

    if (! (node instanceof Element))
      return null;

    Element elt = (Element) node;

    // ioc/2235
    if (elt.getAttributes().getLength() > 0)
      return null;

    for (Node child = elt.getFirstChild();
	 child != null;
	 child = child.getNextSibling()) {
      if (child instanceof Element) {
	return null;
      }
      
      else if (child instanceof CharacterData) {
	String data = ((CharacterData) child).getData();

	if (child.getNextSibling() == null && ! XmlUtil.isWhitespace(data)) {
	  return data;
	}
	else
	  return null;
      }
    }

    return null;
  }

  private Object eval(ConfigType type, String data)
  {
    ELContext elContext = getELContext();
    
    ELParser parser = new ELParser(elContext, data);
    
    Expr expr = parser.parse();

    Object value = type.valueOf(elContext, expr);

    if (value != null)
      return value;
    else
      return NULL;
  }

  /**
   * Returns the text value of the node.
   */
  Object getElementValue(Attribute attr, Node node)
  {
    if (! (node instanceof Element))
      return null;

    Element elt = (Element) node;
    Element childElt = null;

    for (Node child = elt.getFirstChild();
	 child != null;
	 child = child.getNextSibling()) {
      if (child instanceof Element) {
	if (childElt != null)
	  return null;
	
	childElt = (Element) child;
      }
      
      else if (child instanceof CharacterData
	       && ! XmlUtil.isWhitespace(((CharacterData) child).getData())) {
	String data = ((CharacterData) child).getData();

	if (isEL() && attr.isEL() && childElt == null
	    && child.getNextSibling() == null
	    && (data.indexOf("#{") >= 0 || data.indexOf("${") >= 0)) {
	  ELContext elContext = getELContext();

	  ELParser parser = new ELParser(elContext, data.trim());
    
	  Expr expr = parser.parse();

	  Object value = attr.getConfigType().valueOf(elContext, expr);

	  if (value != null)
	    return value;
	  else
	    return NULL;
	}
	
	return null;
      }
    }

    if (childElt == null)
      return null;

    TypeFactory factory = TypeFactory.getFactory();

    QName qName = ((QElement) childElt).getQName();

    ConfigType childType
      = factory.getEnvironmentType(((QElement) childElt).getQName());

    if (childType != null) {
      Object childBean = childType.create(null, qName);
      
      configureNode(childElt, childBean, childType);
      
      childType.init(childBean);

      Object value = childType.replaceObject(childBean);

      if (value != null)
	return value;
      else
	return NULL;
    }

    return null;
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

      if (value == null)
	return "";

      return value;
    }
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

      return expr.getValue(getELContext());
    }
    else
      return exprString;
  }

  public static RuntimeException error(String msg, Node node)
  {
    String systemId = null;
    String filename = null;
    int line = 0;

    if (node instanceof QAbstractNode) {
      QAbstractNode qnode = (QAbstractNode) node;
      
      systemId = qnode.getBaseURI();
      filename = qnode.getFilename();
      line = qnode.getLine();
    }

    if (systemId != null) {
      String sourceLines = getSourceLines(systemId, line);
      
      msg = msg + sourceLines;
    }
      
    if (filename != null)
      return new LineConfigException(filename, line, msg);
    else
      return new LineConfigException(msg);
  }
  
  public static RuntimeException error(Throwable e, Node node)
  {
    String systemId = null;
    String filename = null;
    int line = 0;

    if (e instanceof RuntimeException
	&& e instanceof DisplayableException
	&& ! ConfigException.class.equals(e.getClass())) {
      return (RuntimeException) e;
    }

    if (node instanceof QAbstractNode) {
      QAbstractNode qnode = (QAbstractNode) node;
      
      systemId = qnode.getBaseURI();
      filename = qnode.getFilename();
      line = qnode.getLine();
    }

    for (; e.getCause() != null; e = e.getCause()) {
      if (e instanceof LineCompileException)
        break;
      else if (e instanceof LineConfigException)
        break;
      else if (e instanceof CompileException)
        break;
    }

    if (e instanceof LineConfigException)
      return (LineConfigException) e;
    else if (e instanceof LineCompileException) {
      return new LineConfigException(e.getMessage(), e);
    }
    else if (e instanceof ConfigException
	     && e.getMessage() != null
	     && filename != null) {
      String sourceLines = getSourceLines(systemId, line);

      return new LineConfigException(filename, line,
				     e.getMessage() + sourceLines,
				     e);
    }
    else if (e instanceof CompileException && e.getMessage() != null) {
      return new LineConfigException(filename, line, e);
    }
    else {
      String sourceLines = getSourceLines(systemId, line);
      
      String msg = filename + ":" + line + ": " + e + sourceLines;

      if (e instanceof RuntimeException) {
	throw new LineConfigException(msg, e);
      }
      else if (e instanceof Error) {
	// server/1711
	throw new LineConfigException(msg, e);
	// throw (Error) e;
      }
      else
	return new LineConfigException(msg, e);
    }
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

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _dependentScope + "]";
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

  public void push(Object obj)
  {
  }

  static {
    _resinClassSet.add(RESIN_CLASS_NS);
    _resinClassSet.add(RESIN_CLASS);
    _resinClassSet.add(RESIN_TYPE_NS);
    _resinClassSet.add(RESIN_TYPE);
    _resinClassSet.add(RESIN_PARAM_NS);
    _resinClassSet.add(RESIN_PARAM);
    /*
    _resinClassSet.add(new QName("resin:type", "http://caucho.com/ns/resin"));
    _resinClassSet.add(new QName("resin:class", "http://caucho.com/ns/resin"));
    */
  }
}
