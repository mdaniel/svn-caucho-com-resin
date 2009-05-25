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
import com.caucho.config.inject.AbstractBean;
import com.caucho.util.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

import javax.enterprise.context.spi.CreationalContext;
import javax.decorator.Decorates;
import javax.enterprise.inject.AnnotationLiteral;
import javax.enterprise.inject.BindingType;
import javax.enterprise.inject.Current;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * DecoratorBean represents a Java decorator
 */
public class DecoratorBean implements Decorator
{
  private static final L10N L = new L10N(DecoratorBean.class);

  private InjectManager _beanManager;
  
  private Class _type;

  private AbstractBean _bean;

  private Field _delegateField;

  private ArrayList<Class> _types
    = new ArrayList<Class>();

  private HashSet<Annotation> _bindings
    = new HashSet<Annotation>();
  
  public DecoratorBean(InjectManager beanManager,
		       Class type)
  {
    _beanManager = beanManager;

    _type = type;

    _bean = null;//new SimpleBean(_type);

    init();
  }

  //
  // metadata for the bean
  //

  /**
   * Returns the bean's binding types
   */
  @Override
  public Set<Annotation> getBindings()
  {
    return _bindings;
  }

  /**
   * Returns the bean's deployment type
   */
  public Class<? extends Annotation> getDeploymentType()
  {
    return _bean.getDeploymentType();
  }

  /**
   * Returns the bean's name or null if the bean does not have a
   * primary name.
   */
  public String getName()
  {
    return _bean.getName();
  }

  /**
   * Returns true if the bean can be null
   */
  public boolean isNullable()
  {
    return false;
  }

  /**
   * Returns true if the bean is serializable
   */
  public boolean isPassivationCapable()
  {
    return false;
  }

  /**
   * Returns the bean's scope
   */
  public Class<? extends Annotation> getScopeType()
  {
    return _bean.getScopeType();
  }

  /**
   * Returns the types that the bean implements
   */
  public Set<Type> getTypes()
  {
    return _bean.getTypes();
  }

  /**
   * Returns the types for the decorated
   */
  public Set<Type> getDecoratedTypes()
  {
    throw new UnsupportedOperationException();
  }

  //
  // lifecycle
  //
  
  public Object create(CreationalContext creationalContext)
  {
    return _bean.create(creationalContext);
  }

  /*
  public void destroy(Object instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  /**
   * Returns the set of injection points, for validation.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _bean.getInjectionPoints();
  }

  /**
   * Create a new instance of the bean.
   */
  /*
  public Object create()
  {
    return _bean.create();
  }
  */

  /**
   * Destroys a bean instance
   */
  /*
  public void destroy(Object instance)
  {
  }
  */

  //
  // decorator
  //

  /**
   * Returns the type of the delegated object
   */
  public Class<?> getDelegateType()
  {
    if (_delegateField != null)
      return _delegateField.getType();
    else
      return null;
  }

  /**
   * Returns the bindings for the delegated object
   */
  public Set<Annotation> getDelegateBindings()
  {
    return _bindings;
  }

  /**
   * Sets the delegate for an object
   */
  public void setDelegate(Object instance,
			  Object delegate)
  {
    if (! _type.isAssignableFrom(instance.getClass())) {
      throw new ConfigException(L.l("{0} is an invalid @Decorator instance because it does not extend the implementation class {1}",
				    instance.getClass().getName(),
				    _type.getName()));
    }

    if (! getDelegateType().isAssignableFrom(delegate.getClass())) {
      throw new ConfigException(L.l("{0} is an invalid @Decorator delegate because it does not implement the delegate {1}",
				    delegate.getClass().getName(),
				    getDelegateType()));
    }

    try {
      _delegateField.set(instance, delegate);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  //
  // introspection
  //

  public void init()
  {
    // _bean.init();
    
    introspect();

    if (_delegateField == null)
      throw new ConfigException(L.l("{0} is missing a @Decorates field.  All @Decorators need a @Decorates field for a delegate injection",
				    _type.getName()));
  }

  protected void introspect()
  {
    introspectDelegateField();
  }

  protected void introspectDelegateField()
  {
    if (_delegateField == null) {
      
      for (Field field : _type.getDeclaredFields()) {
	if (Modifier.isStatic(field.getModifiers()))
	  continue;

	if (! field.isAnnotationPresent(Decorates.class))
	  continue;

	Class fieldType = field.getType();

	if (! fieldType.isInterface()) {
	  throw new ConfigException(L.l("{0}.{1} is an invalid @Decorates field because its type '{2}' is not an interface",
					_type.getName(),
					field.getName(),
					fieldType.getName()));
	}

	for (Class iface : _type.getInterfaces()) {
	  if (! iface.isAssignableFrom(fieldType)) {
	    throw new ConfigException(L.l("{0}.{1} is an invalid @Decorates field because {2} does not implement the API {3}",
					  _type.getName(),
					  field.getName(),
					  fieldType.getName(),
					  iface.getName()));
	  }
	}

	if (_delegateField != null) {
	  throw new ConfigException(L.l("{0}: @Decorator field '{1}' conflicts with earlier field '{2}'.  A decorator must have exactly one delegate field.",
					_type.getName(),
					_delegateField,
					field.getName()));
	}

	field.setAccessible(true);
	_delegateField = field;

	introspectBindingTypes(field.getAnnotations());
      }
    }
  }

  protected void introspectBindingTypes(Annotation []annList)
  {
    for (Annotation ann : annList) {
      if (ann.annotationType().isAnnotationPresent(BindingType.class)) {
	_bindings.add(ann);
      }
    }

    if (_bindings.size() == 0)
      _bindings.add(new AnnotationLiteral<Current>() {});
  }
  
  /**
   * Instantiate the bean.
   */
  public Object instantiate()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Inject the bean.
   */
  public void inject(Object instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Call post-construct
   */
  public void postConstruct(Object instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Call pre-destroy
   */
  public void preDestroy(Object instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  /**
   * Call destroy
   */
  public void destroy(Object instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  private void addType(Class type)
  {
    _types.add(type);
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(_type.getSimpleName());

    if (_delegateField != null)
      sb.append(",").append(_delegateField.getType().getSimpleName());

    sb.append("]");
    
    return sb.toString();
  }
}
