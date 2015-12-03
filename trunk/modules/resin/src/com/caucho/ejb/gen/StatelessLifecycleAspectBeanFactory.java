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

package com.caucho.ejb.gen;

import java.io.IOException;
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

import com.caucho.config.gen.AspectBeanFactory;
import com.caucho.config.gen.AspectFactory;
import com.caucho.config.gen.AspectGenerator;
import com.caucho.config.gen.AsynchronousFactory;
import com.caucho.config.gen.CandiAspectBeanFactory;
import com.caucho.config.gen.InterceptorFactory;
import com.caucho.config.gen.LifecycleAspectBeanFactory;
import com.caucho.config.gen.LockFactory;
import com.caucho.config.gen.SecurityFactory;
import com.caucho.config.gen.XaFactory;
import com.caucho.config.inject.InjectManager;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents a stateless local business method
 */
@Module
public class StatelessLifecycleAspectBeanFactory<X>
  extends LifecycleAspectBeanFactory<X>
{
  public StatelessLifecycleAspectBeanFactory(AspectBeanFactory<X> next,
                                    InjectManager manager,
                                    AnnotatedType<X> beanType)
                                    
  {
    super(next, manager, beanType);
  }

  @Override
  public String getInterceptorInstance()
  {
    return "_statelessPool.getLifecycleInstance()";
  }
  
  /**
   * Generates data associated with the bean
   */
  @Override
  public String getBeanInfo()
  {
    return "_statelessPool.getLifecycleItem()";
  }
}
