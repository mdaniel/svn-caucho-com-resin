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

package com.caucho.ejb3.gen;

import com.caucho.java.JavaWriter;
import com.caucho.java.gen.GenClass;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.util.L10N;

import java.io.*;
import java.lang.reflect.*;
import javax.annotation.security.*;
import javax.ejb.*;

/**
 * Represents a business method
 */
public class BusinessMethod {
  private Method _method;

  private boolean _isPlain = true;

  private String []_roles;
  private String _roleVar;

  private String _runAs;

  private TransactionAttributeType _xa;
  
  public BusinessMethod(Method method)
  {
    _method = method;

    introspect();
  }
  
  public boolean hasXA()
  {
    return (_xa != null && ! _xa.equals(TransactionAttributeType.SUPPORTS));
  }

  public boolean isPlain()
  {
    if (_roles != null)
      return false;
    else if (_runAs != null)
      return false;
    else if (_xa != null && ! _xa.equals(TransactionAttributeType.SUPPORTS))
      return false;

    return true;
  }

  protected void introspect()
  {
    Class cl = _method.getDeclaringClass();

    introspectSecurity(cl);
    introspectTransaction(cl);
  }
  
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

  public void generate(JavaWriter out)
    throws IOException
  {
    if (isPlain())
      return;

    generatePrologue(out);

    out.println();
    if (Modifier.isPublic(_method.getModifiers()))
      out.print("public ");
    else if (Modifier.isProtected(_method.getModifiers()))
      out.print("protected ");
    else
      throw new IllegalStateException(_method.toString());

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
    
    generateSuper(out);
    
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
