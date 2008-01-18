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

package com.caucho.ejb.gen21;

import com.caucho.ejb.cfg21.EjbEntityBean;
import com.caucho.ejb.gen.*;
import com.caucho.amber.field.IdField;
import com.caucho.amber.type.EntityType;
import com.caucho.config.ConfigException;
import com.caucho.ejb.cfg.*;
import com.caucho.ejb.ql.EjbSelectQuery;
import com.caucho.java.JavaWriter;
import com.caucho.java.gen.BaseMethod;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.reflect.*;

/**
 * Generates the code for a query
 */
abstract public class AbstractQueryMethod extends BaseMethod {
  private static final L10N L = new L10N(AbstractQueryMethod.class);

  private ApiMethod _method;
  private EjbEntityBean _bean;
  private EjbSelectQuery _query;
  private boolean _queryLoadsBean = true;
  
  protected AbstractQueryMethod(EjbEntityBean bean,
				ApiMethod method,
				EjbSelectQuery query)
    throws ConfigException
  {
    super(method.getMethod());

    _bean = bean;
    _query = query;
    _method = method;

    /*
    EjbConfig ejbConfig = beanType.getConfig();

    AmberPersistenceUnit amberManager = beanType.getConfig().getEJBManager().getAmberManager();
    */
  }

  /**
   * Sets the query-loads-bean propery.
   */
  public void setQueryLoadsBean(boolean queryLoadsBean)
  {
    _queryLoadsBean = queryLoadsBean;
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

  protected String generateBeanId()
  {
    return "bean.__caucho_getPrimaryKey()";
  }
  
  void generatePrepareQuery(JavaWriter out, String []args)
    throws IOException
  {
    out.println("com.caucho.amber.AmberQuery query;");

    out.print("query = trans.getAmberConnection().prepareQuery(\"");
    out.print(_query.toAmberQuery(args));
    out.println("\");");

    int len = args.length;

    if (_query.getMaxArg() < len)
      len = _query.getMaxArg();

    if (len > 0 || _query.getThisExpr() != null)
      out.println("int index = 1;");

    Class []paramTypes = getParameterTypes();
    
    for (int i = 0; i < len; i++) {
      generateSetParameter(out, paramTypes[i], args[i]);
    }

    if (_query.getThisExpr() != null)
      generateSetThis(out, _bean, "query");
    
    if (_query.getOffsetValue() > 0)
      out.println("query.setFirstResult(" + _query.getOffsetValue() + ");");
    else if (_query.getOffsetArg() > 0)
      out.println("query.setFirstResult(" + args[_query.getOffsetArg() - 1] + ");");

    if (_query.getLimitValue() > 0)
      out.println("query.setMaxResults(" + _query.getLimitValue() + ");");
    else if (_query.getLimitArg() > 0)
      out.println("query.setMaxResults(" + args[_query.getLimitArg() - 1] + ");");

    if (! _queryLoadsBean)
      out.println("query.setLoadOnQuery(false);");
  }
  
  public static void generateSetThis(JavaWriter out,
				     EjbEntityBean bean,
				     String query)
    throws IOException
  {
    EntityType amberType = bean.getEntityType();

    ArrayList<IdField> keys = new ArrayList<IdField>();
    keys.addAll(amberType.getId().getKeys());
    Collections.sort(keys, new IdFieldCompare());
      
    for (IdField field : keys) {
      field.generateSet(out, query + "", "index", "super");
    }
  }
  
  public void generateSetParameter(JavaWriter out, Class type, String arg)
    throws IOException
  {
    generateSetParameter(out, _bean.getConfig(), type, "query", arg);
  }
  
  public static void generateSetParameter(JavaWriter out,
					  EjbConfig config,
					  Class type,
					  String query,
					  String arg)
    throws IOException
  {
    if (type.getName().equals("boolean")) {
      // printCheckNull(out, type, arg);
      
      out.println(query + ".setBoolean(index++, " + arg + ");");
    }
    else if (type.getName().equals("byte")) {
      // printCheckNull(index, type, expr);
      
      out.println(query + ".setInt(index++, " + arg + ");");
    }
    else if (type.getName().equals("short")) {
      // printCheckNull(index, type, expr);
      
      out.println(query + ".setInt(index++, " + arg + ");");
    }
    else if (type.getName().equals("int")) {
      // printCheckNull(index, type, expr);
      
      out.println(query + ".setInt(index++, " + arg + ");");
    }
    else if (type.getName().equals("long")) {
      // printCheckNull(index, type, expr);
      
      out.println(query + ".setLong(index++, " + arg + ");");
    }
    else if (type.getName().equals("char")) {
      // printCheckNull(index, type, expr);
      
      out.println(query + ".setString(index++, String.valueOf(" + arg + "));");
    }
    else if (type.getName().equals("float")) {
      // printCheckNull(index, type, expr);
      
      out.println(query + ".setFloat(index++, " + arg + ");");
    }
    else if (type.getName().equals("double")) {
      // printCheckNull(index, type, expr);
      
      out.println(query + ".setDouble(index++, " + arg + ");");
    }
    else if (java.sql.Timestamp.class.isAssignableFrom(type)) {
      out.println(query + ".setTimestamp(index++, " + arg + ");");
    }
    else if (java.sql.Date.class.isAssignableFrom(type))
      out.println(query + ".setDate(index++, " + arg + ");");
    else if (java.sql.Time.class.isAssignableFrom(type))
      out.println(query + ".setTime(index++, " + arg + ");");
    else if (java.util.Date.class.isAssignableFrom(type)) {
      out.println("{");
      out.println("  java.util.Date _caucho_tmp_date = " + arg + ";");
      out.println("  if (_caucho_tmp_date == null)");
      out.println("    " + query + ".setNull(index++, java.sql.Types.TIMESTAMP);");
      out.println("  else");
      out.println("    " + query + ".setTimestamp(index++, new java.sql.Timestamp(_caucho_tmp_date.getTime()));");
      out.println("}");
    }
    else if (Boolean.class.equals(type)) {
      out.println("if (" + arg + " == null)");
      out.println("  " + query + ".setNull(index++, java.sql.Types.BIT);");
      out.println("else");
      out.println("  " + query + ".setBoolean(index++, " + arg + ".booleanValue());");
    }
    else if (Character.class.equals(type)) {
      out.println("if (" + arg + " == null)");
      out.println("  " + query + ".setNull(index++, java.sql.Types.VARCHAR);");
      out.println("else");
      out.println("  " + query + ".setString(index++, " + arg + ".toString());");
    }
    else if (String.class.equals(type)) {
      out.println("  " + query + ".setString(index++, " + arg + ");");
    }
    else if (Byte.class.equals(type)) {
      out.println("if (" + arg + " == null)");
      out.println("  " + query + ".setNull(index++, java.sql.Types.TINYINT);");
      out.println("else");
      out.println("  " + query + ".setInt(index++, " + arg + ".byteValue());");
    }
    else if (Short.class.equals(type)) {
      out.println("if (" + arg + " == null)");
      out.println("  " + query + ".setNull(index++, java.sql.Types.SMALLINT);");
      out.println("else");
      out.println("  " + query + ".setInt(index++, " + arg + ".shortValue());");
    }
    else if (Integer.class.equals(type)) {
      out.println("if (" + arg + " == null)");
      out.println("  " + query + ".setNull(index++, java.sql.Types.INTEGER);");
      out.println("else");
      out.println("  " + query + ".setInt(index++, " + arg + ".intValue());");
    }
    else if (Long.class.equals(type)) {
      out.println("if (" + arg + " == null)");
      out.println("  " + query + ".setNull(index++, java.sql.Types.BIGINT);");
      out.println("else");
      out.println("  " + query + ".setLong(index++, " + arg + ".longValue());");
    }
    else if (Float.class.equals(type)) {
      out.println("if (" + arg + " == null)");
      out.println("  " + query + ".setNull(index++, java.sql.Types.REAL);");
      out.println("else");
      out.println("  " + query + ".setDouble(index++, " + arg + ".floatValue());");
    }
    else if (Double.class.equals(type)) {
      out.println("if (" + arg + " == null)");
      out.println("  " + query + ".setNull(index++, java.sql.Types.DOUBLE);");
      out.println("else");
      out.println("  " + query + ".setDouble(index++, " + arg + ".doubleValue());");
    }
    else if (java.math.BigDecimal.class.equals(type)) {
      out.println("if (" + arg + " == null)");
      out.println("  " + query + ".setNull(index++, java.sql.Types.NUMERIC);");
      out.println("else");
      out.println("  " + query + ".setBigDecimal(index++, " + arg + ");");
    }
    else if (byte[].class.equals(type)) {
      out.println("if (" + arg + " == null)");
      out.println("  " + query + ".setNull(index++, java.sql.Types.VARBINARY);");
      out.println("else {");
      out.println("  byte []bArray = (byte []) " + arg + ";");
      out.println("  " + query + ".setBinaryStream(index++, new java.io.ByteArrayInputStream(bArray), bArray.length);");
      out.println("}");

      // XXX; fixing oracle issues
      // out.println("  " + query + ".setBytes(index++, (byte []) " + arg + ");");
    }
    /*
    else if (Serializable.class.isAssignableFrom(type)) {
      out.println("if (" + arg + " == null)");
      out.println("  " + query + ".setNull(index++, 0);");
      out.println("else");
      out.println("  " + query + ".setBytes(index++, _caucho_serialize(" + arg + "));");
      hasSerialization = true;
    }
    */
    else if (javax.ejb.EJBLocalObject.class.isAssignableFrom(type)) {
      EjbEntityBean bean = config.findEntityByLocal(type);

      if (bean == null)
	throw new IllegalStateException(L.l("can't find bean for {0}",
					    type.getName()));

      EntityType amberType = bean.getEntityType();

      ArrayList<IdField> keys = new ArrayList<IdField>();
      keys.addAll(amberType.getId().getKeys());
      Collections.sort(keys, new IdFieldCompare());

      String var = "_expr" + out.generateId();
      out.printClass(type);
      out.println(" " + var + " = " + arg + ";");

      out.println("if (" + var + " != null) {");
      out.pushDepth();
      
      for (IdField field : keys) {
	field.generateSet(out, query + "", "index", arg);
      }
      
      out.popDepth();
      out.println("} else {");
      out.pushDepth();
      
      for (IdField field : keys) {
	field.generateSet(out, query + "", "index", null);
      }
      
      out.popDepth();
      out.println("}");
    }
    else {
      Field []fields = type.getFields();

      String var = "_expr" + out.generateId();
      out.printClass(type);
      out.println(" " + var + " = " + arg + ";");

      out.println("if (" + var + " != null) {");
      out.pushDepth();
      for (Field field : fields) {
	generateSetParameter(out, config,
			     field.getType(),
			     query, arg + "." + field.getName());
      }
      out.popDepth();
      out.println("} else {");
      out.pushDepth();

      // XXX:
      for (int i = 0; i < fields.length; i++) {
	out.println(query + ".setNull(index++, 0);");
      }
	
      out.popDepth();
      out.println("}");
    }
  }
}
