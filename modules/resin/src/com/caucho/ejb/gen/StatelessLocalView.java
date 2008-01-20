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
 * Represents a public interface to a bean, e.g. a local stateless view
 */
public class StatelessLocalView extends View {
  private static final L10N L = new L10N(StatelessLocalView.class);

  private ArrayList<StatelessLocalMethod> _businessMethods
    = new ArrayList<StatelessLocalMethod>();

  public StatelessLocalView(BeanGenerator bean, ApiClass api)
  {
    super(bean, api);
  }

  protected String getViewClassName()
  {
    return getApi().getSimpleName() + "__EJBLocal";
  }

  /**
   * Introspects the APIs methods, producing a business method for
   * each.
   */
  public void introspect()
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
      
      StatelessLocalMethod bizMethod
	= new StatelessLocalMethod(implClass,
				   apiMethod.getMethod(),
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
    out.println("  return new " + getViewClassName() + "(this);");
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
    out.println(", StatelessProvider");
    out.println("{");
    out.pushDepth();
    
    out.println("private " + getBean().getClassName() + " _cxt;");

    out.println();
    out.println(getViewClassName() + "(" + getBean().getClassName() + " cxt)");
    out.println("{");
    out.println("  _cxt = cxt;");
    out.println("}");

    out.println("public Object __caucho_get()");
    out.println("{");
    out.println("  return this;");
    out.println("}");

    HashMap map = new HashMap();
    for (StatelessLocalMethod method : _businessMethods) {
      method.generate(out, map);
    }
    
    out.popDepth();
    out.println("}");
  }
}
