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

import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.java.gen.MethodCallChain;
import com.caucho.java.gen.FilterCallChain;
import com.caucho.util.L10N;

import javax.ejb.Remove;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.reflect.*;

/**
 * Generates the bean instance for a method call.
 */
public class StatefulPoolChain extends SessionPoolChain {
  private static L10N L = new L10N(StatefulPoolChain.class);

  private final EjbBean _bean;
  private Method _implMethod;

  public StatefulPoolChain(EjbBean bean,
			   CallChain next,
			   BaseMethod apiMethod,
			   boolean isRemote)
  {
    super(next, apiMethod, isRemote);

    _bean = bean;

    CallChain callChain = apiMethod.getCall();

    MethodCallChain methodCallChain = (MethodCallChain) callChain;

    _implMethod = methodCallChain.getMethod();
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
    ArrayList<Interceptor> interceptors
      = _bean.getInvokeInterceptors(_apiMethod.getMethodName());
    boolean hasInterceptors = false;

    boolean isRemove = false;
    boolean retain = false;

    if (_implMethod.isAnnotationPresent(Remove.class)) {
      isRemove = true;
      javax.ejb.Remove removeAnn =
	(javax.ejb.Remove) _implMethod.getAnnotation(javax.ejb.Remove.class);
      retain = removeAnn.retainIfException();
    } else {
      RemoveMethod removeMethod = _bean.getRemoveMethod(_implMethod);

      if (removeMethod != null) {
        isRemove = true;
        retain = removeMethod.isRetainIfException();
      }
    }

    if (isRemove) {
      out.println("Exception exn = null;");
      out.println();
    }

    out.println("Bean ptr = _context._ejb_begin(trans);");

    out.println("try {");
    out.pushDepth();

    // ejb/0fba
    if (! isRemove) {
      // ejb/0fbk
      if (interceptors != null || _bean.getAroundInvokeMethodName() != null) {
        generateCallInterceptors(out, args);
        hasInterceptors = true;
      }
      else
        super.generateCall(out, retVar, "ptr", args);
    }
    else { // The interceptor calls ctx.proceed() which invokes the business method.
      super.generateCall(out, retVar, "ptr", args);
    }

    out.popDepth();

    // ejb/0fba
    if (hasInterceptors) {
      generateInterceptorExceptionHandling(out);
    }

    out.println("} catch (com.caucho.ejb.EJBExceptionWrapper e) {");
    out.pushDepth();

    // Application exception: cannot set null since the finally block
    // needs to free up the bean first.
    // out.println("ptr = null;");

    if (isRemove) {
      out.println("exn = e;");
      out.println();
    }

    out.println("throw e;");

    out.popDepth();

    out.println("} catch (javax.ejb.EJBException e) {");
    out.pushDepth();

    // XXX: ejb/02d1 vs TCK
    out.println("ptr = null;");

    if (isRemove) {
      out.println("exn = e;");
      out.println();
    }

    out.println("throw e;");

    out.popDepth();

    out.println("} catch (RuntimeException e) {");
    out.pushDepth();

    // XXX TCK, needs QA out.println("ptr = null;");

    if (isRemove) {
      out.println("exn = e;");
      out.println();
    }

    out.println("throw e;");

    out.popDepth();

    if (isRemove) {
      // ejb/0fe6
      out.println("} catch (Exception e) {");
      out.println("  exn = e;");
      out.println("  throw e;");
    }

    out.println("} finally {");
    out.pushDepth();

    out.println("_context._ejb_free(ptr);");

    // ejb/0fba
    if (isRemove) {
      // ejb/0fe6
      if (retain) {
        Class exnTypes[] = _implMethod.getExceptionTypes();

        boolean isFirst = true;

        for (Class cl : exnTypes) {
          if (isFirst)
            isFirst = false;
          else
            out.print("else ");

          out.println("if (exn instanceof " + cl.getName() + ") {");
          out.println("}");
        }

        if (! isFirst) {
          out.println("else");
          out.print("  ");
        }
      }

      if (isRemote())
	out.println("_server.remove(_context.getPrimaryKey());");
    }

    out.println("ptr = null;");

    out.popDepth();
    out.println("}");

    // ejb/0fba
    if (! isRemove) {
      // generateExceptionHandling(out);
    }
    else {
      // XXX TCK: ejb30/sec
      if (void.class.equals(_implMethod.getReturnType())) {
        out.popDepth();
        out.println("} catch (javax.ejb.NoSuchEJBException e) {");
        out.println("  throw e;");

        // ejb/0fe1: always remove after system exception.
        out.println("} catch (RuntimeException e) {");
        out.pushDepth();
      }
    }
  }
}
