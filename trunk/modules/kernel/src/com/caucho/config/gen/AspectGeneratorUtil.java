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
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;

import com.caucho.config.reflect.BaseType;
import com.caucho.config.reflect.VarType;
import com.caucho.inject.Module;
import com.caucho.java.JavaWriter;

/**
 * Generates the method aspect code for the head or proxy of the method.
 */
@Module
public class AspectGeneratorUtil {
  /**
   * Generates the method's signature before the call:
   *
   * <code><pre>
   * MyValue myCall(int a0, String, a1, ...)
   *   throws MyException, ...
   * </pre><?code>
   * @param prefix TODO
  */
  public static void generateHeader(JavaWriter out,
                                    boolean isOverride,
                                    String accessModifier,
                                    String methodName,
                                    AnnotatedMethod<?> method,
                                    Set<VarType<?>> typeVariables,
                                    Class<?> []exnList)
    throws IOException
  {
    out.println();
    
    if (isOverride)
      out.println("@Override");

    if (accessModifier != null) {
      out.print(accessModifier);
      out.print(" ");
    }
        
    if (typeVariables != null && typeVariables.size() > 0) {
      out.print("<");
      boolean isFirst = true;
      for (VarType<?> var : typeVariables) {
        if (! isFirst)
          out.print(",");
        isFirst = false;
        
        out.printVarType(var);
      }
      out.println(">");
    }

    Type returnType = method.getBaseType();

    out.printType(returnType);
    out.print(" ");
    out.print(methodName);
    out.print("(");
    
    Method javaMethod = method.getJavaMember();

    List<AnnotatedParameter<?>> params = (List) method.getParameters();
    for (int i = 0; i < params.size(); i++) {
      AnnotatedParameter<?> param = params.get(i);
      Type type = param.getBaseType();
      Class<?> cl = null;
      
      if (type instanceof Class<?>)
        cl = (Class<?>) type;

      if (i != 0)
        out.print(", ");

      if (i == params.size() - 1 && cl != null && cl.isArray() && javaMethod.isVarArgs()) {
        out.printClass(cl.getComponentType());
        out.print("...");
      } else
        out.printType(type);

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
}
