/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

import java.util.ArrayList;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ResourceProgramManager;
import com.caucho.inject.Module;

/**
 * SimpleBean represents a POJO Java bean registered as a CDI bean.
 */
@Module
public class InjectionTargetBuilderJavaee<X> extends InjectionTargetBuilder<X>
{
  public InjectionTargetBuilderJavaee(InjectManagerResin cdiManager,
                                      AnnotatedType<X> beanType,
                                      Bean<X> bean)
  {
    super(cdiManager, beanType, bean);
  }
  
  public InjectionTargetBuilderJavaee(InjectManagerResin cdiManager,
                                      AnnotatedType<X> beanType)
  {
    super(cdiManager, beanType);
  }
  
  protected InjectManagerResin getInjectManager()
  {
    return (InjectManagerResin) super.getInjectManager();
  }
  
  protected void introspectInject(ArrayList<ConfigProgram> injectProgramList)
  {
    super.introspectInject(injectProgramList);
    
    Class<?> rawType = getRawClass();

    if (rawType == null || Object.class.equals(rawType)) {
      return;
    }
    
    ResourceProgramManager resourceManager = getInjectManager().getResourceManager();
    
    resourceManager.buildInject(rawType, injectProgramList);
  }
  
  /*
  @Override
  protected boolean isStateful()
  {
    return getAnnotatedType().isAnnotationPresent(Stateful.class);
  }
  */
}
