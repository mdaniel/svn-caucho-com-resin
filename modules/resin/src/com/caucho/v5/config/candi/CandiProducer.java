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

package com.caucho.v5.config.candi;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.Interceptors;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.bytecode.ScopeProxy;
import com.caucho.v5.config.gen.CandiEnhancedBean;
import com.caucho.v5.config.gen.CandiUtil;
import com.caucho.v5.config.gen.InterceptorException;
import com.caucho.v5.config.inject.InjectContext;
import com.caucho.v5.config.program.Arg;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.util.ObjectFactory;
import com.caucho.v5.config.util.ObjectFactoryBuilder;
import com.caucho.v5.inject.Module;
import com.caucho.v5.util.L10N;

/**
 * SimpleBean represents a POJO Java bean registered as a WebBean.
 */
@Module
public class CandiProducer<X> implements InjectionTarget<X>
{
  private static final L10N L = new L10N(CandiProducer.class);
  private static final Logger log
    = Logger.getLogger(CandiProducer.class.getName());
  private static final Object []NULL_ARGS = new Object[0];

  private CandiManager _injectManager;
  private BeanManagerBase _beanManager;
  private Class<X> _instanceClass;

  private Bean<X> _bean;

  private AnnotatedConstructor<X> _beanCtor;
  private Constructor<X> _javaCtor;
  private Arg []_args;

  private ConfigProgram []_injectProgram;
  private ConfigProgram []_initProgram;
  private ConfigProgram []_destroyProgram;

  private Set<InjectionPoint> _injectionPointSet;

  private Object _decoratorClass;
  private List<Decorator<?>> _decoratorBeans;
  private Annotation []_cntrInterceptorBindings;
  private ArrayList<InterceptorRuntimeBean<?>> _staticInterceptors;
  private int []_staticInterceptorIndex;

  private boolean _isInterceptor = false;
  private ObjectFactory<X> _instanceFactory;
  private MethodHandle _javaCtorHandle;

  public CandiProducer(Bean<X> bean,
                       Class<X> instanceClass,
                       AnnotatedConstructor<X> beanCtor,
                       Constructor<X> javaCtor,
                       Arg []args,
                       ConfigProgram []injectProgram,
                       ConfigProgram []initProgram,
                       ConfigProgram []destroyProgram,
                       Set<InjectionPoint> injectionPointSet,
                       boolean isInterceptor)
  {
    _injectManager = CandiManager.create();
    _beanManager = _injectManager.getBeanManager(bean);
    
    _bean = bean;
    _instanceClass = instanceClass;
    
    ObjectFactoryBuilder factory = ObjectFactoryBuilder.getInstance();

    _instanceFactory = factory.build(_instanceClass);

    try {
      _javaCtor = javaCtor;
      
      if (javaCtor != null
          && javaCtor.getParameterTypes().length == 0
          && (beanCtor == null)) {
        _javaCtor.setAccessible(true);
        _javaCtorHandle = MethodHandles.lookup().unreflectConstructor(javaCtor)
                                       .asType(MethodType.genericMethodType(0));
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    
    _beanCtor = beanCtor;
    
    _args = args;
    _injectProgram = injectProgram;
    _initProgram = initProgram;
    _destroyProgram = destroyProgram;
    _injectionPointSet = injectionPointSet;
    _isInterceptor = isInterceptor;

    Objects.requireNonNull(injectionPointSet);

    if (instanceClass != null
        && CandiEnhancedBean.class.isAssignableFrom(instanceClass)) {
      try {
        Method method = instanceClass.getMethod("__caucho_decorator_init");

        _decoratorClass = method.invoke(null);

        Annotation []qualifiers = new Annotation[bean.getQualifiers().size()];
        bean.getQualifiers().toArray(qualifiers);
        
        BeanManagerBase beanManager = _injectManager.getBeanManager(bean);

        List<Decorator<?>> decorators = beanManager.resolveDecorators(bean.getTypes(), qualifiers);
        // _injectManager.filterDecorators(decorators, bean.getBeanClass());

        _decoratorBeans = decorators;

        method = instanceClass.getMethod("__caucho_init_decorators",
                                         List.class);

        method.invoke(null, _decoratorBeans);
      } catch (InvocationTargetException e) {
        throw ConfigException.create(e.getCause());
      } catch (Exception e) {
        log.log(Level.FINEST, e.toString(), e);
      }
    }

    introspect();
  }

  /**
   * Returns the injection points.
   */
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _injectionPointSet;
  }

  public void bind()
  {
    for (ConfigProgram program : _injectProgram) {
      program.bind();
    }
  }

  private void introspect()
  {
    if (_isInterceptor)
      return;

    Interceptors interceptors = null;
    
    if (_beanCtor != null) {
      interceptors = _beanCtor.getDeclaringType().getAnnotation(Interceptors.class);
    }

    List<Class> staticInterceptors = new ArrayList<>();

    if (interceptors != null)
      Collections.addAll(staticInterceptors, interceptors.value());

    ArrayList<InterceptorRuntimeBean<?>> interceptorBeans = new ArrayList<>();

    BeanManagerBase beanManager = _beanManager;
    
    for (Class<?> interceptorClass : staticInterceptors) {
      InterceptorBean<?> interceptorBean
        = new InterceptorBean(_injectManager, interceptorClass, beanManager);
      if (interceptorBean.isAroundConstruct())
        interceptorBeans.add(interceptorBean);
    }

    _staticInterceptors = interceptorBeans;
    _staticInterceptorIndex = new int[_staticInterceptors.size()];

    for (int i = 0; i < _staticInterceptorIndex.length; i++) {
      _staticInterceptorIndex[i] = i;
    }

    Annotation []annotations = _beanCtor.getJavaMember().getAnnotations();
    List<Annotation> annotationList = new ArrayList<>();
    for (Annotation annotation : annotations) {
      if (_injectManager.isInterceptorBinding(annotation.annotationType()))
        annotationList.add(annotation);
    }

    Set<Annotation> classAnnotations
      = _beanCtor.getDeclaringType().getAnnotations();

    for (Annotation annotation : classAnnotations) {
      Class<? extends Annotation> annotationType = annotation.annotationType();

      if (! _injectManager.isInterceptorBinding(annotationType))
        continue;

      boolean isAdd = true;
      for (Annotation ann : annotationList) {
        Class<? extends Annotation> annType = ann.annotationType();
        if (annType.equals(annotationType))
          isAdd = false;

        if (isAdd)
          break;
      }

      if (isAdd)
        annotationList.add(annotation);
    }

    _cntrInterceptorBindings = new Annotation[annotationList.size()];
    annotationList.toArray(_cntrInterceptorBindings);
  }

  @Override
  public X produce(CreationalContext<X> ctx)
  {
    try {
      CreationalContextImpl<X> env = null;

      if (ctx instanceof CreationalContextImpl<?>)
        env = (CreationalContextImpl<X>) ctx;

      Object []delegates = null;

      InjectionPoint oldPoint = null;
      InjectionPoint ip = null;

      if (_decoratorBeans != null) {
        if (env != null) {
          oldPoint = env.findInjectionPoint();
          ip = oldPoint;
        }

        if (_decoratorBeans.size() > 0) {
          Decorator<?> dec = (Decorator<?>) _decoratorBeans.get(_decoratorBeans.size() - 1);

          if (dec instanceof DecoratorBean<?> && env != null) {
            ip = ((DecoratorBean<?>) dec).getDelegateInjectionPoint();
            env.setInjectionPoint(ip);
          }
        }
      }

      Object []args;

      try {
        args = evalArgs(env);
      } catch (UnsatisfiedResolutionException e) {
        throw new UnsatisfiedResolutionException(_instanceClass.getName() + ": " + e.getMessage(),
                                                 e);
      }

      X value;

      value = createWebBean(ctx, args);

      if (isCandiEnhancedBean())
        value = createEnhancedBean(value);

      destroyTransientReferences(args);

      if (env != null) {
        env.push(value);
      }

      if (_decoratorBeans != null) {
        if (env != null) {
          env.setInjectionPoint(oldPoint);
        }

        delegates = CandiUtil.generateProxyDelegate(_injectManager,
                                                    _decoratorBeans,
                                                    _decoratorClass,
                                                    env);

        if (env != null) {
          env.setInjectionPoint(ip);
        }
      }

      // server/4750
      if (value instanceof CandiEnhancedBean) {
        CandiEnhancedBean enhancedBean = (CandiEnhancedBean) value;

        Object []interceptors = null;

        enhancedBean.__caucho_inject(delegates, interceptors, env);
      }

      return value;
    } catch (final InterceptorException e) {
      Throwable cause = e.getCause();

      while (cause instanceof InterceptorException)
        cause = cause.getCause();

      while (cause instanceof InvocationTargetException)
        cause = cause.getCause();

      if (cause instanceof RuntimeException)
        throw (RuntimeException)cause;

      throw new CreationException(cause);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();

      if (cause instanceof RuntimeException)
        throw (RuntimeException) cause;

      throw new CreationException(cause);
    } catch (InstantiationException e) {
      throw new CreationException(L.l("Exception while creating {0}\n  {1}",
                                      _javaCtor != null ? _javaCtor : _instanceClass,
                                      e),
                                  e);
    } catch (Exception e) {
      throw new CreationException(e);
    } catch (ExceptionInInitializerError e) {
      throw new CreationException(e);
    }
  }

  private X createWebBean(CreationalContext ctx, Object []args)
    throws Exception
  {
    X value;

    if (isConstructorIntercepted())
      value = newInstanceIntercepted(ctx, args);
    else if (_javaCtorHandle != null) {
      try {
        Object newValue = _javaCtorHandle.invoke(); 
        return (X) newValue;
      } catch (Exception e) {
        throw e;
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }
    else if (_beanCtor != null)
      value = _beanCtor.getJavaMember().newInstance(args);
    else
      value = _instanceClass.newInstance();

    return value;
  }

  private X createEnhancedBean(Object target)
    throws InstantiationException, InvocationTargetException,
    IllegalAccessException
  {
    X x = allocateInstance();

    CandiEnhancedBean candiEnhancedBean = (CandiEnhancedBean) x;

    candiEnhancedBean.__caucho_init();
    candiEnhancedBean.__caucho_setBean(target);

    return x;
  }

  private X newInstanceIntercepted(CreationalContext ctx, Object []args)
    throws Exception
  {
    ArrayList<Interceptor<?>> interceptorList = new ArrayList<>();

    int []indexes
      = CandiUtil.createInterceptors(_beanManager,
                                     _staticInterceptors,
                                     interceptorList,
                                     _staticInterceptorIndex,
                                     InterceptionType.AROUND_CONSTRUCT,
                                     _bean.getBeanClass(),
                                     _cntrInterceptorBindings);

    Interceptor []methods =
      CandiUtil.createMethods(interceptorList,
                              InterceptionType.AROUND_CONSTRUCT,
                              indexes);

    Object []interceptors = new Object[interceptorList.size()];

    for (int i = 0; i < interceptorList.size(); i++) {
      Bean bean = interceptorList.get(i);
      CreationalContext env
        = new DependentCreationalContext(bean,
                                         (CreationalContextImpl<?>) ctx,
                                         null);

      interceptors[i] = _injectManager.getReference(bean,
                                                    bean.getBeanClass(),
                                                    env);
      if (interceptors[i] == null && (bean instanceof InterceptorSelfBean))
        interceptors[i] = _bean;
      else if (interceptors[i] == null)
        throw new NullPointerException(String.valueOf(bean));
    }

    CandiConstructorInvocationContext invocation
      = new CandiConstructorInvocationContext(_injectManager,
                                              ctx,
                                              _beanCtor,
                                              args,
                                              methods,
                                              interceptors,
                                              indexes);

    invocation.proceed();

    return (X) invocation.getTarget();
  }

  private boolean isCandiEnhancedBean() {
    if (_instanceClass == null)
      return false;

    if (CandiEnhancedBean.class.isAssignableFrom(_instanceClass))
      return true;

    return false;
  }

  private X allocateInstance()
    throws InstantiationException, InvocationTargetException,
    IllegalAccessException
  {
    X x = _instanceFactory.allocate();

    return x;
  }

  private boolean isConstructorIntercepted()
  {
    boolean isIntercepted = _cntrInterceptorBindings != null
                            && _cntrInterceptorBindings.length > 0;

    isIntercepted = isIntercepted
                    || (_staticInterceptors != null
                        && _staticInterceptors.size() > 0);

    return isIntercepted;
  }

  private Object []evalArgs(CreationalContextImpl<?> env)
  {
    Arg []args = _args;

    if (args == null)
      return NULL_ARGS;

    int size = args.length;

    if (size > 0) {
      Object []argValues = new Object[size];

      for (int i = 0; i < size; i++) {
        argValues[i] = args[i].eval(env);
      }

      return argValues;
    }
    else
      return NULL_ARGS;
  }

  private void destroyTransientReferences(Object []args) {
    for (int i = 0; i < _args.length; i++) {
      Arg arg = _args[i];
      Object value = args[i];

      arg.destroy(value, null);
    }
  }

  @Override
  public void inject(X instance, CreationalContext<X> cxt)
  {
    try {
      if (instance instanceof CandiEnhancedBean) {
        instance = (X) ((CandiEnhancedBean) instance).__caucho_getDelegate();
      }
      
      InjectContext env = null;
      
      if (cxt instanceof InjectContext) {
        env = (InjectContext) cxt;
      }

      for (ConfigProgram program : _injectProgram) {
        // log.info("INJECT: " + program);
        program.inject(instance, env);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new CreationException(e);
    }
  }

  public ConfigProgram []getPostConstructProgram()
  {
    return _initProgram;
  }

  public void setPostConstructProgram(ConfigProgram []initProgram)
  {
    _initProgram = initProgram;
  }

  @Override
  public void postConstruct(X instance)
  {
    try {
      InjectContext env = null;

      // server/4750, ioc/0c29
      if (instance instanceof CandiEnhancedBean) {
        CandiEnhancedBean bean = (CandiEnhancedBean) instance;
        bean.__caucho_postConstruct();
      }
      else {
        for (ConfigProgram program : _initProgram) {
          // log.info("POST: " + program);
          if (program != null)
            program.inject(instance, env);
        }
      }

      /*
      if (instance instanceof HandleAware) {
        SerializationAdapter.setHandle(instance, getHandle());
      }
      */
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new CreationException(e);
    }
  }

  /**
   * Call pre-destroy
   */
  @Override
  public void preDestroy(X instance)
  {
    try {
      Objects.requireNonNull(instance);
      
      CreationalContextImpl<X> env = null;

      CandiEnhancedBean enhancedBean = null;

      if (instance instanceof CandiEnhancedBean) {
        enhancedBean = (CandiEnhancedBean) instance;
      }
      else if (instance instanceof ScopeProxy) {
        // ioc/0ce2
        Object obj = ((ScopeProxy) instance).__caucho_getDelegate();
        enhancedBean = obj instanceof CandiEnhancedBean ? (CandiEnhancedBean) obj : null;
      }

      // server/4750
      if (enhancedBean != null) {
        enhancedBean.__caucho_destroy(env);
      } else {
        // ioc/055a
        for (ConfigProgram program : _destroyProgram) {
          program.inject(instance, env);
        }
      }

/*
      // server/4750
      if (instance instanceof CandiEnhancedBean) {
        CandiEnhancedBean bean = (CandiEnhancedBean) instance;
        bean.__caucho_destroy(env);
      }
      else {
        // ioc/055a
        for (ConfigProgram program : _destroyProgram) {
          program.inject(instance, env);
        }
      }
*/
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new CreationException(e);
    }
  }

  public void dispose(X instance)
  {
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _bean + "]";
  }
}
