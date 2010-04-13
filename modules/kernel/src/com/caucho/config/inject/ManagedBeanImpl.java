/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.Timer;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;

import com.caucho.config.Names;
import com.caucho.config.bytecode.ScopeAdapter;
import com.caucho.config.program.Arg;
import com.caucho.config.program.BeanArg;
import com.caucho.config.timer.ScheduleIntrospector;
import com.caucho.config.timer.TimeoutCaller;
import com.caucho.config.timer.TimerTask;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * SimpleBean represents a POJO Java bean registered as a WebBean.
 */
@Module
public class ManagedBeanImpl<X> extends InjectionTargetImpl<X>
  implements ScopeAdapterBean<X>
{
  private static final L10N L = new L10N(ManagedBeanImpl.class);
  
  private AnnotatedType<X> _annotatedType;

  private InjectionTarget<X> _injectionTarget;

  private HashSet<Bean<?>> _producerBeans
    = new LinkedHashSet<Bean<?>>();

  private HashSet<ObserverMethodImpl<X,?>> _observerMethods
    = new LinkedHashSet<ObserverMethodImpl<X,?>>();

  private Object _scopeAdapter;

  public ManagedBeanImpl(InjectManager webBeans,
                         AnnotatedType<X> beanType)
  {
    super(webBeans, beanType);

    _annotatedType = beanType;

    /*
    if (beanType.getType() instanceof Class)
      validateType((Class) beanType.getType());
    */

    _injectionTarget = this;
  }

  public ManagedBeanImpl(InjectManager webBeans,
                         AnnotatedType<X> beanType,
                         InjectionTarget<X> injectionTarget)
  {
    this(webBeans, beanType);

    _injectionTarget = injectionTarget;
  }

  @Override
  public AnnotatedType<X> getAnnotatedType()
  {
    return _annotatedType;
  }

  @Override
  public InjectionTarget<X> getInjectionTarget()
  {
    return _injectionTarget;
  }

  public void setInjectionTarget(InjectionTarget<X> target)
  {
    _injectionTarget = target;
  }

  /**
   * Creates a new instance of the component.
   */
  @Override
  public X create(CreationalContext<X> context)
  {
    X instance = _injectionTarget.produce(context);

    if (context != null)
      context.push(instance);

    _injectionTarget.inject(instance, context);
    _injectionTarget.postConstruct(instance);

    return instance;
  }

  @Override
  public X getScopeAdapter(Bean<?> topBean, CreationalContext<X> cxt)
  {
    NormalScope scopeType = getScope().getAnnotation(NormalScope.class);

    // ioc/0520
    if (scopeType != null
        && ! getScope().equals(ApplicationScoped.class)) {
      // && scopeType.normal()
      //  && ! env.canInject(getScope())) {

      Object value = _scopeAdapter;

      if (value == null) {
        ScopeAdapter scopeAdapter = ScopeAdapter.create(getBaseType().getRawClass());
        _scopeAdapter = scopeAdapter.wrap(getBeanManager(), topBean);
        value = _scopeAdapter;
      }

      return (X) value;
    }

    return null;
  }
  
  protected boolean isProxiedScope()
  {
    NormalScope scopeType = getScope().getAnnotation(NormalScope.class);
    
    if (scopeType != null
        && ! getScope().equals(ApplicationScoped.class)) {
      return true;
    }
    else
      return false;
  }

  public Object getScopeAdapter()
  {
    Object value = _scopeAdapter;

    if (value == null) {
      ScopeAdapter scopeAdapter = ScopeAdapter.create(getTargetClass());
      _scopeAdapter = scopeAdapter.wrap(getBeanManager(), this);
      value = _scopeAdapter;
    }

    return value;
  }

  public Set<Bean<?>> getProducerBeans()
  {
    return _producerBeans;
  }

  /**
   * Returns the observer methods
   */
  public Set<ObserverMethodImpl<X,?>> getObserverMethods()
  {
    return _observerMethods;
  }

  /**
   * Call post-construct
   */
  public void dispose(X instance)
  {

  }

  /**
   * Call pre-destroy
   */
  public void destroy(X instance, CreationalContext<X> env)
  {
    getInjectionTarget().preDestroy(instance);
  }

  //
  // introspection
  //

  @Override
  public void introspect()
  {
    super.introspect();

    // ioc/0e13
    if (_injectionTarget instanceof PassivationSetter)
      ((PassivationSetter) _injectionTarget).setPassivationId(getId());
  }

  /**
   * Called for implicit introspection.
   */
  @Override
  public void introspect(AnnotatedType<X> beanType)
  {
    super.introspect(beanType);

    introspectProduces(beanType);

    introspectObservers(beanType);
  }

  /**
   * Introspects the methods for any @Produces
   */
  protected void introspectProduces(AnnotatedType<X> beanType)
  {
    for (AnnotatedMethod<? super X> beanMethod : beanType.getMethods()) {
      if (beanMethod.isAnnotationPresent(Produces.class)) {
        AnnotatedMethod<? super X> disposesMethod 
          = findDisposesMethod(beanType, beanMethod);
        
        addProduces(beanMethod, disposesMethod);
      }
    }
    
    for (AnnotatedField<?> beanField : beanType.getFields()) {
      if (beanField.isAnnotationPresent(Produces.class))
        addProduces(beanField);
    }
  }

  protected void addProduces(AnnotatedMethod producesMethod,
                             AnnotatedMethod disposesMethod)
  {
    Arg<?> []args = introspectArguments(producesMethod.getParameters());

    ProducesBean<?,?> bean = ProducesBean.create(getBeanManager(), this, 
                                                 producesMethod, args,
                                                 disposesMethod);

    // bean.init();

    _producerBeans.add(bean);
  }
  
  private AnnotatedMethod<? super X>
  findDisposesMethod(AnnotatedType<X> beanType,
                     AnnotatedMethod<? super X> producesMethod)
  {
    for (AnnotatedMethod beanMethod : beanType.getMethods()) {
      List<AnnotatedParameter<?>> params = beanMethod.getParameters();
      
      if (params.size() != 1)
        continue;
      
      AnnotatedParameter<?> param = params.get(0);
      
      if (! param.isAnnotationPresent(Disposes.class))
        continue;
      
      if (! producesMethod.getBaseType().equals(param.getBaseType()))
        continue;


      // XXX: check @Qualifiers
      
      return beanMethod;
    }
    
    return null;
  }

  protected void addProduces(AnnotatedField<?> beanField)
  {
    ProducesFieldBean bean
      = ProducesFieldBean.create(getBeanManager(), this, beanField);

    // bean.init();

    _producerBeans.add(bean);
  }

  protected Arg []introspectArguments(List<AnnotatedParameter<X>> params)
  {
    Arg []args = new Arg[params.size()];

    for (int i = 0; i < args.length; i++) {
      AnnotatedParameter<X> param = params.get(i);

      if (InjectionPoint.class.equals(param.getBaseType()))
        args[i] = new InjectionPointArg();
      else
        args[i] = new BeanArg(param.getBaseType(), getQualifiers(param));
    }

    return args;
  }

  private Annotation []getQualifiers(Annotated annotated)
  {
    ArrayList<Annotation> qualifierList = new ArrayList<Annotation>();

    for (Annotation ann : annotated.getAnnotations()) {
      if (ann.annotationType().equals(Named.class)) {
        String namedValue = getNamedValue(ann);

        if ("".equals(namedValue)) {
          String name = getBeanClass().getSimpleName();

          ann = Names.create(name);
        }

        qualifierList.add(ann);

      }
      else if (ann.annotationType().isAnnotationPresent(Qualifier.class)) {
        qualifierList.add(ann);
      }
    }

    if (qualifierList.size() == 0)
      qualifierList.add(CurrentLiteral.CURRENT);

    Annotation []qualifiers = new Annotation[qualifierList.size()];
    qualifierList.toArray(qualifiers);

    return qualifiers;
  }

  /**
   * Introspects the methods for any @Produces
   */
  protected void introspectObservers(AnnotatedType<?> beanType)
  {
    for (AnnotatedMethod<?> beanMethod : beanType.getMethods()) {
      for (AnnotatedParameter param : beanMethod.getParameters()) {
        if (param.isAnnotationPresent(Observes.class)) {
          addObserver(beanMethod);
          break;
        }
      }
    }
  }

  protected void addObserver(AnnotatedMethod beanMethod)
  {
    int param = findObserverAnnotation(beanMethod);

    if (param < 0)
      return;

    Method method = beanMethod.getJavaMember();
    Type eventType = method.getGenericParameterTypes()[param];

    HashSet<Annotation> bindingSet = new HashSet<Annotation>();

    Annotation [][]annList = method.getParameterAnnotations();
    if (annList != null && annList[param] != null) {
      for (Annotation ann : annList[param]) {
        if (ann.annotationType().isAnnotationPresent(Qualifier.class))
          bindingSet.add(ann);
      }
    }

    if (method.isAnnotationPresent(Inject.class)) {
      throw InjectManager.error(method, L.l("A method may not have both an @Observer and an @Inject annotation."));
    }

    if (method.isAnnotationPresent(Produces.class)) {
      throw InjectManager.error(method, L.l("A method may not have both an @Observer and a @Produces annotation."));
    }

    if (method.isAnnotationPresent(Disposes.class)) {
      throw InjectManager.error(method, L.l("A method may not have both an @Observer and a @Disposes annotation."));
    }

    ObserverMethodImpl observerMethod
      = new ObserverMethodImpl(getBeanManager(), this, beanMethod,
                               eventType, bindingSet);

    _observerMethods.add(observerMethod);
  }

  private <X> int findObserverAnnotation(AnnotatedMethod<X> method)
  {
    List<AnnotatedParameter<X>> params = method.getParameters();
    int size = params.size();
    int observer = -1;

    for (int i = 0; i < size; i++) {
      AnnotatedParameter param = params.get(i);

      for (Annotation ann : param.getAnnotations()) {
        if (ann instanceof Observes) {
          if (observer >= 0)
            throw InjectManager.error(method.getJavaMember(), L.l("Only one param may have an @Observer"));

          observer = i;
        }
      }
    }

    return observer;
  }

  /**
   * 
   */
  public void scheduleTimers(Object value)
  {
    ScheduleIntrospector introspector = new ScheduleIntrospector();
    
    TimeoutCaller timeoutCaller = new BeanTimeoutCaller(value);
    
    ArrayList<TimerTask> taskList
      = introspector.introspect(timeoutCaller, _annotatedType);
    
    if (taskList != null) {
      for (TimerTask task : taskList) {
        task.start();
      }
    }
  }
  
  static class BeanTimeoutCaller implements TimeoutCaller 
  {
    private Object _bean;
    
    BeanTimeoutCaller(Object bean)
    {
      _bean = bean;
    }

    @Override
    public void timeout(Method method)
    throws InvocationTargetException,
           IllegalAccessException
    {
      method.invoke(_bean);
    }

    @Override
    public void timeout(Method method, Timer timer)
        throws InvocationTargetException, IllegalAccessException
    {
      
    }
    
  }
}
