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

package com.caucho.ejb.gen;

import java.io.IOException;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;

/**
 * Generates the skeleton for a session view.
 */
public class SessionHomeView extends ViewClass {
  private static L10N L = new L10N(SessionHomeView.class);

  private JClass _remoteClass;
  private String _prefix;
  private String _contextClassName;
  private boolean _isStateless;

  public SessionHomeView(JClass remoteClass,
			 String contextClassName,
			 String prefix,
			 boolean isStateless)
  {
    super(prefix, isStateless ? "StatelessHome" : "SessionHome");

    addInterfaceName(remoteClass.getName());

    _contextClassName = contextClassName;
    _prefix = prefix;
    _isStateless = isStateless;

    setStatic(true);
  }

  /**
   * Adds a business method.
   */
  public void addMethod(BaseMethod method)
  {
    addComponent(method);
  }

  /**
   * Adds the pool chaining.
   */
  public BaseMethod createCreateMethod(JMethod apiMethod,
				       JMethod implMethod,
				       String fullClassName,
				       String prefix)
  {
    if (_isStateless)
      return new StatelessCreateMethod(apiMethod,
				       fullClassName,
				       prefix);
    else
      return new SessionCreateMethod(apiMethod,
				     implMethod,
				     fullClassName,
				     prefix);
  }

  /**
   * Adds the pool chaining.
   */
  public CallChain createPoolChain(CallChain call)
  {
    return call;
  }

  public void generate(JavaWriter out)
    throws IOException
  {
    generateGetter(out);

    out.println();
    super.generate(out);
  }

  private void generateGetter(JavaWriter out)
    throws IOException
  {
    if (_prefix.equals("RemoteHome")) {
      out.println();
      out.println("public EJBHome createRemoteHomeView()");
      out.println("{");
      out.println("  return new RemoteHome(this);");
      out.println("}");
      out.println();
    }
    else {
      out.println();
      out.println("public EJBLocalHome createLocalHome()");
      out.println("{");
      out.println("  return new LocalHome(this);");
      out.println("}");
      out.println();
    }
  }
	   
  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    out.println("private " + _contextClassName + " _context;");
    out.println("private EjbTransactionManager _xaManager;");
    out.println();
    out.println(_prefix + "(" + _contextClassName + " context)");
    out.println("{");
    if (_isStateless)
      out.println("  super(context.getStatelessServer());");
    else
      out.println("  super(context.getSessionServer());");

    out.println("  _context = context;");
    out.println("  _xaManager = _server.getTransactionManager();");
    out.println("}");
    out.println();
    out.println("public AbstractContext getContext()");
    out.println("{");
    out.println("  return _context;");
    out.println("}");

    out.println();
    
    generateComponents(out);
  }
}
