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

import java.io.IOException;
import javax.ejb.*;

/**
 * Generates the skeleton for a session bean.
 */
public class StatelessGenerator extends SessionGenerator {
  private static final L10N L = new L10N(StatelessGenerator.class);

  public StatelessGenerator(String ejbName, ApiClass ejbClass)
  {
    super(ejbName, ejbClass);
  }

  public boolean isStateless()
  {
    return true;
  }

  @Override
  protected View createLocalView(ApiClass api)
  {
    return new StatelessLocalView(this, api);
  }

  @Override
  protected View createLocalHomeView(ApiClass api)
  {
    return new StatelessLocalHomeView(this, api);
  }

  @Override
  protected View createRemoteView(ApiClass api)
  {
    return new StatelessRemoteView(this, api);
  }

  @Override
  protected View createRemoteHomeView(ApiClass api)
  {
    return new StatelessRemoteHomeView(this, api);
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
    out.println("  extends StatelessContext");
    out.println("{");
    out.pushDepth();

    generateContext(out);

    for (View view : getViews()) {
      view.generateContextPrologue(out);
    }
    
    generateCreateProvider(out);
    
    generateViews(out);
    
    out.popDepth();
    out.println("}");
  }

  protected void generateCreateProvider(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public StatelessProvider getProvider(Class api)");
    out.println("{");
    out.pushDepth();
    
    for (View view : getViews()) {
      StatelessView sView = (StatelessView) view;

      sView.generateCreateProvider(out, "api");
    }

    out.println();
    out.println("return super.getProvider(api);");

    out.popDepth();
    out.println("}");
  }

  public void generateViews(JavaWriter out)
    throws IOException
  {
    for (View view : getViews()) {
      out.println();

      view.generate(out);
    }
  }
  
  @Override
  protected void generateContext(JavaWriter out)
    throws IOException
  {
    String shortContextName = getEjbClass().getSimpleName();

    int freeStackMax = 16;

    out.println("protected static final java.util.logging.Logger __caucho_log = java.util.logging.Logger.getLogger(\"" + getFullClassName() + "\");");
    out.println("protected static final boolean __caucho_isFiner = __caucho_log.isLoggable(java.util.logging.Level.FINER);");
    out.println();
    out.println("com.caucho.ejb.xa.EjbTransactionManager _xaManager;");

    String beanClass = getEjbClass().getName();

    out.println("private " + beanClass + " []_freeBeanStack = new "
		+ beanClass + "[" + freeStackMax + "];");
    out.println("private int _freeBeanTop;");
    out.println();
    out.println("public " + getClassName() + "(StatelessServer server)");
    out.println("{");
    out.pushDepth();
    
    out.println("super(server);");
    out.println("_xaManager = server.getTransactionManager();");

    for (View view : getViews()) {
      view.generateContextHomeConstructor(out);
    }
    
    out.popDepth();
    out.println("}");

    out.println();
    out.println(beanClass + " _ejb_begin()");
    out.println("{");
    out.pushDepth();
    out.println(beanClass + " bean;");
    out.println("synchronized (this) {");
    out.println("  if (_freeBeanTop > 0) {");
    out.println("    bean = _freeBeanStack[--_freeBeanTop];");
    out.println("    return bean;");
    out.println("  }");
    out.println("}");
    out.println();
    out.println("try {");
    out.println("  bean = new " + beanClass + "();");

    if (hasMethod("setSessionContext", new Class[] { SessionContext.class })) {
      out.println("  bean.setSessionContext(this);");
    }

    if (hasMethod("ejbCreate", new Class[0])) {
      // ejb/0fe0: ejbCreate can be private, out.println("  bean.ejbCreate();");
      out.println("  bean.ejbCreate();");
      
      // out.println("  invokeMethod(bean, \"ejbCreate\", new Class[] {}, new Object[] {});");
    }
    
    out.println("  getStatelessServer().initInstance(bean);");

    out.println("  return bean;");
    out.println("} catch (Exception e) {");
    out.println("  throw com.caucho.ejb.EJBExceptionWrapper.create(e);");
    out.println("}");
    out.popDepth();
    out.println("}");

    out.println();
    out.println("void _ejb_free(" + beanClass + " bean)");
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

    /*
    if (hasMethod("ejbRemove", new Class[0])) {
      out.println();
      // ejb/0fe0: ejbRemove() can be private, out.println("bean.ejbRemove();");
      out.println("invokeMethod(bean, \"ejbRemove\", new Class[] {}, new Object[] {});");
    }
    */

    out.popDepth();
    out.println("}");

    

    out.println();
    out.println("public void destroy()");
    out.println("{");
    out.pushDepth();
    out.println(beanClass + " ptr;");
    out.println(beanClass + " []freeBeanStack;");
    out.println("int freeBeanTop;");

    out.println("synchronized (this) {");
    out.println("  freeBeanStack = _freeBeanStack;");
    out.println("  freeBeanTop = _freeBeanTop;");
    out.println("  _freeBeanStack = null;");
    out.println("  _freeBeanTop = 0;");
    out.println("}");

    if (hasMethod("ejbRemove", new Class[0])) {
      out.println();
      out.println("for (int i = 0; i < freeBeanTop; i++) {");
      out.pushDepth();

      out.println("try {");
      out.println("  if (freeBeanStack[i] != null)");
      // ejb/0fe0: ejbRemove() can be private out.println("    freeBeanStack[i].ejbRemove();");
      out.println("    freeBeanStack[i].ejbRemove();");
      out.println("} catch (Throwable e) {");
      out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
      out.println("}");

      out.popDepth();
      out.println("}");
    }
    
    out.popDepth();
    out.println("}");
  }

  protected void generateNewInstance(JavaWriter out)
    throws IOException
  {
  }
}
