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
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.*;
import javax.enterprise.inject.*;
import javax.enterprise.inject.spi.*;
import javax.interceptor.InterceptorBindingType;

/**
 * SimpleBean represents a POJO Java bean registered as a WebBean.
 */
public class InjectionTargetImpl<X> extends AbstractIntrospectedBean<X>
  implements InjectionTarget<X>
{
  private static final L10N L = new L10N(InjectionTargetImpl.class);
  private static final Logger log
    = Logger.getLogger(InjectionTargetImpl.class.getName());

  private static final Object []NULL_ARGS = new Object[0];

  private boolean _isBound;

  private Class _instanceClass;

  private AnnotatedType<X> _beanType;

  private Set<Annotation> _interceptorBindings;
  
  private AnnotatedConstructor _beanCtor;
  private Constructor _javaCtor;
  private Arg []_args;

  private Method _cauchoPostConstruct;
  
  private ConfigProgram []_newArgs;
  private ConfigProgram []_injectProgram;
  private ConfigProgram []_initProgram;
  private ConfigProgram []_destroyProgram;

  private Set<InjectionPoint> _injectionPointSet
    = new HashSet<InjectionPoint>();

  private ArrayList<ConfigProgram> _injectProgramList
    = new ArrayList<ConfigProgram>();

  private ArrayList<SimpleBeanMethod> _methodList
    = new ArrayList<SimpleBeanMethod>();

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

  public AnnotatedType getAnnotatedType()
  {
    return _beanType;
  }

  /**
   * Checks for validity for classpath scanning.
   */
  public static boolean isValid(Class type)
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

  public static boolean isValidConstructor(Class type)
  {
    for (Constructor ctor : type.getDeclaredConstructors()) {
      if (ctor.getParameterTypes().length == 0)
	return true;

      if (ctor.isAnnotationPresent(Initializer.class))
	return true;
    }

    return false;
  }

  private void validateType(Class type)
  {
    if (type.isInterface())
      throw new ConfigException(L.l("'{0}' is an invalid SimpleBean because it is an interface",
				    type));

    /*
    Type []typeParam = type.getTypeParameters();
    if (typeParam != null && typeParam.length > 0) {
      StringBuilder sb = new StringBuilder();
      sb.append(type.getName());
      sb.append("<");
      for (int i = 0; i < typeParam.length; i++) {
	if (i > 0)
	  sb.append(",");
	sb.append(typeParam[i]);
      }
      sb.append(">");
      
      throw new ConfigException(L.l("'{0}' is an invalid SimpleBean class because it defines type variables",
				    sb));
    }
    */
  }

  public void setConstructor(Constructor ctor)
  {
    // XXX: handled differently now
    throw new IllegalStateException();
    // _ctor = ctor;
  }

  private Class getInstanceClass()
  {
    return _instanceClass;
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

  /**
   * Adds a configured method
   */
  public void addMethod(SimpleBeanMethod simpleMethod)
  {
    throw new UnsupportedOperationException();
    /*
    Method method = simpleMethod.getMethod();
    Annotation []annotations = simpleMethod.getAnnotations();

    if (isAnnotationPresent(annotations, Produces.class))
      addProduces(method, annotations);
    else if (isAnnotationDeclares(annotations, InterceptorBindingType.class)) {
      _methodList.add(simpleMethod);
    }
    else
      System.out.println("M: " + method);
    */
  }

  public Set<Annotation> getInterceptorBindings()
  {
    return _interceptorBindings;
  }

  /**
   * Adds a configured method
   */
  public void addField(SimpleBeanField simpleField)
  {
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

  //
  // InjectionTarget
  //

  @Override
  public X produce(CreationalContext contextEnv)
  {
    try {
      if (! _isBound)
	bind();

      ConfigContext env = (ConfigContext) contextEnv;

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

      /*
      if (isSingleton()) {
	SerializationAdapter.setHandle(value, getHandle());
      }
      */

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

  public void inject(X instance, CreationalContext<X> createEnv)
  {
    try {
      if (! _isBound)
	bind();

      ConfigContext env = (ConfigContext) createEnv;

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

      ConfigContext env = (ConfigContext) null;

      for (ConfigProgram program : _initProgram) {
	program.inject(instance, env);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new CreationException(e);
    }
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

      Class cl = getTargetClass();

      HashMap<Method,Annotation[]> methodMap
	= new HashMap<Method,Annotation[]>();

      for (SimpleBeanMethod beanMethod : _methodList) {
	methodMap.put(beanMethod.getMethod(),
		      beanMethod.getAnnotations());
      }

      /*
      ArrayList<ConfigProgram> injectList = new ArrayList<ConfigProgram>();
      InjectIntrospector.introspectInject(injectList, cl);
      _injectProgram = new ConfigProgram[injectList.size()];
      injectList.toArray(_injectProgram);
      */
      
      ArrayList<ConfigProgram> initList = new ArrayList<ConfigProgram>();
      InjectIntrospector.introspectInit(initList, cl, methodMap);
      _initProgram = new ConfigProgram[initList.size()];
      initList.toArray(_initProgram);
      
      ArrayList<ConfigProgram> destroyList = new ArrayList<ConfigProgram>();
      InjectIntrospector.introspectDestroy(destroyList, cl);
      _destroyProgram = new ConfigProgram[destroyList.size()];
      destroyList.toArray(_destroyProgram);

      if (_beanCtor == null) {
	// XXX:
	AnnotatedType beanType = _beanType;
	if (beanType != null)
	  beanType = new AnnotatedTypeImpl(cl, cl);
	
	introspectConstructor(beanType);
      }

      /*
      if (_ctor != null) {
	String loc = _ctor.getDeclaringClass().getName() + "(): ";
	Type []param = _ctor.getGenericParameterTypes();
	Annotation [][]paramAnn = _ctor.getParameterAnnotations();

	Arg []ctorArgs = new Arg[param.length];

	for (int i = 0; i < param.length; i++) {
	  ComponentImpl arg;

	  if (_newArgs != null && i < _newArgs.length) {
	    ConfigProgram argProgram = _newArgs[i];
	    ConfigType type = TypeFactory.getType(param[i]);

	    ctorArgs[i] = new ProgramArg(type, argProgram);
	  }

	  if (ctorArgs[i] == null) {
	    ctorArgs[i] = new BeanArg(loc, param[i], paramAnn[i]);
	  }
	}
	
	_ctorArgs = ctorArgs;
      }
      */

      // introspectObservers(getTargetClass());

      PojoBean bean = new PojoBean(new ApiClass(_beanType));
      /*
      //bean.setSingleton(isSingleton());
      bean.setBindings(getBindings());

      bean.setInterceptorBindings(getInterceptorBindings());

      for (AnnotatedMethod method : _beanType.getMethods()) {
	bean.setMethodAnnotations(method.getJavaMember(), method);
      }
      */
      
      bean.introspect();

      Class instanceClass = bean.generateClass();

      /*
      if (instanceClass == getTargetClass()
	  && isSingleton()
	  && ! isUnbound()) {
	instanceClass = SerializationAdapter.gen(instanceClass);
      }
      */
      
      if (instanceClass != null && instanceClass != _instanceClass) {
	try {
	  if (_javaCtor != null)
	    _javaCtor = instanceClass.getConstructor(_javaCtor.getParameterTypes());
	  
	  _instanceClass = instanceClass;
	} catch (Exception e) {
	  throw ConfigException.create(e);
	}
      }

      if (instanceClass != null) {
	for (Method method : instanceClass.getDeclaredMethods()) {
	  if (method.getName().equals("__caucho_postConstruct")) {
	    method.setAccessible(true);
	    _cauchoPostConstruct = method;
	  }
	}
      }
    }
  }

  private boolean isUnbound()
  {
    for (Object annObj : getBindings()) {
      Annotation ann = (Annotation) annObj;
      
      if (Unbound.class.equals(ann.annotationType()))
	return true;
    }

    return false;
  }

  /*
  protected ComponentImpl createArg(ConfigType type, ConfigProgram program)
  {
    Object value = program.configure(type);

    if (value != null)
      return new SingletonBean(getWebBeans(), value);
    else
      return null;
  }
  */
  
  /**
   * Call pre-destroy
   */
  public void preDestroy(X instance)
  {
    try {
      if (!_isBound)
        bind();

      ConfigContext env = (ConfigContext) null;

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
  public void destroy(X instance)
  {

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

  @Override
  public void introspect()
  {
    super.introspect();
    
    introspect(_beanType);
  }

  /**
   * Called for implicit introspection.
   */
  public void introspect(AnnotatedType beanType)
  {
    Class cl = getIntrospectionClass();
    Class scopeClass = null;

    //introspectTypes(beanType.getType());

    //introspectAnnotations(beanType.getAnnotations());

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
  protected void introspectConstructor(AnnotatedType<?> beanType)
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
      
      AnnotatedConstructor best = null;
      AnnotatedConstructor second = null;

      for (AnnotatedConstructor<?> ctor : beanType.getConstructors()) {
	if (_newArgs != null
	    && ctor.getParameters().size() != _newArgs.length) {
	  continue;
	}
	else if (best == null) {
	  best = ctor;
	}
	else if (hasBindingAnnotation(ctor)) {
	  if (best != null && hasBindingAnnotation(best))
	    throw new ConfigException(L.l("Simple bean {0} can't have two constructors with @BindingType or @Initializer, because the Manager can't tell which one to use.",
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
	throw new ConfigException(L.l("{0}: Bean does not have a unique constructor.  One constructor must be marked with @In or have a binding annotation.",
				      beanType.getJavaClass().getName()));

      _beanCtor = best;
      _javaCtor = _beanCtor.getJavaMember();

      _args = introspectArguments(_beanCtor.getParameters());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  protected Arg []introspectArguments(List<AnnotatedParameter> params)
  {
    Arg []args = new Arg[params.size()];

    for (int i = 0; i < args.length; i++) {
      AnnotatedParameter param = params.get(i);

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

  private void introspectInject(AnnotatedType<?> type)
  {
    // configureClassResources(injectList, type);

    for (AnnotatedField field : type.getFields()) {
      if (hasBindingAnnotation(field)) {
	boolean isOptional = isBindingOptional(field);

	InjectionPoint ij = new InjectionPointImpl(this, field);

	_injectionPointSet.add(ij);
	
	_injectProgramList.add(new FieldInjectProgram(field.getJavaMember(), ij));
      }
    }

    for (AnnotatedMethod method : type.getMethods()) {
      if (method.isAnnotationPresent(Initializer.class)) {
	// boolean isOptional = isBindingOptional(field);

	List<AnnotatedParameter> params = method.getParameters();
	
	InjectionPoint []args = new InjectionPoint[params.size()];

	for (int i = 0; i < args.length; i++) {
	  InjectionPoint ij = new InjectionPointImpl(this, params.get(i));

	  _injectionPointSet.add(ij);

	  args[i] = ij;
	}
	
	_injectProgramList.add(new MethodInjectProgram(method.getJavaMember(),
						       args));
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
  
  private static boolean hasBindingAnnotation(AnnotatedField field)
  {
    for (Annotation ann : field.getAnnotations()) {
      Class annType = ann.annotationType();

      if (annType.isAnnotationPresent(BindingType.class))
	return true;
    }

    return false;
  }

  private static boolean isBindingOptional(AnnotatedField field)
  {
    for (Annotation ann : field.getAnnotations()) {
      Class annType = ann.annotationType();
      
      if (annType.isAnnotationPresent(BindingType.class))
	return false;
    }

    return false;
  }

  private static boolean hasBindingAnnotation(AnnotatedConstructor ctor)
  {
    return ctor.isAnnotationPresent(Initializer.class);
  }

  private static boolean hasBindingAnnotation(Method method)
  {
    for (Annotation ann : method.getAnnotations()) {
      Class annType = ann.annotationType();
      
      if (annType.equals(Produces.class))
	return false;
      /*
      else if (annType.equals(Destroys.class))
	return false;
      */
    }

    boolean hasBinding = false;
    for (Annotation []annList : method.getParameterAnnotations()) {
      if (annList == null)
        continue;

      for (Annotation ann : annList) {
	Class annType = ann.annotationType();
	
	if (annType.equals(Observes.class))
	  return false;
	if (annType.equals(Disposes.class))
	  return false;
        else if (annType.isAnnotationPresent(BindingType.class))
	  hasBinding = true;
      }
    }

    return hasBinding;
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
    
    public void inject(Object instance, ConfigContext env)
    {
      try {
	Object value = _beanManager.getInjectableReference(_ij, env);

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
    
    public void inject(Object instance, ConfigContext env)
    {
      try {
	Object []args = new Object[_args.length];

	for (int i = 0; i < _args.length; i++)
	  args[i] = _beanManager.getInjectableReference(_args[i], env);

	_method.invoke(instance, args);
      } catch (Exception e) {
	throw ConfigException.create(_method, e);
      }
    }
  }
}
