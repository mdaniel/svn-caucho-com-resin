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

import com.caucho.java.JavaWriter;
import com.caucho.ejb.cfg.*;
import com.caucho.util.L10N;

import javax.ejb.*;
import java.io.IOException;

/**
 * Generates the skeleton for a stateful bean.
 */
public class StatefulGenerator extends SessionGenerator {
  private static final L10N L = new L10N(StatefulGenerator.class);

  public StatefulGenerator(String ejbName, ApiClass ejbClass)
  {
    super(ejbName, ejbClass);
  }

  public boolean isStateless()
  {
    return false;
  }

  protected View generateLocalView(ApiClass api)
  {
    return new StatefulLocalView(this, api);
  }

  /**
   * Generates the stateful session bean
   */
  @Override
  public void generate(JavaWriter out)
    throws IOException
  {
    generateTopComment(out);

    out.println();
    out.println("package " + getPackageName() + ";");

    out.println();
    out.println("import com.caucho.config.*;");
    out.println("import com.caucho.ejb.session.*;");
    out.println();
    out.println("import javax.ejb.*;");
    out.println("import javax.transaction.*;");
    
    out.println();
    out.println("public class " + getClassName());
    out.println("  extends StatefulContext");
    out.println("{");
    out.pushDepth();

    out.println();
    out.println("public " + getClassName() + "(StatefulServer server)");
    out.println("{");
    out.println("  super(server);");
    out.println("}");

    generateCreateProvider(out);
    generateViews(out);
    
    out.popDepth();
    out.println("}");
  }

  protected void generateCreateProvider(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public SessionProvider getProvider(Class api)");
    out.println("{");
    out.pushDepth();
    
    for (View view : getViews()) {
      StatefulLocalView sView = (StatefulLocalView) view;

      sView.generateCreateProvider(out, "api");
    }

    out.println();
    out.println("return super.getProvider(api);");

    out.popDepth();
    out.println("}");
  }

  protected void generateViews(JavaWriter out)
    throws IOException
  {
    for (View view : getViews()) {
      out.println();

      view.generate(out);
    }
  }

  protected void generateContext(JavaWriter out)
    throws IOException
  {
    String shortContextName = _contextClassName;
    int p = shortContextName.lastIndexOf('.');

    if (p > 0)
      shortContextName = shortContextName.substring(p + 1);

    out.println("protected static final java.util.logging.Logger __caucho_log = java.util.logging.Logger.getLogger(\"" + _contextClassName + "\");");
    out.println("protected static final boolean __caucho_isFiner = __caucho_log.isLoggable(java.util.logging.Level.FINER);");
    out.println();
    out.println("com.caucho.ejb.xa.EjbTransactionManager _xaManager;");
    out.println("private Bean _freeBean;");
    out.println();
    out.println("public " + shortContextName + "(com.caucho.ejb.session.SessionServer server)");
    out.println("{");
    out.println("  super(server);");
    out.println("  _xaManager = server.getTransactionManager();");
    out.println("}");

    out.println();
    out.println("Bean _ejb_begin(com.caucho.ejb.xa.TransactionContext trans)");
    out.println("  throws javax.ejb.EJBException");
    out.println("{");
    out.pushDepth();

    out.println("Bean bean;");
    out.println("synchronized (this) {");
    out.println("  bean = _freeBean;");
    out.println("  if (bean != null) {");
    out.println("    _freeBean = null;");
    if (SessionSynchronization.class.isAssignableFrom(_ejbClass.getJavaClass()))
      out.println("    trans.addSession(bean);");
    out.println("    return bean;");
    out.println("  }");
    out.println("}");
    out.println();

    out.println("throw new EJBException(\"session bean is not reentrant\");");
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
    out.println("  if (_freeBean == null) {");
    out.println("    _freeBean = bean;");
    out.println("    return;");
    out.println("  }");
    out.println("}");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public void destroy()");
    out.println("{");
    out.pushDepth();
    out.println("Bean ptr;");
    out.println("synchronized (this) {");
    out.println("  ptr = _freeBean;");
    out.println("  _freeBean = null;");
    out.println("}");

    if (hasMethod("ejbRemove", new Class[0])) {
      out.println();
      out.println("try {");
      out.println("  if (ptr != null)");
      // ejb/0fe0: ejbRemove() can be private, out.println("    ptr.ejbRemove();");
      out.println("    invokeMethod(ptr, \"ejbRemove\", new Class[] {}, new Object[] {});");
      out.println("} catch (Throwable e) {");
      out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
      out.println("}");
    }
    out.popDepth();
    out.println("}");

    generateInvokeMethod(out);
  }

  @Override
  protected void generateNewInstance(JavaWriter out, String suffix)
    throws IOException
  {
    // ejb/0g27
    /*
    if (_bean.getLocalHome() == null && _bean.getLocalList().size() == 0)
      return;
     */

    if (! isStateless()) {
      out.println();
      out.print("protected Object _caucho_newInstance" + suffix);
      out.println("(com.caucho.config.ConfigContext env)");
      out.println("{");
      out.pushDepth();

      out.println(_contextClassName + " cxt = new " + _contextClassName + "(_server);");

      // XXX TCK: bb/session/stateful/cm/allowed/afterBeginSetRollbackOnlyTest (infinite recursion issue)

      out.println("Bean bean = new Bean(cxt, env);");

      out.println("cxt._ejb_free(bean);");

      out.println();

      out.println();

      out.println("return cxt.createLocalObject" + suffix + "();");

      out.popDepth();
      out.println("}");
    }

    out.println();
    out.println("protected Object _caucho_newInstance" + suffix + "()");
    out.println("{");
    out.pushDepth();

    out.println(_contextClassName + " cxt = new " + _contextClassName + "(_server);");

    // XXX TCK: bb/session/stateful/cm/allowed/afterBeginSetRollbackOnlyTest (infinite recursion issue)

    out.println("Bean bean = new Bean(cxt, null);");

    out.println("cxt._ejb_free(bean);");

    out.println();

    out.println("return cxt.createLocalObject" + suffix + "();");

    out.popDepth();
    out.println("}");
  }
}
