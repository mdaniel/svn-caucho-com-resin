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

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.security.*;
import javax.ejb.*;
import javax.interceptor.*;

/**
 * Represents a stateful create business method
 */
public class StatefulCreateMethod extends BusinessMethodGenerator
{
  private StatefulGenerator _bean;
  private View _objectView;
  
  public StatefulCreateMethod(StatefulGenerator bean,
			      StatefulView homeView,
			      View objectView,
			      ApiMethod apiMethod,
			      Method implMethod,
			      int index)
  {
    super(homeView, apiMethod, implMethod, index);

    _bean = bean;
    _objectView = objectView;

    if (_objectView == null)
      throw new NullPointerException();
  }

  /**
   * Session bean default is REQUIRED
   */
  @Override
  public void introspect(Method apiMethod, Method implMethod)
  {
    getXa().setContainerManaged(false);

    super.introspect(apiMethod, implMethod);
  }

  protected TransactionAttributeType getDefaultTransactionType()
  {
    return TransactionAttributeType.REQUIRED;
  }
  
  /**
   * Returns true if any interceptors enhance the business method
   */
  @Override
  public boolean isEnhanced()
  {
    return true;
  }

  protected void generatePreCall(JavaWriter out)
    throws IOException
  {
    out.println("Thread thread = Thread.currentThread();");
    out.println("ClassLoader oldLoader = thread.getContextClassLoader();");
    out.println("try {");
    out.pushDepth();
    out.println("thread.setContextClassLoader(getStatefulServer().getClassLoader());");
    
    out.println(_objectView.getViewClassName() + " remote ="
		+ " new " + _objectView.getViewClassName() + "(getStatefulServer(), (javax.context.CreationalContext) null);");
    
    out.print(_objectView.getBeanClassName());
    out.print(" bean = remote._bean;");

    out.println("StatefulContext context = new " + _bean.getFullClassName() + "(_context, remote);");
    out.println("remote.__caucho_setContext(context);");
    if (SessionBean.class.isAssignableFrom(_bean.getEjbClass().getJavaClass()))
      out.println("bean.setSessionContext(context);");
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generateThis(JavaWriter out)
    throws IOException
  {
    out.print("bean");
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generateSuper(JavaWriter out)
    throws IOException
  {
    out.print("bean");
  }

  @Override
  protected String getSuper()
  {
    return "bean";
  }

  /**
   * Generates the underlying bean instance
   */
  @Override
  protected void generatePostCall(JavaWriter out)
    throws IOException
  {
    out.println("return remote;");
    out.popDepth();
    out.println("} finally {");
    out.println("  thread.setContextClassLoader(oldLoader);");
    out.println("}");
  }
}
