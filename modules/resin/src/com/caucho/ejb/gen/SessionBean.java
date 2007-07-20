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
import com.caucho.bytecode.JClassLoader;
import com.caucho.ejb.cfg.EjbSessionBean;
import com.caucho.ejb.cfg.Interceptor;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.ClassComponent;
import com.caucho.util.L10N;

import javax.ejb.SessionContext;
import java.io.IOException;

/**
 * Generates the skeleton for a session bean.
 */
public class SessionBean extends ClassComponent {
  private static final L10N L = new L10N(SessionBean.class);

  private EjbSessionBean _bean;
  private JClass _ejbClass;
  protected String _contextClassName;

  public SessionBean(EjbSessionBean bean,
                     JClass ejbClass,
                     String contextClassName)
  {
    _bean = bean;
    _ejbClass = ejbClass;
    _contextClassName = contextClassName;
  }

  public void generate(JavaWriter out)
    throws IOException
  {
    generateContext(out);

    generateNewInstance(out);
    generateNewRemoteInstance(out);

    generateBean(out);
  }

  protected void generateContext(JavaWriter out)
    throws IOException
  {
    String shortContextName = _contextClassName;
    int p = shortContextName.lastIndexOf('.');

    if (p > 0)
      shortContextName = shortContextName.substring(p + 1);

    out.println("protected static final java.util.logging.Logger __caucho_log = java.util.logging.Logger.getLogger(\"" + _contextClassName + "\");");
    out.println();
    out.println("com.caucho.ejb.xa.EjbTransactionManager _xaManager;");
    out.println("private Bean _freeBean;");
    out.println();
    out.println("public " + shortContextName + "(com.caucho.ejb.session.SessionServer server)");
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
    out.println("  bean = _freeBean;");
    out.println("  if (bean != null) {");
    out.println("    _freeBean = null;");
    if (_ejbClass.isAssignableTo(javax.ejb.SessionSynchronization.class))
      out.println("      trans.addSession(bean);");
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

    if (hasMethod("ejbRemove", new JClass[0])) {
      out.println();
      out.println("try {");
      out.println("  if (ptr != null)");
      out.println("    ptr.ejbRemove();");
      out.println("} catch (Throwable e) {");
      out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
      out.println("}");
    }
    out.popDepth();
    out.println("}");
  }

  protected void generateNewInstance(JavaWriter out)
    throws IOException
  {
    // ejb/0g27
    if (_bean.getLocalHome() == null && _bean.getLocalList().size() == 0)
      return;

    out.println();
    out.println("protected Object _caucho_newInstance()");
    out.println("{");
    out.pushDepth();

    out.println(_contextClassName + " cxt = new " + _contextClassName + "(_server);");

    out.println("Bean bean = new Bean(cxt);");

    out.println("cxt._ejb_free(bean);");

    out.println();

    /*
      Class retType = getReturnType();
      if ("RemoteHome".equals(_prefix))
        out.println("return (" + retType.getName() + ") cxt.getRemoteView();");
      else if ("LocalHome".equals(_prefix))
        out.println("return (" + retType.getName() + ") cxt.getLocalView();");
      else
        throw new IOException(L.l("trying to create unknown type {0}",
                              _prefix));
    */
    out.println("return cxt.getEJBLocalObject();");

    out.popDepth();
    out.println("}");
  }

  protected void generateNewRemoteInstance(JavaWriter out)
    throws IOException
  {
    // ejb/0g27
    if (_bean.getRemoteHome() == null && _bean.getRemoteList().size() == 0)
      return;

    out.println();
    out.println("protected Object _caucho_newRemoteInstance()");
    out.println("{");
    out.pushDepth();

    out.println(_contextClassName + " cxt = new " + _contextClassName + "(_server);");

    out.println("Bean bean = new Bean(cxt);");

    out.println("cxt._ejb_free(bean);");

    out.println();

    /*
      Class retType = getReturnType();
      if ("RemoteHome".equals(_prefix))
        out.println("return (" + retType.getName() + ") cxt.getRemoteView();");
      else if ("LocalHome".equals(_prefix))
        out.println("return (" + retType.getName() + ") cxt.getLocalView();");
      else
        throw new IOException(L.l("trying to create unknown type {0}",
                              _prefix));
    */
    out.println("return cxt.getEJBObject();");

    out.popDepth();
    out.println("}");
  }

  private void generateBean(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public static class Bean extends " + _ejbClass.getName() + " {");
    out.pushDepth();

    out.println();
    out.println("protected final static java.util.logging.Logger __caucho_log = com.caucho.log.Log.open(" + _ejbClass.getName() + ".class);");
    out.println("private static int __caucho_dbg_id;");
    out.println("private final String __caucho_id;");

    out.println(_contextClassName + " _ejb_context;");
    out.println("boolean _ejb_isActive;");

    int i = 0;

    for (Interceptor interceptor : _bean.getInterceptors()) {
      String clName = interceptor.getInterceptorClass();

      out.println("Object _interceptor" + i++ + ";");
    }

    out.println();
    out.println("Bean(" + _contextClassName + " context)");
    out.println("{");
    out.pushDepth();

    out.println("synchronized (" + _ejbClass.getName() + ".class) {");
    out.println("  __caucho_id = __caucho_dbg_id++ + \"-" + _ejbClass.getName() + "\";");
    out.println("}");
    out.println("__caucho_log.fine(__caucho_id + \":new()\");");

    out.println("try {");
    out.pushDepth();

    out.println("_ejb_context = context;");

    if (hasMethod("setSessionContext", new JClass[] { JClassLoader.systemForName(SessionContext.class.getName()) })) {
      out.println("setSessionContext(context);");
    }

    out.println();
    out.println("context.getServer().initInstance(this);");

    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Throwable e) {");
    out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("  throw com.caucho.ejb.EJBExceptionWrapper.create(e);");
    out.println("}");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public javax.interceptor.InvocationContext __caucho_callInterceptors(Object []args)");
    out.println("{");
    out.pushDepth();

    out.println("javax.interceptor.InvocationContext invocationContext;");

    // XXX: invocation context pool ???
    out.println();
    out.println("invocationContext = new com.caucho.ejb.interceptor.InvocationContextImpl();");
    out.println("invocationContext.setParameters(args);");

    out.println();
    out.println("try {");
    out.pushDepth();

    i = 0;

    for (Interceptor interceptor : _bean.getInterceptors()) {
      String aroundInvokeMethodName = interceptor.getAroundInvokeMethodName();
      String clName = interceptor.getInterceptorClass();

      int j = i++;

      out.println("Class cl" + j + " = Class.forName(\"" + clName + "\");");

      out.println();
      out.println("if (_interceptor" + j + " == null) {");
      out.println("  _interceptor" + j + " = cl" + j + ".newInstance();");
      out.println("}");

      out.println();
      out.println("java.lang.reflect.Method method" + j + " = cl" + j + ".getDeclaredMethod(\"" + aroundInvokeMethodName + "\", new Class[] { javax.interceptor.InvocationContext.class });");

      // XXX: private/protected methods. Restore access after making it accessible?
      out.println("com.caucho.ejb.cfg.Interceptor.makeAccessible(method" + j + ");");
      out.println();

      out.println();
      out.println("method" + j + ".invoke(_interceptor" + j + ", new Object[] { invocationContext });");
    }

    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Throwable e) {");
    out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("  throw com.caucho.ejb.EJBExceptionWrapper.create(e);");
    out.println("}");

    out.println();
    out.println("return invocationContext;");

    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Returns true if the method is implemented.
   */
  protected boolean hasMethod(String methodName, JClass []paramTypes)
  {
    return BeanAssembler.hasMethod(_ejbClass, methodName, paramTypes);
  }
}
