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

package com.caucho.config.extension;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.ProcessBean;

import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;

@Module
public class ProcessBeanImpl<X> implements ProcessBean<X>
{
  private InjectManager _cdiManager;
  private Bean<X> _bean;
  private Annotated _annotated;
  private boolean _isVeto;

  public ProcessBeanImpl(InjectManager manager, 
                         Bean<X> bean,
                         Annotated annotated)
  {
    _cdiManager = manager;
    _bean = bean;

    if (annotated == null)
      throw new NullPointerException();

    _annotated = annotated;
  }

  public InjectManager getManager()
  {
    return _cdiManager;
  }

  @Override
  public Annotated getAnnotated()
  {
    return _annotated;
  }

  @Override
  public Bean<X> getBean()
  {
    return _bean;
  }
    
  public void setBean(Bean<X> bean)
  {
    _bean = bean;
  }

  @Override
  public void addDefinitionError(Throwable t)
  {
    _cdiManager.addDefinitionError(t);
  }

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
    return getClass().getSimpleName() + "[" + _bean + "]";
  }
}
