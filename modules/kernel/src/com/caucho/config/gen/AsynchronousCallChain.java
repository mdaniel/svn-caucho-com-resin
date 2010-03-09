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
import java.util.HashMap;

import javax.ejb.Asynchronous;

import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

/**
 * Represents the @Asynchronous interception
 */
public class AsynchronousCallChain extends AbstractCallChain {
  private static final L10N L = new L10N(AsynchronousCallChain.class);

  private BusinessMethodGenerator _bizMethod;
  private EjbCallChain _next;

  private boolean _isAsynchronous;

  private String _methodId;

  private String _varName;
 
  public AsynchronousCallChain(BusinessMethodGenerator bizMethod,
                               EjbCallChain next)
  {
    super(next);
    
    _bizMethod = bizMethod;
    _next = next;
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

  /**
   * Introspect EJB security annotations:
   *   @RunAs
   *   @RolesAllowed
   *   @PermitAll
   *   @DenyAll
   */
  @Override
  public void introspect(ApiMethod apiMethod, ApiMethod implMethod)
  {
    ApiClass apiClass = apiMethod.getDeclaringClass();
    ApiClass implClass = null;

    if (implMethod != null) {
      implClass = implMethod.getDeclaringClass();
    }
    
    if (implMethod != null && implMethod.isAnnotationPresent(Asynchronous.class))
      _isAsynchronous = true;
    
    if (implClass != null && implClass.isAnnotationPresent(Asynchronous.class))
      _isAsynchronous = true;
    
    if (apiMethod != null && apiMethod.isAnnotationPresent(Asynchronous.class))
      _isAsynchronous = true;
    
    if (apiClass != null && apiClass.isAnnotationPresent(Asynchronous.class))
      _isAsynchronous = true;
    
    if (! _isAsynchronous)
      return;

    if (! void.class.equals(apiMethod.getReturnType())) {
      throw ConfigException.create(apiMethod.getMethod(),
                                   L.l("@Asynchronous method must return void"));
    }
  }
  
  //
  // business method interception
  //
  
  @Override
  public void generateMethodPrologue(JavaWriter out, HashMap<String,Object> map)
    throws IOException
  {
    super.generateMethodPrologue(out, map);
    
    if (_isAsynchronous) {
      _varName = "_caucho_async_" + out.generateId();
      
      out.print("private com.caucho.config.async.AsyncQueue " + _varName);
      out.println(" = new com.caucho.config.async.AsyncQueue();");
    }
  }
  
  @Override
  public void generateAsync(JavaWriter out)
    throws IOException
  {
    if (! _isAsynchronous) {
      return;
    }
    
    Class<?> []paramType = _bizMethod.getApiMethod().getParameterTypes();
    
    for (int i = 0; i < paramType.length; i++) {
      out.print("final ");
      out.printClass(paramType[i]);
      out.println(" ac_" + i + " = a" + i + ";");
    }
    
    out.println("com.caucho.config.async.AsyncItem task;");
    out.println("task = new com.caucho.config.async.AsyncItem() {");
    out.pushDepth();
    
    out.println("public void runTask() throws Exception");
    out.println("{");
    out.pushDepth();
    
    out.print(_bizMethod.getApiMethod().getName() + "__caucho_async(");
    
    for (int i = 0; i < paramType.length; i++) {
      if (i != 0)
        out.print(", ");
      
      out.print("ac_" + i);
    }
    
    out.println(");");
    
    out.popDepth();
    out.println("}");
    
    out.popDepth();
    out.println("};");
    
    out.println(_varName + ".offer(task);");
  }
}
