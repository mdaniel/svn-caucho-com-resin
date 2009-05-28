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

package com.caucho.config.gen;

import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.FilterCallChain;
import com.caucho.java.gen.MethodCallChain;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.*;

import javax.ejb.*;
import javax.ejb.ApplicationException;
/**
 * Generates the skeleton for a method call.
 */
public class TransactionChain extends FilterCallChain
{
  private static final Logger log
    = Logger.getLogger(TransactionChain.class.getName());
  private static final L10N L = new L10N(TransactionChain.class);

  private ApiMethod _apiMethod;
  private ApiMethod _implMethod;

  private ApiClass _businessInterface;

  private TransactionAttributeType _xaType;

  private boolean _isEJB3;

  private ArrayList<ApplicationExceptionConfig> _appExceptions;

  public TransactionChain(CallChain next,
                          TransactionAttributeType xaType,
                          ApiMethod apiMethod,
                          ApiMethod implMethod)
  {
    this(next, xaType, apiMethod, implMethod, false, null);
  }

  public TransactionChain(CallChain next,
                          TransactionAttributeType xaType,
                          ApiMethod apiMethod,
                          ApiMethod implMethod,
                          boolean isEJB3,
                          ArrayList<ApplicationExceptionConfig> appExceptions)
  {
    super(next);

    _xaType = xaType;
    _apiMethod = apiMethod;
    _implMethod = implMethod;
    _isEJB3 = isEJB3;
    _appExceptions = appExceptions;

    if (implMethod == null)
      _implMethod = apiMethod;
  }

  public static TransactionChain create(CallChain next,
                                        TransactionAttributeType xaType,
                                        ApiMethod apiMethod,
                                        ApiMethod implMethod)
  {
    return new TransactionChain(next, xaType, apiMethod, implMethod);
  }

  public static TransactionChain create(CallChain next,
                                        TransactionAttributeType xaType,
                                        ApiMethod apiMethod,
                                        ApiMethod implMethod,
                                        boolean isEJB3,
                                        ArrayList<ApplicationExceptionConfig> appExceptions)
  {
    return new TransactionChain(next, xaType, apiMethod, implMethod, isEJB3, appExceptions);
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
    // ejb/0ff0 TCK: ejb30/bb/session/stateful/sessioncontext/annotated/getInvokedBusinessInterfaceRemote2
    //out.println("if (getServer().getContext() != null)");
    //out.println("  getServer().getContext().__caucho_setInvokedBusinessInterface(_businessInterface);");

    if (_isEJB3 && _businessInterface != null) {
      out.println("if (_context != null)");
      out.println("  _context.__caucho_setInvokedBusinessInterface(" + _businessInterface.getName() + ");");
      out.println();
    }

    out.println("Thread thread = Thread.currentThread();");
    out.println("ClassLoader oldLoader = thread.getContextClassLoader();");

    out.print("javax.transaction.Transaction oldTrans = _xaManager.getTransaction();");
    out.println();

    out.print("com.caucho.ejb.xa.TransactionContext trans");

    if (_xaType != null) {
      switch (_xaType) {
	/*
	  case EjbMethod.TRANS_SINGLE_READ:
	  out.println(" = _xaManager.beginSingleRead();");
	  break;
	*/

      case REQUIRES_NEW:
	out.println(" = _xaManager.beginRequiresNew();");
	break;
	//case EjbMethod.TRANS_BEAN:
      case NOT_SUPPORTED:
	out.println(" = _xaManager.suspend();");
	break;
      case NEVER:
	out.println(" = _xaManager.beginNever();");
	break;
      case REQUIRED:
	out.println(" = _xaManager.beginRequired();");
	break;
      case MANDATORY:
	out.println(" = _xaManager.beginMandatory();");
	break;
      default:
      case SUPPORTS:
	out.println(" = _xaManager.beginSupports();");
	break;
      }
    }

    out.println("try {");
    out.pushDepth();

    out.println("thread.setContextClassLoader(_context._server.getClassLoader());");
    out.println();

    /* XXX: need to check something like _context.isDead() instead
    // ejb/0fe4: throws exception if this context has been removed.
    out.println("getServer().getContext(_context.getPrimaryKey());");
    out.println();
    */

    super.generateCall(out, retType, var, args);

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.pushDepth();

    ApiClass beanClass = _implMethod.getDeclaringClass();

    if (_isEJB3) { // XXX && ! _implMethod.isAnnotationPresent(javax.ejb.Remove.class)) {
      generateExceptionHandling(out);
    }

    /*
    if (! out.isSession())
      out.println("if (ptr != null) ptr._ejb_state = QEntity._CAUCHO_IS_DEAD;");
    */

    out.println("e = com.caucho.ejb.EJBExceptionWrapper.create(e);");
    out.println();

    // TCK: needs QA, ejb30/bb/localaccess/statefulclient/exceptionTest1
    //if (_xaType != EjbMethod.TRANS_BEAN) {
    if (_xaType != null) {
      out.println("if (trans.getTransaction() != oldTrans) {");
      out.println("  throw trans.setRollbackOnly(e);");
      out.println("}");
    }

    // ejb/02b1
    out.println("throw (javax.ejb.EJBException) e;");

    out.popDepth();

    out.println("} finally {");
    out.pushDepth();

    out.println("thread.setContextClassLoader(oldLoader);");

    // TCK: needs QA, ejb30/bb/localaccess/statefulclient/exceptionTest1
    // if (_xaType != EjbMethod.TRANS_BEAN)
    // ejb/0224 vs TCK
    //out.println("if (trans.getTransaction() != oldTrans)");
    // XXX TCK: ejb30/bb/session/stateful/sessioncontext/annotated/getInvokedBusinessInterfaceLocal1
    out.println("  trans.commit();");

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
    // boolean isCmt = _xaType != EjbMethod.TRANS_BEAN;
    boolean isCmt = _xaType != null;

    // ejb/0fb9
    out.println("if (e instanceof com.caucho.ejb.EJBExceptionWrapper)");
    out.println("  e = (Exception) e.getCause();");
    out.println();

    out.println("if (e instanceof java.lang.reflect.InvocationTargetException)");
    out.println("  e = (Exception) e.getCause();");
    out.println();

    ApiClass beanClass = _implMethod.getDeclaringClass();

    // ejb/0500
    Class exnTypes[]; // = getExceptionTypes();

    // ejb/0fb3, ejb/0fbg
    for (Class cl : _implMethod.getExceptionTypes()) {
      if (! Exception.class.isAssignableFrom(cl)) {
        // XXX:
        // hessian/3600
        log.info(cl + " is not handled by EJB");
        continue;
      }

      out.println("if (e instanceof " + cl.getName() + ") {");
      out.pushDepth();

      if (isCmt) {
        // ejb/0fc0, ejb/0fc1
        // TCK: ejb30/bb/session/stateful/annotation/appexception/annotated/atCheckedRollbackAppExceptionTest

        boolean isApplicationException = false;
        boolean isRollback = false;

        // Check @ApplicationException(rollback=true/false)
        ApplicationException ann =
	  (ApplicationException) cl.getAnnotation(ApplicationException.class);

        if (ann != null) {
          isApplicationException = true;
          isRollback = ann.rollback();
        } else if (_appExceptions != null) {
          // ejb/0fc3
          for (ApplicationExceptionConfig cfg : _appExceptions) {
            if (cfg.getExceptionClass().equals(cl.getName())) {
              isApplicationException = true;
              isRollback = cfg.isRollback();
              break;
            }
          }
        }

        if (! isApplicationException) {
          // ejb/0fc0
          out.println("if (trans.getTransaction() != oldTrans)");
          out.println("  trans.setRollbackOnly(e);");
        } else if (isRollback) {
          // ejb/0fc1
          out.println("trans.setRollbackOnly(e);");
        }
        // else do not rollback.
      }

      out.println("throw (" + cl.getName() + ") e;");

      out.popDepth();
      out.println("}");
      out.println();
    }
  }
}
