/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Represents a lifecycle business method
 */
@Module
public class LifecycleMethodTailGenerator<X> extends MethodTailGenerator<X>
{
  private String _invokeMethodName;
  
  public LifecycleMethodTailGenerator(LifecycleMethodTailFactory<X> factory,
                                      AnnotatedMethod<? super X> method)
  {
    super(factory, method);
    
    Method javaMethod = method.getJavaMember();
    
    String declName = javaMethod.getDeclaringClass().getSimpleName();
    String methodName = javaMethod.getName();
    _invokeMethodName = "__caucho_postConstruct_" + declName + "_" + methodName;
    // _invokeMethodName = "__caucho_postConstructImpl";
  }
  
  @Override
  public void generateMethodPrologue(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    Method method = getMethod().getJavaMember();
    
    out.println("private static final java.lang.reflect.Method " + _invokeMethodName);
    out.print(" = " + CandiUtil.class.getName() + ".findMethod(");
    out.print(method.getDeclaringClass().getName());
    out.println(".class, \"" + method.getName() + "\");");
    
    out.println("static {");
    out.println("  " + _invokeMethodName + ".setAccessible(true);");
    out.println("}");
    
    out.println();
    out.println("public void " + _invokeMethodName + "()");
    out.println("{");
    out.pushDepth();
    
    out.println("try {");
    out.pushDepth();
    
    String superVar = _factory.getAspectBeanFactory().getInterceptorInstance();

    out.println("if (" + superVar + " != null)");
    out.println("  " + _invokeMethodName + ".invoke(" + superVar + ");");
    
    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (java.lang.reflect.InvocationTargetException e) {");
    out.println("  if (e.getCause() instanceof RuntimeException)");
    out.println("    throw (RuntimeException) e.getCause();");
    out.println("  else");
    out.println("  throw new RuntimeException(e.getCause());");
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");

    out.popDepth();
    out.println("}");
  }
  
  @Override
  public void generateCall(JavaWriter out)
    throws IOException
  {
    Method method = getMethod().getJavaMember();
    String methodName = method.getName();
    
    out.println(_invokeMethodName + "();");
    /*
    String superVar = _factory.getAspectBeanFactory().getBeanSuper();
 
    out.println("try {");
    out.pushDepth();
    
    out.println(_invokeMethodName + ".invoke(" + superVar + ");");
    
    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (java.lang.reflect.InvocationTargetException e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
    */
  }
}
