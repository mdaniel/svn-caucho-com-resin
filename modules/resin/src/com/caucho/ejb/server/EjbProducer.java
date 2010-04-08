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

package com.caucho.ejb.server;

import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.ejb.TimedObject;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import com.caucho.config.gen.BeanProducer;
import com.caucho.config.inject.AbstractBean;
import com.caucho.config.inject.BeanFactory;
import com.caucho.config.inject.CreationalContextImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.InjectionTargetImpl;
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.ejb.cfg.PostConstructConfig;
import com.caucho.ejb.cfg.PreDestroyConfig;
import com.caucho.ejb.timer.EjbTimerService;

/**
 * Creates an configures an ejb instance
 */
public class EjbProducer<T> {
  private AbstractServer<T> _server;
  
  private Class<T> _ejbClass;
  private AnnotatedType<T> _annotatedType;
  
  private Bean<T> _bean;
  
  private ClassLoader _envLoader;
  
  private BeanProducer<T> _beanProducer;
  
  private InjectionTarget<T> _injectionTarget;

  private ConfigProgram _initProgram;
  private ConfigProgram[] _initInject;
  
  private ConfigProgram[] _destroyInject;
  
  private PreDestroyConfig _preDestroyConfig;
  private PostConstructConfig _postConstructConfig;
  private Method _cauchoPostConstruct;

  private Method _timeoutMethod;
  private TimerService _timerService;
  
  EjbProducer(AbstractServer<T> server,
              AnnotatedType<T> annotatedType)
  {
    _server = server;
    _ejbClass = annotatedType.getJavaClass();
    _annotatedType = annotatedType;
    
    try {
      _cauchoPostConstruct = _ejbClass.getDeclaredMethod("__caucho_postConstruct");
      _cauchoPostConstruct.setAccessible(true);
    } catch (NoSuchMethodException e) {
    }
  }
  
  /**
   * Sets the classloader for the EJB's private environment
   * 
   * @param loader the environment classloader
   */
  public void setEnvLoader(ClassLoader envLoader)
  {
    _envLoader = envLoader;
  }
  

  /**
   * Sets the injection target
   */
  public void setInjectionTarget(InjectionTarget<T> injectionTarget)
  {
    _injectionTarget = injectionTarget;
    
    if (injectionTarget instanceof InjectionTargetImpl<?>) {
      InjectionTargetImpl<T> targetImpl = (InjectionTargetImpl<T>) injectionTarget;
      
      targetImpl.setGenerateInterception(false);
    }
  }

  /**
   * Gets the injection target
   */
  public InjectionTarget<T> getInjectionTarget()
  {
    return _injectionTarget;
  }

  /**
   * Sets the init program.
   */
  public void setInitProgram(ConfigProgram init)
  {
    _initProgram = init;
  }

  /**
   * Gets the init program.
   */
  public ConfigProgram getInitProgram()
  {
    return _initProgram;
  }

  public PostConstructConfig getPostConstruct()
  {
    return _postConstructConfig;
  }

  public PreDestroyConfig getPreDestroy()
  {
    return _preDestroyConfig;
  }

  public void setPostConstruct(PostConstructConfig postConstruct)
  {
    _postConstructConfig = postConstruct;
  }

  public void setPreDestroy(PreDestroyConfig preDestroy)
  {
    _preDestroyConfig = preDestroy;
  }
  
  public void setBeanProducer(BeanProducer<T> producer)
  {
    _beanProducer = producer;
  }
  
  public BeanProducer<T> getBeanProducer()
  {
    return _beanProducer;
  }


  public void bindInjection()
  {
    InjectManager beanManager = InjectManager.create();
    ManagedBeanImpl<T> managedBean
      = beanManager.createManagedBean(_annotatedType);
    
    _bean = managedBean;
    setInjectionTarget(managedBean.getInjectionTarget());

    _timeoutMethod = getTimeoutMethod(_bean.getBeanClass());

    if (_timeoutMethod != null)
      _timerService = new EjbTimerService(_server);

    // Injection binding occurs in the start phase

    InjectManager inject = InjectManager.create();

    // server/4751
    if (_injectionTarget == null)
      _injectionTarget = inject.createInjectionTarget(_ejbClass);

    if (_timerService != null) {
      BeanFactory<TimerService> factory = inject.createBeanFactory(TimerService.class);
      inject.addBean(factory.singleton(_timerService));
    }
    /*
    ArrayList<ConfigProgram> injectList = new ArrayList<ConfigProgram>();
    InjectIntrospector.introspectInject(injectList, getEjbClass());
    // XXX: add inject from xml here
    */

    ArrayList<ConfigProgram> injectList = null;
    if (_initProgram != null) {
      injectList = new ArrayList<ConfigProgram>();
      injectList.add(_initProgram);
    }

    // InjectIntrospector.introspectInit(injectList, getEjbClass(), null);
    // XXX: add init from xml here

    if (injectList != null && injectList.size() > 0) {
      ConfigProgram[] injectArray = new ConfigProgram[injectList.size()];
      injectList.toArray(injectArray);

      if (injectArray.length > 0)
        _initInject = injectArray;
    }

    injectList = new ArrayList<ConfigProgram>();

    // XXX:
    // InjectIntrospector.introspectDestroy(injectList, _ejbClass);

    ConfigProgram[] injectArray;
    injectArray = new ConfigProgram[injectList.size()];
    injectList.toArray(injectArray);

    if (injectArray.length > 0)
      _destroyInject = injectArray;
  }

  private Method getTimeoutMethod(Class<?> targetBean)
  {
    if (TimedObject.class.isAssignableFrom(targetBean)) {
      try {
        return targetBean.getMethod("ejbTimeout", Timer.class);
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }

    for (Method method : targetBean.getMethods()) {
      if (method.getAnnotation(Timeout.class) != null) {
        return method;
      }
    }

    return null;
  }

  public T newInstance()
  {
    T instance = _beanProducer.__caucho_new();
    initInstance(instance);
    _beanProducer.__caucho_postConstruct(instance);
    
    return instance;
  }
  
  /**
   * Initialize an instance
   */
  public void initInstance(T instance)
  {
    initInstance(instance, null, null, CreationalContextImpl.create());
  }

  /**
   * Initialize an instance
   */
  public <X> void initInstance(T instance,
                               InjectionTarget<T> target,
                               X proxy,
                               CreationalContext<X> env)
  {
    Bean<T> bean = _bean;

    if (env != null && bean != null) {
      // server/4762
      // env.put((AbstractBean) bean, proxy);
      env.push(proxy);
    }

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    CreationalContextImpl<T> cxt = new CreationalContextImpl<T>(bean, env);

    try {
      thread.setContextClassLoader(_envLoader);

      if (target != null) {
        target.inject(instance, cxt);
      }

      if (getInjectionTarget() != null && target != getInjectionTarget()) {
        getInjectionTarget().inject(instance, cxt);
      }

      if (_initInject != null) {
        for (ConfigProgram inject : _initInject)
          inject.inject(instance, cxt);
      }

      if (_initProgram != null) {
        _initProgram.inject(instance, cxt);
      }
      
      if (getInjectionTarget() != null) {
        getInjectionTarget().postConstruct(instance);
      }

      /*
      try {
        if (_cauchoPostConstruct != null)
          _cauchoPostConstruct.invoke(instance);
      } catch (RuntimeException e) {
        throw e;
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e.getCause());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      */
      
      if (_beanProducer != null)
        _beanProducer.__caucho_postConstruct(instance);
    } finally {
      thread.setContextClassLoader(oldLoader);
    }

    /*
    if (cxt != null && bean != null)
      cxt.remove(bean);
      */
  }
  
  /**
   * Remove an object.
   */
  public void destroyInstance(T instance)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(_envLoader);

      /*
      if (_destroyInject != null) {
        ConfigContext env = null;
        if (env == null)
          env = new ConfigContext();
  
        for (ConfigProgram inject : _destroyInject)
          inject.inject(instance, env);
      }
      */

      if (getInjectionTarget() != null) {
        getInjectionTarget().preDestroy(instance);
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _ejbClass + "]";
  }
}
