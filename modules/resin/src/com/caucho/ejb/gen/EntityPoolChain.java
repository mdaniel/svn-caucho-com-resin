/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.io.IOException;

import com.caucho.util.L10N;

import com.caucho.java.JavaWriter;

import com.caucho.java.gen.FilterCallChain;
import com.caucho.java.gen.CallChain;

/**
 * Generates the bean instance for a method call.
 */
public class EntityPoolChain extends FilterCallChain {
  private static final L10N L = new L10N(EntityPoolChain.class);

  private boolean _isHome;
  private boolean _doLoad;

  public EntityPoolChain(CallChain next, boolean isHome)
  {
    super(next);

    _isHome = isHome;
    _doLoad = ! isHome;
    // _doLoad = false;
  }
  
  /**
   * Prints a call within the same JVM
   *
   * @param out the java source stream
   * @param retVar the variable to store the return value in
   * @param var the object with the method
   * @param args the call's arguments
   */
  public void generateCall(JavaWriter out, String retVar,
			   String var, String []args)
    throws IOException
  {
    out.println("Bean ptr = _context._ejb_begin(trans, " + _isHome +", " + _doLoad + ");");
    
    super.generateCall(out, retVar, "ptr", args);
  }
}
