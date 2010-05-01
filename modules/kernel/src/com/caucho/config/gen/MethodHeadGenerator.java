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

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;

import javax.ejb.ApplicationException;
import javax.enterprise.inject.spi.AnnotatedMethod;

import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Generates the method aspect code for the head or proxy of the method.
 */
@Module
public class MethodHeadGenerator<X> extends AbstractAspectGenerator<X> {
  public MethodHeadGenerator(MethodHeadFactory<X> factory,
                             AnnotatedMethod<? super X> method,
                             AspectGenerator<X> next)
  {
    super(factory, method, next);
  }
 
  protected Class<?> []getThrowsExceptions()
  {
    return getJavaMethod().getExceptionTypes();
  }

  //
  // business method interception
  //

  //
  // generation for the actual method
  //

  /**
   * Generates the overridden method.
   */
  public final void generate(JavaWriter out,
                             HashMap<String,Object> prologueMap)
    throws IOException
  {
    generateMethodPrologue(out, prologueMap);
    
    String suffix = "";
    
    /*
    if (isAsync()) {
      suffix = "__caucho_async";
      
      generateHeader(out, "");

      out.println("{");
      out.pushDepth();

      generateAsync(out);

      out.popDepth();
      out.println("}");
    }
    */

    generateHeader(out, suffix, getJavaMethod(), getThrowsExceptions());

    out.println("{");
    out.pushDepth();

    generateContent(out);

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the method's signature before the call:
   *
   * <code><pre>
   * MyValue myCall(int a0, String, a1, ...)
   *   throws MyException, ...
   * </pre><?code>
  */
  public static void generateHeader(JavaWriter out,
                                    String suffix,
                                    Method method,
                                    Class<?> []exnList)
    throws IOException
  {
    out.println();
    
    int modifiers = method.getModifiers();
    
    if (Modifier.isPublic(modifiers))
      out.print("public ");
    else if (Modifier.isProtected(modifiers))
      out.print("protected ");
    else
      throw new IllegalStateException(method.toString()
                                      + " must be public or protected");

    out.printClass(method.getReturnType());
    out.print(" ");
    out.print(method.getName() + suffix);
    out.print("(");

    Class<?>[] types = method.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      Class<?> type = types[i];

      if (i != 0)
        out.print(", ");

      if (i == types.length - 1 && type.isArray() && method.isVarArgs()) {
        out.printClass(type.getComponentType());
        out.print("...");
      } else
        out.printClass(type);

      out.print(" a" + i);
    }

    out.println(")");
    
    generateThrows(out, exnList);
  }

  /**
   * Generates the method's "throws" declaration in the
   * method signature.
   *
   * @param out generated Java output
   * @param exnCls the exception classes
   */
  protected static void generateThrows(JavaWriter out,
                                       Class<?>[] exnCls)
    throws IOException
  {
    if (exnCls.length == 0)
      return;

    out.print(" throws ");

    for (int i = 0; i < exnCls.length; i++) {
      if (i != 0)
        out.print(", ");

      out.printClass(exnCls[i]);
    }
    out.println();
  }
  
  /**
   * Generates the body of the method.
   *
   * <code><pre>
   * MyType myMethod(...)
   * {
   *   [pre-try]
   *   try {
   *     [pre-call]
   *     [call]   // retValue = super.myMethod(...)
   *     [post-call]
   *     return retValue;
   *   } catch (RuntimeException e) {
   *     [system-exception]
   *     throw e;
   *   } catch (Exception e) {
   *     [application-exception]
   *     throw e;
   *   } finally {
   *     [finally]
   *   }
   * </pre></code>
   */
  protected void generateContent(JavaWriter out)
    throws IOException
  {
    generatePreTry(out);

    out.println();
    out.println("try {");
    out.pushDepth();

    Method method = getJavaMethod();
    
    if (! void.class.equals(method.getReturnType())) {
      out.printClass(method.getReturnType());
      out.println(" result;");
    }

    generatePreCall(out);

    generateCall(out);

    generatePostCall(out);

    if (! void.class.equals(method.getReturnType()))
      out.println("return result;");

    out.popDepth();

    generateExceptions(out);

    out.println("} finally {");
    out.pushDepth();

    generateFinally(out);

    out.popDepth();
    out.println("}");
  }

  private void generateExceptions(JavaWriter out)
    throws IOException
  {
    HashSet<Class<?>> exceptionSet
      = new HashSet<Class<?>>();

    for (Class<?> exn : getThrowsExceptions()) {
      exceptionSet.add(exn);
    }

    exceptionSet.add(RuntimeException.class);

    Class<?> exn;
    while ((exn = selectException(exceptionSet)) != null) {
      boolean isSystemException
        = (RuntimeException.class.isAssignableFrom(exn)
            && ! exn.isAnnotationPresent(ApplicationException.class));

      out.println("} catch (" + exn.getName() + " e) {");
      out.pushDepth();

      if (isSystemException)
        generateSystemException(out, exn);
      else
        generateApplicationException(out, exn);

      out.println();
      out.println("throw e;");

      out.popDepth();
    }
  }

  private Class<?> selectException(HashSet<Class<?>> exnSet)
  {
    for (Class<?> exn : exnSet) {
      if (isMostSpecific(exn, exnSet)) {
        exnSet.remove(exn);

        return exn;
      }
    }

    return null;
  }

  private boolean isMostSpecific(Class<?> exn, HashSet<Class<?>> exnSet)
  {
    for (Class<?> testExn : exnSet) {
      if (exn == testExn)
        continue;

      if (exn.isAssignableFrom(testExn))
        return false;
    }

    return true;
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (!(o instanceof MethodHeadGenerator<?>))
      return false;

    MethodHeadGenerator<?> bizMethod = (MethodHeadGenerator<?>) o;

    return getJavaMethod().getName().equals(bizMethod.getJavaMethod().getName());
  }
}
