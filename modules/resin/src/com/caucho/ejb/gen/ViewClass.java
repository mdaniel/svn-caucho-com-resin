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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.gen;

import java.io.IOException;

import com.caucho.bytecode.JMethod;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

import com.caucho.java.gen.BaseClass;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;

import com.caucho.ejb.cfg.EjbMethod;
import com.caucho.ejb.cfg.EjbEntityBean;

/**
 * Generates the skeleton for a session view.
 */
public class ViewClass extends BaseClass {
  private static L10N L = new L10N(ViewClass.class);

  public ViewClass(String className, String superClassName)
  {
    super(className, superClassName);
  }

  /**
   * Adds the pool chaining.
   */
  public BaseMethod createCreateMethod(JMethod apiMethod,
				       JMethod implMethod,
				       String fullClassName,
				       String viewPrefix)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public BaseMethod createCreateMethod(EjbEntityBean bean,
				       JMethod api,
				       JMethod create,
				       JMethod postCreate,
				       String fullClassName)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Creates a business method.
   */
  public BaseMethod createBusinessMethod(EjbMethod ejbMethod)
  {
    BaseMethod method = new BaseMethod(ejbMethod.getApiMethod(),
				       ejbMethod.getImplMethod());

    method.setCall(createPoolChain(method.getCall()));
		    
    return method;
  }

  /**
   * Adds the pool chaining.
   */
  public CallChain createPoolChain(CallChain call)
  {
    return call;
  }
}
