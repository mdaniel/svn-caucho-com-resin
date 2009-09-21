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
import com.caucho.config.inject.ManagedBeanImpl;
import com.caucho.config.scope.ScopeContext;
import com.caucho.config.scope.ApplicationScope;

import java.io.Closeable;
import java.lang.annotation.*;
import java.lang.reflect.Type;
import java.util.Set;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.context.spi.PassivationCapable;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * SingletonBean represents a singleton instance exported as a web beans.
 *
 * <code><pre>
 * @Current Manager manager;
 *
 * manager.addBean(new SingletonBean(myValue));
 * </pre></code>
 */
abstract public class AbstractSingletonBean extends BeanWrapper
  implements Closeable, AnnotatedBean, PassivationCapable
{
  private ManagedBeanImpl _managedBean;
  
  private Set<Type> _types;
  private Annotated _annotated;
  private Set<Annotation> _bindings;
  private Set<Annotation> _stereotypes;
  private Class<? extends Annotation> _scopeType;
  private String _name;

  private String _passivationId;
  
  AbstractSingletonBean(ManagedBeanImpl managedBean,
			Set<Type> types,
			Annotated annotated,
			Set<Annotation> bindings,
			Set<Annotation> stereotypes,
			Class<? extends Annotation> scopeType,
			String name)
  {
    super(managedBean.getBeanManager(), managedBean);

    _managedBean = managedBean;

    _types = types;
    _annotated = annotated;
    _bindings = bindings;
    _stereotypes = stereotypes;
    _scopeType = scopeType;
    _name = name;
  }
      
  //
  // metadata for the bean
  //

  public Annotated getAnnotated()
  {
    if (_annotated != null)
      return _annotated;
    else
      return _managedBean.getAnnotated();
  }

  public AnnotatedType getAnnotatedType()
  {
    if (_annotated instanceof AnnotatedType)
      return (AnnotatedType) _annotated;
    else
      return _managedBean.getAnnotatedType();
  }

  public Set<Annotation> getBindings()
  {
    if (_bindings != null)
      return _bindings;
    else
      return super.getBindings();
  }

  public Set<Annotation> getStereotypes()
  {
    if (_stereotypes != null)
      return _stereotypes;
    else
      return getBean().getStereotypes();
  }

  public String getName()
  {
    if (_name != null)
      return _name;
    else
      return getBean().getName();
  }
  
  /**
   * Return passivation id
   */
  public String getId()
  {
    if (_passivationId == null)
      _passivationId = calculatePassivationId();
    
    return _passivationId;
  }

  /**
   * Returns the bean's scope type.
   */
  public Class<? extends Annotation> getScope()
  {
    if (_scopeType != null)
      return _scopeType;
    else
      return getBean().getScope();
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
