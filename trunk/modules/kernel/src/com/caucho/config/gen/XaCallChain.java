/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

import static javax.ejb.TransactionAttributeType.NOT_SUPPORTED;
import static javax.ejb.TransactionAttributeType.REQUIRED;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import static javax.ejb.TransactionAttributeType.SUPPORTS;

import java.io.IOException;
import java.util.HashMap;

import javax.ejb.ApplicationException;
import javax.ejb.SessionSynchronization;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import com.caucho.java.JavaWriter;

/**
 * Represents the XA interception
 */
public class XaCallChain extends AbstractCallChain {
  private BusinessMethodGenerator _bizMethod;
  private EjbCallChain _next;

  private TransactionAttributeType _transactionType;
  private boolean _isContainerManaged = true;
  private boolean _isSessionSynchronization;

  public XaCallChain(BusinessMethodGenerator bizMethod, EjbCallChain next)
  {
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
    return (_isContainerManaged && _transactionType != null && !_transactionType
        .equals(SUPPORTS));
  }

  /**
   * Returns the transaction type
   */
  public TransactionAttributeType getTransactionType()
  {
    return _transactionType;
  }

  /**
   * Introspects the method for the default values
   */
  public void introspect(ApiMethod apiMethod, ApiMethod implMethod)
  {
    ApiClass apiClass = apiMethod.getDeclaringClass();
    ApiClass beanClass = _bizMethod.getBeanClass();

    TransactionManagement xaManagement = beanClass
        .getAnnotation(TransactionManagement.class);

    if (xaManagement == null)
      xaManagement = apiClass.getAnnotation(TransactionManagement.class);

    if (xaManagement != null
        && xaManagement.value() != TransactionManagementType.CONTAINER) {
      _isContainerManaged = false;
      return;
    }

    Class<?> javaClass = beanClass.getJavaClass();

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
      _transactionType = xaAttr.value();
  }

  //
  // bean prologue generation
  //

  /**
   * Generates the static class prologue
   */
  @Override
  public void generateMethodPrologue(JavaWriter out, HashMap<String, Object> map)
      throws IOException
  {
    if (map.get("caucho.ejb.xa") == null) {
      map.put("caucho.ejb.xa", "done");

      out.println();
      out.println("private static final com.caucho.ejb.util.XAManager _xa");
      out.println("  = new com.caucho.ejb.util.XAManager();");
    }

    _next.generateMethodPrologue(out, map);
  }

  //
  // method generation code
  //

  /**
   * Generates code before the "try" block <code><pre>
   * retType myMethod(...)
   * {
   *   [pre-try]
   *   try {
   *     ...
   * }
   * </pre></code>
   */
  @Override
  public void generatePreTry(JavaWriter out) throws IOException
  {
    if (_isContainerManaged) {
      out.println();
      out.println("boolean isXAValid = false;");
    }

    if (!_isContainerManaged) {
      out.println();
      out.println("Transaction xa = null;");
    } else if (_transactionType != null) {
      switch (_transactionType) {
      case NOT_SUPPORTED:
      case REQUIRED:
      case REQUIRES_NEW: {
        out.println();
        out.println("Transaction xa = null;");
        break;
      }
      }
    }

    super.generatePreTry(out);
  }

  /**
   * Generates the interceptor code after the try-block and before the call.
   * 
   * <code><pre>
   * retType myMethod(...)
   * {
   *   try {
   *     [pre-call]
   *     retValue = super.myMethod(...);
   * }
   * </pre></code>
   */
  @Override
  public void generatePreCall(JavaWriter out) throws IOException
  {
    if (!_isContainerManaged) {
      out.println("xa = _xa.beginNotSupported();");
    } else if (_transactionType != null) {
      switch (_transactionType) {
      case MANDATORY: {
        out.println();
        out.println("_xa.beginMandatory();");
        break;
      }

      case NEVER: {
        out.println();
        out.println("_xa.beginNever();");
        break;
      }

      case NOT_SUPPORTED: {
        out.println();
        out.println("xa = _xa.beginNotSupported();");
        break;
      }

      case REQUIRED: {
        out.println();
        out.println("xa = _xa.beginRequired();");
        break;
      }

      case REQUIRES_NEW: {
        out.println();
        out.println("xa = _xa.beginRequiresNew();");
        break;
      }
      }
    }

    if (_isContainerManaged && _isSessionSynchronization) {
      out.print("_xa.registerSynchronization(");
      _bizMethod.generateThis(out);
      out.println(");");
    }

    super.generatePreCall(out);
  }

  /**
   * Generates the interceptor code after invocation and before the call.
   * 
   * <code><pre>
   * retType myMethod(...)
   * {
   *   try {
   *     retValue = super.myMethod(...);
   *     [post-call]
   *     return retValue;
   *   } finally {
   *     ...
   *   }
   * }
   * </pre></code>
   */
  @Override
  public void generatePostCall(JavaWriter out) throws IOException
  {
    super.generatePostCall(out);

    if (_isContainerManaged
        && (_transactionType == REQUIRED || _transactionType == REQUIRES_NEW)) {
      out.println("isXAValid = true;");
    }
  }

  /**
   * Generates aspect code for an application exception
   * 
   */
  @Override
  public void generateApplicationException(JavaWriter out, Class<?> exception)
      throws IOException
  {
    super.generateApplicationException(out, exception);

    ApplicationException applicationException = exception
        .getAnnotation(ApplicationException.class);

    if ((applicationException != null) && (applicationException.rollback())) {
      if (_isContainerManaged && (_transactionType != NOT_SUPPORTED)) {
        out.println("if (_xa.getTransaction() != null)");
        out.println("  _xa.markRollback(e);");
      }
    } else if (_isContainerManaged
        && (_transactionType == REQUIRED || _transactionType == REQUIRES_NEW)) {
      out.println("isXAValid = true;");
    }
  }

  @Override
  public void generateSystemException(JavaWriter out, Class<?> exn)
      throws IOException
  {
    if (_isContainerManaged) {
      out.println("if (_xa.getTransaction() != null) {");
      out.println("  _xa.markRollback(e);");
      out.println("  isXAValid = true;");
      out.println("}");
    }
  }

  /**
   * Generates the aspect code in the finally block.
   * 
   * <code><pre>
   * retType myMethod(...)
   * {
   *   try {
   *     ...
   *   } finally {
   *     [finally]
   *   }
   * }
   * </pre></code>
   */
  @Override
  public void generateFinally(JavaWriter out) throws IOException
  {
    super.generateFinally(out);

    if (!_isContainerManaged) {
      out.println("if (xa != null)");
      out.println("  _xa.resume(xa);");
    } else if (_transactionType != null) {
      switch (_transactionType) {
      case NOT_SUPPORTED: {
        out.println("if (xa != null)");
        out.println("  _xa.resume(xa);");
        break;
      }

      case REQUIRED: {
        out.println();
        out.println("if (xa == null)");
        out.println("  _xa.commit(isXAValid);");
        break;
      }

      case REQUIRES_NEW: {
        out.println("_xa.endRequiresNew(xa, isXAValid);");
        break;
      }
      }
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _bizMethod + "]";
  }
}