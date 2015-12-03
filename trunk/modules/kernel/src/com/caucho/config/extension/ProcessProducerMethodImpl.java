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

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.ProcessProducerMethod;

import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.ProducesMethodBean;
import com.caucho.inject.Module;

@Module
public class ProcessProducerMethodImpl<X,T> extends ProcessBeanImpl<T>
  implements ProcessProducerMethod<X,T>
{
  private ProducesMethodBean<X,T> _bean;
  
  protected ProcessProducerMethodImpl(InjectManager manager, 
                                      ProducesMethodBean<X,T> bean)
  {
    super(manager, bean, bean.getProducesMethod());
    
    _bean = bean;
  }

  @Override
  public AnnotatedParameter<X> getAnnotatedDisposedParameter()
  {
    return (AnnotatedParameter<X>) _bean.getDisposedParameter();
  }

  @Override
  public AnnotatedMethod<X> getAnnotatedProducerMethod()
  {
    return (AnnotatedMethod<X>) _bean.getProducesMethod();
  }
}
