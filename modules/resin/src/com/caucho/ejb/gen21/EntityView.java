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

package com.caucho.ejb.gen21;

import com.caucho.ejb.gen.*;
import com.caucho.ejb.gen21.EntityPoolChain;
import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the skeleton for a session view.
 */
public class EntityView extends ViewClass {
  private static L10N L = new L10N(SessionView.class);

  private ApiClass _remoteClass;
  private String _prefix;
  private String _contextClassName;

  public EntityView(ApiClass remoteClass,
                    String contextClassName,
                    String prefix)
  {
    super(prefix, "EntityObject");

    addInterfaceName(remoteClass.getName());

    _contextClassName = contextClassName;
    _prefix = prefix;

    setStatic(true);
  }

  /**
   * Adds the pool chaining.
   */
  public CallChain createPoolChain(CallChain call, BaseMethod method)
  {
    return new EntityPoolChain(call, false);
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
  }

  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    out.println("private " + _contextClassName + " _context;");
    out.println("private EjbTransactionManager _xaManager;");

    out.println();
    out.println(_prefix + "(" + _contextClassName + " context)");
    out.println("{");
    out.println("  _context = context;");
    out.println("  _xaManager = context.getEntityServer().getTransactionManager();");
    out.println("}");

    out.println();
    out.println("public QEntityContext getEntityContext()");
    out.println("{");
    out.println("  return _context;");
    out.println("}");

    generateGetBean(out);

    generateComponents(out);
  }

  /**
   * Generates the get bean code, which is required when the Local getters
   * aren't expoxed.
   */
  protected void generateGetBean(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public Object _caucho_getBean(com.caucho.ejb.xa.TransactionContext trans, boolean doLoad)");
    out.println("{");
    out.pushDepth();

    out.println("return _context._ejb_begin(trans, false, true);");

    out.popDepth();
    out.println("}");
    out.println();

    out.println("public Object _caucho_getBean()");
    out.println("{");
    out.pushDepth();

    out.println("com.caucho.ejb.xa.TransactionContext trans;");
    out.println("trans = _context._server.getTransactionManager().getTransactionContext();");
    out.println("return _context._ejb_begin(trans, false, false);");

    out.popDepth();
    out.println("}");
  }
}
