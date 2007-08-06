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

package com.caucho.ejb.cfg;

import com.caucho.bytecode.JMethod;
import com.caucho.config.ConfigException;
import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.gen.ViewClass;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Configuration for a method of a view.
 */
public class EjbMethod {
  private static final L10N L = new L10N(EjbMethod.class);

  public static final int TRANS_BEAN = 0;
  public static final int TRANS_NOT_SUPPORTED = TRANS_BEAN + 1;
  public static final int TRANS_SUPPORTS = TRANS_NOT_SUPPORTED + 1;
  public static final int TRANS_REQUIRED = TRANS_SUPPORTS + 1;
  public static final int TRANS_REQUIRES_NEW = TRANS_REQUIRED + 1;
  public static final int TRANS_MANDATORY = TRANS_REQUIRES_NEW + 1;
  public static final int TRANS_NEVER = TRANS_MANDATORY + 1;
  public static final int TRANS_SINGLE_READ = TRANS_NEVER + 1;

  public final static int RESIN_DATABASE = 0;
  public final static int RESIN_READ_ONLY = 1;
  public final static int RESIN_ROW_LOCKING = 2;

  private EjbView _view;

  private JMethod _apiMethod;
  private JMethod _implMethod;

  /**
   * Creates a new method.
   *
   * @param view the owning view
   * @param apiMethod the method from the view
   * @param implMethod the method from the implementation
   */
  public EjbMethod(EjbView view, JMethod apiMethod, JMethod implMethod)
  {
    if (apiMethod == null)
      throw new NullPointerException();

    _view = view;
    _apiMethod = apiMethod;
    _implMethod = implMethod;
  }

  /**
   * Returns the view.
   */
  public EjbView getView()
  {
    return _view;
  }

  /**
   * Returns the view prefix.
   */
  public String getViewPrefix()
  {
    return _view.getPrefix();
  }

  /**
   * Returns the API method.
   */
  public JMethod getApiMethod()
  {
    return _apiMethod;
  }

  /**
   * Returns the Impl method.
   */
  public JMethod getImplMethod()
  {
    return _implMethod;
  }

  /**
   * Assembles the bean method.
   */
  public void assembleBean(BeanAssembler beanAssembler, String fullClassName)
    throws ConfigException
  {
  }

  /**
   * Assembles the method.
   */
  public BaseMethod assemble(ViewClass viewAssembler, String fullClassName)
    throws ConfigException
  {
    if (getImplMethod() == null)
      throw new NullPointerException("no impl: " + getApiMethod());

    BaseMethod method = viewAssembler.createBusinessMethod(this);

    method.setCall(assembleCallChain(method.getCall()));

    return method;
  }

  /**
   * Assembles the call chain.
   */
  protected CallChain assembleCallChain(CallChain call)
  {
    call = getView().getTransactionChain(call,
                                         getApiMethod(),
                                         getImplMethod(),
                                         getViewPrefix());

    call = getView().getSecurityChain(call,
                                      getApiMethod(),
                                      getViewPrefix());

    return call;
  }

  /**
   * Pushes a required transaction.
   */
  protected void pushRequired(JavaWriter out)
    throws IOException
  {

    out.println("com.caucho.ejb.xa.TransactionContext xa = _xaManager.beginRequired();");
    out.println("try {");
    out.pushDepth();
  }

  /**
   * Pops a required transaction.
   */
  protected void popRequired(JavaWriter out)
    throws IOException
  {
    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw trans.setRollbackOnly(e);");
    out.println("} finally {");
    out.println("  trans.commit();");
    out.println("}");
  }

  /**
   * Returns true if these are equivalent.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof EjbMethod))
      return false;

    EjbMethod method = (EjbMethod) o;

    if (_view != method._view)
      return false;
    else if (! _apiMethod.equals(method._apiMethod))
      return false;
    else
      return true;
  }

  public String toString()
  {
    return "EJBMethod[" + _apiMethod + "]";
  }
}
