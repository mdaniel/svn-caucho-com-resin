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
 * @author Scott Ferguson
 */

package com.caucho.v5.config.extension;

import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.config.candi.ManagedBeanImpl;
import com.caucho.v5.inject.Module;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;

import java.lang.annotation.Annotation;

@Module
public class BeforeBeanDiscoveryImpl implements BeforeBeanDiscovery
{
  private CandiManager _cdiManager;
  
  BeforeBeanDiscoveryImpl(CandiManager cdiManager)
  {
    _cdiManager = cdiManager;
  }
  
  @Override
  public void addAnnotatedType(AnnotatedType<?> annType)
  {
    addAnnotatedType(annType, null);
  }

  @Override
  public void addAnnotatedType(AnnotatedType<?> type, String id)
  {
    _cdiManager.addSyntheticBeforeBeanDiscovery(type, id);
  }

  @Override
  public void addQualifier(AnnotatedType<? extends Annotation> qualifier)
  {
    throw new AbstractMethodError();
  }

  @Override
  public void addQualifier(Class<? extends Annotation> qualifier)
  {
    _cdiManager.addQualifier(qualifier);
  }

  @Override
  public void addScope(Class<? extends Annotation> scopeType,
                       boolean isNormal,
                       boolean isPassivating)
  {
    _cdiManager.addScope(scopeType, isNormal, isPassivating);
  }

  @Override
  public void addStereotype(Class<? extends Annotation> stereotype,
                            Annotation... stereotypeDef)
  {
    _cdiManager.addStereotype(stereotype, stereotypeDef);
  }

  @Override
  public void addInterceptorBinding(AnnotatedType<? extends Annotation> bindingType)
  {
    throw new AbstractMethodError();
  }

  @Override
  public void addInterceptorBinding(Class<? extends Annotation> bindingType,
                                    Annotation... bindings)
  {
    throw new AbstractMethodError();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cdiManager + "]";
  }
}
