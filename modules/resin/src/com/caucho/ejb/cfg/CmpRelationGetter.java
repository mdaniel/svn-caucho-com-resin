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

import com.caucho.ejb.gen.CmpManyToOneGetter;
import com.caucho.ejb.gen.ViewClass;
import com.caucho.java.gen.BaseMethod;
import com.caucho.util.L10N;

/**
 * Configuration for a method of a view.
 */
public class CmpRelationGetter extends EjbMethod {
  private static final L10N L = new L10N(CmpRelationGetter.class);

  private CmpRelationRole _relationRole;
  
  /**
   * Creates a new method.
   *
   * @param view the owning view
   * @param apiMethod the method from the view
   * @param implMethod the method from the implementation
   */
  public CmpRelationGetter(EjbView view,
			   ApiMethod apiMethod, ApiMethod implMethod,
			   CmpRelationRole relationRole)
  {
    super(view, apiMethod, implMethod);

    _relationRole = relationRole;
  }

  /**
   * Assembles the method.
   */
  public BaseMethod assemble(ViewClass viewAssembler, String fullClassName)
  {
    BaseMethod method;

    method = new CmpManyToOneGetter(getApiMethod(),
				    getView().getImplClass().getName());
    
    method.setCall(assembleCallChain(method.getCall()));
    
    return method;
  }
}
