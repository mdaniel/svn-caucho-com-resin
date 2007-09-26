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

package com.caucho.ejb.gen;

import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.FilterCallChain;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * The pooling interceptor for stateless session beans.
 */
public class StatelessPoolChain extends SessionPoolChain {
  private static L10N L = new L10N(StatelessPoolChain.class);

  public StatelessPoolChain(CallChain next, BaseMethod method)
  {
    super(next, method);
  }

  /**
   * Prints a call within the same JVM
   *
   * @param out the java source stream
   * @param var the object with the method
   * @param args the call's arguments
   */
  public void generateCall(JavaWriter out, String retVar,
                           String var, String []args)
    throws IOException
  {
    out.println("Bean ptr = _context._ejb_begin(trans);");

    out.println("try {");
    out.pushDepth();

    // XXX: ejb/02i0
    generateCallInterceptors(out, args);

    // The interceptor calls ctx.proceed() which invokes the business method.
    // generateFilterCall(out, retVar, "ptr", args);

    out.popDepth();

    // ejb/0fb0
    // XXX: ejb/02i0
    generateInterceptorExceptionHandling(out);

    // ejb/0f06 vs ejb/0271
    out.println("} catch (com.caucho.ejb.EJBExceptionWrapper e) {");
    out.pushDepth();

    // Application exception: cannot set null since the finally block
    // needs to free up the bean first.
    // ejb/0f06 out.println("ptr = null;");
    out.println("throw e;");

    out.popDepth();
    out.println("} catch (RuntimeException e) {");
    out.pushDepth();

    // ejb/0271
    out.println("ptr = null;");

    out.println("throw e;");

    out.popDepth();

    out.println("} finally {");
    out.pushDepth();

    out.println("_context._ejb_free(ptr);");

    out.popDepth();
    out.println("}");
  }
}
