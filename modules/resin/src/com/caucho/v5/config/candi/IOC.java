/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Alex Rojkov
 */

package com.caucho.v5.config.candi;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.TypeLiteral;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class IOC extends CDI<Object>
{
  @Override
  public BeanManager getBeanManager()
  {
    return CandiManager.getCurrent();
  }

  @Override
  public Instance<Object> select(Annotation... qualifiers)
  {
    CandiManager manager = (CandiManager) getBeanManager();

    InstanceImpl instance = new InstanceImpl(manager,
                                             manager.getBeanManager(),
                                             Object.class,
                                             qualifiers,
                                             new Inj());

    return instance;
  }

  @Override
  public <U extends Object> Instance<U> select(Class<U> subtype,
                                               Annotation... qualifiers)
  {
    CandiManager manager = (CandiManager) getBeanManager();

    InstanceImpl instance = new InstanceImpl(manager,
                                             manager.getBeanManager(),
                                             subtype,
                                             qualifiers,
                                             new Inj());

    return instance;
  }

  @Override
  public <U extends Object> Instance<U> select(TypeLiteral<U> subtype,
                                               Annotation... qualifiers)
  {
    return null;
  }

  @Override
  public boolean isUnsatisfied()
  {
    return false;
  }

  @Override
  public boolean isAmbiguous()
  {
    return true;
  }

  @Override
  public Iterator<Object> iterator()
  {
    return null;
  }

  @Override
  public Object get()
  {
    return null;
  }

  @Override
  public void destroy(Object instance)
    throws UnsupportedOperationException
  {
    throw new AbstractMethodError();
  }

  @Override
  public String toString()
  {
    return IOC.class.getSimpleName() + "[]";
  }
}

class Inj implements InjectionPoint
{
  @Override
  public Type getType()
  {
    throw new AbstractMethodError();
  }

  @Override
  public Set<Annotation> getQualifiers()
  {
    throw new AbstractMethodError();
  }

  @Override
  public Bean<?> getBean()
  {
    throw new AbstractMethodError();
  }

  @Override
  public Member getMember()
  {
    throw new AbstractMethodError();
  }

  @Override
  public Annotated getAnnotated()
  {
    throw new AbstractMethodError();
  }

  @Override
  public boolean isDelegate()
  {
    return false;
  }

  @Override
  public boolean isTransient()
  {
    return false;
  }

  class Ann implements Annotated
  {
    @Override
    public Type getBaseType()
    {
      return Object.class;
    }

    @Override
    public Set<Type> getTypeClosure()
    {
      return new HashSet<>();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType)
    {
      return null;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType)
    {
      return false;
    }

    @Override
    public Set<Annotation> getAnnotations()
    {
      return null;
    }
  }
}