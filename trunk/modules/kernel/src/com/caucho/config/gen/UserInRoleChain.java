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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.gen;

import com.caucho.java.JavaWriter;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.FilterCallChain;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Generates the code for the user in role filter.
 */
public class UserInRoleChain extends FilterCallChain {
  private static final L10N L = new L10N(UserInRoleChain.class);

  private ArrayList<String> _roles;
  
  public UserInRoleChain(CallChain next, ArrayList<String> roles)
  {
    super(next);
    
    _roles = roles;
  }
  
  /**
   * Prints a call within the same JVM
   *
   * @param methodName the name of the method to call
   * @param method the method to call
   */
  public void generateCall(JavaWriter out, String retType,
			   String var, String []args)
    throws IOException
  {
    out.print("if (! com.caucho.security.SecurityContext.isUserInRole(new String[] {");

    for (int i = 0; i < _roles.size(); i++) {
      String role = _roles.get(i);

      if (i != 0)
	out.print(", ");
      
      out.print("\"" + role + "\"");
    }
    out.println("})) {");

    out.println("  throw new javax.ejb.EJBException(\"permission denied\");");
    out.println("}");

    out.println();
    
    super.generateCall(out, retType, var, args);
  }
}
