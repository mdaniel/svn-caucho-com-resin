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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.java.gen;

import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.lang.reflect.*;

/**
 * Generates the skeleton for a method call.
 */
public class SuperMethodCallChain extends MethodCallChain {
  private static L10N L = new L10N(SuperMethodCallChain.class);

  /**
   * Creates the chain.
   */
  public SuperMethodCallChain()
  {
  }

  /**
   * Creates the chain with the method.
   */
  public SuperMethodCallChain(Method method)
  {
    super(method);
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
    super.generateCall(out, retVar, "super", args);
  }
}
