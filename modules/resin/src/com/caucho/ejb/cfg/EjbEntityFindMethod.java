/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.ejb.gen.*;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.util.L10N;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

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
			     ApiMethod apiMethod,
			     ApiMethod implMethod)
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
  public EjbEntityFindMethod(EjbView view, ApiMethod apiMethod)
  {
    super(view, apiMethod, null);
  }

  /**
   * Assembles the method.
   */
  public BaseMethod assemble(ViewClass viewAssembler, String fullClassName)
  {
    if (((EjbEntityBean) getView().getBean()).isCMP()
	&& (getImplMethod() == null
	    || getImplMethod().isAbstract())) {
      return new AmberFindMethod(getApiMethod(),
				 fullClassName,
				 getViewPrefix());
    }
    else if (((EjbEntityBean) getView().getBean()).isCMP1()
	     && (getImplMethod() == null
		 || getImplMethod().isAbstract())) {
      return new CMP10FindMethod(getApiMethod(),
				 fullClassName,
				 getViewPrefix());
    }
    else {
      BaseMethod method;

      Class apiReturnType = getApiMethod().getReturnType();
      
      if (Collection.class.isAssignableFrom(apiReturnType)
	  || Enumeration.class.isAssignableFrom(apiReturnType)
	  || Iterator.class.isAssignableFrom(apiReturnType))
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
