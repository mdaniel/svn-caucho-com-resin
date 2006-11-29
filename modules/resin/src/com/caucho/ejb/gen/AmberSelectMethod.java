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

import com.caucho.amber.type.Type;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.config.ConfigException;
import com.caucho.ejb.cfg.EjbEntityBean;
import com.caucho.ejb.ql.EjbSelectQuery;
import com.caucho.java.JavaWriter;
import com.caucho.util.L10N;

import java.io.IOException;

/**
 * Generates the code for a query
 */
public class AmberSelectMethod extends AbstractQueryMethod {
  private static L10N L = new L10N(AmberSelectMethod.class);

  private EjbEntityBean _returnType;
  private JMethod _method;
  private String _contextClassName;
  private EjbSelectQuery _query;
  private Type _amberType;
  
  public AmberSelectMethod(EjbEntityBean type,
			   JMethod method,
			   String contextClassName,
			   EjbSelectQuery query,
			   Type amberType)
    throws ConfigException
  {
    super(type, method, query);

    _returnType = type;
    _method = method;
    _contextClassName = contextClassName;
    _query = query;
    _amberType = amberType;

    if (amberType == null)
      throw new NullPointerException();
  }

  /**
   * Gets the parameter types
   */
  public JClass []getParameterTypes()
  {
    return _method.getParameterTypes();
  }

  /**
   * Gets the return type.
   */
  public JClass getReturnType()
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

    out.println("com.caucho.amber.query.ResultSetImpl rs = null;");
    out.println("try {");
    out.pushDepth();

    generatePrepareQuery(out, args);

    out.println("rs = (com.caucho.amber.query.ResultSetImpl) query.executeQuery();");

    out.println("if (rs.next()) {");
    out.pushDepth();

    out.print(getReturnType().getPrintName());
    out.print(" v = ");

    if (getReturnType().isPrimitive()) {
      _amberType.generateLoad(out, "rs", "0", 1);
      out.println(";");
    }
    else {
      _amberType.generateLoad(out, "rs", "0", 1, getReturnType());
      out.println(";");
    }

    out.println();
    out.println("return v;");
    
    out.popDepth();
    out.println("}");
    out.println();

    if (getReturnType().isPrimitive())
      out.println("return 0;");
    else
      out.println("return null;");

    out.popDepth();
    out.println("} catch (java.sql.SQLException e) {");
    out.println("  throw new com.caucho.ejb.FinderExceptionWrapper(e);");
    out.println("} finally {");
    out.println("if (rs != null)");
    out.println("  rs.close();");

    out.println("  trans.commit();");
    out.println("}");
  }
}
