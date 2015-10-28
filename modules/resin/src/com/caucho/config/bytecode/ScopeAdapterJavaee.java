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

package com.caucho.config.bytecode;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.bytecode.ScopeAdapter;
import com.caucho.v5.inject.Module;

/**
 * Scope adapting
 */
@Module
public class ScopeAdapterJavaee extends ScopeAdapter
{
  private static final Logger log
    = Logger.getLogger(ScopeAdapterJavaee.class.getName());
  
  protected ScopeAdapterJavaee(Class<?> beanClass, Class<?> cl, Class<?> []types)
  {
    super(beanClass, cl, types);
  }
  
  /*
  protected boolean isRemoveMethod(Class<?> beanClass, Method method)
  {
    if (method.isAnnotationPresent(Remove.class)) {
      return true;
    }
    
    try {
      Method beanMethod = beanClass.getMethod(method.getName(), method.getParameterTypes());
      
      return beanMethod.isAnnotationPresent(Remove.class);
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
      
      return false;
    }
  }
  */
}
