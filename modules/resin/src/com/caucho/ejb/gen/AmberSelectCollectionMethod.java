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

import com.caucho.config.ConfigException;
import com.caucho.ejb.cfg.*;
import com.caucho.ejb.ql.EjbSelectQuery;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.*;

/**
 * Generates the code for a query
 */
public class AmberSelectCollectionMethod extends AbstractQueryMethod {
  private static final L10N L = new L10N(AmberSelectCollectionMethod.class);

  private EjbEntityBean _returnType;
  private ApiMethod _method;
  private String _contextClassName;
  
  public AmberSelectCollectionMethod(EjbEntityBean type,
				     ApiMethod method,
				     String contextClassName,
				     EjbSelectQuery query)
    throws ConfigException
  {
    super(type, method, query);

    _returnType = type;
    _method = method;
    _contextClassName = contextClassName;
  }

  /**
   * Gets the parameter types
   */
  public Class []getParameterTypes()
  {
    return _method.getParameterTypes();
  }

  /**
   * Gets the return type.
   */
  public Class getReturnType()
  {
    return _method.getReturnType();
  }

  /**
   * Prints the create method
   *
   * @param method the create method
   */
  public void generateCall(JavaWriter out, String []args)
    throws IOException
  {
    out.print("com.caucho.ejb.xa.TransactionContext trans");
    out.println(" = _ejb_context.getTransactionManager().beginSupports();");

    out.println("try {");
    out.pushDepth();

    generatePrepareQuery(out, args);

    out.println("com.caucho.amber.query.ResultSetImpl rs = (com.caucho.amber.query.ResultSetImpl) query.executeQuery();");
    out.println("java.util.ArrayList list = new java.util.ArrayList();");

    out.println("while (rs.next()) {");
    out.pushDepth();

    String beanClass = _returnType.getEJBClass().getName();

    // out.println("com.caucho.amber.entity.EntityItem item = rs.findEntityItem(1);");
    // out.println(beanClass + " bean = (" + beanClass + ") rs.getObject(1);");
    // out.println(_contextClassName + " context = (" + _contextClassName + ") _ejb_context.getServer().getContext(" + generateBeanId() ");");
    // out.println("list.add(context.getEJBLocalObject());");
    out.println("list.add(rs.getObject(1));");
    
    out.popDepth();
    out.println("}");
    out.println();
    
    // XXX:
    Class retType = _method.getReturnType();
    if (Collection.class.isAssignableFrom(retType))
      out.println("return list;");
    else
      out.println("return null;");
    
    
    /*
    Class retType = getReturnType();

    if ("RemoteHome".equals(_prefix))
      out.println("return (" + retType.getName() + ") _server.getContext(" + args[0] + ", true).getEJBObject();");
    else if ("LocalHome".equals(_prefix))
      out.println("return (" + retType.getName() + ") _server.getContext(" + args[0] + ", true).getEJBLocalObject();");
    else
      throw new IllegalStateException(L.l("'{0}' is an unknown type",
					  _prefix));
    */

    out.popDepth();
    out.println("} catch (java.sql.SQLException e) {");
    out.println("  throw new com.caucho.ejb.FinderExceptionWrapper(e);");
    out.println("} finally {");
    out.println("  trans.commit();");
    out.println("}");
  }
}
