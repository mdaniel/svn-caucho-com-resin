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
import com.caucho.config.inject.ComponentImpl;
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
import javax.context.CreationalContext;
import javax.inject.manager.Bean;
import javax.inject.manager.InjectionPoint;
import javax.inject.manager.Manager;

/**
 * Configuration for a @Produces method
 */
public class ProducesInjectionPointBean extends Bean {
  private static final L10N L = new L10N(ProducesInjectionPointBean.class);

  private final ProducesBean _producesBean;
  private final InjectionPoint _ij;

  ProducesInjectionPointBean(ProducesBean producesBean,
			     InjectionPoint ij)
  {
    super(producesBean.getManager());

    _producesBean = producesBean;
    _ij = ij;
  }

  //
  // metadata for the bean
  //

  /**
   * Returns the bean's binding annotations.
   */
  public Set<Annotation> getBindings()
  {
    return _producesBean.getBindings();
  }

  /**
   * Returns the bean's deployment type
   */
  public Class<? extends Annotation> getDeploymentType()
  {
    return _producesBean.getDeploymentType();
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
  public boolean isSerializable()
  {
    return _producesBean.isSerializable();
  }

  /**
   * Returns the bean's scope type.
   */
  public Class<? extends Annotation> getScopeType()
  {
    return _producesBean.getScopeType();
  }

  /**
   * Returns the types that the bean exports for bindings.
   */
  public Set<Class<?>> getTypes()
  {
    return _producesBean.getTypes();
  }
  
  public Object create(CreationalContext creationalContext)
  {
    return _producesBean.createNew(creationalContext, _ij);
  }

  public void destroy(Object instance)
  {
    _producesBean.destroy(instance);
  }
}
