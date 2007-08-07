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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.gen;

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.ejb.cfg.EjbMethod;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.FilterCallChain;
import com.caucho.java.gen.MethodCallChain;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the skeleton for a method call.
 */
public class TransactionChain extends FilterCallChain {
  private static final L10N L = new L10N(TransactionChain.class);

  private JMethod _apiMethod;
  private JMethod _implMethod;

  private int _xaType;

  public TransactionChain(CallChain next,
                          int xaType,
                          JMethod apiMethod,
                          JMethod implMethod)
  {
    super(next);

    _xaType = xaType;
    _apiMethod = apiMethod;
    _implMethod = implMethod;
  }

  public static TransactionChain create(CallChain next,
                                        int xaType,
                                        JMethod apiMethod,
                                        JMethod implMethod)
  {
    return new TransactionChain(next, xaType, apiMethod, implMethod);
  }

  /**
   * Prints a call within the same JVM
   *
   * @param methodName the name of the method to call
   * @param method the method to call
   */
  public void generateCall(JavaWriter out, String retType,
         String var, String []args)
    throws IOException
  {
    out.println("Thread thread = Thread.currentThread();");
    out.println("ClassLoader oldLoader = thread.getContextClassLoader();");

    out.print("javax.transaction.Transaction oldTrans = _xaManager.getTransaction();");
    out.println();

    out.print("com.caucho.ejb.xa.TransactionContext trans");

    switch (_xaType) {
    case EjbMethod.TRANS_SINGLE_READ:
      out.println(" = _xaManager.beginSingleRead();");
      break;

    case EjbMethod.TRANS_REQUIRES_NEW:
      out.println(" = _xaManager.beginRequiresNew();");
      break;
    case EjbMethod.TRANS_BEAN:
    case EjbMethod.TRANS_NOT_SUPPORTED:
       out.println(" = _xaManager.suspend();");
      break;
    case EjbMethod.TRANS_NEVER:
      out.println(" = _xaManager.beginNever();");
      break;
    case EjbMethod.TRANS_REQUIRED:
      out.println(" = _xaManager.beginRequired();");
      break;
    case EjbMethod.TRANS_MANDATORY:
      out.println(" = _xaManager.beginMandatory();");
      break;
    default:
    case EjbMethod.TRANS_SUPPORTS:
      out.println(" = _xaManager.beginSupports();");
      break;
    }

    out.println("try {");
    out.pushDepth();

    out.println("thread.setContextClassLoader(_context._server.getClassLoader());");

    super.generateCall(out, retType, var, args);

    if (! _implMethod.isAnnotationPresent(javax.ejb.Remove.class)) {
      generateExceptionHandling(out);
    }

    out.pushDepth();

    /*
    if (! out.isSession())
      out.println("if (ptr != null) ptr._ejb_state = QEntity._CAUCHO_IS_DEAD;");
    */

    out.println("e = com.caucho.ejb.EJBExceptionWrapper.create(e);");
    out.println();

    // TCK: needs QA, ejb30/bb/localaccess/statefulclient/exceptionTest1
    if (_xaType != EjbMethod.TRANS_BEAN) {
      out.println("if (trans.getTransaction() != oldTrans) {");
      out.println("  throw trans.setRollbackOnly(e);");
      out.println("}");
    }

    out.println("throw (com.caucho.ejb.EJBExceptionWrapper) e;");

    out.popDepth();

    out.println("} finally {");
    out.pushDepth();

    out.println("thread.setContextClassLoader(oldLoader);");

    // TCK: needs QA, ejb30/bb/localaccess/statefulclient/exceptionTest1
    if (_xaType != EjbMethod.TRANS_BEAN) {
      // ejb/0224 out.println("if (trans.getTransaction() != oldTrans)");
      out.println("trans.commit();");
    }

    /*
    if (out.isSession())
      out.println("if (ptr != null) ptr._ejb_isActive = false;");
    */

    out.popDepth();
    out.println("}");
  }

  protected void generateExceptionHandling(JavaWriter out)
    throws IOException
  {
    boolean isCmt = _xaType != EjbMethod.TRANS_BEAN;

    // ejb/0fb9
    out.popDepth();
    out.println("} catch (Exception e) {");
    out.pushDepth();

    out.println("if (e instanceof com.caucho.ejb.EJBExceptionWrapper)");
    out.println("  e = (Exception) e.getCause();");
    out.println();

    out.println("if (e instanceof java.lang.reflect.InvocationTargetException)");
    out.println("  e = (Exception) e.getCause();");
    out.println();

    for (JClass cl : _implMethod.getExceptionTypes()) {
      out.println("if (e instanceof " + cl.getName() + ") {");

      if (isCmt) {
        out.println("  if (trans.getTransaction() != oldTrans)");
        out.println("    trans.setRollbackOnly(e);");
      }

      out.println("  throw (" + cl.getName() + ") e;");
      out.println("}");
      out.println();
    }
  }
}
