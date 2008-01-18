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

import com.caucho.config.*;
import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import javax.ejb.*;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * Represents a public interface to a bean, e.g. a local stateful view
 */
public class StatefulLocalView extends View {
  private static final L10N L = new L10N(StatefulView.class);

  private ArrayList<StatefulLocalMethod> _businessMethods
    = new ArrayList<StatefulLocalMethod>();

  public StatefulLocalView(BeanGenerator bean, ApiClass api)
  {
    super(bean, api);

    introspect();
  }

  protected String getViewClassName()
  {
    return getApi().getSimpleName() + "__EJBLocal";
  }

  /**
   * Introspects the APIs methods, producing a business method for
   * each.
   */
  protected void introspect()
  {
    ApiClass implClass = getEjbClass();
    ApiClass apiClass = getApi();
    
    for (ApiMethod apiMethod : apiClass.getMethods()) {
      ApiMethod implMethod = implClass.getMethod(apiMethod);

      if (implMethod == null) {
	throw ConfigException.create(apiMethod.getMethod(),
				     L.l("api method has no corresponding implementation in '{0}'",
					 implClass.getName()));
      }

      int index = _businessMethods.size();
      
      StatefulLocalMethod bizMethod
	= new StatefulLocalMethod(apiMethod.getMethod(),
				  implMethod.getMethod(),
				  index);

      _businessMethods.add(bizMethod);
    }
  }

  /**
   * Generates code to create the provider
   */
  public void generateCreateProvider(JavaWriter out, String var)
    throws IOException
  {
    out.println();
    out.println("if (" + var + " == " + getApi().getName() + ".class)");
    out.println("  return new " + getViewClassName() + "(getStatefulServer());");
  }

  /**
   * Generates the view code.
   */
  public void generate(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public static class " + getViewClassName());
    out.print("  implements " + getApi().getName());
    out.println(", SessionProvider");
    out.println("{");
    out.pushDepth();

    out.println("private StatefulServer _server;");
    out.println("private " + getEjbClass().getName() + " _bean;");

    out.println();
    out.println("public " + getViewClassName() + "(StatefulServer server)");
    out.println("{");
    out.println("  _server = server;");
    out.println("}");

    out.println();
    out.println("public " + getViewClassName() + "(ConfigContext env)");
    out.println("{");
    out.println("  _bean = new " + getEjbClass().getName() + "();");
    out.println("}");

    out.println();
    out.println("public Object __caucho_createNew(ConfigContext env)");
    out.println("{");
    out.print("  " + getViewClassName() + " bean"
	      + " = new " + getViewClassName() + "(env);");
    out.println("  _server.initInstance(bean._bean, env);");
    out.println("  return bean;");
    out.println("}");

    HashMap map = new HashMap();
    for (StatefulLocalMethod method : _businessMethods) {
      method.generate(out, map);
    }
    
    out.popDepth();
    out.println("}");
  }
}
