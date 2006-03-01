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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.cfg;

import com.caucho.bytecode.JMethod;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.java.gen.BaseMethod;

import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.gen.ViewClass;
import com.caucho.ejb.gen.EntityCreateMethod;
import com.caucho.ejb.gen.AmberCreateMethod;

/**
 * Configuration for a method of a view.
 */
public class EjbEntityCreateMethod extends EjbMethod {
  private static final L10N L = new L10N(EjbEntityCreateMethod.class);

  private JMethod _createMethod;
  private JMethod _postCreateMethod;
  
  /**
   * Creates a new method.
   *
   * @param view the owning view
   * @param apiMethod the method from the view
   * @param implMethod the method from the implementation
   */
  public EjbEntityCreateMethod(EjbView view,
			       JMethod apiMethod,
			       JMethod createMethod,
			       JMethod postCreateMethod)
  {
    super(view, apiMethod, null);

    _createMethod = createMethod;
    _postCreateMethod = postCreateMethod;
  }

  /**
   * Assembles the method.
   */
  public BaseMethod assemble(ViewClass viewAssembler, String fullClassName)
  {
    BaseMethod method;

    method = viewAssembler.createCreateMethod((EjbEntityBean) getView().getBean(),
					      getApiMethod(),
					      _createMethod,
					      _postCreateMethod,
					      fullClassName);
    
    method.setCall(assembleCallChain(method.getCall()));

    return method;
  }
}
