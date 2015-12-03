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
 * Represents the @Asynchronous interception
 */
@Module
public class AsynchronousGenerator<X> extends NullGenerator<X> {
  private boolean _isAsynchronous;

  private String _varName;
  private AspectGenerator<X> _head;
  private AnnotatedMethod<? super X> _method;
 
  public AsynchronousGenerator(AsynchronousFactory<X> factory,
                               AnnotatedMethod<? super X> method,
                               AspectGenerator<X> head)
  {
    _method = method;
    _head = head;
  }
  
  /**
   * Returns true if the business method has any active XA annotation.
   */
  public boolean isEnhanced()
  {
    return _isAsynchronous;
  }
  
  public boolean isAsync()
  {
    return _isAsynchronous;
  }
  //
  // business method interception
  //
  
  @Override
  public void generateMethodPrologue(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    _head.generate(out, map);
    
    _varName = "_caucho_async_" + out.generateId();
      
    out.print("private com.caucho.config.async.AsyncQueue " + _varName);
    out.println(" = new com.caucho.config.async.AsyncQueue();");
  }
  
  @Override
  public void generate(JavaWriter out, HashMap<String, Object> prologueMap)
    throws IOException
  {
    _head.generate(out, prologueMap);
  }

  @Override
  public void generateCall(JavaWriter out)
    throws IOException
  {
    Method javaApiMethod = getJavaMethod();
    
    Class<?> []paramType = javaApiMethod.getParameterTypes();
    
    for (int i = 0; i < paramType.length; i++) {
      out.print("final ");
      out.printClass(paramType[i]);
      out.println(" ac_" + i + " = a" + i + ";");
    }
    
    out.println("com.caucho.config.async.AsyncItem task;");
    out.println("task = new com.caucho.config.async.AsyncItem() {");
    out.pushDepth();
    
    out.print("private ");
    // out.print(ProtocolConnection.class.getName());
    out.print("com.caucho.network.listen.ProtocolConnection");
    out.println(" _requestContext");
    out.print("  = ");
    // out.print(CandiUtil.class.getName());
    out.print("com.caucho.ejb.util.EjbUtil");
    out.println(".createRequestContext();");
    
    out.println();
    out.println("public java.util.concurrent.Future runTask() throws Exception");
    out.println("{");
    out.pushDepth();
    
    out.println();
    // out.print(ProtocolConnection.class.getName());
    out.print("com.caucho.network.listen.ProtocolConnection");
    out.print(" oldContext = ");
    // out.print(TcpSocketLink.class.getName());
    out.print("com.caucho.network.listen.TcpSocketLink");
    out.println(".getCurrentRequest();");
    out.println();
    
    out.println("try {");
    out.pushDepth();
    // out.print(TcpSocketLink.class.getName());
    out.print("com.caucho.network.listen.TcpSocketLink");
    out.println(".setCurrentRequest(_requestContext);");
    out.println();
    
    if (! void.class.equals(javaApiMethod.getReturnType()))
      out.print("return ");
    
    out.print(javaApiMethod.getName() + "_async(");
    
    for (int i = 0; i < paramType.length; i++) {
      if (i != 0)
        out.print(", ");
      
      out.print("ac_" + i);
    }
    
    out.println(");");
    
    if (void.class.equals(javaApiMethod.getReturnType())) {
      out.println();
      out.println("return null;");
    }

    out.popDepth();
    out.println("} finally {");
    out.pushDepth();
    
    // out.print(TcpSocketLink.class.getName());
    out.print("com.caucho.network.listen.TcpSocketLink");
    out.println(".setCurrentRequest(oldContext);");
    
    out.popDepth();
    out.println("}");
    
    out.popDepth();
    out.println("}");
    
    out.popDepth();
    out.println("};");
    
    out.println(_varName + ".offer(task);");
    
    if (! void.class.equals(javaApiMethod.getReturnType())) {
      out.println("result = task;");
    }
  }
  
  protected Method getJavaMethod()
  {
    return _method.getJavaMember();
  }

}
