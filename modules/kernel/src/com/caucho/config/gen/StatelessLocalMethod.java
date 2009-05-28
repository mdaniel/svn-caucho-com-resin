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
 * Represents a stateless local business method
 */
public class StatelessLocalMethod extends BusinessMethodGenerator
{
  private ApiClass _ejbClass;
  private String _beanClassName;
  
  public StatelessLocalMethod(ApiClass ejbClass,
			      String beanClassName,
			      StatelessView view,
			      ApiMethod apiMethod,
			      ApiMethod implMethod,
			      int index)
  {
    super(view, apiMethod, implMethod, index);

    _beanClassName = beanClassName;
    _ejbClass = ejbClass;
  }

  /**
   * Session bean default is REQUIRED
   */
  @Override
  public void introspect(ApiMethod apiMethod, ApiMethod implMethod)
  {
    getXa().setTransactionType(TransactionAttributeType.REQUIRED);

    super.introspect(apiMethod, implMethod);
  }

  /**
   * Returns true if any interceptors enhance the business method
   */
  @Override
  public boolean isEnhanced()
  {
    return true;
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generatePreCall(JavaWriter out)
    throws IOException
  {
    out.println("Thread thread = Thread.currentThread();");
    out.println("ClassLoader oldLoader = thread.getContextClassLoader();");

    out.println();
    out.println(_beanClassName + " bean = _ejb_begin();");
    out.println("boolean isValid = false;");
    out.println();
    out.println("try {");
    out.pushDepth();
    out.println("thread.setContextClassLoader(_context.getStatelessServer().getClassLoader());");
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
  @Override
  protected String getSuper()
  {
    return "bean";
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generatePreReturn(JavaWriter out)
    throws IOException
  {
    out.println("isValid = true;");
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generatePostCall(JavaWriter out)
    throws IOException
  {
    out.popDepth();
    out.println("} finally {");
    out.println("  thread.setContextClassLoader(oldLoader);");

    out.println("  if (bean == null) {");
    out.println("  } else if (isValid)");
    out.println("    _ejb_free(bean);");
    
    out.println("}");
  }
}
