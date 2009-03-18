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

package com.caucho.config.gen;

import com.caucho.config.*;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import javax.ejb.*;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * Represents a public interface to a bean's home interface, i.e. the
 * EJB 2.1-style factory
 */
abstract public class StatelessHomeView extends StatelessView {
  private static final L10N L = new L10N(StatelessHomeView.class);

  public StatelessHomeView(StatelessGenerator bean, ApiClass api)
  {
    super(bean, api);
  }

  abstract protected String getViewClassName();

  /**
   * Generates code to create the provider
   */
  public void generateCreateProvider(JavaWriter out, String var)
    throws IOException
  {
    out.println();
    out.println("if (" + var + " == " + getApi().getName() + ".class)");
    out.println("  return _localHome;");
  }

  /**
   * Generates the view code.
   */
  public void generate(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public static class " + getViewClassName());
    out.println("  extends StatelessHome");
    out.println("  implements StatelessProvider, " + getApi().getName());
    out.println("{");
    out.pushDepth();

    generateClassContent(out);

    out.popDepth();
    out.println("}");
  }

  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    out.println("private " + getContextClassName() + " _context;");
    out.println("private StatelessServer _server;");

    out.println();
    out.println(getViewClassName() + "(" + getContextClassName() + " context)");
    out.println("{");
    out.pushDepth();
    
    generateSuper(out, "context.getStatelessServer()");

    out.println("_context = context;");
    out.println("_server = context.getStatelessServer();");
    
    generateBusinessConstructor(out);
    
    out.popDepth();
    out.println("}");

    out.println();
    out.println("public Object __caucho_createNew(javax.inject.manager.Bean bean, javax.context.CreationalContext env)");
    out.println("{");
    out.println("  return this;");
    out.println("}");

    out.println();
    out.println("public StatelessServer getStatelessServer()");
    out.println("{");
    out.println("  return _server;");
    out.println("}");
    out.println();

    out.println();
    out.println("public Object __caucho_get()");
    out.println("{");
    out.println("  return this;");
    out.println("}");
    out.println();

    generateBusinessMethods(out);
  }

  @Override
  protected BusinessMethodGenerator createMethod(ApiMethod apiMethod,
						 int index)
  {
    if (apiMethod.getName().equals("create")) {
      ApiMethod implMethod = getEjbClass().getMethod("ejbCreate",
						     apiMethod.getParameterTypes());

      // ejbCreate optional
      /*
      throw ConfigException.create(apiMethod.getMethod(),
				     L.l("can't find ejbCreate"));
      */

      View localView = getStatelessBean().getView(apiMethod.getReturnType());

      if (localView == null)
	throw ConfigException.create(apiMethod.getMethod(),
				     L.l("'{0}' is an unknown object interface",
					 apiMethod.getReturnType()));
      
      return new StatelessCreateMethod(getStatelessBean(),
				       this,
				       localView,
				       apiMethod,
				       implMethod,
				       index);
    }
    else {
      return super.createMethod(apiMethod, index);
    }
  }

  protected ApiMethod findImplMethod(ApiMethod apiMethod)
  {
    if (apiMethod.getName().equals("create"))
      return getEjbClass().getMethod("ejbCreate", apiMethod.getParameterTypes());
    else
      return super.findImplMethod(apiMethod);
  }

  protected void generateSuper(JavaWriter out, String serverVar)
    throws IOException
  {
    out.println("super(" + serverVar + ");");
  }

  @Override
  protected void generateExtends(JavaWriter out)
    throws IOException
  {
    out.println("  extends StatelessHome");
  }
}
