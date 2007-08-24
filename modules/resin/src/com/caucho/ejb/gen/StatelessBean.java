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
import com.caucho.java.JavaWriter;
import com.caucho.ejb.cfg.*;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the skeleton for a session bean.
 */
public class StatelessBean extends SessionBean {
  private static final L10N L = new L10N(StatelessBean.class);

  public StatelessBean(EjbSessionBean bean,
                       JClass ejbClass,
                       String contextClassName)
  {
    super(bean, ejbClass, contextClassName);
  }

  protected void generateContext(JavaWriter out)
    throws IOException
  {
    String shortContextName = _contextClassName;
    int p = shortContextName.lastIndexOf('.');

    if (p > 0)
      shortContextName = shortContextName.substring(p + 1);

    int freeStackMax = 16;

    out.println("protected static final java.util.logging.Logger __caucho_log = com.caucho.log.Log.open(" + _contextClassName + ".class);");
    out.println();
    out.println("com.caucho.ejb.xa.EjbTransactionManager _xaManager;");

    out.println("private Bean []_freeBeanStack = new Bean[" + freeStackMax + "];");
    out.println("private int _freeBeanTop;");
    out.println();
    out.println("public " + shortContextName + "(com.caucho.ejb.session.StatelessServer server)");
    out.println("{");
    out.println("  super(server);");
    out.println("  _xaManager = server.getContainer().getTransactionManager();");
    out.println("}");

    out.println();
    out.println("Bean _ejb_begin(com.caucho.ejb.xa.TransactionContext trans)");
    out.println("  throws javax.ejb.EJBException");
    out.println("{");
    out.pushDepth();
    out.println("Bean bean;");
    out.println("synchronized (this) {");
    out.println("  if (_freeBeanTop > 0) {");
    out.println("    bean = _freeBeanStack[--_freeBeanTop];");
    out.println("    return bean;");
    out.println("  }");
    out.println("}");
    out.println();
    out.println("try {");
    out.println("  bean = new Bean(this);");

    if (hasMethod("ejbCreate", new JClass[0])) {
      // ejb/0fe0: ejbCreate can be private, out.println("  bean.ejbCreate();");
      out.println("  invokeMethod(bean, \"ejbCreate\", new Class[] {}, new Object[] {});");
    }

    out.println("  return bean;");
    out.println("} catch (Exception e) {");
    out.println("  throw com.caucho.ejb.EJBExceptionWrapper.create(e);");
    out.println("}");
    out.popDepth();
    out.println("}");

    out.println();
    out.println("void _ejb_free(Bean bean)");
    out.println("  throws javax.ejb.EJBException");
    out.println("{");
    out.pushDepth();
    out.println("if (bean == null)");
    out.println("  return;");
    out.println();
    out.println("synchronized (this) {");
    out.println("  if (_freeBeanTop < _freeBeanStack.length) {");
    out.println("    _freeBeanStack[_freeBeanTop++] = bean;");
    out.println("    return;");
    out.println("  }");
    out.println("}");

    if (hasMethod("ejbRemove", new JClass[0])) {
      out.println();
      // ejb/0fe0: ejbRemove() can be private, out.println("bean.ejbRemove();");
      out.println("invokeMethod(bean, \"ejbRemove\", new Class[] {}, new Object[] {});");
    }

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void destroy()");
    out.println("{");
    out.pushDepth();
    out.println("Bean ptr;");
    out.println("Bean []freeBeanStack;");
    out.println("int freeBeanTop;");

    out.println("synchronized (this) {");
    out.println("  freeBeanStack = _freeBeanStack;");
    out.println("  freeBeanTop = _freeBeanTop;");
    out.println("  _freeBeanStack = null;");
    out.println("  _freeBeanTop = 0;");
    out.println("}");

    if (hasMethod("ejbRemove", new JClass[0])) {
      out.println();
      out.println("for (int i = 0; i < freeBeanTop; i++) {");
      out.pushDepth();

      out.println("try {");
      out.println("  if (freeBeanStack[i] != null)");
      // ejb/0fe0: ejbRemove() can be private out.println("    freeBeanStack[i].ejbRemove();");
      out.println("    invokeMethod(freeBeanStack[i], \"ejbRemove\", new Class[] {}, new Object[] {});");
      out.println("} catch (Throwable e) {");
      out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
      out.println("}");

      out.popDepth();
      out.println("}");
    }
    out.popDepth();
    out.println("}");

    generateInvokeMethod(out);
  }

  protected void generateNewInstance(JavaWriter out)
    throws IOException
  {
  }
}
