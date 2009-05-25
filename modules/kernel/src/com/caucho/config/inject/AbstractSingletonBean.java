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

import com.caucho.config.ConfigContext;
import com.caucho.config.inject.HandleAware;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.scope.ApplicationScope;

import java.io.Closeable;
import java.lang.annotation.*;
import java.lang.reflect.Type;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ManagedBean;

/**
 * SingletonBean represents a singleton instance exported as a web beans.
 *
 * <code><pre>
 * @Current Manager manager;
 *
 * manager.addBean(new SingletonBean(myValue));
 * </pre></code>
 */
abstract public class AbstractSingletonBean extends ManagedBeanWrapper
  implements Closeable
{
  private Set<Type> _types;
  private Class<? extends Annotation> _deploymentType;
  private Set<Annotation> _bindings;
  private Class<? extends Annotation> _scopeType;
  private String _name;
  
  AbstractSingletonBean(ManagedBean managedBean,
			Set<Type> types,
			Class<? extends Annotation> deploymentType,
			Set<Annotation> bindings,
			Class<? extends Annotation> scopeType,
			String name)
  {
    super(managedBean);

    _types = types;
    _deploymentType = deploymentType;
    _bindings = bindings;
    _scopeType = scopeType;
    _name = name;
  }
      
  //
  // metadata for the bean
  //

  public Set<Annotation> getBindings()
  {
    if (_bindings != null)
      return _bindings;
    else
      return super.getBindings();
  }

  public Class<? extends Annotation> getDeploymentType()
  {
    if (_deploymentType != null)
      return _deploymentType;
    else
      return getBean().getDeploymentType();
  }

  public String getName()
  {
    if (_name != null)
      return _name;
    else
      return getBean().getName();
  }

  /**
   * Returns the bean's scope type.
   */
  public Class<? extends Annotation> getScopeType()
  {
    if (_scopeType != null)
      return _scopeType;
    else
      return getBean().getScopeType();
  }

  /**
   * Returns the types that the bean exports for bindings.
   */
  public Set<Type> getTypes()
  {
    if (_types != null)
      return _types;
    else
      return getBean().getTypes();
  }

  @Override
  abstract public Object create(CreationalContext env);


  /**
   * Frees the singleton on environment shutdown
   */
  public void close()
  {
  }
}
