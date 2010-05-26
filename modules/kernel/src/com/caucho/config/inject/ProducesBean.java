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
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.Specializes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.Producer;

import com.caucho.config.bytecode.ScopeAdapter;
import com.caucho.config.program.Arg;
import com.caucho.config.reflect.BaseType;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/*
 * Configuration for a @Produces method
 */
@Module
public class ProducesBean<X,T> extends AbstractIntrospectedBean<T>
  implements InjectionTarget<T>, ScopeAdapterBean<X>
{
  private static final L10N L = new L10N(ProducesBean.class);

  private static final Object []NULL_ARGS = new Object[0];

  private final Bean<X> _producerBean;
  private final AnnotatedMethod<? super X> _producesMethod;
  private final AnnotatedMethod<? super X> _disposesMethod;
  
  private LinkedHashSet<InjectionPoint> _injectionPointSet
    = new LinkedHashSet<InjectionPoint>();

  private Producer<T> _producer = this;

  private Arg<T> []_producesArgs;
  private Arg<T> []_disposesArgs;

  private boolean _isBound;

  private Object _scopeAdapter;

  protected ProducesBean(InjectManager manager,
                         Bean<X> producerBean,
                         AnnotatedMethod<? super X> producesMethod,
                         Arg<T> []producesArgs,
                         AnnotatedMethod<? super X> disposesMethod,
                         Arg<T> []disposesArgs)
  {
    super(manager, producesMethod.getBaseType(), producesMethod);

    _producerBean = producerBean;
    _producesMethod = producesMethod;
    _disposesMethod = disposesMethod;
    _producesArgs = producesArgs;
    _disposesArgs = disposesArgs;
    
    if (producesMethod != null)
      producesMethod.getJavaMember().setAccessible(true);
    
    if (disposesMethod != null)
      disposesMethod.getJavaMember().setAccessible(true);

    if (producesMethod == null)
      throw new NullPointerException();

    if (producesArgs == null)
      throw new NullPointerException();
    
    introspectInjectionPoints();
  }

  public static <X,T> ProducesBean<X,T> 
  create(InjectManager manager,
         Bean<X> producer,
         AnnotatedMethod<? super X> producesMethod,
         Arg<T> []producesArgs,
         AnnotatedMethod<? super X> disposesMethod,
         Arg<T> []disposesArgs)
  {
    ProducesBean<X,T> bean = new ProducesBean<X,T>(manager, producer, 
                                                   producesMethod, producesArgs,
                                                   disposesMethod, disposesArgs);
    bean.introspect();
    bean.introspect(producesMethod);
    
    BaseType type = manager.createSourceBaseType(producesMethod.getBaseType());
    
    if (type.isGeneric()) {
      // ioc/07f0
      throw new InjectionException(L.l("'{0}' is an invalid @Produces method because it returns a generic type {1}",
                                       producesMethod.getJavaMember(),
                                       type));
    }

    return bean;
  }

  public Producer<T> getProducer()
  {
    return _producer;
  }

  public void setProducer(Producer<T> producer)
  {
    _producer = producer;
  }

  @Override
  protected String getDefaultName()
  {
    String methodName = _producesMethod.getJavaMember().getName();

    if (methodName.startsWith("get") && methodName.length() > 3) {
      return (Character.toLowerCase(methodName.charAt(3))
              + methodName.substring(4));
    }
    else
      return methodName;
  }

  public boolean isInjectionPoint()
  {
    for (Class<?> paramType : _producesMethod.getJavaMember().getParameterTypes()) {
      if (InjectionPoint.class.equals(paramType))
        return true;
    }

    return false;
  }

  @Override
  public boolean isNullable()
  {
    return ! getBaseType().isPrimitive();
  }

  /**
   * Returns the declaring bean
   */
  public Bean<X> getParentBean()
  {
    return _producerBean;
  }
  
  @Override
  public Class<?> getBeanClass()
  {
    return _producerBean.getBeanClass();
  }
  
  public AnnotatedMethod<? super X> getProducesMethod()
  {
    return _producesMethod;
  }
  
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _injectionPointSet;
  }
  
  //
  // introspection
  //

  /**
   * Adds the stereotypes from the bean's annotations
   */
  @Override
  protected void introspectSpecializes(Annotated annotated)
  {
    if (! annotated.isAnnotationPresent(Specializes.class))
      return;
  }
  
  private void introspectInjectionPoints()
  {
    for (AnnotatedParameter<?> param : _producesMethod.getParameters()) {
      InjectionPointImpl ip = new InjectionPointImpl(getBeanManager(), this, param);

      _injectionPointSet.add(ip);
    }
  }
  
  //
  // Producer
  //


  @Override
  public T create(CreationalContext<T> createEnv)
  {
    T value = _producer.produce(createEnv);
    
    createEnv.push(value);
    
    return value;
  }
  
  @Override
  public void dispose(T value)
  {
  }

  @Override
  public InjectionTarget<T> getInjectionTarget()
  {
    return this;
  }

  /**
   * Produces a new bean instance
   */
  @Override
  public T produce(CreationalContext<T> cxt)
  {
    Class<?> type = _producerBean.getBeanClass();
    
    CreationalContextImpl<X> parentEnv
      = new CreationalContextImpl<X>(_producerBean, cxt);

    X factory = (X) getBeanManager().getReference(_producerBean, type, parentEnv);
    
    if (factory == null) {
      throw new IllegalStateException(L.l("{0}: unexpected null factory for {1}",
                                          this, _producerBean));
    }
    
    return produce(factory, cxt);
  }

  /**
   * Produces a new bean instance
   */
  private T produce(X bean, CreationalContext<T> cxt)
  
  {
    InjectionPoint ij = null;
    
    if (cxt instanceof CreationalContextImpl<?>) {
      CreationalContextImpl<T> env = (CreationalContextImpl<T>) cxt;

      ij = env.getInjectionPoint();
    }
    
    try {
      // InjectManager inject = getBeanManager();

      Object []args;

      if (_producesArgs.length > 0) {
        args = new Object[_producesArgs.length];

        for (int i = 0; i < args.length; i++) {
          if (_producesArgs[i] instanceof InjectionPointArg<?>)
            args[i] = ij;
          else
            args[i] = _producesArgs[i].eval(cxt);
        }
      }
      else
        args = NULL_ARGS;

      // ioc/0084
      _producesMethod.getJavaMember().setAccessible(true);
      
      if (cxt instanceof CreationalContextImpl<?>) {
        CreationalContextImpl<X> env = (CreationalContextImpl<X>) cxt;
        // ioc/07b0
        env.postConstruct();
      }
      
      
      T value = (T) _producesMethod.getJavaMember().invoke(bean, args);
      
      cxt.push(value);
      
      if (value != null)
        return value;
      
      if (Dependent.class.equals(getScope()))
        return null;
      
      throw new IllegalProductException(L.l("producer {0} returned null, which is not allowed by the CDI spec.",
                                            this));
    } catch (RuntimeException e) {
      throw e;
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof RuntimeException)
        throw (RuntimeException) e.getCause();
      else
        throw new CreationException(e.getCause());
    } catch (Exception e) {
      throw new CreationException(e);
    }
  }

  @Override
  public X getScopeAdapter(Bean<?> topBean, CreationalContext<X> cxt)
  {
    NormalScope scopeType = getScope().getAnnotation(NormalScope.class);

    // ioc/0520
    if (scopeType != null) {
      //  && ! getScope().equals(ApplicationScoped.class)) {
      // && scopeType.normal()
      //  && ! env.canInject(getScope())) {

      Object value = _scopeAdapter;

      if (value == null) {
        ScopeAdapter scopeAdapter = ScopeAdapter.create(getBaseType().getRawClass());
        _scopeAdapter = scopeAdapter.wrap(getBeanManager().createNormalInstanceFactory(topBean));
        value = _scopeAdapter;
      }

      return (X) value;
    }

    return null;
  } 
  
  @Override
  public void inject(T instance, CreationalContext<T> cxt)
  {
  }

  @Override
  public void postConstruct(T instance)
  {
  }

  @Override
  public void bind()
  {
    synchronized (this) {
      if (_isBound)
        return;

      _isBound = true;
    }
  }

  public Bean<T> bindInjectionPoint(InjectionPoint ij)
  {
    return new ProducesInjectionPointBean<T>(this, ij);
  }

  /**
   * Call destroy
   */
  @Override
  public void destroy(T instance, CreationalContext<T> cxt)
  {
    if (_disposesMethod != null) {
      try {
        CreationalContextImpl<T> env = (CreationalContextImpl<T>) cxt;
        
        Object producer = null;
        
        if (env != null)
          producer = env.getAny(_producerBean);
        else
          Thread.dumpStack();
        
        if (producer == null) {
          CreationalContext<X> parentEnv
            = getBeanManager().createCreationalContext(_producerBean, env);

          producer = getBeanManager().getReference(_producerBean, 
                                                   _producerBean.getBeanClass(), 
                                                   parentEnv);
        }
        
        Object []args = new Object[_disposesArgs.length];
        for (int i = 0; i < args.length; i++) {
          if (_disposesArgs[i] == null)
            args[i] = instance;
          else {
            args[i] = _disposesArgs[i].eval(env);
          }
        }
        
        if (env instanceof CreationalContextImpl<?>) {
          ((CreationalContextImpl<?>) env).postConstruct();
        }
        
        _disposesMethod.getJavaMember().invoke(producer, args);
      } catch (Exception e) {
        throw new RuntimeException(_disposesMethod.getJavaMember() + ":" + e, e);
      }
    }
    
    cxt.release();
  }

  /**
   * Disposes a bean instance
   */
  @Override
  public void preDestroy(T instance)
  {
  }

  /**
   * Returns the owning producer
   */
  /*
  public AnnotatedMember<X> getProducerMember()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }*/

  /**
   * Returns the owning disposer
   */
  public AnnotatedMethod<X> getAnnotatedDisposer()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public AnnotatedParameter<X> getDisposedParameter()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    Method method = _producesMethod.getJavaMember();

    sb.append(getTargetSimpleName());
    sb.append(", ");
    sb.append(method.getDeclaringClass().getSimpleName());
    sb.append(".");
    sb.append(method.getName());
    sb.append("()");

    sb.append(", {");

    boolean isFirst = true;
    for (Annotation ann : getQualifiers()) {
      if (! isFirst)
        sb.append(", ");

      sb.append(ann);

      isFirst = false;
    }

    sb.append("}");

    if (getName() != null) {
      sb.append(", name=");
      sb.append(getName());
    }

    sb.append("]");

    return sb.toString();
  }

}
