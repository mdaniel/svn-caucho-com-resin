/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import com.caucho.config.ConfigException;
import com.caucho.config.gen.ApiMethod;
import com.caucho.util.L10N;

import java.lang.reflect.*;

/**
 * Configuration for bean-method.
 */
public class BeanMethod {
  private static final L10N L = new L10N(BeanMethod.class);

  private String _methodName;

  private MethodParams _methodParams;

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
    _methodParams = methodParams;
  }

  public boolean isMatch(ApiMethod otherMethod)
  {
    return _methodName.equals(otherMethod.getName());
  }

  public boolean isMatch(Method otherMethod)
  {
    return _methodName.equals(otherMethod.getName());
  }
}
