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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.gen;

import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the skeleton for the create method.
 */
public class SessionCreateMethod extends BaseMethod {
  private static final L10N L = new L10N(StatelessCreateMethod.class);

  private ApiMethod _method;
  private String _contextClassName;
  private String _prefix;

  public SessionCreateMethod(ApiMethod apiMethod,
                             ApiMethod implMethod,
                             String contextClassName,
                             String prefix)
  {
    super(apiMethod.getMethod(), implMethod.getMethod());

    _method = apiMethod;
    _contextClassName = contextClassName;
    _prefix = prefix;
  }

  /**
   * Gets the parameter types
   */
  public Class []getParameterTypes()
  {
    return _method.getParameterTypes();
  }

  /**
   * Gets the return type.
   */
  public Class getReturnType()
  {
    return _method.getReturnType();
  }

  /**
   * Prints the create method
   *
   * @param method the create method
   */
  public void generateCall(JavaWriter out, String []args)
    throws IOException
  {
    out.println("Thread thread = Thread.currentThread();");
    out.println("ClassLoader oldLoader = thread.getContextClassLoader();");
    out.println();
    out.println("try {");
    out.pushDepth();
    out.println("thread.setContextClassLoader(_server.getClassLoader());");
    out.println();

    out.println(_contextClassName + " cxt = new " + _contextClassName + "(_server);");

    out.println("Bean bean = new Bean(cxt, null);");

    getCall().generateCall(out, null, "bean", args);

    out.println("cxt._ejb_free(bean);");

    out.println();
    out.println("_server.createSessionKey(cxt);");

    Class retType = getReturnType();

    // ejb/02j4
    if (! retType.getName().equals("void"))
      out.print("return (" + retType.getName() + ") ");

    if ("RemoteHome".equals(_prefix))
      out.println("cxt.getRemoteView();");
    else if ("LocalHome".equals(_prefix))
      out.println("cxt.getEJBLocalObject();");
    else
      throw new IOException(L.l("trying to create unknown type {0}",
                                _prefix));

    out.popDepth();
    out.println("} finally {");
    out.println("  thread.setContextClassLoader(oldLoader);");
    out.println("}");
  }
}
