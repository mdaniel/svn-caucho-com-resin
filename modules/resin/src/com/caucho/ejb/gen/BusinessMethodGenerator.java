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
public class BusinessMethodGenerator {
  private static final L10N L = new L10N(BusinessMethodGenerator.class);
  
  private Method _method;

  private String _uniqueName;

  private boolean _isEnhanced = true;

  private String []_roles;
  private String _roleVar;

  private String _runAs;

  private TransactionAttributeType _xa;
  
  private ArrayList<Class> _interceptors = new ArrayList<Class>();
  
  public BusinessMethodGenerator(Method method, int index)
  {
    _method = method;

    _uniqueName = "_" + _method.getName() + "_" + index;

    introspect();
  }
  
  /**
   * Returns true if the business method has any active XA annotation.
   */
  public boolean hasXA()
  {
    return (_xa != null && ! _xa.equals(TransactionAttributeType.SUPPORTS));
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
    else if (_xa != null && ! _xa.equals(TransactionAttributeType.SUPPORTS))
      return true;
    else if (_interceptors != null && _interceptors.size() > 0)
      return true;

    return false;
  }

  protected void introspect()
  {
    Class cl = _method.getDeclaringClass();

    introspectSecurity(cl);
    introspectTransaction(cl);
    introspectInterceptors(cl);
  }

  public ArrayList<Class> getInterceptors()
  {
    return _interceptors;
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
    
    rolesAllowed = _method.getAnnotation(RolesAllowed.class);

    if (rolesAllowed != null)
      _roles = rolesAllowed.value();
    
    permitAll = (PermitAll) _method.getAnnotation(PermitAll.class);

    if (permitAll != null)
      _roles = null;
    
    denyAll = (DenyAll) _method.getAnnotation(DenyAll.class);

    if (denyAll != null)
      _roles = new String[0];
  }

  /**
   * Introspects the @TransactionAttribute annotation on the method
   * and the class.
   */
  protected void introspectTransaction(Class cl)
  {
    TransactionAttribute xaAttr;
    
    xaAttr = _method.getAnnotation(TransactionAttribute.class);

    if (xaAttr == null) {
      xaAttr = (TransactionAttribute)
	cl.getAnnotation(TransactionAttribute.class);
    }

    if (xaAttr != null)
      _xa = xaAttr.value();
  }
  
  /**
   * Introspects the @Interceptors annotation on the method
   * and the class.
   */
  protected void introspectInterceptors(Class cl)
  {
    ArrayList<Class> interceptorList = new ArrayList<Class>();
    
    Interceptors interceptors
      = _method.getAnnotation(Interceptors.class);

    if (interceptors != null) {
      for (Class interceptorClass : interceptors.value())
	interceptorList.add(interceptorClass);
    }

    _interceptors = interceptorList;
  }

  public void generate(JavaWriter out)
    throws IOException
  {
    if (isEnhanced())
      return;

    generatePrologue(out);

    out.println();
    if (Modifier.isPublic(_method.getModifiers()))
      out.print("public ");
    else if (Modifier.isProtected(_method.getModifiers()))
      out.print("protected ");
    else
      throw new IllegalStateException(_method.toString() + " must be public or protected");

    out.printClass(_method.getReturnType());
    out.print(" ");
    out.print(_method.getName());
    out.print("(");

    Class []types = _method.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      if (i != 0)
	out.print(", ");

      out.printClass(types[i]);
      out.print(" a" + i);
    }
    
    out.println(")");
    generateThrows(out, _method.getExceptionTypes());

    out.println("{");
    out.pushDepth();

    generateSecurity(out);

    out.popDepth();
    out.println("}");
  }

  protected void generatePrologue(JavaWriter out)
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

    if (_interceptors != null && _interceptors.size() > 0) {
      generateInterceptorsPrologue(out);
    }
  }

  protected void generateInterceptorsPrologue(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("private static java.lang.reflect.Method " + _uniqueName + "_method;");
    out.println("private static java.lang.reflect.Method []" + _uniqueName + "_methodChain;");

    Class cl = _method.getDeclaringClass();
    
    out.println();
    out.println("static {");
    out.pushDepth();
    
    out.println("try {");
    out.pushDepth();
    
    out.print(_uniqueName + "_method = ");
    generateGetMethod(out, _method);
    out.println(";");

    out.println(_uniqueName + "_methodChain = new java.lang.reflect.Method[] {");
    out.pushDepth();
    
    for (Class iClass : _interceptors) {
      Method method = findInterceptorMethod(iClass);
      
      generateGetMethod(out, method);
      out.println(", ");
    }
    out.popDepth();
    out.println("};");
    
    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
    out.popDepth();
    out.println("}");
  }

  protected Method findInterceptorMethod(Class cl)
  {
    for (Method method : cl.getMethods()) {
      if (method.isAnnotationPresent(AroundInvoke.class))
	return method;
    }

    throw new IllegalStateException(L.l("Can't find @AroundInvoke in '{0}'",
					cl.getName()));
  }

  protected void generateGetMethod(JavaWriter out, Method method)
    throws IOException
  {
    Class cl = method.getDeclaringClass();
    
    out.print(cl.getName() + ".class");
    out.print(".getMethod(\"" + method.getName() + "\", new Class[] { ");
    
    for (Class type : method.getParameterTypes()) {
      out.printClass(type);
      out.print(".class, ");
    }
    out.print("})");
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
    
    generateXA(out);

    if (_runAs != null) {
      out.popDepth();
      out.println("} finally {");
      out.println("  com.caucho.security.SecurityContext.runAs(oldRunAs);");
      out.println("}");
    }
  }

  protected void generateXA(JavaWriter out)
    throws IOException
  {
    if (_xa != null) {
      switch (_xa) {
      case MANDATORY:
	{
	  out.println("_xa.beginMandatory();");
	}
	break;
	
      case NEVER:
	{
	  out.println("_xa.beginNever();");
	}
	break;
	
      case NOT_SUPPORTED:
	{
	  out.println("Transaction xa = _xa.beginNotSupported();");
	  out.println();
	  out.println("try {");
	  out.pushDepth();
	}
	break;
	
      case REQUIRED:
	{
	  out.println("Transaction xa = _xa.beginRequired();");
	  out.println();
	  out.println("try {");
	  out.pushDepth();
	}
	break;
	
      case REQUIRES_NEW:
	{
	  out.println("Transaction xa = _xa.beginRequiresNew();");
	  out.println();
	  out.println("try {");
	  out.pushDepth();
	}
	break;
      }
    }
    
    generateInterceptors(out);
    
    if (_xa != null) {
      switch (_xa) {
      case NOT_SUPPORTED:
	{
	  out.popDepth();
	  out.println("} finally {");
	  out.println("  if (xa != null)");
	  out.println("    _xa.resume(xa);");
	  out.println("}");
	}
	break;
      
      case REQUIRED:
	{
	  out.popDepth();
	  out.println("} finally {");
	  out.println("  if (xa == null)");
	  out.println("    _xa.commit();");
	  out.println("}");
	}
	break;
      
      case REQUIRES_NEW:
	{
	  out.popDepth();
	  out.println("} finally {");
	  out.println("  _xa.endRequiresNew(xa);");
	  out.println("}");
	}
	break;
      }
    }
  }

  protected void generateInterceptors(JavaWriter out)
    throws IOException
  {
    if (_interceptors == null || _interceptors.size() == 0) {
      generateSuper(out);
      return;
    }

    out.println("try {");
    out.pushDepth();
    
    if (! void.class.equals(_method.getReturnType())) {
      out.print("return (");
      printCastClass(out, _method.getReturnType());
      out.print(") ");
    }
    
    out.print("new com.caucho.ejb3.gen.InvocationContextImpl(this, ");
    out.print(_uniqueName + "_method, ");
    out.print(_uniqueName + "_methodChain, ");
    out.print("null, ");
    out.print("new Object[] { ");
    for (int i = 0; i < _method.getParameterTypes().length; i++) {
      out.print("a" + i + ", ");
    }
    out.println("}).proceed();");
    
    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.println("  throw e;");
    out.println("} catch (Exception e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");
  }

  protected void printCastClass(JavaWriter out, Class type)
    throws IOException
  {
    if (! type.isPrimitive())
      out.printClass(type);
    else if (boolean.class.equals(type))
      out.print("Boolean");
    else if (char.class.equals(type))
      out.print("Character");
    else if (byte.class.equals(type))
      out.print("Byte");
    else if (short.class.equals(type))
      out.print("Short");
    else if (int.class.equals(type))
      out.print("Integer");
    else if (long.class.equals(type))
      out.print("Long");
    else if (float.class.equals(type))
      out.print("Float");
    else if (double.class.equals(type))
      out.print("Double");
    else
      throw new IllegalStateException(type.getName());
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

  protected void generateSuper(JavaWriter out)
    throws IOException
  {
    if (! void.class.equals(_method.getReturnType()))
      out.print("return ");
    
    out.print("super." + _method.getName() + "(");

    Class []types = _method.getParameterTypes();
    for (int i = 0; i < types.length; i++) {
      if (i != 0)
	out.print(", ");

      out.print(" a" + i);
    }
    
    out.println(");");
  }
}
