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

package com.caucho.v5.config.extension;

import com.caucho.v5.config.candi.CandiManager;
import com.caucho.v5.inject.Module;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.ProcessBeanAttributes;

@Module
public class ProcessBeanAttributesImpl<T> implements ProcessBeanAttributes<T>
{
  private Annotated _annotated;
  private BeanAttributes _beanAttributes;
  private boolean _isVeto;
  private CandiManager _cdiManager;

  public ProcessBeanAttributesImpl(CandiManager cdiManager,
                                   BeanAttributes beanAttributes,
                                   Annotated annotated)
  {
    _annotated = annotated;
    _beanAttributes = beanAttributes;
    _cdiManager = cdiManager;
  }

  @Override
  public void addDefinitionError(Throwable t)
  {
    _cdiManager.addDefinitionError(t);
  }

  @Override
  public Annotated getAnnotated()
  {
    return _annotated;
  }

  @Override
  public BeanAttributes<T> getBeanAttributes()
  {
    return _beanAttributes;
  }

  @Override
  public void setBeanAttributes(BeanAttributes<T> beanAttributes)
  {
    _beanAttributes = beanAttributes;
  }

  @Override
  public void veto()
  {
    _isVeto = true;
  }

  public boolean isVeto()
  {
    return _isVeto;
  }

  @Override
  public String toString()
  {
    return "ProcessBeanAttributesImpl[" + _beanAttributes + (_isVeto ?
      ", vetoed" :
      "") + ']';
  }
}
