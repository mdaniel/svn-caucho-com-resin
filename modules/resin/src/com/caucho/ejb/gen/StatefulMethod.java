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

import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.security.*;
import javax.ejb.*;
import javax.interceptor.*;

/**
 * Represents a stateful local business method
 */
public class StatefulMethod extends BusinessMethodGenerator
{
  private boolean _isRemove;
  private boolean _isRemoveRetainIfException;
  
  public StatefulMethod(StatefulView view,
			ApiMethod apiMethod,
			Method implMethod,
			int index)
  {
    super(view, apiMethod, implMethod, index);
  }

  @Override
  public void setRemove(boolean isRemove)
  {
    _isRemove = isRemove;
  }

  @Override
  public void setRemoveRetainIfException(boolean isRetain)
  {
    _isRemoveRetainIfException = isRetain;
  }

  /**
   * Session bean default is REQUIRED
   */
  @Override
  public void introspect(Method apiMethod, Method implMethod)
  {
    getXa().setTransactionType(getDefaultTransactionType());

    super.introspect(apiMethod, implMethod);

    Remove remove = implMethod.getAnnotation(Remove.class);
    if (remove != null) {
      _isRemove = true;
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

  protected void generateContent(JavaWriter out)
    throws IOException
  {
    if (getView().isRemote()
	&& hasException(java.rmi.NoSuchObjectException.class)) {
      out.println("if (_bean == null)");
      out.println("  throw new java.rmi.NoSuchObjectException(\"stateful instance "
		  + getEjbClass().getSimpleName() + " is no longer valid\");");
    }
    else {
      out.println("if (_bean == null)");
      out.println("  throw new javax.ejb.NoSuchEJBException(\"stateful instance "
		  + getEjbClass().getSimpleName() + " is no longer valid\");");
    }

    if (_isRemove) {
      out.println("boolean isRemove = false;");
      out.println("try {");
      out.pushDepth();
    }
    
    super.generateContent(out);

    if (_isRemove) {
      out.println("isRemove = true;");
      
      out.popDepth();

      if (_isRemoveRetainIfException) {
	out.println("} catch (RuntimeException e) {");
	out.println("  isRemove = true;");
	out.println("  throw e;");
      }
      
      out.println("} finally {");
      
      if (_isRemoveRetainIfException) {
	out.println("if (isRemove) {");
	out.pushDepth();
      }
      
      out.println("  Object bean = _bean;");
      out.println("  _bean = null;");
      out.println("  _server.destroyInstance(bean);");
      
      if (_isRemoveRetainIfException) {
	out.popDepth();
	out.println("}");
      }
      out.println("}");
    }
  }

  protected void generatePreCall(JavaWriter out)
    throws IOException
  {
    out.println("if (_isActive)");
    out.println("  throw new EJBException(\"session bean is not reentrant\");");
    out.println();
    
    out.println("Thread thread = Thread.currentThread();");
    out.println("ClassLoader oldLoader = thread.getContextClassLoader();");
    out.println("try {");
    out.pushDepth();
    out.println("thread.setContextClassLoader(_server.getClassLoader());");
    out.println("_isActive = true;");
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generateThis(JavaWriter out)
    throws IOException
  {
    out.print("_bean");
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generateSuper(JavaWriter out)
    throws IOException
  {
    out.print("_bean");
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
    out.println("  _isActive = false;");
    
    out.println("}");
  }
}
