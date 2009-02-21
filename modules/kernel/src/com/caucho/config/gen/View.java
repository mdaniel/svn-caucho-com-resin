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

import javax.ejb.*;
import javax.decorator.Decorator;
import javax.interceptor.Interceptor;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Represents a public interface to a bean, e.g. a local stateful view
 */
abstract public class View {
  private static final L10N L = new L10N(View.class);

  protected final BeanGenerator _bean;
  protected final ApiClass _api;

  protected ArrayList<Annotation> _interceptorBindings;

  protected View(BeanGenerator bean, ApiClass api)
  {
    _bean = bean;
    _api = api;

    _bean.addDependency(api.getJavaClass());
  }

  /**
   * Returns the owning bean.
   */
  protected BeanGenerator getBean()
  {
    return _bean;
  }
  
  /**
   * Returns the bean's ejbclass
   */
  protected ApiClass getEjbClass()
  {
    return _bean.getEjbClass();
  }

  protected String getViewClassName()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  protected String getBeanClassName()
  {
    return getViewClassName();
  }

  public boolean isRemote()
  {
    return false;
  }

  /**
   * Returns the API class.
   */
  protected ApiClass getApi()
  {
    return _api;
  }

  /**
   * Returns any interceptor bindings
   */
  public ArrayList<Annotation> getInterceptorBindings()
  {
    return _interceptorBindings;
  }

  /**
   * Sets any interceptor bindings
   */
  public void setInterceptorBindings(ArrayList<Annotation> bindings)
  {
    if (_bean.isAnnotationPresent(Interceptor.class)
	|| _bean.isAnnotationPresent(Decorator.class)) {
      throw new IllegalStateException(L.l("{0}: invalid because introspectors and decorators may not have interceptors",
					  this));
    }
    
    _interceptorBindings = bindings;
  }

  /**
   * Introspects the view
   */
  public void introspect()
  {
  }

  /**
   * Returns the introspected methods
   */
  public ArrayList<? extends BusinessMethodGenerator> getMethods()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns any around-invoke method
   */
  public Method getAroundInvokeMethod()
  {
    return getBean().getAroundInvokeMethod();
  }

  /**
   * Generates prologue for the context.
   */
  public void generateContextPrologue(JavaWriter out)
    throws IOException
  {
    
  }

  /**
   * Generates context home's constructor
   */
  public void generateContextHomeConstructor(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates context object's constructor
   */
  public void generateContextObjectConstructor(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates timer code
   */
  public void generateTimer(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates any global destroy
   */
  public void generateDestroy(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the view code.
   */
  abstract public void generate(JavaWriter out)
    throws IOException;

  /**
   * Generates constructor addiontions
   */
  public void generateBusinessConstructor(JavaWriter out)
    throws IOException
  {
    HashMap map = new HashMap();
    generateBusinessConstructor(out, map);
  }

  /**
   * Generates constructor addiontions
   */
  public void generateBusinessConstructor(JavaWriter out, HashMap map)
    throws IOException
  {
    for (BusinessMethodGenerator method : getMethods()) {
      method.generateConstructorTop(out, map);
    }
  }

  /**
   * Generates prologue additions
   */
  public void generateBusinessPrologue(JavaWriter out)
    throws IOException
  {
    generateBusinessPrologue(out, new HashMap());
  }

  /**
   * Generates prologue additions
   */
  public void generateBusinessPrologue(JavaWriter out, HashMap map)
    throws IOException
  {
    for (BusinessMethodGenerator method : getMethods()) {
      method.generatePrologueTop(out, map);
    }
  }

  protected void generatePostConstruct(JavaWriter out)
     throws IOException
   {
     out.println();
     out.println("private void __caucho_postConstruct()");
     out.println("{");
     out.pushDepth();

     HashMap map = new HashMap();
     for (BusinessMethodGenerator method : getMethods()) {
       method.generatePostConstruct(out, map);
     }

     out.popDepth();
     out.println("}");
   }


  /**
   * Generates view's business methods
   */
  public void generateBusinessMethods(JavaWriter out)
    throws IOException
  {
    HashMap map = new HashMap();
    for (BusinessMethodGenerator method : getMethods()) {
      method.generate(out, map);
    }
  }

  /**
   * Returns a full method name with arguments.
   */
  public static String getFullMethodName(ApiMethod method)
  {
    return getFullMethodName(method.getName(), method.getParameterTypes());
  }

  /**
   * Returns a full method name with arguments.
   */
  public static String getFullMethodName(Method method)
  {
    return getFullMethodName(method.getName(), method.getParameterTypes());
  }

  /**
   * Returns a full method name with arguments.
   */
  public static String getFullMethodName(String methodName, Class []params)
  {
    String name = methodName + "(";

    for (int i = 0; i < params.length; i++) {
      if (i != 0)
        name += ", ";

      name += params[i].getSimpleName();
    }

    return name + ")";
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getEjbClass() + "]";
  }
}
