/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.config.scope;

import java.lang.annotation.Annotation;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import com.caucho.inject.Module;


/**
 * The dependent context.
 */
@Module
public class DependentContext implements Context {
  @Override
  public <T> T get(Contextual<T> bean)
  {
    return null;
  }

  @Override
  public <T> T get(Contextual<T> bean, CreationalContext<T> creationalContext)
  {
    if (creationalContext == null)
      return null;
    
    T instance = bean.create(creationalContext);
    
    return instance;
  }

  @Override
  public Class<? extends Annotation> getScope()
  {
    return Dependent.class;
  }

  @Override
  public boolean isActive()
  {
    return true;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

}
