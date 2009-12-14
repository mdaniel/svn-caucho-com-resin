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
 * Represents a public interface to a bean's home interface, i.e. the
 * EJB 2.1-style factory
 */
abstract public class StatefulHomeView extends StatefulView {
  private static final L10N L = new L10N(StatefulHomeView.class);

  public StatefulHomeView(StatefulGenerator bean, ApiClass api)
  {
    super(bean, api);
  }

  abstract public String getViewClassName();

  /**
   * Generates code to create the provider
   */
  @Override
  public void generateCreateProvider(JavaWriter out, String var)
    throws IOException
  {
    out.println();
    out.println("if (" + var + " == " + getViewClass().getName() + ".class)");
    out.println("  return _localHome;");
  }
  
  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    out.println("private " + getContextClassName() + " _context;");
    out.println("private StatefulServer _server;");
    
    generateBeanPrologue(out);

    out.println();
    out.println(getViewClassName() + "(" + getContextClassName() + " context)");
    out.println("{");
    out.pushDepth();
    
    generateSuper(out, "context.getStatefulServer()");

    out.println("_context = context;");
    out.println("_server = context.getStatefulServer();");
    
    out.popDepth();
    out.println("}");

    out.println();
    out.println("public Object __caucho_createNew(javax.enterprise.inject.spi.InjectionTarget bean, javax.enterprise.context.spi.CreationalContext env)");
    out.println("{");
    out.println("  return this;");
    out.println("}");

    out.println();
    out.println("public StatefulServer getStatefulServer()");
    out.println("{");
    out.println("  return _server;");
    out.println("}");
    out.println();

    generateCreate(out);
    
    generateBusinessMethods(out);
  }

  /**
   * Generates any create() method
   */
  protected void generateCreate(JavaWriter out)
    throws IOException
  {
  }

  @Override
  protected BusinessMethodGenerator
    createMethod(ApiMethod apiMethod, int index)
  {
    if (apiMethod.getName().startsWith("create")) {
      String implName = "ejbC" + apiMethod.getName().substring(1);
      
      ApiMethod implMethod = getBeanClass().getMethod(implName,
						     apiMethod.getParameterTypes());

      if (implMethod == null)
	throw ConfigException.create(apiMethod.getMethod(),
				     L.l("api has no matching '{0}' method in '{1}'",
					 implName, getBeanClass().getName()));

      View localView = getSessionBean().getView(apiMethod.getReturnType());

      if (localView == null)
	throw ConfigException.create(apiMethod.getMethod(),
				     L.l("'{0}' is an unknown object interface",
					 apiMethod.getReturnType()));
      
      BusinessMethodGenerator method
	= new StatefulCreateMethod(getSessionBean(),
				   this,
				   localView,
				   apiMethod,
				   implMethod,
				   index);

      // method.getXa().setContainerManaged(false);

      return method;
    }
    else {
      return super.createMethod(apiMethod, index);
    }
  }

  protected ApiMethod findImplMethod(ApiMethod apiMethod)
  {
    Method javaMethod = apiMethod.getJavaMember();
    
    if (apiMethod.getName().equals("create")) {
      return getBeanClass().getMethod("ejbCreate",
				     apiMethod.getParameterTypes());
    }
    else if (apiMethod.getName().equals("remove")
	     && javaMethod.getDeclaringClass().getName().startsWith("javax.ejb")) {
      return null;
    }
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
    out.println("  extends StatefulHome");
  }
}
