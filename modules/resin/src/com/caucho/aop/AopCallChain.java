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

package com.caucho.aop;

import java.io.IOException;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

import com.caucho.java.gen.FilterCallChain;
import com.caucho.java.gen.CallChain;

/**
 * Enhancing a method objects.
 */
public class AopCallChain extends FilterCallChain {
  private static final L10N L = new L10N(AopCallChain.class);

  private AopVarComponent _aopVar;
  
  public AopCallChain(CallChain next, AopVarComponent var)
  {
    super(next);
    
    _aopVar = var;
  }

  /**
   * Generates the code for the method call.
   *
   * @param out the writer to the output stream.
   * @param retVar the variable to hold the return value
   * @param var the object to be called
   * @param args the method arguments
   */
  public void generateCall(JavaWriter out, String retVar,
			   String var, String []args)
    throws IOException
  {
    String fieldName = _aopVar.getFieldName();
    
    out.print(fieldName + " invocation");
    out.print(" = new " + fieldName + "(this");

    for (int i = 0; i < args.length; i++) {
      out.print(", ");
      out.print(args[i]);
    }
    out.println(");");

    out.println("try {");
    out.pushDepth();
    
    out.println("Object result = invocation._interceptor.invoke(invocation);");

    out.popDepth();
    out.println("} catch (Throwable e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
  }
}
