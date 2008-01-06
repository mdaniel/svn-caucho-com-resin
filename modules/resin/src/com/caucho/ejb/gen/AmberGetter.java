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

import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Generates the skeleton for the create method.
 */
public class AmberGetter extends BaseMethod {
  private static L10N L = new L10N(AmberGetter.class);

  private ApiMethod _method;
  private String _implClassName;
  private boolean _isReadOnly;
  
  public AmberGetter(ApiMethod method,
		     String implClassName)
  {
    super(method.getMethod());

    _method = method;
    _implClassName = implClassName;
  }

  /**
   * Set true if the field is read-only.
   */
  public void setReadOnly(boolean isReadOnly)
  {
    _isReadOnly = isReadOnly;
  }

  /**
   * Prints the create method
   *
   * @param method the create method
   */
  public void generateCall(JavaWriter out, String []args)
    throws IOException
  {
    Class returnType = _method.getReturnType();

    if (! _isReadOnly) {
      
      out.println("com.caucho.ejb.xa.TransactionContext xa = _xaManager.beginSingleRead();");
      out.println();

      out.println("try {");
      out.pushDepth();

      out.println("if (xa == null) {");
      out.pushDepth();
    }

    out.printClass(returnType);
    out.print(" value = ");
    out.print("((" + _implClassName + ") _context.__caucho_getAmberCacheItem().loadEntity(0))." + _method.getName() + "(");
    for (int i = 0; i < args.length; i++) {
      if (i != 0)
	out.print(", ");
      out.print(args[i]);
    }
    out.println(");");

    if (Collection.class.isAssignableFrom(returnType)) {
      out.println("if (value == null)");
      out.println("  return value;");

      if (Set.class.isAssignableFrom(returnType))
	out.println("return new java.util.HashSet(value);");
      else
	out.println("return new java.util.ArrayList(value);");
    }
    else {
      out.println("return value;");
    }

    if (! _isReadOnly) {
      out.popDepth();
      out.println("} else {");
      out.println("  Bean ptr = _context._ejb_begin(xa, false, true);");
      out.print("  return ptr." + _method.getName() + "(");    
      for (int i = 0; i < args.length; i++) {
	if (i != 0)
	  out.print(", ");
	out.print(args[i]);
      }
      out.println(");");
      out.println("}");
    
      out.popDepth();
      out.println("} finally {");
      out.println("  if (xa != null) xa.commit();");
      out.println("}");
    }
  }
}
