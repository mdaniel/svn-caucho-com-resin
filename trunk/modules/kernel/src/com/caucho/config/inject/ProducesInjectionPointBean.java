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

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.config.*;
import com.caucho.config.cfg.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.Set;

import javax.annotation.*;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.BeanManager;

/**
 * Configuration for a @Produces method
 */
public class ProducesInjectionPointBean<X> implements Bean<X> {
  private static final L10N L = new L10N(ProducesInjectionPointBean.class);

  private final ProducesBean _producesBean;
  private final InjectionPoint _ij;

  ProducesInjectionPointBean(ProducesBean producesBean,
                             InjectionPoint ij)
  {
    _producesBean = producesBean;
    _ij = ij;
  }

  //
  // metadata for the bean
  //

  /**
   * Returns the bean's binding annotations.
   */
  public Set<Annotation> getQualifiers()
  {
    return _producesBean.getQualifiers();
  }

  /**
   * Returns the bean's stereotype annotations.
   */
  public Set<Class<? extends Annotation>> getStereotypes()
  {
    return _producesBean.getStereotypes();
  }

  public Class getBeanClass()
  {
    return _producesBean.getBeanClass();
  }

  /**
   * Returns the set of injection points, for validation.
   */
  public Set<InjectionPoint> getInjectionPoints()
  {
    return _producesBean.getInjectionPoints();
  }

  /**
   * Returns the bean's name or null if the bean does not have a
   * primary name.
   */
  public String getName()
  {
    return _producesBean.getName();
  }

  /**
   * Returns true if the bean can be null
   */
  public boolean isNullable()
  {
    return _producesBean.isNullable();
  }

  /**
   * Returns true if the bean is serializable
   */
  public boolean isPassivationCapable()
  {
    return _producesBean.isPassivationCapable();
  }

  /**
   * Returns true if the bean is serializable
   */
  public boolean isAlternative()
  {
    return _producesBean.isAlternative();
  }

  /**
   * Returns the bean's scope type.
   */
  public Class<? extends Annotation> getScope()
  {
    return _producesBean.getScope();
  }

  /**
   * Returns the types that the bean exports for bindings.
   */
  public Set<Type> getTypes()
  {
    return _producesBean.getTypes();
  }

  public Object create(CreationalContext creationalContext)
  {
    Object instance = _producesBean.produce(creationalContext);
    //_producesBean.inject(instance);

    return instance;
  }

  /**
   * Instantiate the bean.
   */
  public X produce(CreationalContext cxt)
  {
    return (X) _producesBean.produce(cxt);
  }

  /**
   * Inject the bean.
   */
  public void inject(X instance, CreationalContext cxt)
  {
  }

  /**
   * Call post-construct
   */
  public void postConstruct(X instance)
  {
  }

  /**
   * Call pre-destroy
   */
  public void preDestroy(X instance)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Call destroy
   */
  public void destroy(X instance, CreationalContext<X> env)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /*
  public void destroy(Object instance)
  {
    _producesBean.destroy(instance);
  }
  */
}
