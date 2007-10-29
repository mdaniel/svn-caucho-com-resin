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

package com.caucho.ejb.gen;

import com.caucho.bytecode.JClass;
import com.caucho.ejb.cfg.EjbBean;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Generates the skeleton for a session view.
 */
public class SessionView extends ViewClass {
  private static L10N L = new L10N(SessionView.class);

  private EjbBean _bean;
  private ArrayList<JClass> _apiList;
  private String _prefix;
  private String _contextClassName;
  private boolean _isStateless;

  public SessionView(EjbBean bean, ArrayList<JClass> apiList,
                     String contextClassName,
                     String prefix,
                     boolean isStateless)
  {
    super(prefix, isStateless ? "StatelessObject" : "SessionObject");

    _bean = bean;

    for (JClass api : apiList)
      addInterfaceName(api.getName());

    _apiList = apiList;

    _contextClassName = contextClassName;
    _prefix = prefix;
    _isStateless = isStateless;

    setStatic(true);
  }

  /**
   * Adds the pool chaining.
   */
  public CallChain createPoolChain(CallChain call, BaseMethod method)
  {
    if (_isStateless)
      return new StatelessPoolChain(_bean, call, method);
    else
      return new SessionPoolChain(_bean, call, method);
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
    out.println("private " + _prefix + " _view" + _prefix + ";");

    out.println();
    if (_prefix.equals("Local"))
      out.println("public EJBLocalObject getEJBLocalObject()");
    else
      out.println("public EJBObject getRemoteView()");

    out.println("{");
    out.println("  if (_view" + _prefix + " == null)");
    out.println("    _view" + _prefix + " = new " + _prefix + "(this);");

    out.println();
    out.println("  return _view" + _prefix + ";");
    out.println("}");

    if (_prefix.equals("Local")) {
      out.println();
      out.println("public EJBLocalObject createLocalObject()");
      out.println("{");
      out.println("  return new Local(this);");
      out.println("}");
    }
  }

  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    out.println("private final " + _contextClassName + " _context;");
    out.println("private final EjbTransactionManager _xaManager;");

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
    out.println("private " + _contextClassName + " getContext()");
    out.println("{");
    out.println("  return _context;");
    out.println("}");

    if (! _isStateless) {
      out.println();
      out.println("public String __caucho_getId()");
      out.println("{");
      out.println("  return _context.getPrimaryKey();");
      out.println("}");
    }

    out.println();
    generateComponents(out);
  }
}
