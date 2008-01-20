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
 * Represents a public interface to a stateful bean, e.g. a stateful view
 */
abstract public class StatefulView extends View {
  private static final L10N L = new L10N(StatefulView.class);

  private StatefulGenerator _sessionBean;
  
  private ArrayList<StatefulMethod> _businessMethods
    = new ArrayList<StatefulMethod>();

  public StatefulView(StatefulGenerator bean, ApiClass api)
  {
    super(bean, api);

    _sessionBean = bean;
  }

  public StatefulGenerator getSessionBean()
  {
    return _sessionBean;
  }

  public String getContextClassName()
  {
    return getSessionBean().getClassName();
  }

  abstract protected String getViewClassName();

  /**
   * Returns the introspected methods
   */
  public ArrayList<? extends BusinessMethodGenerator> getMethods()
  {
    return _businessMethods;
  }

  /**
   * Introspects the APIs methods, producing a business method for
   * each.
   */
  @Override
  public void introspect()
  {
    ApiClass implClass = getEjbClass();
    ApiClass apiClass = getApi();
    
    for (ApiMethod apiMethod : apiClass.getMethods()) {
      if (apiMethod.getDeclaringClass().equals(Object.class))
	continue;
      if (apiMethod.getDeclaringClass().getName().startsWith("javax.ejb."))
	continue;

      int index = _businessMethods.size();
      
      StatefulMethod bizMethod = createMethod(apiMethod, index);
      
      if (bizMethod != null)
	_businessMethods.add(bizMethod);
    }
  }

  protected ApiMethod findImplMethod(ApiMethod apiMethod)
  {
    return getEjbClass().getMethod(apiMethod);
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

    generateExtends(out);
    
    out.print("  implements " + getApi().getName());
    out.println(", SessionProvider");
    out.println("{");
    out.pushDepth();

    generateClassContent(out);

    HashMap map = new HashMap();
    for (StatefulMethod method : _businessMethods) {
      method.generate(out, map);
    }
    
    out.popDepth();
    out.println("}");
  }

  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    out.println("private StatefulContext _context;");
    out.println("private StatefulServer _server;");
    out.println("private " + getEjbClass().getName() + " _bean;");

    out.println();
    out.println(getViewClassName() + "(StatefulServer server)");
    out.println("{");
    out.pushDepth();
    
    generateSuper(out, "server");
    
    out.println("_server = server;");
    
    out.popDepth();
    out.println("}");

    out.println();
    out.println("public " + getViewClassName() + "(ConfigContext env)");
    out.println("{");
    generateSuper(out, "null");
    out.println("  _bean = new " + getEjbClass().getName() + "();");
    out.println("}");

    generateSessionProvider(out);

    out.println();
    out.println("public " + getViewClassName()
		+ "(StatefulServer server, "
		+ getEjbClass().getName() + " bean)");
    out.println("{");
    generateSuper(out, "server");
    out.println("  _server = server;");
    out.println("  _bean = bean;");
    out.println("}");

    out.println();
    out.println("public StatefulServer getStatefulServer()");
    out.println("{");
    out.println("  return _server;");
    out.println("}");
    out.println();

    out.println();
    out.println("void __caucho_setContext(StatefulContext context)");
    out.println("{");
    out.println("  _context = context;");
    out.println("}");
  }

  protected void generateSessionProvider(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public Object __caucho_createNew(ConfigContext env)");
    out.println("{");
    out.print("  " + getViewClassName() + " bean"
	      + " = new " + getViewClassName() + "(env);");
    out.println("  _server.initInstance(bean._bean, env);");
    out.println("  return bean;");
    out.println("}");
  }

  protected StatefulMethod createMethod(ApiMethod apiMethod, int index)
  {
    ApiMethod implMethod = findImplMethod(apiMethod);

    if (implMethod == null) {
      throw ConfigException.create(apiMethod.getMethod(),
				   L.l("api method has no corresponding implementation in '{0}'",
				       getEjbClass().getName()));
    }

    StatefulMethod bizMethod
      = new StatefulMethod(apiMethod.getMethod(),
				implMethod.getMethod(),
				index);

    return bizMethod;
  }

  protected void generateSuper(JavaWriter out, String serverVar)
    throws IOException
  {
  }

  protected void generateExtends(JavaWriter out)
    throws IOException
  {
  }
}
