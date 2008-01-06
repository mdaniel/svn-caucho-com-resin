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

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.config.ConfigException;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Configuration for a particular view.
 */
public class EjbStatelessHomeView extends EjbHomeView {
  private static final Logger log = Log.open(EjbStatelessHomeView.class);
  private static final L10N L = new L10N(EjbStatelessHomeView.class);

  /**
   * Creates a new entity bean configuration.
   */
  public EjbStatelessHomeView(EjbBean bean, ApiClass apiClass, String prefix)
    throws ConfigException
  {
    super(bean, apiClass, prefix);
  }

  /**
   * Introspects an ejb method.
   */
  protected EjbMethod introspectApiMethod(ApiMethod apiMethod)
    throws ConfigException
  {
    String methodName = apiMethod.getName();
    Class []paramTypes = apiMethod.getParameterTypes();

    if (methodName.equals("create") && paramTypes.length == 0) {
      return new EjbCreateMethod(this, apiMethod, null);
    }
    else if (methodName.startsWith("create")) {
      throw error(L.l("{0}: '{1}' forbidden in stateless session home.  The create() method for a stateless session bean must have zero arguments.",
                      apiMethod.getDeclaringClass().getName(),
                      getFullMethodName(apiMethod)));
      
    }
    else
      return super.introspectApiMethod(apiMethod);
  }
}
