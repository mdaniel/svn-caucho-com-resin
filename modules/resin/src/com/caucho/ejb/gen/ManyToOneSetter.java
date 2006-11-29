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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.gen;

import com.caucho.bytecode.JMethod;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the skeleton for the create method.
 */
public class ManyToOneSetter extends BaseMethod {
  private static final L10N L = new L10N(ManyToOneSetter.class);

  private JMethod _method;
  private String _implClassName;
  
  public ManyToOneSetter(JMethod method,
			 String implClassName)
  {
    super(method);

    _method = method;
    _implClassName = implClassName;
  }

  /**
   * Prints the create method
   *
   * @param method the create method
   */
  public void generateCall(JavaWriter out, String []args)
    throws IOException
  {
    out.println("com.caucho.ejb.xa.TransactionContext xa = _xaManager.beginRequired();");
    out.println();

    out.println("try {");
    out.println("  Bean ptr = _context._ejb_begin(xa, false, true);");

    String methodName = _method.getName();

    out.println("  if (" + args[0] + " != null) {");
    out.println("     ptr._amber_" + methodName + "(" + args[0] + ".getId());");
    out.println("  } else {");
    out.println("     ptr._amber_" + methodName + "(null);");
    out.println("  }");
    
    out.println("} finally {");
    out.println("  xa.commit();");
    out.println("}");
  }
}
