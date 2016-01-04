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
 */

package com.caucho.v5.config.xml;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELException;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.caucho.v5.config.UserMessage;
import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.DisplayableException;
import com.caucho.v5.config.UserMessageLocation;
import com.caucho.v5.config.ConfigExceptionLocation;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.core.ContextConfig;
import com.caucho.v5.config.expr.ExprCfg;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.RecoverableProgram;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.config.type.StringType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.inject.impl.InjectContext;
import com.caucho.v5.io.Dependency;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.vfs.Depend;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.xml.QAbstractNode;
import com.caucho.v5.xml.QAttributedNode;
import com.caucho.v5.xml.QDocument;
import com.caucho.v5.xml.QElement;
import com.caucho.v5.xml.QNode;
import com.caucho.v5.xml.XmlUtil;

import io.baratine.inject.InjectManager;

/**
 * The ConfigContext contains the state of the current configuration.
 */
@ModulePrivate
public class ContextConfigXml extends ContextConfig
{
  private final static L10N L = new L10N(ContextConfigXml.class);
  private final static Logger log
    = Logger.getLogger(ContextConfigXml.class.getName());

  public final static NameCfg TEXT = new NameCfg("#text");
  private final static Object NULL = new Object();

  //private CreationalContextImpl<?> _beanStack;

  private Document _dependDocument;
  private InjectContext _beanStack;

  public ContextConfigXml(ContextConfigXml parent)
  {
    super(parent);
  }

  public ContextConfigXml(ConfigContext config)
  {
    super(config);
  }

  public static ContextConfigXml getCurrent()
  {
    return (ContextConfigXml) ContextConfig.getCurrent();
  }
  
  @Override
  public ConfigXml getConfig()
  {
    return (ConfigXml) super.getConfig();
  }
  
  /**
   * Returns the component value for the dependent scope
   */
  //@Override
  /*
  public Object get(Bean<?> bean)
  {
    return CreationalContextImpl.find(_beanStack, bean);
  }
  */
  
  /*
  @Override
  public Object findByName(String name)
  {
    //CreationalContextImpl.findByName(_beanStack, name);
    return InjectManager.current().createByName(name);
  }
  
  @Override
  public InjectContext setCreationalContext(InjectContext cxt)
  {
    InjectContext oldCxt = _beanStack;
    
    _beanStack = cxt; // (CreationalContextImpl<?>) cxt;
    
    return oldCxt;
  }
  */

  /**
   * External call to configure a bean based on a top-level node.
   * The init() and replaceObject() are not called.
   *
   * @param bean the object to be configured.
   */
  public Object configure(Object bean, Node top)
    throws ConfigExceptionLocation
  {
    if (bean == null)
      throw new NullPointerException(L.l("unexpected null bean at node '{0}'", top));

    ContextConfigXml oldBuilder = getCurrent();
    
    try {
      setCurrent(this);

      ConfigType<?> type = getTypeFactory().getType(bean);

      configureBean(bean, top);

      type.init(bean);

      return type.replaceObject(bean);
    } catch (ConfigExceptionLocation e) {
      throw e;
    } catch (Exception e) {
      throw error(e, top);
    } finally {
      setCurrent(oldBuilder);
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
    throws ConfigExceptionLocation
  {
    ContextConfigXml oldBuilder = getCurrent();
    String oldFile = getBaseUri();
    ArrayList<Dependency> oldDependList = this.getDependencyList();

    try {
      setCurrent(this);

      if (top instanceof QNode) {
        QNode qNode = (QNode) top;

        setBaseUri(qNode.getBaseURI());
      }

      setDependencyList(getDependencyList(top));

      ConfigType<?> type = getTypeFactory().getType(bean);

      configureNode(top, bean, type);
    } finally {
      setCurrent(oldBuilder);

      setDependencyList(oldDependList);
      
      setBaseUri(oldFile);
    }
  }
  
  

  /**
   * External call to configure a bean's attribute.
   *
   * @param bean the bean to be configured
   * @param attribute the node representing the configured attribute
   * @throws ConfigExceptionLocation
   */
  public void configureAttribute(Object bean, Node attribute)
    throws ConfigExceptionLocation
  {
    String attrName = attribute.getNodeName();

    if (attrName.startsWith("xmlns"))
      return;

    String oldFile = getBaseUri();
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    ContextConfigXml oldBuilder = getCurrent();
    
    try {
      setCurrent(this);

      setBaseUri(attribute.getBaseURI());

      ConfigType<?> type = getTypeFactory().getType(bean);

      NameCfg qName = ((QAbstractNode) attribute).getQName();

      type.beforeConfigure(this, bean, attribute);

      configureChildNode(attribute, qName, bean, type, false);

      type.afterConfigure(this, bean);
    }
    catch (ConfigExceptionLocation e) {
      throw e;
    }
    catch (Exception e) {
      throw error(e, attribute);
    } finally {
      setBaseUri(oldFile);
      setCurrent(oldBuilder);
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
   * @throws ConfigExceptionLocation
   */
  public Object configureNode(Node node,
                              Object bean,
                              ConfigType<?> beanType)
    throws ConfigExceptionLocation
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      beanType.beforeConfigure(this, bean, node);
      beanType.beforeConfigureBean(this, bean, node);

      if (log.isLoggable(Level.ALL))
        log.log(Level.ALL, "config begin " + beanType);

      configureNodeAttributes(node, bean, beanType);

      for (Node childNode = node.getFirstChild();
           childNode != null;
           childNode = childNode.getNextSibling()) {
        NameCfg qName = ((QAbstractNode) childNode).getQName();

        configureChildNode(childNode, qName, bean, beanType, false);
      }

      if (log.isLoggable(Level.ALL))
        log.log(Level.ALL, "config end " + beanType);

      beanType.afterConfigure(this, bean);
    } catch (ConfigExceptionLocation e) {
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
   * @throws ConfigExceptionLocation
   */
  private void configureNodeAttributes(Node node,
                                       Object bean,
                                       ConfigType<?> type)
  {
    if (node instanceof QAttributedNode) {
      Node child = ((QAttributedNode) node).getFirstAttribute();

      for (; child != null; child = child.getNextSibling()) {
        Attr attr = (Attr) child;
        NameCfg qName = ((QNode) attr).getQName();

        configureChildNode(attr, qName, bean, type, false);
      }
    }
    else {
      NamedNodeMap attrList = node.getAttributes();
      if (attrList != null) {
        int length = attrList.getLength();
        for (int i = 0; i < length; i++) {
          Attr attr = (Attr) attrList.item(i);
          NameCfg qName = ((QNode) attr).getQName();

          configureChildNode(attr, qName, bean, type, false);
        }
      }
    }
  }

  /**
   * Configures a child node for a bean.
   * 
   * Properties and fields will be set.
   * Flow will be invoked (like resin:if)
   */
  private void configureChildNode(Node childNode,
                                  NameCfg qName,
                                  Object parentBean,
                                  ConfigType<?> parentType,
                                  boolean allowParam)
  {
    if (qName.getName().startsWith("xmlns")) {
      return;
    }

    AttributeConfig attrStrategy;

   try {
      attrStrategy = getAttribute(parentType, qName, childNode);
      
      if (attrStrategy == null) {
        if (qName.equals(TEXT)) {
          validateEmptyText(parentBean, childNode);
        }
      }
      else if (attrStrategy.isProgram()) {
        attrStrategy.setValue(parentBean, qName,
                              buildProgram(attrStrategy, childNode));
      }
      else if (attrStrategy.isNode()) {
        attrStrategy.setValue(parentBean, qName, childNode);
      }
      else if (configureInlineText(parentBean, childNode, qName, attrStrategy)) {
      }
      else if (configureInlineBean(parentBean, childNode, attrStrategy)) {
      }
      else {
        configureBeanProperties(childNode, qName, parentBean,
                                parentType, attrStrategy,
                                allowParam);
      }
    } catch (ConfigExceptionLocation e) {
      throw e;
    } catch (Exception e) {
      throw error(e, childNode);
    }
  }
  
  private void validateEmptyText(Object bean, Node childNode)
  {
    CharacterData cData = (CharacterData) childNode;
    
    String data = cData.getData().trim();
    
    if (data.length() > 0) {
      throw new ConfigException(L.l("Unexpected text in {0} at\n  '{1}'",
                                    bean.getClass().getName(),
                                    data));
    }
  }

  private AttributeConfig getAttribute(ConfigType<?> type,
                                 NameCfg qName,
                                 Node childNode)
  {
    AttributeConfig attrStrategy;

    attrStrategy = type.getAttribute(qName);
    
    if (attrStrategy == null) {
      attrStrategy = type.getDefaultAttribute(qName);
    }

    if (attrStrategy != null) {
      return attrStrategy;
    }

    if (childNode instanceof Element || childNode instanceof Attr) {
      String localName = qName.getLocalName();

      if (localName.indexOf(':') >= 0) {
        // XXX: need ioc QA
        throw error(L.l("'{0}' does not have a defined namespace for 'xmlns:{1}'.  Tags with prefixes need defined namespaces.",
                        qName.getName(),
                        localName.substring(0, localName.indexOf(':'))),
                    childNode);
      }

      // ioc/23m2
      if ("new".equals(localName)) {
        return null;
      }

      throw error(L.l("'{0}' is an unknown property of '{1}'.",
                      qName.getName(), type.getTypeName()),
                  childNode);
    }

    return null;
  }

  private void setText(Object bean,
                       NameCfg qName,
                       String text,
                       AttributeConfig attrStrategy,
                       boolean isTrim)
  {
    ConfigType<?> attrType = attrStrategy.getConfigType();

    if (isTrim && ! attrType.isNoTrim())
      text = text.trim();

    if (isEL() && attrType.isEL() && text.indexOf("${") >= 0) {
      Object value = attrType.valueOf(evalObject(text));

      attrStrategy.setValue(bean, qName, value);
    }
    else
      attrStrategy.setText(bean, qName, text);
  }

  private boolean configureInlineText(Object bean,
                                      Node childNode,
                                      NameCfg qName,
                                      AttributeConfig attrStrategy)
  {
    // ioc/2013
    if (! attrStrategy.isSetter())
      return false;

    boolean isTrim = ! attrStrategy.getConfigType().isNoTrim();
    String text = getTextValue(childNode, isTrim);

    if (text == null)
      return false;

    isTrim = isTrim(childNode);

    if (isEL() && attrStrategy.isEL() && text.indexOf("${") >= 0) {
      if (isTrim)
        text = text.trim();

      Object elValue = eval(attrStrategy.getConfigType(), text);

      // ioc/2410
      if (elValue != NULL) {
        attrStrategy.setValue(bean, qName, elValue);
      }
      else {
        try {
          attrStrategy.setValue(bean, qName, null);
        } catch (Exception e) {
          throw ConfigException.wrap(L.l("{0} value must not be null.\n  ", text), e);
        }
      }

      return true;
    }
    else if (attrStrategy.isAllowText()) {
      setText(bean, qName, text, attrStrategy, isTrim);

      return true;
    }
    else
      return false;
  }

  private boolean configureInlineBean(Object parent,
                                      Node node,
                                      AttributeConfig attrStrategy)
  {
    /* server/0219
    if (! attrStrategy.isAllowInline()) {
      return false;
    }
    */

    Node childNode = getChildElement(node);

    if (childNode == null) {
      return false;
    }
    
    NameCfg parentQname = ((QNode) node).getQName();
    NameCfg childQname = ((QNode) childNode).getQName();

    ConfigType<?> type = getTypeFactory().getEnvironmentType(childQname);

    if (type == null || ! attrStrategy.isInlineType(type)) {
      // server/6500
      return false;
    }

    // server/0219
    // Object childBean = attrStrategy.create(parent, qName, type);

    Element childNew = getChildNewElement(childNode);

    Object childBean;

    if (childNew != null)
      childBean = createNew(type, parent, childNew);
    else if (type.isQualifier()) {
      // ioc/04f8
      Object qualifier = type.create(parent, childQname);

      ConfigType<?> qualifierType = getTypeFactory().getType(qualifier);

      qualifier = configureChildBean(qualifier, qualifierType,
                                     childNode, attrStrategy);
      
      InjectManager cdiManager = InjectManager.current();
      
      Class<?> attrType = attrStrategy.getConfigType().getType();
      
      childBean = cdiManager.instance(attrType, (Annotation) qualifier);

      /*
      Set<Bean<?>> beans
        = cdiManager.getBeans(attrType, (Annotation) qualifier);
      
      Bean<?> bean = cdiManager.resolve(beans);
      
      CreationalContext cxt = null;
      
      childBean = cdiManager.getReference(bean, attrType, cxt);
      */ 
    }
    else {
      childBean = type.create(parent, childQname);
    }

    if (childBean == null) {
      return false;
    }

    // server/1af3
    ConfigType<?> childType = getTypeFactory().getType(childBean);

    childBean = configureChildBean(childBean, childType,
                                   childNode, attrStrategy);

    // ejb/7006
    attrStrategy.setValue(parent, parentQname, childBean);

    return true;
  }
  
  protected TypeFactoryConfig getTypeFactory()
  {
    return TypeFactoryConfig.getFactory();
  }

  private void configureBeanProperties(Node childNode,
                                       NameCfg qName,
                                       Object bean,
                                       ConfigType<?> type,
                                       AttributeConfig attrStrategy,
                                       boolean allowParam)
  {
    Object childBean = attrStrategy.create(bean, qName);

    if (childBean == null) {
      throw unableToCreateError(attrStrategy, bean, qName, childNode);
    }

    ConfigType<?> childBeanType = getTypeFactory().getType(childBean);
    //ConfigType<?> childBeanType = attrStrategy.getType(childBean);

    childBean = configureChildBean(childBean, childBeanType,
                                   childNode, attrStrategy);

    if (! childBeanType.isEnvBean()) {
      attrStrategy.setValue(bean, qName, childBean);
    }
  }

  private Object configureChildBean(Object childBean,
                                    ConfigType<?> childBeanType,
                                    Node childNode,
                                    AttributeConfig attrStrategy)
  {
    if (childNode instanceof Element) {
      configureNode(childNode, childBean, childBeanType);
    }
    else {
      configureChildNode(childNode, TEXT, childBean, childBeanType, false);
    }

    childBeanType.init(childBean);

    Object newBean = attrStrategy.replaceObject(childBean);
    
    if (newBean == childBean) {
      return childBeanType.replaceObject(childBean);
    }
    else {
      return newBean;
    }
  }
  
  private ConfigException unableToCreateError(AttributeConfig attr,
                                              Object bean,
                                              NameCfg qName,
                                              Node node)
  {
    Element child = getUniqueChildElement(node);
    
    if (child == null) {
      throw error(L.l("unable to create inline attribute '{2}' for '{1}' because no unique child Element exists.  Attribute = {0}",
                      attr, bean, qName),
                      node);
    }
    

    NameCfg childQName = ((QNode) child).getQName();
    String uri = childQName.getNamespaceURI();

    if (uri == null || ! uri.startsWith("urn:java:")) {
      throw error(L.l("unable to create inline attribute '{0}' for '{1}' because the child <{2}> uri must start with xmlns='urn:java:...' but uri='{3}' for {4}",
                      qName.getName(), bean, childQName.getName(), uri, attr),
                      node);
    }
    
    throw throwUnableToCreateError(attr, bean, qName, node);
  }
  
  private ConfigException throwUnableToCreateError(AttributeConfig attr,
                                                   Object bean,
                                                   NameCfg qName,
                                                   Node childNode)
  {
    throw error(L.l("unable to create attribute {0} for {1} and {2}",
                    attr, bean, qName),
                    childNode);
  }

  private Node getChildElement(Node node)
  {
    Element child = getUniqueChildElement(node);
    
    if (child == null)
      return null;

    NameCfg qName = ((QNode) child).getQName();
    String uri = qName.getNamespaceURI();

    if (uri == null || ! uri.startsWith("urn:java:"))
      return null;

    return child;
  }
  
  private Element getUniqueChildElement(Node node)
  {
    if (! (node instanceof Element))
      return null;

    Element elt = (Element) node;

    Node child = elt.getFirstChild();

    if (child == null)
      return null;

    if (isEmptyText(child)) {
      child = child.getNextSibling();

      if (child == null)
        return null;
    }

    Node next = child.getNextSibling();

    if (next != null) {
      if (next.getNextSibling() != null || ! isEmptyText(next))
        return null;
    }
    
    if (child instanceof Element)
      return (Element) child;
    else
      return null;
  }

  private Object createNew(ConfigType<?> type,
                           Object parent,
                           Element newNode)
  {
    boolean isTrim = true;
    
    String text = getTextValue(newNode, isTrim);

    if (text != null) {
      text = text.trim();

      return type.valueOf(create(newNode, StringType.TYPE));
    }

    int count = countNewChildren(newNode);

    Constructor<?> ctor = type.getConstructor(count);

    Class<?> []paramTypes = ctor.getParameterTypes();

    Object []args = new Object[paramTypes.length];
    int i = 0;
    for (Node child = newNode.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if (! (child instanceof Element))
        continue;

      ConfigType<?> childType = getTypeFactory().getType(paramTypes[i]);

      args[i++] = create(child, childType);
    }

    try {
      return ctor.newInstance(args);
    } catch (InvocationTargetException e) {
      throw ConfigExceptionLocation.wrap(ctor, e.getCause());
    } catch (Exception e) {
      throw ConfigExceptionLocation.wrap(ctor, e);
    }
  }

  private int countNewChildren(Element newNode)
  {
    int count = 0;

    for (Node child = newNode.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if (child instanceof Element)
        count++;
    }

    return count;
  }

  private Element getChildNewElement(Node node)
  {
    if (! (node instanceof Element))
      return null;

    Element elt = (Element) node;

    for (Node child = elt.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if ("new".equals(child.getLocalName()))
        return (Element) child;
    }

    return null;
  }

  private boolean isEmptyText(Node node)
  {
    if (! (node instanceof CharacterData))
      return false;

    CharacterData data = (CharacterData) node;

    return data.getData().trim().length() == 0;
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

  private ConfigProgram buildProgram(AttributeConfig attr, Node node)
  {
    ConfigProgram program = new ProgramNodeChild(getConfig(), node);
    
    if (Boolean.TRUE.equals(ConfigContext.getProperty(RecoverableProgram.ATTR))) {
      program = new RecoverableProgram(getConfig(), program);
    }
    
    return program;
  }

  //
  // Used for args
  //
  public Object create(Node childNode, ConfigType<?> type)
    throws ConfigException
  {
    try {
      Object childBean;
      String text;

      if ((text = getArgTextValue(childNode)) != null) {
        boolean isTrim = isTrim(childNode);

        if (isEL() && type.isEL() && text.indexOf("${") >= 0) {
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
          return type.valueOf(text);
        }
      }

      NameCfg qName = ((QNode) childNode).getQName();

      ConfigType<?> childBeanType = type.createType(qName);

      if (childBeanType != null) {
        childBean = childBeanType.create(null, qName);

        if (childNode instanceof Element)
          configureNode(childNode, childBean, childBeanType);
        else
          configureChildNode(childNode, TEXT, childBean, childBeanType, false);

        childBeanType.init(childBean);

        return childBeanType.replaceObject(childBean);
      }
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
    } catch (ConfigExceptionLocation e) {
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

  ArrayList<Dependency> getDependencyList(Node node)
  {
    ArrayList<Dependency> dependList = null;

    if (node instanceof QElement) {
      QElement qelt = (QElement) node;

      QDocument doc = (QDocument) qelt.getOwnerDocument();

      if (doc == null)
        return null;
      else if (doc == _dependDocument) {
        return getDependencyList();
      }

      _dependDocument = doc;

      ArrayList<PathImpl> pathList;
      pathList = doc.getDependList();

      if (pathList != null) {
        dependList = new ArrayList<Dependency>();

        for (int i = 0; i < pathList.size(); i++) {
          dependList.add(new Depend(pathList.get(i)));
        }
      }

      setDependencyList(dependList);
    }

    return dependList;
  }

  /**
   * Returns the variable resolver.
   */
  /*
  public static ConfigELContext getELContext()
  {
    return ConfigELContext.EL_CONTEXT;
  }
  */

  /**
   * Returns the text value of the node.
   */
  private String getTextValue(Node node, boolean isTrim)
  {
    if (node instanceof Attr) {
      Attr attrNode = (Attr) node;
      String data = attrNode.getNodeValue();

      return data;
    }
    else if (node instanceof CharacterData) {
      CharacterData cData = (CharacterData) node;

      return cData.getData();
    }

    if (! (node instanceof Element))
      return null;

    QElement elt = (QElement) node;

    // ioc/2235
    for (Node attr = elt.getFirstAttribute();
         attr != null;
         attr = attr.getNextSibling()) {
      if (! "xml".equals(attr.getPrefix()))
        return null;
    }

    for (Node child = elt.getFirstChild();
         child != null;
         child = child.getNextSibling()) {
      if (child instanceof Element) {
        return null;
      }

      else if (child instanceof CharacterData) {
        String data = ((CharacterData) child).getData();

        if (child.getNextSibling() != null) {
          return null;
        }
        else if (! isTrim || ! XmlUtil.isWhitespace(data)) {
          return data;
        }
        else {
          return null;
        }
      }
    }

    return "";
  }

  /**
   * Returns the text value of the node.
   */
  private String getArgTextValue(Node node)
  {
    if (node instanceof Element && ! node.getLocalName().equals("value"))
      return null;

    boolean isTrim = true;
    return getTextValue(node, isTrim);
  }

  private Object eval(ConfigType<?> type, String data)
  {
    /*
    ELContext elContext = getELContext();

    ELParser parser = new ELParser(elContext, data);

    Expr expr = parser.parse();
    */
    ExprCfg expr = ExprCfg.newParser(data).parse();

    //Object value = type.valueOf(elContext, expr);
    //Object value = expr.evalObject(elContext);
    Object value = expr.eval(ConfigContext.getEnvironment());
    
    value = type.valueOf(value);

    if (value != null)
      return value;
    else
      return NULL;
  }

  /**
   * Returns the text value of the node.
   */
  private static String textValue(Node node)
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
  private static String textValueNoTrim(Node node)
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
  private Object evalObject(String exprString)
    throws ELException
  {
    if (exprString.indexOf("${") >= 0 && isEL()) {
      return ConfigContext.eval(exprString);
      /*
      ELParser parser = new ELParser(getELContext(), exprString);
      parser.setCheckEscape(true);
      Expr expr = parser.parse();

      return expr.getValue(getELContext());
      */
    }
    else
      return exprString;
  }

  private static RuntimeException error(String msg, Node node)
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
      return new ConfigExceptionLocation(filename, line, msg);
    else
      return new ConfigExceptionLocation(msg);
  }

  private static RuntimeException error(Throwable e, Node node)
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
      if (e instanceof UserMessageLocation)
        break;
      else if (e instanceof ConfigExceptionLocation)
        break;
      else if (e instanceof UserMessage)
        break;
    }

    if (e instanceof ConfigExceptionLocation)
      return (ConfigExceptionLocation) e;
    else if (e instanceof UserMessageLocation) {
      return new ConfigExceptionLocation(e.getMessage(), e);
    }
    else if (e instanceof ConfigException
             && e.getMessage() != null
             && filename != null) {
      String sourceLines = getSourceLines(systemId, line);
      
      if (! systemId.startsWith("file:")) {
        filename = systemId;
      }

      return new ConfigExceptionLocation(filename, line,
                                     e.getMessage() + sourceLines,
                                     e);
    }
    else if (e instanceof UserMessage && e.getMessage() != null) {
      return new ConfigExceptionLocation(filename, line, e);
    }
    else {
      String sourceLines = getSourceLines(systemId, line);

      String msg = filename + ":" + line + ": " + e + sourceLines;

      if (e instanceof RuntimeException) {
        throw new ConfigExceptionLocation(msg, e);
      }
      else if (e instanceof Error) {
        // server/1711
        throw new ConfigExceptionLocation(msg, e);
        // throw (Error) e;
      }
      else
        return new ConfigExceptionLocation(msg, e);
    }
  }

  /*
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
  */

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
