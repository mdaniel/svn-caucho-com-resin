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

package com.caucho.config.inject;

import com.caucho.config.annotation.ServiceType;
import com.caucho.config.program.FieldComponentProgram;
import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.types.*;
import com.caucho.naming.*;
import com.caucho.util.*;
import com.caucho.config.*;
import com.caucho.config.bytecode.*;
import com.caucho.config.cfg.*;
import com.caucho.config.event.ObserverImpl;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.logging.*;
import java.io.Serializable;

import javax.annotation.*;
import javax.context.Dependent;
import javax.context.ScopeType;
import javax.event.IfExists;
import javax.event.Observes;
import javax.inject.AnnotationLiteral;
import javax.inject.BindingType;
import javax.inject.Current;
import javax.inject.DeploymentType;
import javax.inject.Initializer;
import javax.inject.Produces;
import javax.inject.Production;
import javax.inject.manager.Bean;
import javax.inject.manager.InjectionPoint;
import javax.inject.manager.Manager;
import javax.interceptor.InterceptorBindingType;

/**
 * Configuration for the xml web bean component.
 */
abstract public class AbstractBean<T> extends CauchoBean<T>
  implements ObjectProxy
{
  private static final L10N L = new L10N(AbstractBean.class);
  private static final Logger log
    = Logger.getLogger(AbstractBean.class.getName());

  private static final Object []NULL_ARGS = new Object[0];
  private static final ConfigProgram []NULL_INJECT = new ConfigProgram[0];

  private static final HashSet<Class> _reservedTypes
    = new HashSet<Class>();

  public static final Annotation []CURRENT_ANN
    = new Annotation[] { new CurrentLiteral() };

  protected InjectManager _webBeans;
  
  private Type _targetType;
  private BaseType _baseType;
  
  private Class<? extends Annotation> _deploymentType;

  private LinkedHashSet<BaseType> _types
    = new LinkedHashSet<BaseType>();

  private LinkedHashSet<Class<?>> _typeClasses
    = new LinkedHashSet<Class<?>>();

  private String _name;
  
  private ArrayList<Annotation> _bindings
    = new ArrayList<Annotation>();

  private ArrayList<Annotation> _interceptorBindings;

  private Class<? extends Annotation> _scopeType;

  private ArrayList<Annotation> _stereotypes
    = new ArrayList<Annotation>();

  // general custom annotations
  private HashMap<Class,Annotation> _annotationMap
    = new HashMap<Class,Annotation>();
  
  private HashMap<Method,ArrayList<WbInterceptor>> _interceptorMap;

  private ArrayList<ProducesComponent> _producesList;

  private boolean _isNullable;

  protected ScopeContext _scope;

  protected ConfigProgram []_injectProgram = NULL_INJECT;
  protected ConfigProgram []_initProgram = NULL_INJECT;
  protected ConfigProgram []_destroyProgram = NULL_INJECT;

  protected Method _cauchoPostConstruct;
  
  public AbstractBean(InjectManager manager)
  {
    super(manager);

    _webBeans = manager;
  }

  public InjectManager getWebBeans()
  {
    return _webBeans;
  }

  /**
   * Sets the component type.
   */
  public void setDeploymentType(Class<? extends Annotation> type)
  {
    if (type == null)
      throw new NullPointerException();

    if (! type.isAnnotationPresent(DeploymentType.class))
      throw new ConfigException(L.l("'{0}' is an invalid deployment type because it does not implement @javax.inject.DeploymentType",
				    type));

    if (_deploymentType != null && ! _deploymentType.equals(type))
      throw new ConfigException(L.l("deployment-type must be unique"));
    
    _deploymentType = type;
  }

  /**
   * Returns the bean's deployment type
   */
  public Class<? extends Annotation> getDeploymentType()
  {
    return _deploymentType;
  }

  public void setTargetType(Type type)
  {
    _targetType = type;

    _baseType = BaseType.create(type, null);
    
    validateClass(_baseType.getRawClass());
  }
  
  public Type getTargetType()
  {
    return _targetType;
  }
  
  public String getTargetSimpleName()
  {
    if (_targetType instanceof Class)
      return ((Class) _targetType).getSimpleName();
    else
      return String.valueOf(_targetType);
  }
  
  public Class getTargetClass()
  {
    return _baseType.getRawClass();
  }

  public String getTargetName()
  {
    if (_targetType instanceof Class)
      return ((Class) _targetType).getName();
    else
      return String.valueOf(_targetType);
  }

  /**
   * Returns the bean's EL binding name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Gets the bean's EL binding name.
   */
  @Override
  public String getName()
  {
    return _name;
  }

  /**
   * Adds a binding annotation
   */
  public void addBinding(Annotation binding)
  {
    if (! binding.annotationType().isAnnotationPresent(BindingType.class))
      throw new ConfigException(L.l("'{0}' is not a valid binding because it does not have a @javax.webbeans.BindingType annotation",
				    binding));
    
    _bindings.add(binding);
  }

  /**
   * Returns the bean's binding types
   */
  @Override
  public Set<Annotation> getBindings()
  {
    Set<Annotation> set = new LinkedHashSet<Annotation>();

    for (Annotation binding : _bindings) {
      set.add(binding);
    }

    return set;
  }

  /**
   * Returns an array of the binding annotations
   */
  public Annotation []getBindingArray()
  {
    if (_bindings == null || _bindings.size() == 0)
      return new Annotation[] { new CurrentLiteral() };

    Annotation []bindings = new Annotation[_bindings.size()];
    _bindings.toArray(bindings);
    
    return bindings;
  }

  /**
   * Adds a binding annotation
   */
  public void addInterceptorBinding(Annotation binding)
  {
    if (! binding.annotationType().isAnnotationPresent(InterceptorBindingType.class))
      throw new ConfigException(L.l("'{0}' is not a valid binding because it does not have a @javax.webbeans.InterceptorBindingType annotation",
				    binding));
    if (_interceptorBindings == null)
      _interceptorBindings = new ArrayList<Annotation>();
    
    _interceptorBindings.add(binding);
  }

  /**
   * Returns the bean's binding types
   */
  public Set<Annotation> getInterceptorBindingTypes()
  {
    Set<Annotation> set = new LinkedHashSet<Annotation>();

    for (Annotation binding : _interceptorBindings) {
      set.add(binding);
    }

    return set;
  }

  /**
   * Returns an array of the binding annotations
   */
  public Annotation []getInterceptorBindingArray()
  {
    if (_interceptorBindings == null)
      return null;

    Annotation []bindings = new Annotation[_interceptorBindings.size()];
    _interceptorBindings.toArray(bindings);
    
    return bindings;
  }

  /**
   * Sets the scope annotation.
   */
  public void setScopeType(Class<? extends Annotation> scopeType)
  {
    if (! scopeType.isAnnotationPresent(ScopeType.class))
      throw new ConfigException(L.l("'{0}' is not a valid scope because it does not have a @javax.webbeans.ScopeType annotation",
				    scopeType));

    if (_scopeType != null && ! _scopeType.equals(scopeType))
      throw new ConfigException(L.l("'{0}' conflicts with an earlier scope type definition '{1}'.  ScopeType must be defined exactly once.",
				    scopeType.getName(),
				    _scopeType.getName()));
    
    _scopeType = scopeType;
  }

  /**
   * Returns the scope
   */
  public Class<? extends Annotation> getScopeType()
  {
    return _scopeType;
  }

  /**
   * Adds a stereotype
   */
  public void addStereotype(Annotation stereotype)
  {
    if (! stereotype.annotationType().isAnnotationPresent(Stereotype.class))
      throw new ConfigException(L.l("'{0}' is not a valid stereotype because it does not have a @javax.webbeans.Stereotype annotation",
				    stereotype));
    
    _stereotypes.add(stereotype);
  }

  /**
   * Adds a custom
   */
  public void addAnnotation(Annotation annotation)
  {
    _annotationMap.put(annotation.annotationType(), annotation);
  }

  /**
   * Returns the services
   */
  public Annotation []getAnnotations()
  {
    Annotation []annotations = new Annotation[_annotationMap.size()];

    _annotationMap.values().toArray(annotations);
    
    return annotations;
  }

  /**
   * Returns the types that the bean implements
   */
  public Set<Class<?>> getTypes()
  {
    return _typeClasses;
  }

  /**
   * Returns the types that the bean implements
   */
  public Set<BaseType> getGenericTypes()
  {
    return _types;
  }

  /**
   * Initialization.
   */
  protected void init()
  {
    introspect();

    initStereotypes();

    initDefault();

    if (_producesList != null) {
      for (ProducesComponent producesBean : _producesList) {
	_webBeans.addBean(producesBean);
      }
    }
  }

  protected void initDefault()
  {
    if (_deploymentType == null)
      _deploymentType = Production.class;

    if (_bindings.size() == 0)
      addBinding(CurrentLiteral.CURRENT);

    if (_scopeType == null)
      _scopeType = Dependent.class;

    if ("".equals(_name)) {
      _name = getDefaultName();
    }
  }

  protected String getDefaultName()
  {
    String name = getTargetSimpleName();

    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  protected void introspect()
  {
    Class cl = getTargetClass();
    
    if (_types.size() == 0)
      introspectTypes(cl);
  }

  /**
   * Introspects all the types implemented by the class
   */
  protected void introspectTypes(Type type)
  {
    if (_types.size() == 0)
      introspectTypes(type, null);
  }

  /**
   * Introspects all the types implemented by the class
   */
  private void introspectTypes(Type type, HashMap paramMap)
  {
    if (type == null || _reservedTypes.contains(type))
      return;

    Class cl = addType(type, paramMap);
    
    if (cl == null)
      return;
    
    introspectTypes(cl.getGenericSuperclass(), paramMap);

    for (Type iface : cl.getGenericInterfaces()) {
      introspectTypes(iface, paramMap);
    }
  }

  protected Class addType(Type type)
  {
    return addType(type, null);
  }

  protected Class addType(Type type, HashMap paramMap)
  {
    BaseType baseType = BaseType.create(type, paramMap);

    if (baseType == null)
      return null;

    if (_types.contains(baseType))
      return null;
    
    _types.add(baseType);

    if (! _typeClasses.contains(baseType.getRawClass()))
      _typeClasses.add(baseType.getRawClass());

    return baseType.getRawClass();
  }

  protected void introspectClass(Class cl)
  {
    introspectAnnotations(cl.getAnnotations());
  }

  protected void introspectAnnotations(Annotation []annotations)
  {
    introspectDeploymentType(annotations);
    introspectScope(annotations);
    introspectBindings(annotations);
    introspectStereotypes(annotations);

    for (Annotation ann : annotations) {
      if (_annotationMap.get(ann.annotationType()) == null)
	_annotationMap.put(ann.annotationType(), ann);
    }
  }

  /**
   * Called for implicit introspection.
   */
  protected void introspectDeploymentType(Class cl)
  {
    introspectDeploymentType(cl.getAnnotations());
  }

  /**
   * Called for implicit introspection.
   */
  protected void introspectDeploymentType(Annotation []annotations)
  {
    Class deploymentType = null;

    if (getDeploymentType() == null) {
      for (Annotation ann : annotations) {
	if (ann.annotationType().isAnnotationPresent(DeploymentType.class)) {
	  if (deploymentType != null)
	    throw new ConfigException(L.l("{0}: @DeploymentType annotation @{1} is invalid because it conflicts with @{2}.  WebBeans components may only have a single @DeploymentType.",
					  getTargetName(),
					  deploymentType.getName(),
					  ann.annotationType().getName()));

	  deploymentType = ann.annotationType();
	  setDeploymentType(deploymentType);
	}
      }
    }
  }

  /**
   * Called for implicit introspection.
   */
  protected void introspectScope(Class cl)
  {
    introspectScope(cl.getAnnotations());
  }

  /**
   * Called for implicit introspection.
   */
  protected void introspectScope(Annotation []annotations)
  {
    Class scopeClass = null;

    if (getScopeType() == null) {
      for (Annotation ann : annotations) {
	if (ann.annotationType().isAnnotationPresent(ScopeType.class)) {
	  if (scopeClass != null)
	    throw new ConfigException(L.l("{0}: @ScopeType annotation @{1} conflicts with @{2}.  WebBeans components may only have a single @ScopeType.",
					  getTargetName(),
					  scopeClass.getName(),
					  ann.annotationType().getName()));

	  scopeClass = ann.annotationType();
	  setScopeType(scopeClass);
	}
      }
    }
  }

  /**
   * Introspects the binding annotations
   */
  protected void introspectBindings(Annotation []annotations)
  {
    if (_bindings.size() == 0) {
      for (Annotation ann : annotations) {
	if (ann.annotationType().isAnnotationPresent(BindingType.class))
	  addBinding(ann);

	if (ann instanceof Named && getName() == null)
	  setName(((Named) ann).value());
      }
    }
  }

  /**
   * Called for implicit introspection.
   */
  protected void introspectStereotypes(Class cl)
  {
    introspectStereotypes(cl.getAnnotations());
  }

  /**
   * Adds the stereotypes from the bean's annotations
   */
  protected void introspectStereotypes(Annotation []annotations)
  {
    if (_stereotypes.size() == 0) {
      for (Annotation ann : annotations) {
	Class annType = ann.annotationType();
      
	if (annType.isAnnotationPresent(Stereotype.class)) {
	  _stereotypes.add(ann);
	}
      }
    }
  }

  /**
   * Adds any values from the stereotypes
   */
  protected void initStereotypes()
  {
    if (_scopeType == null) {
      for (Annotation stereotype : _stereotypes) {
	Class stereotypeType = stereotype.annotationType();
	
	for (Annotation ann : stereotypeType.getDeclaredAnnotations()) {
	  Class annType = ann.annotationType();
	  
	  if (annType.isAnnotationPresent(ScopeType.class))
	    setScopeType(annType);
	}
      }
    }
    
    if (_deploymentType == null) {
      for (Annotation stereotype : _stereotypes) {
	Class stereotypeType = stereotype.annotationType();
	
	for (Annotation ann : stereotypeType.getDeclaredAnnotations()) {
	  Class annType = ann.annotationType();

	  if (! annType.isAnnotationPresent(DeploymentType.class))
	    continue;

	  // XXX: potential issue where getDeploymentPriority isn't set yet
	  
	  if (_deploymentType == null
	      || (_webBeans.getDeploymentPriority(_deploymentType)
		  < _webBeans.getDeploymentPriority(annType))) {
	    _deploymentType = annType;
	  }
	}
      }
    }
    
    if (_name == null) {
      for (Annotation stereotype : _stereotypes) {
	Class stereotypeType = stereotype.annotationType();
	
	for (Annotation ann : stereotypeType.getDeclaredAnnotations()) {
	  Class annType = ann.annotationType();
	  
	  if (annType.equals(Named.class)) {
	    Named named = (Named) ann;
	    _name = "";

	    if (! "".equals(named.value()))
	      throw new ConfigException(L.l("@Named must not have a value in a @Stereotype definition; it must have an empty value=\"\"."));
	  }
	}
      }
    }
    
    for (Annotation stereotypeAnn : _stereotypes) {
      Class stereotypeType = stereotypeAnn.annotationType();

      for (Annotation ann : stereotypeType.getDeclaredAnnotations()) {
	Class annType = ann.annotationType();

	if (annType.isAnnotationPresent(BindingType.class)) {
	  throw new ConfigException(L.l("'{0}' is not allowed on @Stereotype '{1}' because stereotypes may not have @BindingType annotations",
					ann, stereotypeAnn));
	}

	if (ann instanceof Stereotype) {
	  Stereotype stereotype = (Stereotype) ann;
      
	  for (Class requiredType : stereotype.requiredTypes()) {
	    if (! requiredType.isAssignableFrom(getTargetClass())) {
	      throw new ConfigException(L.l("'{0}' may not have '{1}' because it does not implement the required type '{2}'",
					    getTargetName(),
					    stereotypeAnn,
					    requiredType.getName()));
	    }
	  }

	  boolean hasScope = stereotype.supportedScopes().length == 0;
	  for (Class supportedScope : stereotype.supportedScopes()) {
	    Class scopeType = getScopeType();
	    if (scopeType == null)
	      scopeType = Dependent.class;
	    
	    if (supportedScope.equals(scopeType))
	      hasScope = true;
	  }

	  if (! hasScope) {
	    ArrayList<String> scopeNames = new ArrayList<String>();

	    for (Class supportedScope : stereotype.supportedScopes())
	      scopeNames.add("@" + supportedScope.getSimpleName());
	    
	    throw new ConfigException(L.l("'{0}' may not have '{1}' because it does not implement a supported scope {2}",
					  getTargetName(),
					  stereotypeAnn,
					  scopeNames));
	  }
	}
      }
    }
  }

  /**
   * Introspects the methods for any @Produces
   */
  protected void introspectProduces(Class cl)
  {
    if (cl == null)
      return;

    introspectProduces(cl.getDeclaredMethods());
  }

  /**
   * Introspects the methods for any @Produces
   */
  protected void introspectProduces(Method []methods)
  {
    for (Method method : methods) {
      if (Modifier.isStatic(method.getModifiers()))
	continue;

      if (! method.isAnnotationPresent(Produces.class))
	continue;

      addProduces(method, method.getAnnotations());
    }
  }

  protected void addProduces(Method method, Annotation []annList)
  {
    ProducesComponent comp
      = new ProducesComponent(_webBeans, this, method, annList);

    comp.init();

    if (_producesList == null)
      _producesList = new ArrayList<ProducesComponent>();
    _producesList.add(comp);
  }

  protected void addMethod(Method method, Annotation []annList)
  {
    SimpleBeanMethod beanMethod = new SimpleBeanMethod(method, annList);
  }

  /**
   * Introspects any observers.
   */
  protected void introspectObservers(Class cl)
  {
    introspectObservers(cl.getDeclaredMethods());
  }

  /**
   * Introspects any observers.
   */
  protected void introspectObservers(Method []methods)
  {
    Arrays.sort(methods, new MethodNameComparator());
    
    for (Method method : methods) {
      int param = findObserverAnnotation(method);

      if (param < 0)
	continue;

      Type eventType = method.getGenericParameterTypes()[param];

      ArrayList<Annotation> bindingList = new ArrayList<Annotation>();
      
      Annotation [][]annList = method.getParameterAnnotations();
      if (annList != null && annList[param] != null) {
	for (Annotation ann : annList[param]) {
	  if (ann.annotationType().equals(IfExists.class))
	    continue;
	  
	  if (ann.annotationType().isAnnotationPresent(BindingType.class))
	    bindingList.add(ann);
	}
      }

      Annotation []bindings = new Annotation[bindingList.size()];
      bindingList.toArray(bindings);

      ObserverImpl observer = new ObserverImpl(_webBeans, this, method, param);

      _webBeans.addObserver(observer, (Class) eventType, bindings);
    }
  }

  /**
   * Introspects any intercepted methods
   */
  protected void introspectInterceptors(Class cl)
  {
    introspectInterceptors(cl.getMethods());
  }
  
  /**
   * Introspects any intercepted methods
   */
  protected void introspectInterceptors(Method []methods)
  {
    for (Method method : methods) {
      if (method.getDeclaringClass().equals(Object.class))
	continue;
      
      ArrayList<Annotation> interceptorTypes = findInterceptorTypes(method);

      if (interceptorTypes == null)
	continue;

      /* XXX:
      ArrayList<WbInterceptor> interceptors
	= _webBeans.findInterceptors(interceptorTypes);

      if (interceptors != null) {
	if (_interceptorMap == null)
	  _interceptorMap = new HashMap<Method,ArrayList<WbInterceptor>>();

	_interceptorMap.put(method, interceptors);
      }
      */
    }
  }

  private ArrayList<Annotation> findInterceptorTypes(Method method)
  {
    ArrayList<Annotation> types = null;

    for (Annotation ann : method.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(InterceptorBindingType.class)) {
	if (types == null)
	  types = new ArrayList<Annotation>();

	types.add(ann);
      }
    }

    return types;
  }
  
  protected boolean hasBindingAnnotation(Constructor ctor)
  {
    if (ctor.isAnnotationPresent(Initializer.class))
      return true;

    Annotation [][]paramAnn = ctor.getParameterAnnotations();

    for (Annotation []annotations : paramAnn) {
      for (Annotation ann : annotations) {
	if (ann.annotationType().isAnnotationPresent(BindingType.class))
	  return true;
      }
    }

    return false;
  }

  private int findObserverAnnotation(Method method)
  {
    Annotation [][]paramAnn = method.getParameterAnnotations();
    int observer = -1;

    for (int i = 0; i < paramAnn.length; i++) {
      for (Annotation ann : paramAnn[i]) {
	if (ann instanceof Observes) {
	  if (observer >= 0)
	    throw InjectManager.error(method, L.l("Only one param may have an @Observer"));
	  
	  observer = i;
	}
      }
    }

    return observer;
  }

  public boolean isMatch(ArrayList<Annotation> bindings)
  {
    for (int i = 0; i < bindings.size(); i++) {
      if (! isMatch(bindings.get(i)))
	return false;
    }
    
    return true;
  }

  public boolean isMatch(Annotation []bindings)
  {
    for (Annotation binding : bindings) {
      if (! isMatch(binding))
	return false;
    }
    
    return true;
  }

  /**
   * Returns true if at least one of this component's bindings match
   * the injection binding.
   */
  public boolean isMatch(Annotation bindAnn)
  {
    for (int i = 0; i < _bindings.size(); i++) {
      // XXX:
      if (_bindings.get(i).equals(bindAnn))
	return true;
    }
    
    return false;
  }

  /**
   * Returns the bean's name or null if the bean does not have a
   * primary name.
   */
      /*
  public String getName()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
      */

  /**
   * Returns true if the bean can be null
   */
  public boolean isNullable()
  {
    return false;
  }

  /**
   * Returns true if the bean is serializable
   */
  public boolean isSerializable()
  {
    return Serializable.class.isAssignableFrom(getTargetClass());
  }

  protected Bean bindParameter(String loc,
			       Type type,
			       Annotation []bindings)
  {
    Set set = _webBeans.resolve(type, bindings);

    if (set == null || set.size() == 0)
      return null;

    if (set.size() > 1) {
      throw new ConfigException(L.l("{0}: can't bind webbeans '{1}' because multiple matching beans were found: {2}",
				    loc, type, set));
    }

    Iterator iter = set.iterator();
    if (iter.hasNext()) {
      Bean bean = (Bean) iter.next();

      return bean;
    }

    return null;
  }

  protected void validateClass(Class cl)
  {
    ClassLoader webBeansLoader = _webBeans.getClassLoader();
    
    if (webBeansLoader == null)
      webBeansLoader = ClassLoader.getSystemClassLoader();

    ClassLoader beanLoader = cl.getClassLoader();

    if (beanLoader == null)
      beanLoader = ClassLoader.getSystemClassLoader();

    for (ClassLoader loader = webBeansLoader;
	 loader != null;
	 loader = loader.getParent()) {
      if (beanLoader == loader)
	return;
    }

    if (false) {
      // server/2pad
      throw new IllegalStateException(L.l("'{0}' is an invalid class because its classloader '{1}' does not belong to the webbeans classloader '{2}'",
					  cl, beanLoader,
					  webBeansLoader));
    }
    else {
      log.fine(L.l("'{0}' is an invalid class because its classloader '{1}' does not belong to the webbeans classloader '{2}'",
		   cl, beanLoader,
		   webBeansLoader));
    }
  }

  /**
   * Returns the set of injection points, for validation.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String toDebugString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getTargetSimpleName());
    sb.append("[");
    
    if (_name != null) {
      sb.append("name=");
      sb.append(_name);
    }

    for (Annotation binding : _bindings) {
      sb.append(",");
      sb.append(binding);
    }

    if (_deploymentType != null) {
      sb.append(", @");
      sb.append(_deploymentType.getSimpleName());
    }
    
    if (_scopeType != null) {
      sb.append(", @");
      sb.append(_scopeType.getSimpleName());
    }

    sb.append("]");

    return sb.toString();
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    sb.append(getTargetSimpleName());
    sb.append(", ");

    if (_deploymentType != null) {
      sb.append("@");
      sb.append(_deploymentType.getSimpleName());
    }
    else
      sb.append("@null");
    
    if (_name != null) {
      sb.append(", ");
      sb.append("name=");
      sb.append(_name);
    }
    
    if (_scope != null) {
      sb.append(", @");
      sb.append(_scope.getClass().getSimpleName());
    }

    sb.append("]");

    return sb.toString();
  }

  static class MethodNameComparator implements Comparator<Method> {
    public int compare(Method a, Method b)
    {
      return a.getName().compareTo(b.getName());
    }
  }

  static {
    _reservedTypes.add(java.io.Closeable.class);
    _reservedTypes.add(java.io.Serializable.class);
    _reservedTypes.add(Cloneable.class);
    _reservedTypes.add(Object.class);
    _reservedTypes.add(Comparable.class);
  }
}
