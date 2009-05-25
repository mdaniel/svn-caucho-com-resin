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
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.config.*;
import com.caucho.config.cfg.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.*;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProducerBean;

/**
 * Configuration for a @Produces method
 */
public class ProducesBean<X,T> extends AbstractIntrospectedBean<T>
  implements ProducerBean<X,T>, InjectionTarget<T>
{	     
  private static final L10N L = new L10N(ProducesBean.class);

  private static final Object []NULL_ARGS = new Object[0];
  
  private final Bean _producer;
  private final AnnotatedMethod _beanMethod;

  // XXX: needs to be InjectionPoint
  private Arg []_args;

  private boolean _isBound;

  protected ProducesBean(InjectManager inject,
			 Bean producer,
			 AnnotatedMethod beanMethod,
			 Arg []args)
  {
    super(inject, beanMethod.getType(), beanMethod);

    _producer = producer;
    _beanMethod = beanMethod;
    _args = args;

    if (args == null)
      throw new NullPointerException();
  }

  public static ProducesBean create(InjectManager inject,
				    Bean producer,
				    AnnotatedMethod beanMethod,
				    Arg []args)
  {
    return new ProducesBean(inject, producer, beanMethod, args);
  }

  protected Bean getProducer()
  {
    return _producer;
  }

  protected AnnotatedMethod getMethod()
  {
    return _beanMethod;
  }

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
  
  protected Class getDefaultDeploymentType()
  {
    if (_producer.getDeploymentType() != null)
      return _producer.getDeploymentType();

    return null;// super.getDefaultDeploymentType();
  }

  @Override
  protected String getDefaultName()
  { 
    String methodName = _beanMethod.getJavaMember().getName();
      
    if (methodName.startsWith("get") && methodName.length() > 3) {
      return (Character.toLowerCase(methodName.charAt(3))
	      + methodName.substring(4));
    }
    else
      return methodName;
  }

  public boolean isInjectionPoint()
  {
    for (Class paramType : _beanMethod.getJavaMember().getParameterTypes()) {
      if (InjectionPoint.class.equals(paramType))
	return true;
    }

    return false;
  }

  /**
   * Returns the declaring bean
   */
  public Bean<X> getDeclaringBean()
  {
    throw new UnsupportedOperationException(getClass().getName());
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
    ConfigContext env = (ConfigContext) cxt;
      
    X factory = (X) _beanManager.getReference(_producer);

    if (factory == null) {
      throw new IllegalStateException(L.l("{0}: unexpected null factory for {1}",
					  this, _producer));
    }

    return produce(factory);
  }

  /**
   * Produces a new bean instance
   */
  public T produce(X bean)
  {
    try {
      Object []args;

      if (_args.length > 0) {
	args = new Object[_args.length];

	for (int i = 0; i < args.length; i++) {
	  args[i] = _args[i].eval(null);
	  /*
	  if (_args[i] instanceof InjectionPointBean) {
	    if (ij != null)
	      args[i] = ij;
	    else
	      throw new NullPointerException();
	  }
	  // XXX: get initial value
	  else
	    args[i] = _beanManager.getReference(_args[i]);
	  */
	}
      }
      else
	args = NULL_ARGS;
      
      T value = (T) _beanMethod.getJavaMember().invoke(bean, args);
      
      return value;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /*
  @Override
  public X instantiate()
  {
    return createNew(null, null);
  }
  */

  public void inject(T instance, CreationalContext<T> cxt)
  {
  }

  public void postConstruct(T instance, CreationalContext<T> cxt)
  {
  }

  @Override
  public void bind()
  {
    synchronized (this) {
      if (_isBound)
	return;

      _isBound = true;
      
      Method method = _beanMethod.getJavaMember();
    
      String loc = InjectManager.location(method);

      Type []param = method.getGenericParameterTypes();
      // Annotation [][]paramAnn = _method.getParameterAnnotations();
      List<AnnotatedParameter> beanParams = _beanMethod.getParameters();

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
   * Disposes a bean instance
   */
  public void dispose(T instance)
  {
  }
  
  /**
   * Destroys a bean instance
   */
  public void destroys(T instance)
  {
  }
  
  /**
   * Returns the owning producer
   */
  public AnnotatedMethod<X> getAnnotatedProducer()
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

  protected InjectionPointBean createInjectionPointBean(BeanManager manager)
  {
    return new InjectionPointBean(manager);
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");

    Method method = _beanMethod.getJavaMember();

    sb.append(getTargetSimpleName());
    sb.append(", ");
    sb.append(method.getDeclaringClass().getSimpleName());
    sb.append(".");
    sb.append(method.getName());
    sb.append("()");
    
    sb.append(", {");

    boolean isFirst = true;
    for (Object obj : getBindings()) {
      Annotation ann = (Annotation) obj;
      
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

    if (getDeploymentType() != null) {
      sb.append(", @");
      sb.append(getDeploymentType().getSimpleName());
    }
    sb.append("]");

    return sb.toString();
  }
}
