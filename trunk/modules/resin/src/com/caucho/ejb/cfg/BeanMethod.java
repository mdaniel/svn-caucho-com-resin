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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Rodrigo Westrupp
 */

package com.caucho.ejb.cfg;

import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.inject.Module;

/**
 * Configuration for bean-method.
 */
@Module
public class BeanMethod {
  private String _methodName;
  private MethodParams _params;

  public BeanMethod()
  {
  }

  public String getMethodName()
  {
    return _methodName;
  }

  public void setMethodName(String methodName)
  {
    _methodName = methodName;
  }

  public void setMethodParams(MethodParams methodParams)
  {
    _params = methodParams;
  }

  public boolean isMatch(AnnotatedMethod<?> otherMethod)
  {
    if (! _methodName.equals(otherMethod.getJavaMember().getName()))
      return false;
    
    if (_params != null && ! _params.isMatch(otherMethod))
      return false;
    
    return true;
  }

  /*
  public boolean isMatch(Method otherMethod)
  {
    return _methodName.equals(otherMethod.getName());
  }
  */
  
  public MethodSignature getSignature()
  {
    MethodSignature sig = new MethodSignature();
    
    sig.setMethodName(_methodName);
    
    return sig;
  }
}
