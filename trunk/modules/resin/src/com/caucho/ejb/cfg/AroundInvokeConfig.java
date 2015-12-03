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

import java.lang.reflect.Method;

import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.util.L10N;

/**
 * Configuration for around-invoke.
 */
public class AroundInvokeConfig {
  @SuppressWarnings("unused")
  private static final L10N L = new L10N(AroundInvokeConfig.class);

  private String _className;
  private String _methodName;

  public AroundInvokeConfig()
  {
  }

  public String getClassName()
  {
    return _className;
  }

  public String getMethodName()
  {
    return _methodName;
  }

  public void setClass(String className)
  {
    _className = className;
  }

  public void setMethodName(String methodName)
  {
    _methodName = methodName;
  }
  
  public boolean isMatch(AnnotatedMethod<?> method)
  {
    if (! method.getJavaMember().getName().equals(_methodName))
      return false;
    else if (method.getDeclaringType().getJavaClass().getName().equals(_className)
             || _className == null)
      return true;
    else
      return false;
  }
  
  public boolean isMatch(Method method)
  {
    if (! method.getName().equals(_methodName))
      return false;
    else if (method.getDeclaringClass().getName().equals(_className)
             || _className == null)
      return true;
    else
      return false;
  }
}
