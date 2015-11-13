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
 * @author Alex Rojkov
 */

package com.caucho.v5.config.candi;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InjectionTargetFactory;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.interceptor.InterceptorBinding;

import com.caucho.v5.config.CauchoDeployment;
import com.caucho.v5.config.Configured;
import com.caucho.v5.config.candi.CandiManager.TypedBean;
import com.caucho.v5.config.event.EventBeanImpl;
import com.caucho.v5.config.reflect.BaseType;
import com.caucho.v5.config.reflect.ReflectionAnnotatedFactory;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;

public class BeanManagerBase implements BeanManager, Serializable
{
  private static final L10N L = new L10N(BeanManagerBase.class);
  private static final Logger log
    = Logger.getLogger(BeanManagerBase.class.getName());
  
  private final CandiManager _delegate;

  private final InterceptorsBuilder _interceptorsBuilder;
  private final DecoratorsBuilder _decoratorsBuilder;
  
  private final AlternativesBuilder _alternativesBuilder;
  // private List<Class<?>> _appAlternatives = new ArrayList<>();
  
  private BeanManagerBase _parent;

  private List<DecoratorEntry<?>> _decoratorList;

  private ConcurrentHashMap<Bean<?>,ReferenceFactory<?>> _refFactoryMap
    = new ConcurrentHashMap<>();

  private HashMap<Class<?>,Integer> _deploymentMap
    = new HashMap<>();

  private HashMap<String,WebComponent> _beanMap
    = new HashMap<>();

  public BeanManagerBase(CandiManager manager,
                         BeanManagerBase parent)
  {
    Objects.requireNonNull(manager);
    //Objects.requireNonNull(context);

    _delegate = manager;
    _parent = parent;
    
    _interceptorsBuilder = new InterceptorsBuilder(this);
    _decoratorsBuilder = new DecoratorsBuilder(this);
    _alternativesBuilder = new AlternativesBuilder(this);
  }

  public Path getRoot()
  {
    return null;
  }
  
  public BeanManagerBase getParent()
  {
    return _parent;
  }

  public void setFinalDiscovered(AfterTypeDiscovery event)
  {
    _interceptorsBuilder.setFinalDiscovered(event.getInterceptors());
    _decoratorsBuilder.setFinalDiscovered(event.getDecorators());
    _alternativesBuilder.setFinalDiscovered(event.getAlternatives());
  }
  
  public void disable(AnnotatedType<?> type)
  {
    _interceptorsBuilder.disable(type);
    _decoratorsBuilder.disable(type);
    _alternativesBuilder.disable(type);
  }
  
  public List<Class<?>> getInterceptors()
  {
    return _interceptorsBuilder.getDiscovered();
  }
  
  public List<Class<?>> getDecorators()
  {
    return _decoratorsBuilder.getDecorators();
  }

  protected CandiManager getDelegate()
  {
    return _delegate;
  }

  public void addDecoratorClass(Class<?> cl)
  {
    _decoratorsBuilder.addRegistered(cl);
  }

  public void addDecorator(AnnotatedType<?> type,
                           DecoratorEntry<?> entry)
  {
    if (_decoratorsBuilder.isEnabled(type)) {
      _decoratorsBuilder.addDecorator(entry);
    }
    else if (_parent != null) {
      _parent.addDecorator(type, entry);
    }
    else {
      //_decoratorsBuilder.addDecorator(entry);
    }
  }

  public void addInterceptorClass(Class<?> cl)
  {
    _interceptorsBuilder.addRegistered(cl);
  }

  public void addInterceptor(AnnotatedType<?> annType,
                             InterceptorEntry<?> entry)
  {
    //_interceptorsBuilder.discover(annType);
    _interceptorsBuilder.addInterceptor(entry);
  }
  
  public boolean isInterceptorEnabled(AnnotatedType<?> type)
  {
    if (type.isAnnotationPresent(Priority.class)) {
      return true;
    }
    
    return _interceptorsBuilder.isEnabled(type);
  }
  
  public boolean isDecoratorEnabled(AnnotatedType<?> type)
  {
    if (type.isAnnotationPresent(Priority.class)) {
      return true;
    }
    else if (_decoratorsBuilder.isEnabled(type)) {
      return true;
    }
    else if (_parent != null) {
      return _parent.isDecoratorEnabled(type);
    }
    else {
      return false;
    }
  }

  public void addAlternative(Class<?> type)
  {
    _alternativesBuilder.addRegistered(type);
  }
  
  public boolean isAlternativeEnabled(AnnotatedType<?> type)
  {
    if (_alternativesBuilder.isEnabled(type)) {
      return true;
    }
    else if (_parent != null) {
      return _parent.isAlternativeEnabled(type);
    }
    else {
      return false;
    }
  }
  
  public boolean isAlternativeEnabled(Bean<?> bean)
  {
    if (! (bean instanceof ManagedBeanImpl)) {
      return false;
    }
    
    ManagedBeanImpl<?> mBean = (ManagedBeanImpl<?>) bean;
    
    return isAlternativeEnabled(mBean.getAnnotatedType());
  }

  boolean isAlternative(Bean<?> bean)
  {
    List<Class<?>> altList = getAlternatives();
    
    return altList.contains(bean.getBeanClass());
  }

  public List<Class<?>> getAlternatives()
  {
    //List<Class<?>> altList = _alternativesBuilder.getAlternatives();

    //return new ArrayList<>(altList);
    
    return _alternativesBuilder.getAlternatives();
  }
  
  public int getAlternativePriority(Bean<?> bean)
  {
    /*
    Priority priorityAnnotation
      = bean.getBeanClass().getAnnotation(Priority.class);

    if (priorityAnnotation != null) {
      Integer value = priorityAnnotation.value();

      if (value != null) {
        return value;
      }
    }
    */

    List<Class<?>> altList = getAlternatives();
    
    int index = altList.indexOf(bean.getBeanClass());
    
    if (index < 0) {
      // ioc/0pc9
      return index;
    }
    
    // ioc/0pc8 -- altList already sorted by priority?
    /*
    Priority priorityAnnotation
      = bean.getBeanClass().getAnnotation(Priority.class);

    if (priorityAnnotation != null) {
      Integer value = priorityAnnotation.value();

      if (value != null) {
        return value;
      }
    }
    */
    
    return index; 
  }

  public void setDeploymentTypes(ArrayList<Class<?>> deploymentList)
  {
    //    _deploymentMap.clear();

    //    _deploymentMap.put(CauchoDeployment.class, 0);

    if (! _deploymentMap.containsKey(CauchoDeployment.class)) {
      _deploymentMap.put(CauchoDeployment.class, 0);
    }
    // DEFAULT_PRIORITY

    final int priority = CandiManager.DEFAULT_PRIORITY + 1;

/*
    if (! deploymentList.contains(Configured.class)) {
      _deploymentMap.put(Configured.class, priority);
    }
*/

    for (int i = deploymentList.size() - 1; i >= 0; i--) {
      _deploymentMap.put(deploymentList.get(i), priority);
    }

    if (_deploymentMap.containsKey(Configured.class)) {
      _deploymentMap.put(Configured.class, priority);
    }

    for (Class<?> type : deploymentList) {
      // _alternativesBuilder.addRegistered(type);
      
      //beanManager.addAlternative(type);
      addAlternative(type);
    }
  }
  
  void clearCache()
  {
    _beanMap.clear();
  }

  public ManagedBeanImpl<?> createManagedBean(Class<?> cl)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  public void addBeanDiscover(ManagedBeanImpl<?> bean)
  {
    throw new UnsupportedOperationException(getClass().getSimpleName());
  }

  @Override
  public boolean areInterceptorBindingsEquivalent(Annotation ib1,
                                                  Annotation ib2)
  {
    return getDelegate().areInterceptorBindingsEquivalent(ib1, ib2);
  }

  @Override
  public boolean areQualifiersEquivalent(Annotation q1,
                                         Annotation q2)
  {
    return getDelegate().areQualifiersEquivalent(q1, q2);
  }

  @Override
  public boolean isScope(Class<? extends Annotation> annotationType)
  {
    return getDelegate().isScope(annotationType);
  }

  @Override
  public boolean isPassivatingScope(Class<? extends Annotation> annotationType)
  {
    return getDelegate().isPassivatingScope(annotationType);
  }

  @Override
  public boolean isNormalScope(Class<? extends Annotation> annotationType)
  {
    return getDelegate().isNormalScope(annotationType);
  }

  @Override
  public boolean isQualifier(Class<? extends Annotation> annotationType)
  {
    return getDelegate().isQualifier(annotationType);
  }

  @Override
  public boolean isInterceptorBinding(Class<? extends Annotation> annotationType)
  {
    return getDelegate().isInterceptorBinding(annotationType);
  }

  @Override
  public Set<Annotation> getInterceptorBindingDefinition(
    Class<? extends Annotation> bindingType)
  {
    return getDelegate().getInterceptorBindingDefinition(bindingType);
  }

  @Override
  public int getInterceptorBindingHashCode(Annotation binding)
  {
    return getDelegate().getInterceptorBindingHashCode(binding);
  }

  @Override
  public int getQualifierHashCode(Annotation qualififer)
  {
    return getDelegate().getQualifierHashCode(qualififer);
  }

  @Override
  public boolean isStereotype(Class<? extends Annotation> annotationType)
  {
    return getDelegate().isStereotype(annotationType);
  }

  @Override
  public Set<Annotation> getStereotypeDefinition(
    Class<? extends Annotation> stereotype)
  {
    return getDelegate().getStereotypeDefinition(stereotype);
  }

  @Override
  public <T> AnnotatedType<T> createAnnotatedType(Class<T> type)
  {
    return getDelegate().createAnnotatedType(type);
  }

  @Override
  public <T> Bean<T> createBean(BeanAttributes<T> attributes,
                                Class<T> beanClass,
                                InjectionTargetFactory<T> factory)
  {
    return getDelegate().createBean(attributes, beanClass, factory);
  }

  @Override
  public <T, X> Bean<T> createBean(BeanAttributes<T> attributes,
                                   Class<X> beanClass,
                                   ProducerFactory<X> factory)
  {
    return getDelegate().createBean(attributes, beanClass, factory);
  }

  @Override
  public <T> InjectionTarget<T> createInjectionTarget(
    AnnotatedType<T> type)
  {
    return getDelegate().createInjectionTarget(type);
  }
  
  <T> Producer<T> createProducer(Bean<T> bean)
  {
    return new ProducerBase(bean);
  }

  WebComponent getWebComponent(BaseType baseType)
  {
    if (_beanMap == null) {
      return null;
    }

    Class<?> rawClass = baseType.getRawClass();
    String className = rawClass.getName();

    WebComponent beanSet = _beanMap.get(className);

    if (beanSet == null) {
      HashSet<TypedBean> typedBeans = new HashSet<TypedBean>();

      /*
      if (_classLoader != null) {
        FillByType fillByType = new FillByType(baseType, typedBeans, this);

        _classLoader.applyVisibleModules(fillByType);
      }
      */
      
      getDelegate().fillByType(baseType, typedBeans, this);

      beanSet = new WebComponent(getDelegate(), this, className);
      
      _beanMap.put(className, beanSet);
      
      for (TypedBean typedBean : typedBeans) {
        if (getDeploymentPriority(typedBean.getBean()) < 0) {
          continue;
        }

        getDelegate().addPendingValidationBean(typedBean.getBean());
        
        beanSet.addComponent(typedBean.getType(),
                             typedBean.getAnnotated(),
                             typedBean.getBean());
      }
    }
    
    return beanSet;
  }

  public <T> ReferenceFactory<T> getReferenceFactory(Bean<T> bean)
  {
    if (bean == null) {
      return null;
    }

    ReferenceFactory<T> factory = (ReferenceFactory<T>) _refFactoryMap.get(bean);

    if (factory == null) {
      factory = createReferenceFactory(bean);
      _refFactoryMap.put(bean, factory);
      factory.validate();
    }

    return factory;
  }

  private <T> ReferenceFactory<T> createReferenceFactory(Bean<T> bean)
  {
    Class<? extends Annotation> scopeType = bean.getScope();

    if (InjectionPoint.class.equals(bean.getBeanClass())) {
      return (ReferenceFactory) new ReferenceFactoryInjectionPoint();
    }

    if (Dependent.class == scopeType) {
      if (bean instanceof ManagedBeanImpl<?>)
        return new ReferenceFactoryDependentImpl<T>((ManagedBeanImpl<T>) bean);
      else
        return new ReferenceFactoryDependent<T>(bean);
    }

    if (scopeType == null) {
      throw new IllegalStateException("Unknown scope for " + bean);
    }

    CandiManager ownerManager;

    if (bean instanceof BeanBase<?>)
      ownerManager = ((BeanBase<?>) bean).getInjectManager();
    else
      ownerManager = getDelegate();

    Context context = ownerManager.getContextImpl(scopeType);

    /*
    if (context == null)
      return null;
      */
    if (context == null)
      throw new InjectionException(L.l("Bean has an unknown scope '{0}' for bean {1}",
                                       scopeType, bean));

    if (isNormalScope(scopeType) && bean instanceof ScopeAdapterBean<?>) {
      ScopeAdapterBean<T> scopeAdapterBean = (ScopeAdapterBean<T>) bean;

      return new ReferenceFactoryNormalContext<T>(getDelegate(), bean, scopeAdapterBean, context);
    }
    else
      return new ReferenceFactoryContext<T>(bean, context);
  }

  public ReferenceFactory<?> getReferenceFactory(InjectionPoint ij)
  {
    if (ij.isDelegate())
      return new ReferenceFactoryDelegate();
    else if (ij.getType().equals(InjectionPoint.class))
      return new ReferenceFactoryInjectionPoint();
    else if (ij.getQualifiers().contains(DecoratedLiteral.DECORATED))
      return getDelegate().getDecoratedBeanReferenceFactory(ij);
    else if (ij.getQualifiers().contains(InterceptedLiteral.INTERCEPTED))
      return getDelegate().getInterceptedBeanReferenceFactory(ij);

    Type type = ij.getType();
    Set<Annotation> qualifiers = ij.getQualifiers();

    ReferenceFactory<?> factory = getReferenceFactory(type, qualifiers, ij);

    RuntimeException exn = getDelegate().validatePassivation(ij);

    if (exn != null) {
      if (factory.isProducer())
        return new ReferenceFactoryError(exn);
      else
        throw exn;
    }

    return factory;
  }

  public ReferenceFactory<?> getReferenceFactory(Type type,
                                                 Set<Annotation> qualifiers,
                                                 InjectionPoint ij)
  {
    if (ij != null && ij.isDelegate())
      return new ReferenceFactoryDelegate();

    Bean<?> bean = resolveByInjectionPoint(type, qualifiers, ij);
    
    ReferenceFactory<?> referenceFactory = getReferenceFactory(bean);

    if (ij != null && ij.getType().equals(BeanManager.class)) {
      referenceFactory = new ReferenceFactoryBeanManager(getDelegate());
    }

    return referenceFactory;
  }

  private Bean<?> resolveByInjectionPoint(Type type,
                                          Set<Annotation> qualifierSet,
                                          InjectionPoint ij)
  {
    Annotation []qualifiers;
    
    if (qualifierSet != null && qualifierSet.size() > 0) {
      qualifiers = new Annotation[qualifierSet.size()];
      qualifierSet.toArray(qualifiers);

      if (qualifiers.length == 1
          && qualifiers[0].annotationType().equals(New.class)) {
        New newQualifier = (New) qualifiers[0];

        return getDelegate().createNewBean(type, newQualifier);
      }
    }
    else
      qualifiers = new Annotation[] { DefaultLiteral.DEFAULT };

    BaseType baseType = getDelegate().createTargetBaseType(type);
    
    /*
    if (baseType.isGeneric())
      throw new InjectionException(L.l("'{0}' is an invalid type for injection because it's generic. {1}",
                                       baseType, ij));
                                       */
    if (baseType.isGenericVariable())
      throw new InjectionException(L.l("'{0}' is an invalid type for injection because it's a variable generic type.\n  {1}",
                                       baseType, ij));
    
    Set<Bean<?>> set = resolveRec(baseType, qualifiers, ij);

    if (set == null || set.size() == 0) {
      if (InjectionPoint.class.equals(type)) {
        return new InjectionPointBean(this, ij);
      }

      throw getDelegate().unsatisfiedException(type, qualifiers);
    }

    /*
    if (ij != null)
      filterByBeanArchive(ij.getBean(), set);
      */
    
    Bean<?> bean = resolve(set);

    if (bean != null
        && type instanceof Class<?>
        && ((Class<?>) type).isPrimitive()
        && bean.isNullable()) {
      throw new InjectionException(L.l("'{0}' cannot be injected because it's a primitive with {1}",
                                       type, bean));
    }

    return bean;

    /*
    else if (set.size() == 1) {
      Iterator iter = set.iterator();

      if (iter.hasNext()) {
        Bean bean = (Bean) iter.next();

        return bean;
      }
    }
    else {
      throw new AmbiguousResolutionException(L.l("'{0}' with binding {1} matches too many configured beans{2}",
                                                 BaseType.create(type, null),
                                                 bindingSet,
                                                 toLineList(set)));
    }

    return null;
*/
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  Set<Bean<?>> resolveRec(BaseType baseType,
                          Annotation []qualifiers,
                          InjectionPoint ip)
  {
    WebComponent component = getWebComponent(baseType);

    if (component != null) {
      Set<Bean<?>> beans = component.resolve(baseType, 
                                             qualifiers,
                                             ip);
      
      if (beans != null && beans.size() > 0) {
        if (log.isLoggable(Level.FINEST))
          log.finest(this + " bind(" + baseType.getSimpleName()
                     + "," + getDelegate().toList(qualifiers) + ") -> " + beans);

        return beans;
      }
    }

    if (New.class.equals(qualifiers[0].annotationType())) {
      // ioc/0721
      New newQualifier = (New) qualifiers[0];
      HashSet<Bean<?>> set = new HashSet<Bean<?>>();
      Class<?> newClass = newQualifier.value();

      if (newClass == null || newClass.equals(void.class))
        newClass = baseType.getRawClass();

      AnnotatedType<?> ann = ReflectionAnnotatedFactory.introspectType(newClass);
      NewBean<?> newBean = new NewBean(getDelegate(), baseType.getRawClass(), ann);
      newBean.introspect();

      if (component != null) {
        component.addComponent(baseType, null, newBean);
      }

      set.add(newBean);

      return set;
    }

    Class<?> rawType = baseType.getRawClass();

    if (Instance.class.equals(rawType)
        || Provider.class.equals(rawType)) {
      BaseType []param = baseType.getParameters();

      Type beanType;
      if (param.length > 0)
        beanType = param[0].getRawClass();
      else
        beanType = Object.class;

      HashSet<Bean<?>> set = new HashSet<Bean<?>>();
      set.add(new InstanceBeanImpl(getDelegate(), this, beanType, qualifiers, ip));
      return set;
    }
    else if (Event.class.equals(rawType)) {
      if (baseType.isGenericRaw())
        throw new DefinitionException(L.l("Event must have parameters because a non-parameterized Event would observe no events."));

      BaseType []param = baseType.getParameters();

      Type beanType;
      if (param.length > 0)
        beanType = param[0].getRawClass();
      else
        beanType = Object.class;

      HashSet<Annotation> qualifierSet = new LinkedHashSet<Annotation>();

      for (Annotation ann : qualifiers) {
        qualifierSet.add(ann);
      }

      qualifierSet.add(AnyLiteral.ANY);

      HashSet<Bean<?>> set = new HashSet<Bean<?>>();
      set.add(new EventBeanImpl(getDelegate(), beanType, qualifierSet, ip));
      return set;
    }

    component = getDelegate().getProxyComponent();

    if (component != null) {
      Set<Bean<?>> beans = component.resolve(Object.class, qualifiers, ip);

      if (beans != null && beans.size() > 0) {
        if (log.isLoggable(Level.FINEST))
          log.finest(this + " bind-proxy(" + baseType.getSimpleName()
                     + "," + getDelegate().toList(qualifiers) + ") -> " + beans);

        return beans;
      }
    }


    if (_parent != null) {
      return _parent.resolveRec(baseType, qualifiers, ip);
    }
    else if (getDelegate().getParent() != null) {
      return getDelegate().getParent().getBeanManager().resolveRec(baseType, qualifiers, ip);
    }

    for (Annotation ann : qualifiers) {
      if (! ann.annotationType().isAnnotationPresent(Qualifier.class)) {
        throw new IllegalArgumentException(L.l("'{0}' is an invalid binding annotation because it does not have a @Qualifier meta-annotation",
                                               ann));
      }
    }

    if (log.isLoggable(Level.FINEST)) {
      log.finest(this + " bind(" + baseType.getSimpleName()
                + "," + getDelegate().toList(qualifiers) + ") -> none");
    }

    return null;
  }

  public <T> ReferenceFactory<T> createNormalInstanceFactory(Bean<T> bean)
  {
    Class<? extends Annotation> scopeType = bean.getScope();

    if (! isNormalScope(scopeType)) {
      throw new IllegalStateException(L.l("{0} is an invalid normal scope for {1}",
                                          scopeType, bean));
    }

    CandiManager ownerManager;

    if (bean instanceof BeanBase<?>)
      ownerManager = ((BeanBase<?>) bean).getInjectManager();
    else
      ownerManager = getDelegate();

    Context context = ownerManager.getContextImpl(scopeType);

    if (context == null)
      throw new InjectionException(L.l("Bean has an unknown scope '{0}' for bean {1}",
                                       scopeType, bean));

    return new ReferenceFactoryNormalInstance<T>(bean, context);
  }

  
  public int getDeploymentPriority(Bean<?> bean)
  {
    int priority = CandiManager.DEFAULT_PRIORITY;

    if (bean.isAlternative()) {

/*
      priority = -1;
      Integer value = null;

      Priority priorityAnnotation
        = bean.getBeanClass().getAnnotation(Priority.class);

      if (priorityAnnotation != null)
        value = priorityAnnotation.value();

      if (value == null)
        value = getPriority(bean.getBeanClass());

      if (value != null)
        priority = value;
*/
      priority = getAlternativePriority(bean);

      /*
      if (priority == -1) {
        Integer value = getDelegate().getPriority(bean.getBeanClass());

        if (value != null)
          priority = value;
      }
      */
      
      if (priority >= 0) {
        priority++;
      }
    }

    Set<Class<? extends Annotation>> stereotypes = bean.getStereotypes();

    if (stereotypes != null) {
      for (Class<? extends Annotation> annType : stereotypes) {
        //Integer value = _deploymentMap.get(annType);
        Integer value = getPriority(annType);
        
        if (value != null) {
          priority = Math.max(value, priority);
        }
        else if (annType.isAnnotationPresent(Alternative.class)
                 && priority == CandiManager.DEFAULT_PRIORITY)
          priority = -1;
      }
    }

/*
    if (stereotypes != null && _appAlternatives != null) {
      for (Class<? extends Annotation> stereotype : stereotypes) {
        int value = _appAlternatives.indexOf(stereotype);

        if (value > priority)
          priority = value;
      }
    }
*/

    if (priority < 0) {
      return priority;
    }
    else if (bean instanceof BeanBase<?>) {
      // ioc/0213
      BeanBase<?> absBean = (BeanBase<?>) bean;

      if (absBean.getInjectManager() == getDelegate()) {
        priority += 1000000;
      }
    }
    else {
      priority += 1000000;
    }
    
    return priority;
  }

  Integer getPriority(Class<?> cl)
  {
    Integer value = _deploymentMap.get(cl);

    if (value != null) {
      return value;
    }
    else if (_parent != null) {
      return _parent.getPriority(cl);
    }
    else  {
      return getDelegate().getPriority(cl);
    }
  }

  /**
   * Resolves the interceptors for a given interceptor type
   *
   * @param type the main interception type
   * @param qualifiers qualifying bindings
   *
   * @return the matching interceptors
   */
  @Override
  public List<Interceptor<?>> resolveInterceptors(InterceptionType type,
                                                  Annotation... qualifiers)
  {
    if (type == null)
      throw new IllegalArgumentException(L.l("resolveInterceptors requires an InterceptionType"));

    if (qualifiers == null || qualifiers.length == 0)
      throw new IllegalArgumentException(L.l("resolveInterceptors requires at least one @InterceptorBinding"));

    for (int i = 0; i < qualifiers.length; i++) {
      Class<? extends Annotation> annType = qualifiers[i].annotationType();

      if (! annType.isAnnotationPresent(InterceptorBinding.class))
        throw new IllegalArgumentException(L.l("Annotation must be an @InterceptorBinding at '{0}' in resolveInterceptors",
                                               qualifiers[i]));

      for (int j = i + 1; j < qualifiers.length; j++) {
        if (annType.equals(qualifiers[j].annotationType()))
          throw new IllegalArgumentException(L.l("Duplicate binding '{0}' is not allowed in resolveInterceptors",
                                                 qualifiers[i]));
      }
    }
    
    ArrayList<Interceptor<?>> interceptors = new ArrayList<>();
    
    for (BeanManagerBase ptr = this; ptr != null; ptr = ptr.getParent()) {
      ptr._interceptorsBuilder.getInterceptors(interceptors, type, qualifiers);
    }
    
    return interceptors;
  }

  /**
   * Resolves the decorators for a given set of types
   *
   * @param types the types to match for the decorator
   * @param qualifiers qualifying bindings
   *
   * @return the matching interceptors
   */
  @Override
  public List<Decorator<?>> resolveDecorators(Set<Type> types,
                                              Annotation... qualifiers)
  {
    if (types.size() == 0)
      throw new IllegalArgumentException(L.l("type set must contain at least one type"));

    if (qualifiers != null) {
      for (int i = 0; i < qualifiers.length; i++) {
        for (int j = i + 1; j < qualifiers.length; j++) {
          if (qualifiers[i].annotationType() == qualifiers[j].annotationType())
            throw new IllegalArgumentException(L.l("resolveDecorators may not have a duplicate qualifier '{0}'",
                                          qualifiers[i]));
        }
      }
    }

    if (qualifiers == null || qualifiers.length == 0) {
      qualifiers = CandiManager.DEFAULT_ANN;
    }

    /*
    if (_decoratorList == null)
      return decorators;
      */

    for (Annotation ann : qualifiers) {
      if (! isQualifier(ann.annotationType()))
        throw new IllegalArgumentException(L.l("@{0} must be a qualifier", ann.annotationType()));
    }

    ArrayList<BaseType> targetTypes = new ArrayList<BaseType>();

    for (Type type : types) {
      targetTypes.add(getDelegate().createSourceBaseType(type));
    }
    
    ArrayList<Decorator<?>> decorators = new ArrayList<>();
    
    fillDecorators(decorators, this, targetTypes, qualifiers);
    
    return decorators;
  }
  
  private void fillDecorators(ArrayList<Decorator<?>> decorators,
                              BeanManagerBase ptr,
                              ArrayList<BaseType> targetTypes,
                              Annotation []qualifiers)
  {
    if (ptr == null) {
      return;
    }
    
    fillDecorators(decorators, ptr.getParent(), targetTypes, qualifiers);

    ptr._decoratorsBuilder.getDecorators(decorators, targetTypes, qualifiers);
  }

  @Override
  public BeanAttributes<?> createBeanAttributes(AnnotatedMember<?> type)
  {
    return getDelegate().createBeanAttributes(type);
  }

  @Override
  public <T> BeanAttributes<T> createBeanAttributes(AnnotatedType<T> type)
  {
    return getDelegate().createBeanAttributes(type);
  }

  /**
   * Returns the beans matching a class and annotation set
   *
   * @param type the bean's class
   * @param qualifiers required @Qualifier annotations
   */
  @Override
  public Set<Bean<?>> getBeans(Type type,
                               Annotation... qualifiers)
  {
    if (qualifiers != null) {
      for (int i = 0; i < qualifiers.length; i++) {
        Annotation qualifier = qualifiers[i];

        if (qualifier == null) {
          throw new NullPointerException(L.l("Null qualifier"));
        }

        for (int j = i + 1; j < qualifiers.length; j++) {
          if (qualifier.annotationType() == qualifiers[j].annotationType())
            throw new IllegalArgumentException(L.l("getBeans may not have a duplicate qualifier '{0}'",
                                                   qualifier));
        }
      }
    }

    Set<Bean<?>> set = resolve(type, qualifiers, null);

    if (set != null) {
      return set;
    }
    else {
      return new HashSet<>();
    }
  }

  @Override
  public Set<Bean<?>> getBeans(String name)
  {
    Set<Bean<?>> beans = getDelegate().getBeans(name);

    return beans;
  }

  /**
   * Returns the web beans component with a given binding list.
   */
  Set<Bean<?>> resolve(Type type,
                       Annotation []bindings,
                       InjectionPoint ip)
  {
    Objects.requireNonNull(type);

    if (bindings == null || bindings.length == 0) {
      if (Object.class.equals(type)) {
        return getDelegate().resolveAllBeans();
      }

      bindings = CandiManager.DEFAULT_ANN;
    }

    BaseType baseType = getDelegate().createTargetBaseType(type);

    // ioc/024n
    /*
    if (baseType.isGeneric())
      throw new IllegalArgumentException(L.l("'{0}' is an invalid getBeans type because it's generic.",
                                    baseType));
                                    */

    // ioc/02b1
    if (baseType.isVariable())
      throw new IllegalArgumentException(L.l("'{0}' is an invalid getBeans type because it's a type variable.",
                                             baseType));

    return resolveRec(baseType, bindings, ip);
  }


  @Override
  public <X> Bean<? extends X> resolve(Set<Bean<? extends X>> beans)
  {
    if (beans == null) {
      // ioc/090s
      return null;
    }
    Bean<? extends X> bestBean = null;
    Bean<? extends X> secondBean = null;

    int bestPriority = -1;
    boolean isSpecializes = false;
    
    for (Bean<? extends X> bean : beans) {
      if (getDelegate().isSpecialized(bean.getBeanClass())) {
        continue;
      }

      if ((bean instanceof IntrospectedBeanBase<?>)
          && ((IntrospectedBeanBase<?>) bean).getAnnotated().isAnnotationPresent(Specializes.class)) {
        if (! isSpecializes) {
          // ioc/07a3

          bestPriority = -1;
          bestBean = null;
          secondBean = null;
          isSpecializes = true;
        }
      }
      else if (isSpecializes) {
        continue;
      }

      int priority = getDeploymentPriority(bean);

      if (priority < 0) {
        // alternatives
      }
      else if (bestPriority < priority) {
        bestBean = bean;
        secondBean = null;

        bestPriority = priority;
      }
      else if (bestPriority == priority) {
        secondBean = bean;

        // TCK: ProducerFieldDefinitionTest
        boolean isFirstProduces = (bestBean instanceof ProducesMethodBean<?,?>
                                   || bestBean instanceof ProducesFieldBean<?,?>);
        boolean isSecondProduces = (secondBean instanceof ProducesMethodBean<?,?>
                                    || secondBean instanceof ProducesFieldBean<?,?>);

        // ioc/02b0
        if (isFirstProduces && ! isSecondProduces) {
          secondBean = null;
        }
        else if (isSecondProduces && ! isFirstProduces) {
          bestBean = bean;
          secondBean = null;
        }
      }
    }

    if (secondBean == null) {
      return bestBean;
    }
    else {
      throw getDelegate().ambiguousException(beans, bestPriority);
    }
  }

  @Override
  public Bean<?> getPassivationCapableBean(String id)
  {
    return getDelegate().getPassivationCapableBean(id);
  }

  @Override
  public <T> InjectionTargetFactory<T>
  getInjectionTargetFactory(AnnotatedType<T> type)
  {
    return getDelegate().getInjectionTargetFactory(type);
  }

  @Override
  public <X> ProducerFactory<X> getProducerFactory(AnnotatedField<? super X> field,
                                                   Bean<X> declaringBean)
  {
    return getDelegate().getProducerFactory(field, declaringBean);
  }

  @Override
  public <X> ProducerFactory<X> getProducerFactory(
    AnnotatedMethod<? super X> method,
    Bean<X> declaringBean)
  {
    return getDelegate().getProducerFactory(method, declaringBean);
  }

  @Override
  public void validate(InjectionPoint injectionPoint)
  {
    getDelegate().validate(injectionPoint);
  }

  @Override
  public InjectionPoint createInjectionPoint(AnnotatedField<?> field)
  {
    return getDelegate().createInjectionPoint(field);
  }

  @Override
  public InjectionPoint createInjectionPoint(AnnotatedParameter<?> parameter)
  {
    return getDelegate().createInjectionPoint(parameter);
  }

  @Override
  public <T> CreationalContext<T> createCreationalContext(
    Contextual<T> contextual)
  {
    return getDelegate().createCreationalContext(contextual);
  }

  @Override
  public Object getReference(Bean<?> bean,
                             Type beanType,
                             CreationalContext<?> env)
  {
    return getDelegate().getReference(bean, beanType, env);
  }

  @Override
  public Object getInjectableReference(InjectionPoint ij,
                                       CreationalContext<?> ctx)
  {
    return getDelegate().getInjectableReference(ij, ctx);
  }

  @Override
  public Context getContext(Class<? extends Annotation> scopeType)
  {
    return getDelegate().getContext(scopeType);
  }

  @Override
  public ELResolver getELResolver()
  {
    return getDelegate().getELResolver();
  }

  @Override
  public ExpressionFactory wrapExpressionFactory(ExpressionFactory expressionFactory)
  {
    return getDelegate().wrapExpressionFactory(expressionFactory);
  }

  @Override
  public <T> Set<ObserverMethod<? super T>> resolveObserverMethods(T event,
                                                                   Annotation... qualifiers)
  {
    return getDelegate().resolveObserverMethods(event, qualifiers);
  }

  @Override
  public void fireEvent(Object event,
                        Annotation... qualifiers)
  {
    getDelegate().fireEvent(event, qualifiers);
  }

  @Override
  public <T extends Extension> T getExtension(Class<T> extensionClass)
  {
    return getDelegate().getExtension(extensionClass);
  }

  public void discover(AnnotatedType<?> type)
  {
    _interceptorsBuilder.discover(type);
    _decoratorsBuilder.discover(type);
    _alternativesBuilder.discover(type);
  }
  
  private void buildDecorators()
  {
    _decoratorList = _decoratorsBuilder.build();

    CandiManager injectManager = _delegate;
    
    // ioc/0i57 - validation must be early
    for (DecoratorEntry<?> entry : _decoratorList) {
      if (entry.isEnabled()) {
        for (Type type : entry.getDelegateType().getTypeClosure(injectManager.getTypeFactory())) {
          injectManager.validate(type);
        }
      }
    }
  }
  
  private Object writeReplace()
  {
    return new Handle();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getDelegate().getId() + "]";
  }
  
  public static class Handle implements Serializable {
    
    private Object readResolve()
    {
      return CandiManager.getCurrent().getBeanManager();
    }
  }
  
  private class ProducerBase<T> implements Producer<T>
  {
    private Bean<T> _bean;
    
    ProducerBase(Bean<T> bean)
    {
      _bean = bean;
    }

    @Override
    public T produce(CreationalContext<T> env)
    {
      return (T) getReference(_bean, _bean.getBeanClass(), env);
    }

    @Override
    public void dispose(T instance)
    {
      
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints()
    {
      return _bean.getInjectionPoints();
    }
  }
  
}
