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

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.program.Arg;
import com.caucho.config.program.BeanArg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.gen.*;
import com.caucho.config.type.TypeFactory;
import com.caucho.config.type.ConfigType;
import com.caucho.util.*;
import com.caucho.config.bytecode.*;
import com.caucho.config.cfg.*;
import com.caucho.config.event.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.logging.*;

import javax.annotation.*;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.ScopeType;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.*;
import javax.enterprise.inject.*;
import javax.enterprise.inject.spi.*;
import javax.interceptor.InterceptorBindingType;

/**
 * SimpleBean represents a POJO Java bean registered as a WebBean.
 */
public class ManagedBeanImpl<X> extends InjectionTargetImpl<X>
  implements ScopeAdapterBean
{
  private static final L10N L = new L10N(ManagedBeanImpl.class);
  private static final Logger log
    = Logger.getLogger(ManagedBeanImpl.class.getName());

  private static final Object []NULL_ARGS = new Object[0];

  private AnnotatedType _annotatedType;

  private InjectionTarget<X> _injectionTarget;

  private Set<InjectionPoint> _injectionPointSet
    = new LinkedHashSet<InjectionPoint>();

  private HashSet<ProducesBean<X,?>> _producerBeans
    = new LinkedHashSet<ProducesBean<X,?>>();

  private HashSet<ObserverMethodImpl<X,?>> _observerMethods
    = new LinkedHashSet<ObserverMethodImpl<X,?>>();

  private Class _instanceClass;
  private boolean _isBound;
  private Object _scopeAdapter;

  public ManagedBeanImpl(InjectManager webBeans,
			 AnnotatedType beanType)
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
			 AnnotatedType beanType,
			 InjectionTarget injectionTarget)
  {
    this(webBeans, beanType);
    
    _injectionTarget = injectionTarget;
  }

  public AnnotatedType<X> getAnnotatedType()
  {
    return _annotatedType;
  }

  public InjectionTarget getInjectionTarget()
  {
    return _injectionTarget;
  }

  public void setInjectionTarget(InjectionTarget target)
  {
    _injectionTarget = target;
  }

  private void validateType(Class type)
  {
    if (type.isInterface())
      throw new ConfigException(L.l("'{0}' is an invalid ManagedBean because it is an interface",
				    type));
  }

  private boolean isAnnotationPresent(Annotation []annotations, Class type)
  {
    for (Annotation ann : annotations) {
      if (ann.annotationType().equals(type))
	return true;
    }

    return false;
  }

  private boolean isAnnotationDeclares(Annotation []annotations, Class type)
  {
    for (Annotation ann : annotations) {
      if (ann.annotationType().isAnnotationPresent(type))
	return true;
    }

    return false;
  }

  //
  // Create
  //

  /**
   * Creates a new instance of the component.
   */
  public X create(CreationalContext<X> context)
  {
    X instance = _injectionTarget.produce(context);

    context.push(instance);

    _injectionTarget.inject(instance, context);
    _injectionTarget.postConstruct(instance);

    return instance;
  }
  
  /**
   * Creates a new instance of the component.
   */
  /*
  public T create(CreationalContext<T> context,
		  InjectionPoint ij)
  {
    T object = createNew(context, ij);

    init(object, context);
    
    return object;
  }
  */
  
  public Object getScopeAdapter(CreationalContext cxt)
  {
    if (! (cxt instanceof ConfigContext))
      return null;

    ConfigContext env = (ConfigContext) cxt;

    ScopeType scopeType = getScopeType().getAnnotation(ScopeType.class);
    
    // ioc/0520
    if (scopeType != null
	&& scopeType.normal()
	&& ! env.canInject(getScopeType())) {
      Object value = _scopeAdapter;

      if (value == null) {
	ScopeAdapter scopeAdapter = ScopeAdapter.create(getBaseType().getRawClass());
	_scopeAdapter = scopeAdapter.wrap(getBeanManager(), this);
	value = _scopeAdapter;
      }

      return value;
    }

    return null;
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
  
  public Set<ProducesBean<X,?>> getProducerBeans()
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

  /**
   * Returns the injection points.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // introspection
  //

  /**
   * Called for implicit introspection.
   */
  public void introspect(AnnotatedType beanType)
  {
    super.introspect(beanType);

    introspectProduces(beanType);

    introspectObservers(beanType);
  }

  /**
   * Introspects the methods for any @Produces
   */
  protected void introspectProduces(AnnotatedType<?> beanType)
  {
    for (AnnotatedMethod beanMethod : beanType.getMethods()) {
      if (beanMethod.isAnnotationPresent(Produces.class))
	addProduces(beanMethod);
    }
  }

  protected void addProduces(AnnotatedMethod beanMethod)
  {
    Arg []args = introspectArguments(beanMethod.getParameters());
    
    ProducesBean bean = ProducesBean.create(getBeanManager(), this, beanMethod,
					    args);

    // bean.init();

    _producerBeans.add(bean);
  }

  protected Arg []introspectArguments(List<AnnotatedParameter> params)
  {
    Arg []args = new Arg[params.size()];

    for (int i = 0; i < args.length; i++) {
      AnnotatedParameter param = params.get(i);

      if (InjectionPoint.class.equals(param.getBaseType()))
	args[i] = new InjectionPointArg();
      else
	args[i] = new BeanArg(param.getBaseType(), getBindings(param));
    }

    return args;
  }

  private Annotation []getBindings(Annotated annotated)
  {
    ArrayList<Annotation> bindingList = new ArrayList<Annotation>();

    for (Annotation ann : annotated.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class)) {
	bindingList.add(ann);
      }
    }

    if (bindingList.size() == 0)
      bindingList.add(CurrentLiteral.CURRENT);

    Annotation []bindings = new Annotation[bindingList.size()];
    bindingList.toArray(bindings);

    return bindings;
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
	if (ann.annotationType().equals(IfExists.class))
	  continue;
	  
	if (ann.annotationType().isAnnotationPresent(BindingType.class))
	  bindingSet.add(ann);
      }
    }

    if (method.isAnnotationPresent(Initializer.class)) {
      throw InjectManager.error(method, L.l("A method may not have both an @Observer and an @Initializer annotation."));
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
}
