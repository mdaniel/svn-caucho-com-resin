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

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.annotation.security.*;
import javax.ejb.*;
import javax.interceptor.*;

/**
 * Represents a business method
 */
public class BusinessMethodGenerator implements EjbCallChain {
  private static final L10N L = new L10N(BusinessMethodGenerator.class);
  
  private Method _apiMethod;
  private Method _implMethod;

  private String _uniqueName;

  private boolean _isEnhanced = true;

  private String []_roles;
  private String _roleVar;

  private String _runAs;

  private XaCallChain _xa;
  private InterceptorCallChain _interceptor;
  
  public BusinessMethodGenerator(Method apiMethod,
				 Method implMethod,
				 int index)
  {
    _apiMethod = apiMethod;
    _implMethod = implMethod;

    _uniqueName = "_" + _apiMethod.getName() + "_" + index;

    _interceptor = new InterceptorCallChain(this);
    _xa = new XaCallChain(_interceptor);

    introspect(apiMethod, implMethod);
  }
  
  /**
   * Returns true if the business method has any active XA annotation.
   */
  public boolean hasXA()
  {
    return _xa.isEnhanced();
  }

  /**
   * Returns the xa call chain
   */
  public XaCallChain getXa()
  {
    return _xa;
  }

  /**
   * Returns the interceptor call chain
   */
  public InterceptorCallChain getInterceptor()
  {
    return _interceptor;
  }

  /**
   * Returns true if any interceptors enhance the business method
   */
  public boolean isEnhanced()
  {
    if (_roles != null)
      return true;
    else if (_runAs != null)
      return true;
    else if (_xa.isEnhanced())
      return true;
    else if (_interceptor.isEnhanced())
      return true;

    return false;
  }

  public void introspect(Method apiMethod, Method implMethod)
  {
    Class cl = implMethod.getDeclaringClass();

    introspectSecurity(cl);
    _xa.introspect(apiMethod, implMethod);
    _interceptor.introspect(apiMethod, implMethod);
  }
  
  /**
   * Introspect EJB security annotations:
   *   @RunAs
   *   @RolesAllowed
   *   @PermitAll
   *   @DenyAll
   */
  protected void introspectSecurity(Class cl)
  {
    RunAs runAs = (RunAs) cl.getAnnotation(RunAs.class);

    if (runAs != null)
      _runAs = runAs.value();
    
    RolesAllowed rolesAllowed
      = (RolesAllowed) cl.getAnnotation(RolesAllowed.class);
    
    if (rolesAllowed != null)
      _roles = rolesAllowed.value();
    
    PermitAll permitAll = (PermitAll) cl.getAnnotation(PermitAll.class);

    if (permitAll != null)
      _roles = null;
    
    DenyAll denyAll = (DenyAll) cl.getAnnotation(DenyAll.class);

    if (denyAll != null)
      _roles = new String[0];

    // 
    
    rolesAllowed = _apiMethod.getAnnotation(RolesAllowed.class);

    if (rolesAllowed != null)
      _roles = rolesAllowed.value();
    
    permitAll = (PermitAll) _apiMethod.getAnnotation(PermitAll.class);

    if (permitAll != null)
      _roles = null;
    
    denyAll = (DenyAll) _apiMethod.getAnnotation(DenyAll.class);

    if (denyAll != null)
      _roles = new String[0];
  }

  public void generate(JavaWriter out, HashMap prologueMap)
    throws IOException
  {
    if (! isEnhanced())
      return;

    generatePrologue(out, prologueMap);

    out.println();
    if (Modifier.isPublic(_apiMethod.getModifiers()))
      out.print("public ");
    else if (Modifier.isProtected(_apiMethod.getModifiers()))
      out.print("protected ");
    else
      throw new IllegalStateException(_apiMethod.toString() + " must be public or protected");

    out.printClass(_apiMethod.getReturnType());
    out.print(" ");
    out.print(_apiMethod.getName());
    out.print("(");

    Class []types = _apiMethod.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      if (i != 0)
	out.print(", ");

      out.printClass(types[i]);
      out.print(" a" + i);
    }
    
    out.println(")");
    generateThrows(out, _apiMethod.getExceptionTypes());
    out.println();
    out.println("{");
    out.pushDepth();

    generateContent(out);

    out.popDepth();
    out.println("}");
  }

  protected void generateContent(JavaWriter out)
    throws IOException
  {
    generateSecurity(out);
  }

  public void generatePrologue(JavaWriter out, HashMap map)
    throws IOException
  {
    if (_roles != null) {
      _roleVar = "_role_" + out.generateId();

      out.print("private static String []" + _roleVar + " = new String[] {");

      for (int i = 0; i < _roles.length; i++) {
	if (i != 0)
	  out.print(", ");

	out.print("\"");
	out.printJavaString(_roles[i]);
	out.print("\"");
      }

      out.println("};");
    }

    _xa.generatePrologue(out, map);
  }

  protected void generateSecurity(JavaWriter out)
    throws IOException
  {
    if (_roleVar != null) {
      out.println("com.caucho.security.SecurityContext.checkUserInRole(" + _roleVar + ");");
      out.println();
    }

    generateRunAs(out);
  }

  protected void generateRunAs(JavaWriter out)
    throws IOException
  {
    if (_runAs != null) {
      out.print("String oldRunAs ="
		+ " com.caucho.security.SecurityContext.runAs(\"");
      out.printJavaString(_runAs);
      out.println("\");");

      out.println("try {");
      out.pushDepth();
    }

    _xa.generateCall(out);

    if (_runAs != null) {
      out.popDepth();
      out.println("} finally {");
      out.println("  com.caucho.security.SecurityContext.runAs(oldRunAs);");
      out.println("}");
    }
  }

  protected void generateThrows(JavaWriter out, Class []exnCls)
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
  }

  public void generateCall(JavaWriter out)
    throws IOException
  {
    if (! void.class.equals(_implMethod.getReturnType())) {
      out.printClass(_implMethod.getReturnType());
      out.println(" result;");
    }
    
    generatePreCall(out);
    
    if (! void.class.equals(_implMethod.getReturnType()))
      out.print("result = ");

    generateSuper(out);
    out.print("." + _implMethod.getName() + "(");

    Class []types = _implMethod.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      if (i != 0)
	out.print(", ");

      out.print(" a" + i);
    }
    
    out.println(");");

    generatePostCall(out);
    
    if (! void.class.equals(_implMethod.getReturnType()))
      out.println("return result;");
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generatePreCall(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generateSuper(JavaWriter out)
    throws IOException
  {
    out.print("super");
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generateThis(JavaWriter out)
    throws IOException
  {
    out.print("this");
  }

  /**
   * Generates the underlying bean instance
   */
  protected void generatePostCall(JavaWriter out)
    throws IOException
  {
  }

  boolean matches(String name, Class[] parameterTypes)
  {
    if (! _apiMethod.getName().equals(name))
      return false;
    
    Class []methodTypes = _apiMethod.getParameterTypes();
    if (methodTypes.length != parameterTypes.length)
      return false;
    
    for (int i = 0; i < parameterTypes.length; i++) {
      if (! methodTypes[i].equals(parameterTypes[i]))
        return false;
    }
    
    return true;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _apiMethod + "]";
  }
}
