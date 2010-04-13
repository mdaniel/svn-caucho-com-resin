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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.Producer;

import com.caucho.config.bytecode.ScopeAdapter;
import com.caucho.config.program.Arg;
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
  private final AnnotatedMethod<X> _producesMethod;
  private final AnnotatedMethod<X> _disposesMethod;

  private Producer<T> _producer;

  // XXX: needs to be InjectionPoint
  private Arg []_args;

  private boolean _isBound;

  private Object _scopeAdapter;

  protected ProducesBean(InjectManager manager,
                         Bean<X> producerBean,
                         AnnotatedMethod<X> producesMethod,
                         Arg<?> []args,
                         AnnotatedMethod<X> disposesMethod)
  {
    super(manager, producesMethod.getBaseType(), producesMethod);

    _producerBean = producerBean;
    _producesMethod = producesMethod;
    _disposesMethod = disposesMethod;
    
    if (producesMethod != null)
      producesMethod.getJavaMember().setAccessible(true);
    
    if (disposesMethod != null)
      disposesMethod.getJavaMember().setAccessible(true);

    _args = args;

    if (producesMethod == null)
      throw new NullPointerException();

    if (args == null)
      throw new NullPointerException();
  }

  public static <X,T> ProducesBean<X,T> 
  create(InjectManager manager,
         Bean<X> producer,
         AnnotatedMethod<X> producesMethod,
         Arg<?> []args,
         AnnotatedMethod<X> disposesMethod)
  {
    ProducesBean<X,T> bean = new ProducesBean<X,T>(manager, producer, 
                                                   producesMethod, args,
                                                   disposesMethod);
    bean.introspect();
    bean.introspect(producesMethod);

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

  /*
  protected AnnotatedMethod<T> getMethod()
  {
    return _producesMethod;
  }
  */

  /*
  protected Annotation []getAnnotationList()
  {
    return _annotationList;
  }

  protected void initDefault()
  {
    if (getDeploymentType() == null
        && _producer.getDeploymentType() != null) {
      setDeploymentType(_producer.getDeploymentType());
    }

    super.initDefault();
  }
  */

  /*
  protected Class getDefaultDeploymentType()
  {
    if (_producerBean.getDeploymentType() != null)
      return _producerBean.getDeploymentType();

    return null;// super.getDefaultDeploymentType();
  }
  */

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

  /**
   * Returns the declaring bean
   */
  public Bean<X> getParentBean()
  {
    return _producerBean;
  }

  public T create(CreationalContext<T> createEnv)
  {
    return produce(createEnv);
  }

  public InjectionTarget<T> getInjectionTarget()
  {
    return this;
  }

  /**
   * Produces a new bean instance
   */
  public T produce(CreationalContext<T> cxt)
  {
    Class<?> type = _producerBean.getBeanClass();

    X factory = (X) getBeanManager().getReference(_producerBean, type, cxt);

    if (factory == null) {
      throw new IllegalStateException(L.l("{0}: unexpected null factory for {1}",
                                          this, _producerBean));
    }
    
    CreationalContextImpl<T> env = (CreationalContextImpl<T>) cxt;

    return produce(factory, env.getInjectionPoint());
  }

  /**
   * Produces a new bean instance
   */
  private T produce(X bean, InjectionPoint ij)
  {
    try {
      Object []args;

      if (_args.length > 0) {
        args = new Object[_args.length];

        InjectManager inject = getBeanManager();

        CreationalContext<?> env = inject.createCreationalContext(_producerBean);

        for (int i = 0; i < args.length; i++) {
          if (_args[i] instanceof InjectionPointArg<?>)
            args[i] = ij;
          else
            args[i] = _args[i].eval(env);
        }
      }
      else
        args = NULL_ARGS;

      // ioc/0084
      _producesMethod.getJavaMember().setAccessible(true);
      
      T value = (T) _producesMethod.getJavaMember().invoke(bean, args);
      
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

  /*
  @Override
  public X instantiate()
  {
    return createNew(null, null);
  }
  */

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
  
  public void inject(T instance, CreationalContext<T> cxt)
  {
  }

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

      Method method = _producesMethod.getJavaMember();

      String loc = InjectManager.location(method);

      // Annotation [][]paramAnn = _method.getParameterAnnotations();
      // List<AnnotatedParameter<T>> beanParams = _producesMethod.getParameters();

      /*
      _args = new Arg[param.length];

      for (int i = 0; i < param.length; i++) {
        _args[i] = bindParameter(loc, param[i], beanParams.get(i).getAnnotations());

        if (_args[i] != null) {
        }
        else if (InjectionPoint.class.equals(param[i])) {
          _args[i] = createInjectionPointBean(getManager());
        }
        else {
          throw error(_beanMethod.getJavaMember(),
                      L.l("Type '{0}' for method parameter #{1} has no matching component.",
                          getSimpleName(param[i]), i));
        }
      }
      */
    }
  }

  public Bean bindInjectionPoint(InjectionPoint ij)
  {
    return new ProducesInjectionPointBean(this, ij);
  }

  /**
   * Call destroy
   */
  @Override
  public void destroy(T instance, CreationalContext<T> cxt)
  {
    if (_disposesMethod != null) {
      try {
        CreationalContextImpl env = (CreationalContextImpl) cxt;
        
        Object producer = null;
        
        if (env != null)
          producer = env.get(_producerBean);
        else
          Thread.dumpStack();
        
        if (producer == null)
          producer = getBeanManager().getReference(_producerBean, _producerBean.getBeanClass(), cxt);
       
        _disposesMethod.getJavaMember().invoke(producer, instance);
      } catch (Exception e) {
        throw new RuntimeException(_disposesMethod.getJavaMember() + ":" + e, e);
      }
    }
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
  public AnnotatedMember<X> getProducerMember()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

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
