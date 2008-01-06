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

package com.caucho.ejb.gen;

import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.MethodCallChain;
import com.caucho.util.L10N;

import java.io.IOException;
import java.lang.reflect.*;

/**
 * Generates the skeleton for the find method.
 */
public class EntityFindMethod extends BaseMethod {
  private static L10N L = new L10N(EntityFindMethod.class);

  private ApiMethod _apiMethod;
  private String _contextClassName;
  private String _prefix;
  
  public EntityFindMethod(ApiMethod apiMethod,
			  ApiMethod implMethod,
			  String contextClassName,
			  String prefix)
  {
    super(apiMethod.getMethod(),
	  (implMethod != null
	   ? new MethodCallChain(implMethod.getMethod())
	   : null));

    _apiMethod = apiMethod;
    _contextClassName = contextClassName;
    _prefix = prefix;
  }

  /**
   * Gets the parameter types
   */
  public Class []getParameterTypes()
  {
    return _apiMethod.getParameterTypes();
  }

  /**
   * Gets the return type.
   */
  public Class getReturnType()
  {
    return _apiMethod.getReturnType();
  }

  /**
   * Prints the create method
   *
   * @param method the create method
   */
  public void generateCall(JavaWriter out, String []args)
    throws IOException
  {
    Class keyType;

    if (getCall() != null) {
      keyType = getCall().getReturnType();
      out.printClass(keyType);
      out.print(" key;");
      getCall().generateCall(out, "key", "bean", args);
    }
    else {
      keyType = getParameterTypes()[0];
      out.printClass(keyType);
      out.print(" key;");
      out.println("key = " + args[0] + ";");
    }
    
    out.println();

    if ("long".equals(keyType.getName()))
      out.println("Object okey = new Integer(key);");
    else if ("int".equals(keyType.getName()))
      out.println("Object okey = new Integer(key);");
    else
      out.println("Object okey = key;");

    out.print("return (" + getReturnType().getName() + ") ");
    out.print("_server.getContext(okey, false)");

    if ("RemoteHome".equals(_prefix))
      out.println(".getEJBObject();");
    else if ("LocalHome".equals(_prefix))
      out.println(".getEJBLocalObject();");
    else
      throw new IOException(L.l("trying to create unknown type {0}", _prefix));
  }
}
