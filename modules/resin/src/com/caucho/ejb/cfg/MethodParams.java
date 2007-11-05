/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.util.ArrayList;

/**
 * Configuration for method-params.
 */
public class MethodParams {
  private static final L10N L = new L10N(MethodParams.class);

  private ArrayList<Class> _methodParams
    = new ArrayList<Class>();

  public MethodParams()
  {
  }

  public void addMethodParam(Class methodParam)
  {
    _methodParams.add(methodParam);
  }

  public boolean isMatch(JMethod otherMethod)
  {
    JClass otherParams[] = otherMethod.getParameterTypes();

    if (otherParams.length != _methodParams.size())
      return false;

    int i = 0;

    for (Class thisParam : _methodParams) {
      if (! thisParam.equals(otherParams[i++].getJavaClass()))
        return false;
    }

    return true;
  }
}
