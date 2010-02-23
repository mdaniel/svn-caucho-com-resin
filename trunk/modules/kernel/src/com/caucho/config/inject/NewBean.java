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
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.type.*;
import com.caucho.config.types.*;
import com.caucho.config.gen.*;
import com.caucho.util.*;
import com.caucho.config.bytecode.*;
import com.caucho.config.cfg.*;
import com.caucho.config.event.*;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.logging.*;

import javax.annotation.*;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

/**
 * NewBean represents the SimpleBean created through the @New interface.
 *
 * <ul>
 * <li>initializer methods and injected fields are defined by annotations
 * <li>interceptor bindings are defined by annotations
 *
 */
public class NewBean extends InjectionTargetImpl
{
  private AnnotatedType _beanType;

  NewBean(InjectManager inject, AnnotatedType beanType)
  {
    super(inject, beanType);

    _beanType = beanType;

    //addBinding(NewLiteral.NEW);
    //setScopeType(Dependent.class);

    //init();
  }

  /**
   * The @New name is null.
   */
  @Override
  public String getName()
  {
    return null;
  }

  /**
   * The scope for @New is dependent.
   */
  @Override
  public Class<? extends Annotation> getScope()
  {
    return Dependent.class;
  }

  //
  // introspection overrides
  //

  /**
   * @New disables produces introspection.
   */
  /*
  @Override
  protected void introspectProduces(Set<AnnotatedMethod> methods)
  {
  }
  */

  /**
   * @New disables observer introspection.
   */
  /*
  @Override
  protected void introspectObservers(Set<AnnotatedMethod> methods)
  {
  }
  */

  /**
   * Creates a new instance of the component.
   */
  @Override
  public Object create(CreationalContext env)
  {
    InjectionTarget target = this;

    Object value = target.produce(env);
    target.inject(value, env);
    target.postConstruct(value);

    return value;
  }
}
