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

package com.caucho.v5.config.type;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.w3c.dom.Node;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.ConfigExceptionLocation;
import com.caucho.v5.config.ConfigName;
import com.caucho.v5.config.ConfigRest;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.DependencyBean;
import com.caucho.v5.config.annotation.DisableConfig;
import com.caucho.v5.config.annotation.NonEL;
import com.caucho.v5.config.attribute.AddAttribute;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.attribute.CreateAttribute;
import com.caucho.v5.config.attribute.ProgramAttribute;
import com.caucho.v5.config.attribute.PropertyAttribute;
import com.caucho.v5.config.attribute.SetterAttribute;
import com.caucho.v5.config.attribute.TextAttribute;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.core.ContextConfig;
import com.caucho.v5.config.custom.AttributeCustomBean;
import com.caucho.v5.config.custom.CustomBean;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.PropertyStringProgram;
import com.caucho.v5.config.types.RawString;
import com.caucho.v5.inject.InjectManagerAmp;
import com.caucho.v5.io.Dependency;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PersistentDependency;

/**
 * Represents an inline bean type for configuration.
 */
public class InlineBeanType<T> extends ConfigType<T>
{
  private static final L10N L = new L10N(InlineBeanType.class);
  private static final Logger log
    = Logger.getLogger(InlineBeanType.class.getName());
  
  public static final NameCfg TEXT = new NameCfg("#text");
  public static final NameCfg VALUE = new NameCfg("value");

  private static final Object _introspectLock = new Object();

  private final Class<T> _beanClass;
  
  private ConcurrentHashMap<NameCfg,AttributeConfig> _nsAttributeMap
    = new ConcurrentHashMap<>();
  
  private HashMap<String,AttributeConfig> _attributeMap
    = new HashMap<>();
    
  private HashSet<String> _attributeNames = new HashSet<>();

  private Constructor<T> _stringConstructor;
  
  private Method _valueOf;
  private Method _setParent;
  private Method _replaceObject;
  private Method _setConfigLocation;
  private Method _setConfigUriLocation;
  private Method _setConfigNode;
  
  private AttributeConfig _addText;
  private AttributeConfig _addProgram;
  private AttributeConfig _addContentProgram;
  private AttributeConfig _addBeanProgram;
  private AttributeConfig _addBean; // add(Object)
  private AttributeConfig _setProperty;
  
  private boolean _isEL;

  private HashMap<Class<?>,AttributeConfig> _addMethodMap
    = new HashMap<Class<?>,AttributeConfig>();

  private AttributeConfig _addCustomBean;
  
  // private AnnotatedType<T> _annotatedType;
  // private ManagedBeanImpl<T> _bean;
  // private InjectionTarget<T> _injectionTarget;

  //private ArrayList<ConfigProgram> _injectList;
  private ArrayList<ConfigProgram> _initList;
  
  private boolean _isIntrospecting;
  private boolean _isIntrospected;
  private ArrayList<InlineBeanType<?>> _pendingChildList
    = new ArrayList<>();
  private ConfigBeanFactory _beanFactory;

  public InlineBeanType(TypeFactoryConfig typeFactory, Class<T> beanClass)
  {
    super(typeFactory);

    _beanClass = beanClass;
    
    if (EnvBean.class.isAssignableFrom(beanClass)) {
      setEnvBean(true);
    }
  }

  /**
   * Returns the given type.
   */
  @Override
  public Class<T> getType()
  {
    return _beanClass;
  }
  
  @Override
  public boolean isEL()
  {
    return _isEL;
  }

  protected void setAddCustomBean(AttributeConfig addCustomBean)
  {
    _addCustomBean = addCustomBean;
  }

  protected void setAddAnnotation(AttributeConfig addAnnotation)
  {
  }

  /**
   * Creates a new instance
   */
  @Override
  public Object create(Object parent, NameCfg name)
  {
    try {
      ConfigBeanFactory factory = _beanFactory;
      
      
      if (factory == null) {
        _beanFactory = factory = createFactory();
      }
      
      Object bean = factory.create();

      if (_setParent != null
          && parent != null
          && _setParent.getParameterTypes()[0].isAssignableFrom(parent.getClass())) {
        try {
          _setParent.invoke(bean, parent);
        } catch (IllegalArgumentException e) {
          throw ConfigExceptionLocation.wrap(_setParent,
                                       L.l("{0}: setParent value of '{1}' is not valid",
                                           bean, parent),
                                           e);
        } catch (Exception e) {
          throw ConfigExceptionLocation.wrap(_setParent, e);
        }
      }

      return bean;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }
  
  private boolean isCdiBean()
  {
    return false;
  }
  
  private ConfigBeanFactory createFactory()
  {
    if (_beanClass.isInterface()) {
      throw new ConfigException(L.l("{0} cannot be instantiated because it is an interface",
                                    _beanClass.getName()));
    }
    
    InjectManagerAmp inject = InjectManagerAmp.current();
    
    if (inject != null) {
      return new ConfigBeanFactoryInject(inject.supplierNew(_beanClass));
    }
    else {
      return new ConfigBeanFactoryClass(_beanClass);
    }
    /*
    if (! isCdiBean()) {
      return new ConfigBeanFactoryClass(_beanClass); 
    }

    AnnotatedType<T> type = getAnnotatedType();

    CandiManager cdiManager
      = CandiManager.create(_beanClass.getClassLoader());
    
    InjectionTargetBuilder<T> builder
      = new InjectionTargetBuilder<T>(cdiManager, type);

    // server/2m09
    // builder.setGenerateInterception(false);

    Bean<T> bean = null;
    
    return new ConfigBeanFactoryCdi<T>(bean, builder);

    // _bean.getInjectionPoints();
    */
  }

  /*
  private AnnotatedType<T> getAnnotatedType()
  {
    if (_annotatedType == null) {
      CandiManager cdiManager
        = CandiManager.create(_beanClass.getClassLoader());
  
      _annotatedType = cdiManager.createAnnotatedType(_beanClass);
    }
    
    return _annotatedType;
  }
  */

  /**
   * Returns a constructor with a given number of arguments
   */
  @Override
  public Constructor<T> getConstructor(int count)
  {
    for (Constructor<?> ctor : _beanClass.getConstructors()) {
      if (ctor.getParameterTypes().length == count)
        return (Constructor<T>) ctor;
    }
    
    throw new ConfigException(L.l("{0} does not have any constructor with {1} arguments",
                                  this, count));
  }

  /**
   * Called before the children are configured.
   */
  @Override
  public void beforeConfigure(ContextConfig env, Object bean, Node node)
  {
    super.beforeConfigure(env, bean, node);

    if (_setConfigNode != null) {
      try {
        _setConfigNode.invoke(bean, node);
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }
    }
    
    if (bean instanceof DependencyBean) {
      DependencyBean dependencyBean = (DependencyBean) bean;
      
      ArrayList<Dependency> dependencyList = env.getDependencyList();

      if (dependencyList != null) {
        for (Dependency depend : dependencyList) {
          dependencyBean.addDependency((PersistentDependency) depend);
        }
      }
    }
  }
  
  public void setLocation(Object bean, String filename, int line)
  {
    if (_setConfigLocation != null) {
      try {
        // _setConfigLocation.invoke(bean, filename, line);
        _setConfigLocation.invoke(bean, filename, line);
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }
    }

    if (_setConfigUriLocation != null) {
      try {
        _setConfigUriLocation.invoke(bean, filename, line);
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }
    }
  }
  
  public void setLocation(Object bean, String location)
  {
    if (location == null || "".equals(location)) {
      return;
    }

    if (_setConfigLocation != null) {
      try {
        location = location.trim();
        
        if (location.endsWith(":")) {
          location = location.substring(0, location.length() - 1);
        }
        
        int p = location.lastIndexOf(':');
        
        if (p < 0) {
          return;
        }
        
        String filename = location.substring(0, p);
        int line = Integer.parseInt(location.substring(p + 1));

        // _setConfigLocation.invoke(bean, filename, line);
        _setConfigLocation.invoke(bean, filename, line);
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }
    }
  }

  /**
   * Returns the attribute based on the given name.
   */
  @Override
  public AttributeConfig getAttribute(NameCfg name)
  {
    AttributeConfig attr = _nsAttributeMap.get(name);

    if (attr == null) {
      attr = getAttributeImpl(name);

      if (attr != null) {
        _nsAttributeMap.put(name, attr);
      }
    }

    return attr;
  }

  protected AttributeConfig getAttributeImpl(NameCfg name)
  {
    // server/2r10 vs jms/2193
    // attr = _attributeMap.get(name.getLocalName().toLowerCase(Locale.ENGLISH));

    AttributeConfig attr = _attributeMap.get(name.getLocalName());

    if (attr != null)
      return attr;

    String uri = name.getNamespaceURI();

    if (uri == null || ! uri.startsWith("urn:java")) {
      return null;
    }

    Class<?> cl = createClass(name);

    if (cl != null) {
      attr = getAddAttribute(cl);

      if (attr != null)
        return attr;
    }
    
    // server/13jm - environment beans have priority over the custom bean
    ConfigType<?> envBean = getFactory().getEnvironmentType(name);
    if (envBean != null && envBean.isEnvBean()) {
      return null;
    }

    if (_addCustomBean != null) {
      return _addCustomBean;
    }
    else if (_addBean != null) {
      return _addBean;
    }

    return null;
  }

  @Override
  public AttributeConfig getAddBeanAttribute(NameCfg qName)
  {
    return _addBean;
  }

  /**
   * Returns any add attributes to add arbitrary content
   */
  @Override
  public AttributeConfig getAddAttribute(Class<?> cl)
  {
    if (cl == null)
      return null;

    AttributeConfig attr = _addMethodMap.get(cl);

    if (attr != null) {
      return attr;
    }
    
    for (Class<?> iface : cl.getInterfaces()) {
      attr = getAddAttribute(iface);

      if (attr != null)
        return attr;
    }

    return getAddAttribute(cl.getSuperclass());
  }

  private Class<?> createClass(NameCfg name)
  {
    String uri = name.getNamespaceURI();

    if (! uri.startsWith("urn:java:"))
      return null;

    String pkg = uri.substring("urn:java:".length());

    return TypeFactoryConfig.loadClass(pkg, name.getLocalName());
  }

  /**
   * Returns the program attribute.
   */
  @Override
  public AttributeConfig getProgramAttribute()
  {
    if (_setProperty != null)
      return _setProperty;
    else
      return _addProgram;
  }

  /**
   * Returns the content program attribute (program excluding if, choose).
   */
  @Override
  public AttributeConfig getProgramContentAttribute()
  {
    return _addContentProgram;
  }

  /**
   * Returns the bean-program attribute (program allowing beans)
   */
  @Override
  public AttributeConfig getProgramBeanAttribute()
  {
    return _addBeanProgram;
  }

  /**
   * Initialize the type
   */
  @Override
  public void inject(Object bean)
  {
    introspectInject();
    
    /*
    for (int i = 0; i < _injectList.size(); i++)
      _injectList.get(i).inject(bean, null);
      */
  }

  /**
   * Initialize the type
   */
  @Override
  public void init(Object bean)
  {
    introspectInject();
    
    for (int i = 0; i < _initList.size(); i++) {
      _initList.get(i).inject(bean, null);
    }
  }

  /**
   * Return true if the object is replaced
   */
  @Override
  public boolean isReplace()
  {
    return _replaceObject != null;
  }
  
  /**
   * Replace the type with the generated object
   */
  @Override
  public Object replaceObject(Object bean)
  {
    if (_replaceObject != null) {
      try {
        return _replaceObject.invoke(bean);
      } catch (Exception e) {
        throw ConfigExceptionLocation.wrap(_replaceObject, e);
      }
    }
    else
      return bean;
  }
  
  /**
   * Converts the string to the given value.
   */
  @Override
  public Object valueOf(String text)
  {
    if (_valueOf != null) {
      try {
        return _valueOf.invoke(null, text);
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }
    }
    else if (_stringConstructor != null) {
      try {
        return _stringConstructor.newInstance(text);
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }
    }
    else if (_addText != null) {
      Object bean = create(null, TEXT);
      _addText.setText(bean, TEXT, text);

      inject(bean);
      init(bean);
      
      return bean;
    }
    else if (_addProgram != null || _addContentProgram != null) {
      Object bean = create(null, TEXT);

      inject(bean);
      
      try {
        ConfigContext config = ConfigContext.getCurrent();
        
        ConfigProgram program = new PropertyStringProgram(config, TEXT, text);
        
        if (_addProgram != null)
          _addProgram.setValue(bean, TEXT, program);
        else
          _addContentProgram.setValue(bean, TEXT, program);
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }

      init(bean);

      return bean;
    }
    else if ("".equals(text.trim())) {
      Object bean = create(null, TEXT);

      inject(bean);
      init(bean);
      
      return bean;
    }

    throw new ConfigException(L.l("Can't convert to '{0}' from '{1}'.",
                                  _beanClass.getName(), text));
  }

  @Override
  public boolean isConstructableFromString()
  {
    return (_valueOf != null
           || _stringConstructor != null
           || _addText != null
           || _addProgram != null
           || _addContentProgram != null);
  }

  /**
   * Converts the string to the given value.
   */
  @Override
  public Object valueOf(Object value)
  {
    if (value == null)
      return null;
    else if (value instanceof String) {
      return valueOf((String) value);
    }
    else if (_beanClass.isAssignableFrom(value.getClass()))
      return value;
    else if (value.getClass().getName().startsWith("java.lang."))
      return valueOf(String.valueOf(value));
    else
      return value;
  }

  //
  // Introspection
  //

  /**
   * Introspect the bean for configuration
   */
  @Override
  public void introspect()
  {
    // long startTime = System.currentTimeMillis();
    synchronized (_introspectLock) {
      if (_isIntrospecting)
        return;

      _isIntrospecting = true;

      try {
        // ioc/20h4 - after to deal with recursion
        introspectParent();

        //Method []methods = _beanClass.getMethods();
        if (! _isIntrospected) {
          _isIntrospected = true;

          _isEL = ! _beanClass.isAnnotationPresent(NonEL.class);
          
          try {
            // System.out.println("INTROSPECT: " + _beanClass);
            Method []methods = _beanClass.getDeclaredMethods();

            introspectMethods(methods);
          } catch (NoClassDefFoundError e) {
            e.printStackTrace();
            log.fine(_beanClass + " " + e);
          }
        }
      } finally {
        _isIntrospecting = false;
      }
    }

    introspectComplete();
    //long endTime = System.currentTimeMillis();
  }

  private void introspectComplete()
  {
    ArrayList<InlineBeanType<?>> childList
      = new ArrayList<InlineBeanType<?>>(_pendingChildList);

    // ioc/20h4
    for (InlineBeanType<?> child : childList) {
      child.introspectParent();
      child.introspectComplete();
    }
  }
  
  private boolean isIntrospecting()
  {
    if (_isIntrospecting)
      return true;

    Class<?> parentClass = _beanClass.getSuperclass();
    
    if (parentClass != null) {
      ConfigType<?> parentType = TypeFactoryConfig.getType(parentClass);

      if (parentType instanceof InlineBeanType<?>) {
        InlineBeanType<?> parentBean = (InlineBeanType<?>) parentType;

        return parentBean.isIntrospecting();
      }
    }

    return false;
  }

  private void introspectParent()
  {
    Class<?> parentClass = _beanClass.getSuperclass();
    
    if (parentClass != null) {
      ConfigType<?> parentType = TypeFactoryConfig.getType(parentClass);

      if (parentType instanceof InlineBeanType<?>) {
        InlineBeanType<?> parentBean = (InlineBeanType<?>) parentType;

        if (! parentBean._isIntrospected)
          parentBean.introspect();

        // ioc/20h4
        if (parentBean.isIntrospecting()) {
          if (! parentBean._pendingChildList.contains(this))
            parentBean._pendingChildList.add(this);
          return;
        }

        if (_setParent == null)
          _setParent = parentBean._setParent;

        if (_replaceObject == null)
          _replaceObject = parentBean._replaceObject;

        if (_setConfigLocation == null)
          _setConfigLocation = parentBean._setConfigLocation;

        if (_setConfigUriLocation == null)
          _setConfigUriLocation = parentBean._setConfigUriLocation;

        if (_setConfigNode == null)
          _setConfigNode = parentBean._setConfigNode;

        if (_addText == null)
          _addText = parentBean._addText;

        if (_addProgram == null)
          _addProgram = parentBean._addProgram;

        if (_addContentProgram == null)
          _addContentProgram = parentBean._addContentProgram;

        if (_addBeanProgram == null) {
          _addBeanProgram = parentBean._addBeanProgram;
        }

        if (_setProperty == null)
          _setProperty = parentBean._setProperty;

        if (_addCustomBean == null)
          _addCustomBean = parentBean._addCustomBean;

        for (Map.Entry<NameCfg,AttributeConfig> entry : parentBean._nsAttributeMap.entrySet()) {
          if (_nsAttributeMap.get(entry.getKey()) == null)
            _nsAttributeMap.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String,AttributeConfig> entry : parentBean._attributeMap.entrySet()) {
          if (_attributeMap.get(entry.getKey()) == null) {
            _attributeMap.put(entry.getKey(), entry.getValue());
          }
        }
        
        _attributeNames.addAll(parentBean._attributeNames);

        _addMethodMap.putAll(parentBean._addMethodMap);
      }
    }
  }

  /**
   * Introspect the beans methods for setters
   */
  public void introspectMethods(Method []methods)
  {
    Constructor<?> []constructors = _beanClass.getConstructors();

    _stringConstructor = findConstructor(constructors, String.class);

    HashMap<String,Method> createMap = new HashMap<String,Method>(8);
    fillCreateMap(createMap, methods);

    HashMap<String,Method> setterMap = new HashMap<String,Method>(8);
    fillSetterMap(setterMap, methods);

    for (Method method : methods) {
      if (method.getAnnotation(DisableConfig.class) != null) {
        continue;
      }
      
      Class<?> []paramTypes = method.getParameterTypes();

      String name = method.getName();

      if ("replaceObject".equals(name) && paramTypes.length == 0) {
        _replaceObject = method;
        _replaceObject.setAccessible(true);
        continue;
      }

      if ("valueOf".equals(name)
          && paramTypes.length == 1
          && String.class.equals(paramTypes[0])
          && Modifier.isStatic(method.getModifiers())) {
        _valueOf = method;
        _valueOf.setAccessible(true);
        continue;
      }

      if (Modifier.isStatic(method.getModifiers()))
        continue;

      if (! Modifier.isPublic(method.getModifiers()))
        continue;

      if ((name.equals("addBuilderProgram") || name.equals("addProgram"))
          && paramTypes.length == 1
          && paramTypes[0].equals(ConfigProgram.class)) {
        ConfigType<?> type = TypeFactoryConfig.getType(paramTypes[0]);

        _addProgram = new ProgramAttribute(method, type);
      }
      else if (name.equals("addContentProgram")
          && paramTypes.length == 1
          && paramTypes[0].equals(ConfigProgram.class)) {
        ConfigType<?> type = TypeFactoryConfig.getType(paramTypes[0]);

        _addContentProgram = new ProgramAttribute(method, type);
      }
      else if (name.equals("addBeanProgram")
          && paramTypes.length == 1
          && paramTypes[0].equals(ConfigProgram.class)) {
        ConfigType<?> type = TypeFactoryConfig.getType(paramTypes[0]);

        _addBeanProgram = new ProgramAttribute(method, type);
      }
      else if ((name.equals("setConfigLocation")
          && paramTypes.length == 2
          && paramTypes[0].equals(String.class)
          && paramTypes[1].equals(int.class))) {
        _setConfigLocation = method;
      }
      else if ((name.equals("setConfigUriLocation")
          && paramTypes.length == 2
          && paramTypes[0].equals(String.class)
          && paramTypes[1].equals(int.class))) {
        _setConfigUriLocation = method;
      }
      else if ((name.equals("setConfigNode")
          && paramTypes.length == 1
          && paramTypes[0].equals(Node.class))) {
        _setConfigNode = method;
      }
      else if ((name.equals("addCustomBean")
          && paramTypes.length == 1
          && paramTypes[0].equals(CustomBean.class))) {
        ConfigType<?> customBeanType
          = TypeFactoryConfig.getType(CustomBean.class);

        _addCustomBean = new AttributeCustomBean(method, customBeanType);
      }
      else if ((name.equals("add")
          && paramTypes.length == 1
          && paramTypes[0].equals(CustomBean.class))) {
        ConfigType<?> customBeanType
          = TypeFactoryConfig.getType(CustomBean.class);

        _addCustomBean = new AttributeCustomBean(method, customBeanType);
      }
      else if ((name.equals("addAnnotation")
          && paramTypes.length == 1
          && paramTypes[0].equals(Annotation.class))) {
        ConfigType<?> customBeanType
          = TypeFactoryConfig.getType(CustomBean.class);

        _addCustomBean = new AttributeCustomBean(method, customBeanType);
      }
      else if (name.equals("setProperty")
          && paramTypes.length == 2
          && paramTypes[0].equals(String.class)) {
        ConfigType<?> type = TypeFactoryConfig.getType(paramTypes[1]);

        PropertyAttribute attr = new PropertyAttribute(method, type);

        _setProperty = attr;
      }
      else if (name.equals("setParent")
          && paramTypes.length == 1) {
        // XXX: use annotation
        _setParent = method;
      }
      else if (name.equals("add")
               && paramTypes.length == 1) {
        ConfigType<?> type = TypeFactoryConfig.getType(paramTypes[0]);

        AttributeConfig addAttr = new AddAttribute(method, type);

        _addMethodMap.put(paramTypes[0], addAttr);

        /* XXX:
        if (paramTypes[0].equals(Bean.class)) { 
          _addBean = addAttr;
        }
        */
      }
      else if ((name.startsWith("set") || name.startsWith("add"))
          && paramTypes.length == 1
          && createMap.get(name.substring(3)) == null) {
        String className = name.substring(3);
        String xmlName = toXmlName(name.substring(3));

        ConfigName tagName = method.getAnnotation(ConfigName.class);

        if (tagName != null) {
          for (String propName : tagName.value()) {
            addProp(propName, method);
          }
        }
        else {
          addProp(xmlName, method);
        }
        
        ConfigArg arg = method.getAnnotation(ConfigArg.class);
        
        if (arg != null) {
          addProp("_p" + arg.value(), method);
        }

        ConfigRest rest = method.getAnnotation(ConfigRest.class);
        
        if (rest != null) {
          addProp("_rest", method);
        }

        addPropCamel(toCamelName(className), method);
      }
      else if ((name.startsWith("create")
          && paramTypes.length == 0
          && ! void.class.equals(method.getReturnType()))) {
        Class<?> type = method.getReturnType();

        Method setter = setterMap.get(name.substring(6));

        CreateAttribute attr = new CreateAttribute(method, type, setter);

        String xmlName = toXmlName(name.substring(6));

        ConfigName tagName = method.getAnnotation(ConfigName.class);

        if (tagName != null) {
          for (String propName : tagName.value()) {
            addProp(propName, attr);
          }
        }
        else {
          addProp(xmlName, attr);
        }
      }
    }
  }
  
  private void addProp(String propName, 
                       Method method)
  {
    addPropImpl(propName, method);
    
    if (method.isAnnotationPresent(Configurable.class)) {
      _attributeNames.add(propName);
    }
  }
  
  private void addPropCamel(String propName, 
                            Method method)
  {
    addPropImpl(propName, method);
  }
  
  private void addPropImpl(String propName, 
                           Method method)
  {
    AttributeConfig attr;
    
    Class<?> []paramTypes = method.getParameterTypes();
    Class<?> type = paramTypes[0];
    
    if (propName.equals("text")
        && (type.equals(String.class)
            || type.equals(RawString.class))) {
      attr = new TextAttribute(method, type);
      _addText = attr;
      _attributeMap.put("#text", attr);
    }
    else {
      attr = new SetterAttribute(method, type);
    }
    
    addProp(propName, attr);
  }
  
  private void addProp(String propName, AttributeConfig attr)
  {
    AttributeConfig oldAttr = _attributeMap.get(propName);
    
    if (oldAttr == null) {
      _attributeMap.put(propName, attr);
    }
    else if (attr.equals(oldAttr)) {
    }
    else if (oldAttr.isConfigurable() && ! attr.isConfigurable()) {
    }
    else if (attr.isConfigurable() && ! oldAttr.isConfigurable()) {
      _attributeMap.put(propName, attr);
    }
    else if (attr.isAssignableFrom(oldAttr)
             && ! oldAttr.isAssignableFrom(attr)) {
    }
    else if (oldAttr.isAssignableFrom(attr)
             && ! attr.isAssignableFrom(oldAttr)) {
      _attributeMap.put(propName, attr);
    }
    else {
      // config/1251
      _attributeMap.put(propName, attr);
      
      log.finest(L.l("{0}: conflicting attribute for '{1}' between {2} and {3}",
                     this, propName, attr, oldAttr));    
    }
    
    // server/2e28 vs jms/2193
    // _attributeMap.put(className, attr);

    if (propName.equals("value")) {
      _attributeMap.put("#text", attr);

      // server/12aa
      if (_addText == null) {
        _addText = attr;
      }
    }
  }


  /**
   * Introspect the bean for configuration
   */
  private void introspectInject()
  {
    synchronized (_introspectLock) {
      if (_initList != null)
        return;

      ArrayList<ConfigProgram> initList = new ArrayList<>();
    
      //InjectionTargetBuilder.introspectInit(initList, getAnnotatedType());
      // InjectionTargetBuilder.introspectInit(initList, _beanClass);
      
      //InjectionTargetBuilder.introspectInit(initList, _beanClass);
      //InjectManager inject = InjectManager.current();
      /*
      InjectManager inject = InjectManager.create();

      if (inject != null) {
        inject.introspectInject(initList, _beanClass);
        inject.introspectInit(initList, _beanClass);
      }
      */
      
      _initList = initList;
    }
  }

  /*
  public static void
    introspectInit(ArrayList<ConfigProgram> initList,
                   Class<?> cl)
    throws ConfigException
  {
    if (cl == null || Object.class.equals(cl)) {
      return;
    }
    
    introspectInit(initList, cl.getSuperclass());
    
    for (Method method : cl.getDeclaredMethods()) {
      if (! method.isAnnotationPresent(PostConstruct.class)) {
        // && ! isAnnotationPresent(annList, Inject.class)) {
        continue;
      }

      if (method.getParameterTypes().length != 0) {
          throw new ConfigException(L.l("{0}: @PostConstruct is requires zero arguments", method));
      }

      PostConstructProgram initProgram
        = new PostConstructProgram(Config.getDefaultConfig(), method);

      if (! initList.contains(initProgram)) {
        initList.add(initProgram);
      }
    }
  }
  */

  private static Constructor findConstructor(Constructor<?> []constructors,
                                             Class<?> ...types)
  {
    for (Constructor<?> ctor : constructors) {
      Class<?> []paramTypes = ctor.getParameterTypes();

      if (isMatch(paramTypes, types))
        return ctor;
    }

    return null;
  }

  private static boolean isMatch(Class<?> []aTypes, Class<?> []bTypes)
  {
    if (aTypes.length != bTypes.length)
      return false;

    for (int i = aTypes.length - 1; i >= 0; i--) {
      if (! aTypes[i].equals(bTypes[i]))
        return false;
    }

    return true;
  }


  private void fillCreateMap(HashMap<String,Method> createMap,
                             Method []methods)
  {
    for (Method method : methods) {
      String name = method.getName();

      if (name.startsWith("create")
          && ! name.equals("create")
          && method.getParameterTypes().length == 0) {
        createMap.put(name.substring("create".length()), method);
      }
    }
  }

  private void fillSetterMap(HashMap<String,Method> setterMap,
                             Method []methods)
  {
    for (Method method : methods) {
      String name = method.getName();

      if (name.length() > 3
          && (name.startsWith("add") || name.startsWith("set"))
          && method.getParameterTypes().length == 1) {
        setterMap.put(name.substring("set".length()), method);
      }
    }
  }

  private String toXmlName(String name)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);

      if (Character.isUpperCase(ch)
          && i > 0
          && (Character.isLowerCase(name.charAt(i - 1))
              || (i + 1 < name.length()
                  && Character.isLowerCase(name.charAt(i + 1))))) {
        sb.append('-');
      }

      sb.append(Character.toLowerCase(ch));
    }

    return sb.toString();
  }

  private String toCamelName(String name)
  {
    return Introspector.decapitalize(name);
  }

  /**
   * Config usage.
   */
  public String getAttributeUsage()
  {
    ArrayList<String> nameList = new ArrayList<>(_attributeNames);
    
    Collections.sort(nameList);
    
    StringBuilder sb = new StringBuilder();
    
    for (String name : nameList) {
      sb.append("\n  ").append(name);
    }
    
    return sb.toString();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _beanClass.getSimpleName() + "]";
  }
  
  private abstract static class ConfigBeanFactory {
    abstract public Object create();
  }
  
  /*
  private static class ConfigBeanFactoryCdi<T> extends ConfigBeanFactory
  {
    private InjectionTarget<T> _injectionTarget;
    private Bean<T> _bean;
    
    ConfigBeanFactoryCdi(Bean<T> bean,
                         InjectionTarget<T> injectionTarget)
    {
      _bean = bean;
      _injectionTarget = injectionTarget;
    }
  
    @Override
    public Object create()
    {
      InjectionTarget<T> injection = _injectionTarget;
      CreationalContext<T> env = new OwnerCreationalContext<T>(_bean);

      T bean = injection.produce(env);
      injection.inject(bean, env);
      
      return bean;
    }
  }
  */
  private static class ConfigBeanFactoryInject<T> extends ConfigBeanFactory
  {
    private Supplier<T> _supplier;
    
    ConfigBeanFactoryInject(Supplier<T> supplier)
    {
      _supplier = supplier;
    }
  
    @Override
    public Object create()
    {
      Object value = _supplier.get();
      
      return value;
    }
  }
  
  private static class ConfigBeanFactoryClass extends ConfigBeanFactory {
    private Constructor<?> _ctor;
    
    ConfigBeanFactoryClass(Class<?> cl)
    {
      try {
        _ctor = cl.getDeclaredConstructor();
        _ctor.setAccessible(true);
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }
    }
    
    @Override
    public Object create()
    {
      try {
        return _ctor.newInstance();
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }
    }
  }
}
