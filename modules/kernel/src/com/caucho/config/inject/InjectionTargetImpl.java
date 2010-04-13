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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.decorator.Delegate;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.interceptor.InvocationContext;

import com.caucho.config.ConfigException;
import com.caucho.config.SerializeHandle;
import com.caucho.config.bytecode.SerializationAdapter;
import com.caucho.config.gen.ApiClass;
import com.caucho.config.gen.BeanInjectionTarget;
import com.caucho.config.gen.PojoBean;
import com.caucho.config.j2ee.PostConstructProgram;
import com.caucho.config.j2ee.PreDestroyInject;
import com.caucho.config.program.Arg;
import com.caucho.config.program.BeanArg;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ValueArg;
import com.caucho.config.reflect.AnnotatedTypeImpl;
import com.caucho.inject.Module;
import com.caucho.util.L10N;

/**
 * SimpleBean represents a POJO Java bean registered as a WebBean.
 */
@Module
public class InjectionTargetImpl<X> extends AbstractIntrospectedBean<X>
  implements InjectionTarget<X>
{
  private static final L10N L = new L10N(InjectionTargetImpl.class);
  private static final Logger log
    = Logger.getLogger(InjectionTargetImpl.class.getName());

  private static final Object []NULL_ARGS = new Object[0];

  private boolean _isBound;

  private Class<X> _instanceClass;

  private AnnotatedType<X> _beanType;

  private Set<Annotation> _interceptorBindings;

  private AnnotatedConstructor<X> _beanCtor;
  private Constructor<X> _javaCtor;
  private Arg []_args;
  
  private boolean _isGenerateInterception = true;

  private ConfigProgram []_newArgs;
  private ConfigProgram []_injectProgram;
  private ConfigProgram []_initProgram;
  private ConfigProgram []_destroyProgram;

  private Set<InjectionPoint> _injectionPointSet
    = new HashSet<InjectionPoint>();

  private ArrayList<ConfigProgram> _injectProgramList
    = new ArrayList<ConfigProgram>();
  
  public InjectionTargetImpl(InjectManager beanManager,
                             AnnotatedType<X> beanType)
  {
    super(beanManager, beanType.getBaseType(), beanType);

    _beanType = beanType;

    /*
    if (beanType.getType() instanceof Class)
      validateType((Class) beanType.getType());
    */
  }

  public AnnotatedType<X> getAnnotatedType()
  {
    return _beanType;
  }

  /**
   * Checks for validity for classpath scanning.
   */
  public static boolean isValid(Class<?> type)
  {
    if (type.isInterface())
      return false;

    if (type.getTypeParameters() != null
        && type.getTypeParameters().length > 0) {
      return false;
    }

    if (! isValidConstructor(type))
      return false;

    return true;
  }

  public static boolean isValidConstructor(Class<?> type)
  {
    for (Constructor<?> ctor : type.getDeclaredConstructors()) {
      if (ctor.getParameterTypes().length == 0)
        return true;

      if (ctor.isAnnotationPresent(Inject.class))
        return true;
    }

    return false;
  }

  public void setConstructor(Constructor ctor)
  {
    // XXX: handled differently now
    throw new IllegalStateException();
    // _ctor = ctor;
  }

  private Class<?> getInstanceClass()
  {
    return _instanceClass;
  }
  
  public void setGenerateInterception(boolean isEnable)
  {
    _isGenerateInterception = isEnable;
  }

  /**
   * Sets the init program.
   */
  public void setNewArgs(ArrayList<ConfigProgram> args)
  {
    // XXX: handled differently
    if (args != null) {
      _newArgs = new ConfigProgram[args.size()];
      args.toArray(_newArgs);
    }
  }

  public Set<Annotation> getInterceptorBindings()
  {
    return _interceptorBindings;
  }

  private static boolean isAnnotationPresent(Annotation []annotations, Class<?> type)
  {
    for (Annotation ann : annotations) {
      if (ann.annotationType().equals(type))
        return true;
    }

    return false;
  }

  @Override
  public X produce(CreationalContext<X> contextEnv)
  {
    try {
      if (! _isBound)
        bind();

      CreationalContextImpl<X> env = null;
      
      if (contextEnv instanceof CreationalContextImpl<?>)
        env = (CreationalContextImpl<X>) contextEnv;

      if (_args == null)
        throw new IllegalStateException(L.l("Can't instantiate bean because it is not a valid ManagedBean: '{0}'", toString()));

      Object []args;
      int size = _args.length;
      if (size > 0) {
        args = new Object[size];

        for (int i = 0; i < size; i++) {
          args[i] = _args[i].eval(env);
        }
      }
      else
        args = NULL_ARGS;

      Object value = _javaCtor.newInstance(args);

      if (value instanceof HandleAware) {
        SerializationAdapter.setHandle(value, getHandle());
      }

      return (X) value;
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

  protected Object getHandle()
  {
    return new SingletonHandle(getId());
  }

  public void inject(X instance, CreationalContext<X> env)
  {
    try {
      if (! _isBound)
        bind();

      for (ConfigProgram program : _injectProgram) {
        program.inject(instance, env);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new CreationException(e);
    }
  }

  public void postConstruct(X instance)
  {
    try {
      if (! _isBound)
        bind();

      CreationalContext<X> env = null;

      for (ConfigProgram program : _initProgram) {
        program.inject(instance, env);
      }

      // server/4750
      if (instance instanceof BeanInjectionTarget) {
        BeanInjectionTarget bean = (BeanInjectionTarget) instance;
        bean.__caucho_postConstruct();
      }
    } catch (RuntimeException e) {
      throw e;
      /*
    } catch (InvocationTargetException e) {
      throw ConfigException.create(e.getCause());
      */
    } catch (Exception e) {
      throw new CreationException(e);
    }
  }

  public void dispose(X instance)
  {
  }

  /**
   * Binds parameters
   */
  public void bind()
  {
    synchronized (this) {
      if (_isBound)
        return;
      _isBound = true;

      Class<?> cl = getTargetClass();

      HashMap<Method,Annotation[]> methodMap
        = new HashMap<Method,Annotation[]>();

      ArrayList<ConfigProgram> initList = new ArrayList<ConfigProgram>();
      introspectInit(initList, cl, methodMap);
      _initProgram = new ConfigProgram[initList.size()];
      initList.toArray(_initProgram);

      ArrayList<ConfigProgram> destroyList = new ArrayList<ConfigProgram>();
      introspectDestroy(destroyList, cl);
      _destroyProgram = new ConfigProgram[destroyList.size()];
      destroyList.toArray(_destroyProgram);

      if (_beanCtor == null) {
        // XXX:
        AnnotatedType beanType = _beanType;
        if (beanType != null)
          beanType = new AnnotatedTypeImpl(cl, cl);

        introspectConstructor(beanType);
      }

      // introspectObservers(getTargetClass());

      Class<X> instanceClass = null;

      if (_isGenerateInterception) {
        if (! _beanType.isAnnotationPresent(javax.interceptor.Interceptor.class)
            && ! _beanType.isAnnotationPresent(javax.decorator.Decorator.class)) {
          ApiClass apiClass = new ApiClass(_beanType, true);

          PojoBean bean = new PojoBean(apiClass);
          bean.introspect();

          instanceClass = (Class<X>) bean.generateClass();
        }

        if (instanceClass == getTargetClass() && isSerializeHandle()) {
          instanceClass = SerializationAdapter.gen(instanceClass);
        }
      }

      if (instanceClass != null && instanceClass != _instanceClass) {
        try {
          if (_javaCtor != null) {
            _javaCtor = instanceClass.getConstructor(_javaCtor.getParameterTypes());
            _javaCtor.setAccessible(true);
          }

          _instanceClass = instanceClass;
        } catch (Exception e) {
          // server/2423
          log.log(Level.FINE, e.toString(), e);
          // throw ConfigException.create(e);
        }
      }
    }
  }

  public static void
    introspectInit(ArrayList<ConfigProgram> initList,
                   Class<?> type,
                   HashMap<Method,Annotation[]> methodAnnotationMap)
    throws ConfigException
  {
    if (type == null || type.equals(Object.class))
      return;

    introspectInit(initList, type.getSuperclass(), methodAnnotationMap);

    for (Method method : type.getDeclaredMethods()) {
      Annotation []annList = null;

      if (methodAnnotationMap != null)
        annList = methodAnnotationMap.get(method);

      if (annList == null)
        annList = method.getAnnotations();

      if (! isAnnotationPresent(annList, PostConstruct.class)) {
        // && ! isAnnotationPresent(annList, Inject.class)) {
        continue;
      }

      if (method.getParameterTypes().length == 1
          && InvocationContext.class.equals(method.getParameterTypes()[0]))
        continue;

      if (isAnnotationPresent(annList, PostConstruct.class)
          && method.getParameterTypes().length != 0) {
          throw new ConfigException(location(method)
                                    + L.l("{0}: @PostConstruct is requires zero arguments"));
      }

      PostConstructProgram initProgram
        = new PostConstructProgram(method);

      if (! initList.contains(initProgram))
        initList.add(initProgram);
    }
  }

  private void
    introspectDestroy(ArrayList<ConfigProgram> destroyList, Class<?> type)
    throws ConfigException
  {
    if (type == null || type.equals(Object.class))
      return;

    introspectDestroy(destroyList, type.getSuperclass());

    for (Method method : type.getDeclaredMethods()) {
      if (method.isAnnotationPresent(PreDestroy.class)) {
        Class<?> []types = method.getParameterTypes();

        if (types.length == 0) {
        }
        else if (types.length == 1 && types[0].equals(InvocationContext.class)) {
          // XXX:
          continue;
        }
        else
          throw new ConfigException(location(method)
                                    + L.l("@PreDestroy is requires zero arguments"));

        PreDestroyInject destroyProgram
          = new PreDestroyInject(method);

        if (! destroyList.contains(destroyProgram))
          destroyList.add(destroyProgram);
      }
    }
  }

  private static String location(Method method)
  {
    String className = method.getDeclaringClass().getName();

    return className + "." + method.getName() + ": ";
  }

  private boolean isSerializeHandle()
  {
    return getAnnotated().isAnnotationPresent(SerializeHandle.class);
  }

  /**
   * Call pre-destroy
   */
  public void preDestroy(X instance)
  {
    try {
      if (!_isBound)
        bind();

      CreationalContext<X> env = null;

      for (ConfigProgram program : _destroyProgram) {
        program.inject(instance, env);
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new CreationException(e);
    }
  }

  /**
   * Call pre-destroy
   */
  /*
  @Override
  public void destroy(T instance)
  {

  }
  */

  /**
   * Returns the injection points.
   */
  @Override
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _injectionPointSet;
  }

  //
  // introspection
  //

  public void introspect()
  {
    super.introspect();

    introspect(_beanType);
  }

  @Override
  protected Annotated getIntrospectedAnnotated()
  {
    return _beanType;
  }

  /**
   * Called for implicit introspection.
   */
  public void introspect(AnnotatedType<X> beanType)
  {
    Class<X> cl = (Class<X>) getIntrospectionClass();
    introspectConstructor(beanType);
    //introspectBindings(beanType);
    //introspectName(beanType);

    introspectInject(beanType);

    //introspectProduces(beanType);

    //introspectObservers(beanType);

    //introspectMBean();

    _injectProgram = new ConfigProgram[_injectProgramList.size()];
    _injectProgramList.toArray(_injectProgram);
  }

  /**
   * Introspects the constructor
   */
  protected void introspectConstructor(AnnotatedType<X> beanType)
  {
    if (_beanCtor != null)
      return;

    // XXX: may need to modify BeanFactory
    if (beanType.getJavaClass().isInterface())
      return;

    try {
      /*
      Class cl = getInstanceClass();

      if (cl == null)
        cl = getTargetClass();
      */

      AnnotatedConstructor<X> best = null;
      AnnotatedConstructor<X> second = null;

      for (AnnotatedConstructor<X> ctor : beanType.getConstructors()) {
        if (_newArgs != null
            && ctor.getParameters().size() != _newArgs.length) {
          continue;
        }
        else if (best == null) {
          best = ctor;
        }
        else if (hasQualifierAnnotation(ctor)) {
          if (best != null && hasQualifierAnnotation(best))
            throw new ConfigException(L.l("'{0}' can't have two constructors marked by @Inject or by a @Qualifier, because the Java Injection BeanManager can't tell which one to use.",
                                          beanType.getJavaClass().getName()));
          best = ctor;
          second = null;
        }
        else if (ctor.getParameters().size() == 0) {
          best = ctor;
        }
        else if (best.getParameters().size() == 0) {
        }
        else if (ctor.getParameters().size() == 1
                 && ctor.getParameters().get(0).equals(String.class)) {
          second = best;
          best = ctor;
        }
      }

      /*
      if (best == null)
        best = cl.getConstructor(new Class[0]);
      */

      if (best == null) {
        throw new ConfigException(L.l("{0}: no constructor found while introspecting bean for Java Injection",
                                      beanType.getJavaClass().getName()));
      }

      if (second == null) {
      }
      else if (beanType.getJavaClass().getName().startsWith("java.lang")
               && best.getParameters().size() == 1
               && best.getParameters().get(0).equals(String.class)) {
        log.fine(L.l("{0}: WebBean does not have a unique constructor, choosing String-arg constructor",
                     beanType.getJavaClass().getName()));
      }
      else
        throw new ConfigException(L.l("{0}: Bean does not have a unique constructor.  One constructor must be marked with @Inject or have a qualifier annotation.",
                                      beanType.getJavaClass().getName()));

      _beanCtor = best;
      _javaCtor = _beanCtor.getJavaMember();
      _javaCtor.setAccessible(true);

      _args = introspectArguments(_beanCtor.getParameters());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  protected Arg<?> []introspectArguments(List<AnnotatedParameter<X>> params)
  {
    Arg<?> []args = new Arg[params.size()];

    for (int i = 0; i < args.length; i++) {
      AnnotatedParameter<?> param = params.get(i);

      Annotation []qualifiers = getQualifiers(param);

      if (qualifiers.length > 0)
        args[i] = new BeanArg<X>(param.getBaseType(), qualifiers);
      else
        args[i] = new ValueArg<X>(param.getBaseType());
    }

    return args;
  }

  private Annotation []getQualifiers(Annotated annotated)
  {
    ArrayList<Annotation> qualifierList = new ArrayList<Annotation>();

    for (Annotation ann : annotated.getAnnotations()) {
      if (ann.annotationType().isAnnotationPresent(Qualifier.class)) {
        qualifierList.add(ann);
      }
    }

    if (qualifierList.size() == 0)
      qualifierList.add(CurrentLiteral.CURRENT);

    Annotation []qualifiers = new Annotation[qualifierList.size()];
    qualifierList.toArray(qualifiers);

    return qualifiers;
  }

  private void introspectInject(AnnotatedType<?> type)
  {
    // configureClassResources(injectList, type);

    for (AnnotatedField<?> field : type.getFields()) {
      if (field.getAnnotations().size() == 0)
        continue;

      if (field.isAnnotationPresent(Delegate.class))
        continue;
      else if (hasQualifierAnnotation(field)) {
        // boolean isOptional = isQualifierOptional(field);

        InjectionPoint ij = new InjectionPointImpl(getBeanManager(), this, field);

        _injectionPointSet.add(ij);

        _injectProgramList.add(new FieldInjectProgram(field.getJavaMember(), ij));
      }
      else {
        InjectionPointHandler handler
          = getBeanManager().getInjectionPointHandler(field);
        
        if (handler != null) {
          ConfigProgram program = new FieldHandlerProgram(field, handler);
          
          _injectProgramList.add(program);
        }
      }
    }

    for (AnnotatedMethod method : type.getMethods()) {
      if (method.getAnnotations().size() == 0)
        continue;

      if (method.isAnnotationPresent(Inject.class)) {
        // boolean isOptional = isQualifierOptional(field);

        List<AnnotatedParameter> params = method.getParameters();

        InjectionPoint []args = new InjectionPoint[params.size()];

        for (int i = 0; i < args.length; i++) {
          InjectionPoint ij
            = new InjectionPointImpl(getBeanManager(), this, params.get(i));

          _injectionPointSet.add(ij);

          args[i] = ij;
        }

        _injectProgramList.add(new MethodInjectProgram(method.getJavaMember(),
                                                       args));
      }
      else {
        InjectionPointHandler handler
          = getBeanManager().getInjectionPointHandler(method);
        
        if (handler != null) {
          ConfigProgram program = new MethodHandlerProgram(method, handler);
          
          _injectProgramList.add(program);
        }
      }
    }

    /*
    for (Method method : type.getDeclaredMethods()) {
      String fieldName = method.getName();
      Class []param = method.getParameterTypes();

      if (param.length != 1)
        continue;

      introspect(injectList, method);
    }
    */
  }

  private static boolean hasQualifierAnnotation(AnnotatedField field)
  {
    for (Annotation ann : field.getAnnotations()) {
      Class annType = ann.annotationType();

      if (annType.equals(Inject.class))
        return true;
      // XXX: no longer true
      /*
      if (annType.isAnnotationPresent(Qualifier.class))
        return true;
      */
    }

    return false;
  }

  private static boolean isQualifierOptional(AnnotatedField field)
  {
    for (Annotation ann : field.getAnnotations()) {
      Class annType = ann.annotationType();

      if (annType.isAnnotationPresent(Qualifier.class))
        return false;
    }

    return false;
  }

  private static boolean hasQualifierAnnotation(AnnotatedConstructor ctor)
  {
    return ctor.isAnnotationPresent(Inject.class);
  }

  class FieldInjectProgram extends ConfigProgram {
    private final Field _field;
    private final InjectionPoint _ij;

    FieldInjectProgram(Field field, InjectionPoint ij)
    {
      _field = field;
      _field.setAccessible(true);
      _ij = ij;
    }

    @Override
    public <T> void inject(T instance, CreationalContext<T> env)
    {
      try {
        // server/30i1
        InjectManager beanManager = InjectManager.getCurrent();
        
        Object value = beanManager.getInjectableReference(_ij, env);

        _field.set(instance, value);
      } catch (Exception e) {
        throw ConfigException.create(_field, e);
      }
    }
  }

  class MethodInjectProgram extends ConfigProgram {
    private final Method _method;
    private final InjectionPoint []_args;

    MethodInjectProgram(Method method, InjectionPoint []args)
    {
      _method = method;
      _method.setAccessible(true);
      _args = args;
    }

    @Override
    public <T> void inject(T instance, CreationalContext<T> env)
    {
      try {
        Object []args = new Object[_args.length];

        for (int i = 0; i < _args.length; i++)
          args[i] = getBeanManager().getInjectableReference(_args[i], env);

        _method.invoke(instance, args);
      } catch (Exception e) {
        throw ConfigException.create(_method, e);
      }
    }
  }
  
  class FieldHandlerProgram extends ConfigProgram {
    private final AnnotatedField<?> _field;
    private final InjectionPointHandler _handler;
    private ConfigProgram _boundProgram;
    
    FieldHandlerProgram(AnnotatedField<?> field, InjectionPointHandler handler)
    {
      _field = field;
      _handler = handler;
    }
  
    @Override
    public <T> void inject(T instance, CreationalContext<T> env)
    {
      if (_boundProgram == null)
        bind();
      
      _boundProgram.inject(instance, env);
    }
    
    private void bind()
    {
      _boundProgram = _handler.introspectField(_field);
    }
  }
  
  class MethodHandlerProgram extends ConfigProgram {
    private final AnnotatedMethod<?> _method;
    private final InjectionPointHandler _handler;
    private ConfigProgram _boundProgram;
    
    MethodHandlerProgram(AnnotatedMethod<?> method,
                         InjectionPointHandler handler)
    {
      _method = method;
      _handler = handler;
    }
  
    @Override
    public <T> void inject(T instance, CreationalContext<T> env)
    {
      if (_boundProgram == null)
        bind();
      
      _boundProgram.inject(instance, env);
    }
    
    private void bind()
    {
      _boundProgram = _handler.introspectMethod(_method);
    }
  }
}
