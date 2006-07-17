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

package com.caucho.ejb.cfg;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.ejb.CreateException;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.java.gen.BaseClass;

import com.caucho.ejb.gen.BeanAssembler;

/**
 * Configuration for a particular view.
 */
public class EjbSessionHomeView extends EjbHomeView {
  private static final Logger log = Log.open(EjbSessionHomeView.class);
  private static final L10N L = new L10N(EjbSessionHomeView.class);

  /**
   * Creates a new entity bean configuration.
   */
  public EjbSessionHomeView(EjbBean bean, JClass apiClass, String prefix)
    throws ConfigException
  {
    super(bean, apiClass, prefix);
  }

  /**
   * Introspects an ejb method.
   */
  protected EjbMethod introspectEJBMethod(JMethod method)
    throws ConfigException
  {
    JClass apiClass = getApiClass();
    String methodName = method.getName();
    JClass []paramTypes = method.getParameterTypes();

    if (methodName.startsWith("ejbCreate")) {
      String createName = "c" + methodName.substring(4);
      
      JMethod apiMethod = EjbBean.getMethod(apiClass,
					    createName,
					   paramTypes);

      /*
      if (apiMethod == null)
	throw errorMissingMethod(apiClass, createName, method);
      */
      if (apiMethod == null) {
	log.config(errorMissingMethod(apiClass, createName, method).getMessage());
	return null;
      }

      validateException(apiMethod, CreateException.class);      

      return new EjbCreateMethod(this, apiMethod, method);
    }
    
    if (! methodName.startsWith("ejb"))
      return null;
    else if (methodName.equals("ejbRemove"))
      return null;
    else if (methodName.startsWith("ejbCreate"))
      return null;
    else if (methodName.startsWith("ejbPostCreate"))
      return null;
    else if (methodName.startsWith("ejbActivate"))
      return null;
    else if (methodName.startsWith("ejbPassivate"))
      return null;
    // XXX: check
    else if (methodName.startsWith("ejbDestroy"))
      return null;
    else
      throw error(L.l("{0}: '{1}' must not start with 'ejb'. The EJB spec reserves all methods starting with ejb.",
                      method.getDeclaringClass().getName(),
                      getFullMethodName(method)));
  }

  /**
   * Introspects a method in the view api which does not exist in
   * implementation bean.
   */
  protected EjbMethod introspectApiMethod(JMethod apiMethod)
    throws ConfigException
  {
    String apiName = apiMethod.getName();

    if (apiName.equals("create"))
      throw errorMissingMethod(getImplClass(), "ejbCreate", apiMethod);
    else
      return super.introspectApiMethod(apiMethod);
  }
}
