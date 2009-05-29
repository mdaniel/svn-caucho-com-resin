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
 * Represents any stateless view.
 */
public class StatelessView extends View {
  private static final L10N L = new L10N(StatelessView.class);

  private StatelessGenerator _statelessBean;

  private ArrayList<BusinessMethodGenerator> _businessMethods
    = new ArrayList<BusinessMethodGenerator>();

  public StatelessView(StatelessGenerator bean, ApiClass api)
  {
    super(bean, api);

    _statelessBean = bean;
  }

  public StatelessGenerator getStatelessBean()
  {
    return _statelessBean;
  }

  public String getContextClassName()
  {
    return getStatelessBean().getClassName();
  }

  public String getViewClassName()
  {
    return getViewClass().getSimpleName() + "__EJBLocal";
  }

  public String getBeanClassName()
  {
    return getViewClass().getSimpleName() + "__Bean";
  }

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
  public void introspect()
  {
    ApiClass implClass = getBeanClass();
    ApiClass apiClass = getViewClass();
    
    for (ApiMethod apiMethod : apiClass.getMethods()) {
      if (apiMethod.getDeclaringClass().equals(Object.class))
	continue;
      if (apiMethod.getDeclaringClass().getName().startsWith("javax.ejb."))
	continue;
 
      if (apiMethod.getName().startsWith("ejb")) {
	throw new ConfigException(L.l("{0}: '{1}' must not start with 'ejb'.  The EJB spec reserves all methods starting with ejb.",
				      apiMethod.getDeclaringClass(),
				      apiMethod.getName()));
      }

      addBusinessMethod(apiMethod);
    }
  }

  protected void addBusinessMethod(ApiMethod apiMethod)
  {
    int index = _businessMethods.size();
      
    BusinessMethodGenerator bizMethod = createMethod(apiMethod, index);
      
    if (bizMethod != null) {
      bizMethod.introspect(bizMethod.getApiMethod(),
			   bizMethod.getImplMethod());
	
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
    out.println("if (" + var + " == " + getViewClass().getName() + ".class)");
    out.println("  return new " + getViewClassName() + "(this);");
  }

  protected void generateExtends(JavaWriter out)
    throws IOException
  {
    out.println("extends StatelessObject");
  }

  protected BusinessMethodGenerator createMethod(ApiMethod apiMethod,
						 int index)
  {
    ApiMethod implMethod = findImplMethod(apiMethod);

    if (implMethod == null) {
      throw new ConfigException(L.l("'{0}' method '{1}' has no corresponding implementation in '{2}'",
				    apiMethod.getMethod().getDeclaringClass().getSimpleName(),
				    apiMethod.getFullName(),
				    getBeanClass().getName()));
    }

    StatelessLocalMethod bizMethod
      = new StatelessLocalMethod(getBeanClass(),
				 getBeanClassName(),
				 this,
				 apiMethod,
				 implMethod,
				 index);

    return bizMethod;
  }

  /**
   * Generates the view code.
   */
  public void generate(JavaWriter out)
    throws IOException
  {
  }

  protected void generateSuper(JavaWriter out, String serverVar)
    throws IOException
  {
    out.println("super(" + serverVar + ");");
  }

  protected ApiMethod findImplMethod(ApiMethod apiMethod)
  {
    return getBeanClass().getMethod(apiMethod);
  }
}
