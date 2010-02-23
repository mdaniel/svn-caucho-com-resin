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

package com.caucho.ejb.gen;

import java.io.IOException;

import javax.ejb.Remove;
import javax.ejb.TransactionAttributeType;

import com.caucho.config.gen.ApiMethod;
import com.caucho.config.gen.BusinessMethodGenerator;
import com.caucho.java.JavaWriter;

/**
 * Represents a stateful local business method
 */
public class StatefulMethod extends BusinessMethodGenerator
{
  private boolean _isRemoveRetainIfException;
  
  public StatefulMethod(StatefulView view,
                        ApiMethod apiMethod,
                        ApiMethod implMethod,
                        int index)
  {
    super(view, apiMethod, implMethod, index);
  }

  @Override
  public void setRemoveRetainIfException(boolean isRetain)
  {
    _isRemoveRetainIfException = isRetain;
  }

  protected boolean isProxy()
  {
    return getApiMethod() != getImplMethod();
  }

  /**
   * Session bean default is REQUIRED
   */
  @Override
  public void introspect(ApiMethod apiMethod, ApiMethod implMethod)
  {
    // getXa().setTransactionType(getDefaultTransactionType());

    super.introspect(apiMethod, implMethod);

    Remove remove = implMethod.getAnnotation(Remove.class);
    if (remove != null) {
      _isRemoveRetainIfException = remove.retainIfException();
    }
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
  
  @Override
  public void generatePreTry(JavaWriter out)
    throws IOException
  {
    super.generatePreTry(out);
    
    if (getView().isRemote()
        && hasException(java.rmi.NoSuchObjectException.class)) {
      out.println("if (! _isValid)");
      out.println("  throw new java.rmi.NoSuchObjectException(\"stateful instance "
                  + getBeanClass().getSimpleName() + " is no longer valid\");");
    }
    else {
      out.println("if (! _isValid)");
      out.println("  throw new javax.ejb.NoSuchEJBException(\"stateful instance "
                  + getBeanClass().getSimpleName() + " is no longer valid\");");
    }

    out.println("boolean isValid = false;");
    
    out.println("if (_isActive)");
    out.println("  throw new EJBException(\"session bean is not reentrant\");");
    out.println();
    
    out.println("Thread thread = Thread.currentThread();");
    out.println("ClassLoader oldLoader = thread.getContextClassLoader();");
  }

  @Override
  public void generatePreCall(JavaWriter out)
    throws IOException
  {
    out.println("thread.setContextClassLoader(_server.getClassLoader());");
    out.println("_isActive = true;");
    
    super.generatePreCall(out);
  }

  /**
   * Generates the underlying bean instance
   */
  @Override
  public void generatePostCall(JavaWriter out)
    throws IOException
  {
    out.println("isValid = true;");
  }

  /**
   * Generates the underlying bean instance
   */
  @Override
  public void generateApplicationException(JavaWriter out,
                                           Class<?> exn)
    throws IOException
  {
    out.println("isValid = true;");
  }

  @Override
  public void generateFinally(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("_isActive = false;");
    out.println();
    
    if (! _isRemoveRetainIfException) {
      out.println("if (! isValid) {");
      out.pushDepth();
    
      out.println("boolean isOldValid = _isValid;");
      out.println("_isValid = false;");
      out.println();
      out.println("if (isOldValid)");
      out.print("  _server.destroyInstance(");
      generateThis(out);
      out.println(");");
    
      out.popDepth();
      out.println("}");
    }
    
    out.println("thread.setContextClassLoader(oldLoader);");
    
    super.generateFinally(out);
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generateThis(JavaWriter out)
    throws IOException
  {
    if (isProxy())
      out.print("_bean");
    else
      out.print("this");
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generateSuper(JavaWriter out)
    throws IOException
  {
    out.print(getSuper());
  }

  /**
   * Generates the underlying bean instance
   */
  @Override
  protected String getSuper()
    throws IOException
  {
    if (isProxy())
      return "_bean";
    else {
      return "super";
    }
  }
}
