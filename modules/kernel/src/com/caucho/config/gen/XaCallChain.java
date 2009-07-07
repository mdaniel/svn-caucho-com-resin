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
package com.caucho.config.gen;

import java.io.IOException;
import java.util.HashMap;

import javax.ejb.ApplicationException;
import javax.ejb.SessionSynchronization;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.transaction.Synchronization;

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Represents the XA interception
 */
public class XaCallChain extends AbstractCallChain {
  @SuppressWarnings("unused")
  private static final L10N L = new L10N(XaCallChain.class);

  private BusinessMethodGenerator _bizMethod;
  private EjbCallChain _next;

  private TransactionAttributeType _xa;
  private boolean _isContainerManaged = true;
  private boolean _isSessionSynchronization;

  public XaCallChain(BusinessMethodGenerator bizMethod, EjbCallChain next) {
    super(next);

    _bizMethod = bizMethod;
    _next = next;

    _isContainerManaged = bizMethod.isXaContainerManaged();
  }

  protected BusinessMethodGenerator getBusinessMethod()
  {
    return _bizMethod;
  }

  /**
   * Returns true if the business method has any active XA annotation.
   */
  public boolean isEnhanced()
  {
    return (_isContainerManaged
	    && _xa != null
	    && ! _xa.equals(TransactionAttributeType.SUPPORTS));
  }

  /**
   * Returns the transaction type
   */
  public TransactionAttributeType getTransactionType()
  {
    return _xa;
  }

  /**
   * Introspects the method for the default values
   */
  public void introspect(ApiMethod apiMethod, ApiMethod implMethod)
  {
    ApiClass apiClass = apiMethod.getDeclaringClass();
    ApiClass beanClass = _bizMethod.getBeanClass();

    TransactionManagement xaManagement
      = beanClass.getAnnotation(TransactionManagement.class);

    if (xaManagement == null)
      xaManagement = apiClass.getAnnotation(TransactionManagement.class);

    if (xaManagement != null
        && xaManagement.value() != TransactionManagementType.CONTAINER) {
      _isContainerManaged = false;
      return;
    }

    Class javaClass = beanClass.getJavaClass();

    if (javaClass != null
	&& SessionSynchronization.class.isAssignableFrom(javaClass)) {
      _isSessionSynchronization = true;
    }

    TransactionAttribute xaAttr;

    xaAttr = apiMethod.getAnnotation(TransactionAttribute.class);

    if (xaAttr == null) {
      xaAttr = apiClass.getAnnotation(TransactionAttribute.class);
    }

    if (xaAttr == null && implMethod != null) {
      xaAttr = implMethod.getAnnotation(TransactionAttribute.class);
    }

    if (xaAttr == null && beanClass != null) {
      xaAttr = beanClass.getAnnotation(TransactionAttribute.class);
    }

    if (xaAttr != null)
      _xa = xaAttr.value();
  }

  /**
   * Generates the static class prologue
   */
  @SuppressWarnings("unchecked")
  @Override
  public void generatePrologue(JavaWriter out, HashMap map)
    throws IOException
  {
    if (_isContainerManaged && map.get("caucho.ejb.xa") == null) {
      map.put("caucho.ejb.xa", "done");

      out.println();
      out.println("private static final com.caucho.ejb3.xa.XAManager _xa");
      out.println("  = new com.caucho.ejb3.xa.XAManager();");
    }

    _next.generatePrologue(out, map);
  }

  /**
   * Generates the method interceptor code
   */
  @SuppressWarnings("unchecked")
  public void generateCall(JavaWriter out)
    throws IOException
  {
    boolean isPushDepth = false;

    if (_isContainerManaged && _xa != null) {
      switch (_xa) {
      case MANDATORY: {
        out.println("_xa.beginMandatory();");
      }
        break;

      case NEVER: {
        out.println("_xa.beginNever();");
      }
        break;

      case NOT_SUPPORTED: {
        out.println("Transaction xa = _xa.beginNotSupported();");
        out.println();
        out.println("try {");
        out.pushDepth();
        isPushDepth = true;
      }
        break;

      case REQUIRED: {
        out.println("Transaction xa = _xa.beginRequired();");
        out.println();
        out.println("try {");
        out.pushDepth();
        isPushDepth = true;
      }
        break;

      case REQUIRES_NEW: {
        out.println("Transaction xa = _xa.beginRequiresNew();");
        out.println();
        out.println("try {");
        out.pushDepth();
        isPushDepth = true;
      }
        break;
      }
    }

    if (_isContainerManaged && _isSessionSynchronization) {
      out.print("_xa.registerSynchronization(");
      _bizMethod.generateThis(out);
      out.println(");");
    }

    generateNext(out);

    if (_isContainerManaged && _xa != null) {
      if (isPushDepth)
        out.popDepth();

      for (Class exn : _bizMethod.getApiMethod().getExceptionTypes()) {
        ApplicationException appExn = (ApplicationException) exn
            .getAnnotation(ApplicationException.class);

        if (appExn == null)
          continue;

        if (! RuntimeException.class.isAssignableFrom(exn)
	    && appExn.rollback()) {
          out.println("} catch (" + exn.getName() + " e) {");
          out.println("  _xa.markRollback(e);");
          out.println("  throw e;");
        } else if (RuntimeException.class.isAssignableFrom(exn)
		   && !appExn.rollback()) {
          out.println("} catch (" + exn.getName() + " e) {");
          out.println("  throw e;");
        }
      }

      switch (_xa) {
      case REQUIRED:
      case REQUIRES_NEW: {
        out.println("} catch (RuntimeException e) {");
        out.println("  _xa.markRollback(e);");
        out.println("  throw e;");
      }
      }

      switch (_xa) {
      case NOT_SUPPORTED: {
        out.println("} finally {");
        out.println("  if (xa != null)");
        out.println("    _xa.resume(xa);");
        out.println("}");
      }
        break;

      case REQUIRED: {
        out.println("} finally {");
        out.println("  if (xa == null)");
        out.println("    _xa.commit();");
        out.println("}");
      }
        break;

      case REQUIRES_NEW: {
        out.println("} finally {");
        out.println("  _xa.endRequiresNew(xa);");
        out.println("}");
      }
        break;
      }
    }
  }

  protected void generateNext(JavaWriter out)
    throws IOException
  {
    _next.generateCall(out);
  }
}
