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

import com.caucho.config.gen.*;
import com.caucho.config.*;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import javax.ejb.*;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * Represents a public interface to a bean, e.g. a local stateful view
 */
abstract public class StatefulObjectView extends StatefulView {
  private static final L10N L = new L10N(StatefulObjectView.class);

  public StatefulObjectView(StatefulGenerator bean, ApiClass api)
  {
    super(bean, api);
  }

  /**
   * Generates the view code.
   */
  public void generateBean(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public static class " + getBeanClassName());
    out.println("  extends " + getBeanClass().getName());
    out.println("{");
    out.pushDepth();
    
    out.println("private transient " + getViewClassName() + " _context;");

    HashMap map = new HashMap();
    
    generateBusinessPrologue(out, map);

    generatePostConstruct(out);
    //_postConstructInterceptor.generatePrologue(out, map);
    //_preDestroyInterceptor.generatePrologue(out, map);

    out.println();
    out.println(getBeanClassName() + "(" + getViewClassName() + " context)");
    out.println("{");
    out.pushDepth();
    out.println("_context = context;");

    map = new HashMap();
    generateBusinessConstructor(out, map);    
    //_postConstructInterceptor.generateConstructor(out, map);
    //_preDestroyInterceptor.generateConstructor(out, map);

    //_postConstructInterceptor.generateCall(out);

    out.popDepth();
    out.println("}");

    // generateBusinessMethods(out);
    
    out.popDepth();
    out.println("}");
  }

  @Override
  protected BusinessMethodGenerator
    createMethod(ApiMethod apiMethod, int index)
  {
    if (apiMethod.getName().equals("remove")
	&& apiMethod.getDeclaringClass().getName().startsWith("javax.ejb.")) {
      ApiMethod implMethod = findImplMethod(apiMethod);
      
      if (implMethod == null)
	return null;

      StatefulMethod method = new StatefulRemoveMethod(this,
						       apiMethod,
						       implMethod,
						       index);

      // method.getXa().setContainerManaged(false);

      return method;
    }
    else {
      BusinessMethodGenerator method = super.createMethod(apiMethod, index);

      /*
      Class beanClass = getBeanClass().getJavaClass();
      if (SessionSynchronization.class.isAssignableFrom(beanClass)) {
	method.getXa().setSynchronization(true);
      }
      */

      return method;
    }
  }

  @Override
  protected ApiMethod findImplMethod(ApiMethod apiMethod)
  {
    if (apiMethod.getName().equals("remove")
	&& apiMethod.getDeclaringClass().getName().startsWith("javax.ejb")) {
      if (apiMethod.getParameterTypes().length != 0)
	return null;

      return getBeanClass().getMethod("ejbRemove", new Class[0]);
    }
    else
      return super.findImplMethod(apiMethod);
  }
}
