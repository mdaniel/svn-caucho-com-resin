/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.cfg;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;

import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.gen.ViewClass;
import com.caucho.ejb.gen.AmberFindMethod;
import com.caucho.ejb.gen.EntityFindMethod;
import com.caucho.ejb.gen.EntityFindCollectionMethod;
import com.caucho.ejb.gen.EntityHomeSync;
import com.caucho.ejb.gen.EntityHomePoolChain;

/**
 * Configuration for a method of a view.
 */
public class EjbEntityFindMethod extends EjbMethod {
  private static final L10N L = new L10N(EjbEntityFindMethod.class);

  /**
   * Creates a new method.
   *
   * @param view the owning view
   * @param apiMethod the method from the view
   * @param implMethod the method from the implementation
   */
  public EjbEntityFindMethod(EjbView view,
			     JMethod apiMethod,
			     JMethod implMethod)
  {
    super(view, apiMethod, implMethod);
  }

  /**
   * Creates a new method.
   *
   * @param view the owning view
   * @param apiMethod the method from the view
   * @param implMethod the method from the implementation
   */
  public EjbEntityFindMethod(EjbView view, JMethod apiMethod)
  {
    super(view, apiMethod, null);
  }

  /**
   * Assembles the method.
   */
  public BaseMethod assemble(ViewClass viewAssembler, String fullClassName)
  {
    if (((EjbEntityBean) getView().getBean()).isCMP() &&
	(getImplMethod() == null ||
	 getImplMethod().isAbstract())) {
      return new AmberFindMethod(getApiMethod(),
				 fullClassName,
				 getViewPrefix());
    }
    else {
      BaseMethod method;

      JClass apiReturnType = getApiMethod().getReturnType();
      
      if (apiReturnType.isAssignableTo(Collection.class) ||
	  apiReturnType.isAssignableTo(Enumeration.class) ||
	  apiReturnType.isAssignableTo(Iterator.class))
	method = new EntityFindCollectionMethod(getApiMethod(),
						getImplMethod(),
						fullClassName,
						getViewPrefix());
      else
	method = new EntityFindMethod(getApiMethod(),
				      getImplMethod(),
				      fullClassName,
				      getViewPrefix());

      CallChain call = method.getCall();

      if (call != null) {
	call = new EntityHomeSync(call);
	call = new EntityHomePoolChain(call);

	method.setCall(assembleCallChain(call));
      }

      return method;
    }
  }
}
