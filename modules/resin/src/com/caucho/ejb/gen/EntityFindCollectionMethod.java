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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.gen;

import java.io.IOException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.MethodCallChain;

/**
 * Generates the skeleton for the find method.
 */
public class EntityFindCollectionMethod extends BaseMethod {
  private static L10N L = new L10N(EntityFindCollectionMethod.class);

  private JMethod _apiMethod;
  private String _contextClassName;
  private String _prefix;
  
  public EntityFindCollectionMethod(JMethod apiMethod,
				    JMethod implMethod,
				    String contextClassName,
				    String prefix)
  {
    super(apiMethod,
	  implMethod != null ? new MethodCallChain(implMethod) : null);

    _apiMethod = apiMethod;
    _contextClassName = contextClassName;
    _prefix = prefix;
  }

  /**
   * Gets the parameter types
   */
  public JClass []getParameterTypes()
  {
    return _apiMethod.getParameterTypes();
  }

  /**
   * Gets the return type.
   */
  public JClass getReturnType()
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
    out.println(getReturnType().getName() + " keys;");

    getCall().generateCall(out, "keys", "bean", args);
    
    out.println();

    out.println("java.util.ArrayList values = new java.util.ArrayList();");

    JClass retType = getReturnType();
    if (retType.isAssignableTo(Collection.class)) {
      out.println("java.util.Iterator iter = keys.iterator();");
      out.println("while (iter.hasNext()) {");
      out.pushDepth();
      out.println("Object key = iter.next();");
    } else if (retType.isAssignableTo(Iterator.class)) {
      out.println("while (keys.hasNext()) {");
      out.pushDepth();
      out.println("Object key = keys.next();");
    } else if (retType.isAssignableTo(Enumeration.class)) {
      out.println("while (keys.hasMoreElements()) {");
      out.pushDepth();
      out.println("Object key = keys.nextElement();");
    }

    out.print("values.add(_server.getContext(key, false)");

    if ("RemoteHome".equals(_prefix))
      out.print(".getEJBObject());");
    else if ("LocalHome".equals(_prefix))
      out.print(".getEJBLocalObject());");
    else
      throw new IOException(L.l("trying to create unknown type {0}", _prefix));

    out.popDepth();
    out.println("}");

    if (retType.isAssignableTo(Collection.class)) {
      out.println("return values;");
    } else if (retType.isAssignableTo(Iterator.class)) {
      out.println("return values.iterator();");
    } else if (retType.isAssignableTo(Enumeration.class)) {
      out.println("return java.util.Collections.enumeration(values);");
    }
  }
}
