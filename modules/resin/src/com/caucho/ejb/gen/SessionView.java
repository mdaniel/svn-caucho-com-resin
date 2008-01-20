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

import com.caucho.config.ConfigException;
import com.caucho.ejb.gen21.ViewClass;
import com.caucho.ejb.cfg.*;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import javax.ejb.*;

/**
 * Generates the skeleton for a session view.
 */
public class SessionView extends View {
  private static L10N L = new L10N(SessionView.class);
 
  private ArrayList<BusinessMethodGenerator> _methods
    = new ArrayList<BusinessMethodGenerator>();
  
  private String _suffix = "";
  private String _prefix;
  private String _contextClassName;
  private boolean _isStateless;
  private boolean _isRemote;

  public SessionView(BeanGenerator bean,
		     ArrayList<ApiClass> apiList,
                     String contextClassName,
                     String prefix,
                     String suffix, // "21" for EJB 2.1 only
                     boolean isStateless,
		     boolean isRemote)
  {
    super(bean, apiList.get(0));
    
    // super(prefix + getSuffix(apiList), getSuperclass(isStateless, apiList));

    _contextClassName = contextClassName;
    _prefix = prefix;
    _isStateless = isStateless;
    _isRemote = isRemote;
    
    introspect();
  }
  
  /**
   * Introspects the methods in the API list to generate business methods.
   */
  @Override
  public void introspect()
  {
    /*
    for (ApiClass api : _apiList) {
      for (ApiMethod method : api.getMethods()) {
        introspectMethod(method);
      }
    }
    */
  }
  
  protected void introspectMethod(ApiMethod apiMethod)
  {
    if (findMethod(apiMethod) != null)
      return;
    
    ApiMethod implMethod = getEjbClass().getMethod(apiMethod);
    
    if (implMethod == null) {
      throw new ConfigException(L.l("'{0}' does not have a corresponding method in '{1}'",
        apiMethod, getEjbClass()));
    }

    /*
    BusinessMethodGenerator businessMethod
      = createBusinessMethod(apiMethod, implMethod);
    
    _methods.add(businessMethod);
    
    businessMethod.introspect();
    */
  }
  
  /**
   * Creates a new business method instance
   * 
   * @param apiMethod the interface method
   * @param implMethod the implementation method
   * @return the new business method
   */
  protected BusinessMethodGenerator createBusinessMethod(ApiMethod apiMethod,
    ApiMethod implMethod)
  {
    return new SessionBusinessMethod(apiMethod, implMethod, _methods.size());
  }
  
  /**
   * Returns the business method matching the given interface method
   * 
   * @param method the interface method to match
   * 
   * @return the matching method or null if none match
   */
  protected BusinessMethodGenerator findMethod(ApiMethod method)
  {
    for (BusinessMethodGenerator oldMethod : _methods) {
      if (oldMethod.matches(method.getName(), method.getParameterTypes()))
        return oldMethod;
    }
    
    return null;
  }
  
  public void generate(JavaWriter out)
    throws IOException
  {
    generateGetter(out);

    out.println();
    // super.generate(out);
  }

  private void generateGetter(JavaWriter out)
    throws IOException
  {
    out.println("private " + _prefix + _suffix + " _view" + _prefix + _suffix + ";");

    out.println();

    // EJB 2.1 only.
    if (_suffix.equals("21")) {
      String interfaceName;

      if (_prefix.equals("Local")) {
        interfaceName = "EJBLocalObject";
        out.println("public EJBLocalObject getEJBLocalObject()");
        out.println("{");
        out.println("  createLocalObject" + _suffix + "();");
      } else {
        interfaceName = "EJBObject";
        out.println("public EJBObject getRemoteView()");
        out.println("{");
        out.println("  createRemoteView" + _suffix + "();");
      }

      out.println();
      out.println("  if (_view" + _prefix + _suffix + " instanceof " + interfaceName + ")");
      out.println("    return (" + interfaceName + ") _view" + _prefix + _suffix + ";");

      out.println();
      out.println("  throw new IllegalStateException(\"Cannot getEJBObject/getEJBLocalObject when the session bean does not define a 2.1 interface\");");
      out.println("}");

      out.println();
    }

    if (_prefix.equals("Local"))
      out.println("public Object createLocalObject" + _suffix + "()");
    else {
      out.println("public Object createRemoteView" + _suffix + "()");
    }

    out.println("{");
    out.println("  if (_view" + _prefix + _suffix + " == null)");
    out.println("    _view" + _prefix + _suffix + " = new " + _prefix + _suffix + "(this);");
    out.println();
    out.println("  return _view" + _prefix + _suffix + ";");
    out.println("}");
  }

  protected void generateClassContent(JavaWriter out)
    throws IOException
  {
    out.println("private final " + _contextClassName + " _context;");
    out.println("private final EjbTransactionManager _xaManager;");

    out.println();
    out.println(_prefix + _suffix + "(" + _contextClassName + " context)");
    out.println("{");
    if (_isStateless)
      out.println("  super(context.getStatelessServer());");
    else
      out.println("  super(context.getSessionServer());");
    out.println("  _context = context;");
    out.println("  _xaManager = _context.getTransactionManager();");
    out.println("}");

    out.println();
    out.println("private " + _contextClassName + " getContext()");
    out.println("{");
    out.println("  return _context;");
    out.println("}");

    if (! _isStateless) {
      out.println();
      out.println("public String __caucho_getId()");
      out.println("{");
      out.println("  return _context.getPrimaryKey();");
      out.println("}");
    }

    out.println();
  }
}
