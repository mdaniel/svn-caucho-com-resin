/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.amber.field;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Set;

import java.util.logging.Logger;

import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JType;

import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.java.JavaWriter;

import com.caucho.amber.type.Type;
import com.caucho.amber.type.EntityType;

import com.caucho.amber.table.Table;
import com.caucho.amber.table.LinkColumns;

import com.caucho.amber.query.QueryParser;
import com.caucho.amber.query.AmberExpr;
import com.caucho.amber.query.PathExpr;
import com.caucho.amber.query.OneToManyExpr;

/**
 * Represents a field to a collection of objects where the target
 * hold a back-link to the source entity.
 */
public class EntityOneToManyField extends CollectionField {
  private static final L10N L = new L10N(EntityOneToManyField.class);
  protected static final Logger log = Log.open(EntityOneToManyField.class);

  private ArrayList<String> _orderByFields;
  private ArrayList<Boolean> _orderByAscending;

  private EntityManyToOneField _sourceField;

  public EntityOneToManyField(EntityType entityType, String name)
    throws ConfigException
  {
    super(entityType, name);
  }

  public EntityOneToManyField(EntityType entityType)
  {
    super(entityType);
  }

  /**
   * Sets the order by.
   */
  public void setOrderBy(ArrayList<String> orderByFields,
			 ArrayList<Boolean> orderByAscending)
  {
    _orderByFields = orderByFields;
    _orderByAscending = orderByAscending;
  }

  /**
   * Returns the target type as entity.
   */
  public EntityType getEntityTargetType()
  {
    return (EntityType) getTargetType();
  }

  /**
   * Returns the target type as entity.
   */
  public Type getTargetType()
  {
    return _sourceField.getSourceType();
  }

  /**
   * Gets the source field.
   */
  public EntityManyToOneField getSourceField()
  {
    return _sourceField;
  }

  /**
   * Sets the source field.
   */
  public void setSourceField(EntityManyToOneField sourceField)
  {
    _sourceField = sourceField;
  }

  /**
   * Returns the link.
   */
  public LinkColumns getLinkColumns()
  {
    return _sourceField.getLinkColumns();
  }

  /**
   * Initialize.
   */
  public void init()
  {
    if (_sourceField == null || getLinkColumns() == null)
      throw new IllegalStateException();
  }

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return new OneToManyExpr(parser, parent, getLinkColumns());
  }

  /**
   * Generates the set clause.
   */
  public void generateSet(JavaWriter out, String pstmt,
			  String obj, String index)
    throws IOException
  {
  }

  /**
   * Generates the select clause.
   */
  public String generateLoadSelect(String id)
  {
    return null;
  }

  /**
   * Updates from the cached copy.
   */
  public void generateCopyLoadObject(JavaWriter out,
				       String dst, String src,
				       int loadIndex)
    throws IOException
  {
  }

  /**
   * Generates the target select.
   */
  public String generateTargetSelect(String id)
  {
    CharBuffer cb = CharBuffer.allocate();

    Id key = getEntityTargetType().getId();
    
    cb.append(key.generateSelect(id));

    String value = getEntityTargetType().generateLoadSelect(id);

    if (cb.length() > 0 && value.length() > 0)
      cb.append(", ");
    
    cb.append(value);
    
    return cb.close();
  }

  /**
   * Generates the set property.
   */
  public void generateGetProperty(JavaWriter out)
    throws IOException
  {
    String var = "_caucho_" + getGetterName();
    
    boolean isSet = getJavaType().isAssignableTo(Set.class);

    JType type = getJavaType();
    JType []paramArgs = type.getActualTypeArguments();
    JType param = paramArgs.length > 0 ? paramArgs[0] : null;

    if (isSet)
      out.print("private com.caucho.amber.collection.SetImpl");
    else
      out.print("private com.caucho.amber.collection.CollectionImpl");

    if (param != null)
      out.print("<" + param.getPrintName() + ">");
    
    out.println(" " + var + ";");
    
    out.println();
    out.println("public " + getJavaTypeName() + " " + getGetterName() + "()");
    out.println("{");
    out.pushDepth();

    out.println("if (" + var + " != null) {");
    out.pushDepth();
    out.println(var + ".setSession(__caucho_session);");
    out.println("return " + var + ";");
    out.popDepth();
    out.println("}");

    out.println("if (__caucho_session == null) {");
    if (! isAbstract()) {
      out.println("  return super." + getGetterName() + "();");
    }
    out.println("}");

    out.println("try {");
    out.pushDepth();
    
    out.print("String sql=\"");
    
    out.print("SELECT c");
    out.print(" FROM " + getSourceType().getName() + " o,");
    out.print("      o." + getName() + " c");
    out.print(" WHERE ");
    out.print(getSourceType().getId().generateRawWhere("o"));
    
    if (_orderByFields != null) {
      out.print(" ORDER BY ");

      for (int i = 0; i < _orderByFields.size(); i++) {
	if (i != 0)
	  out.print(", ");
	
	out.print("c." + _orderByFields.get(i));
	if (Boolean.FALSE.equals(_orderByAscending.get(i)))
	  out.print(" DESC");
      }
    }

    out.println("\";");
    out.println("com.caucho.amber.AmberQuery query;");
    out.println("query = __caucho_session.prepareQuery(sql);");

    out.println("int index = 1;");
    getSourceType().getId().generateSet(out, "query", "index", "this");

    if (isSet)
      out.print(var + " = new com.caucho.amber.collection.SetImpl");
    else
      out.print(var + " = new com.caucho.amber.collection.CollectionImpl");

    if (param != null)
      out.print("<" + param.getPrintName() + ">");

    out.println("(query);");
    /*
    out.pushDepth();

    //generateAdd(out);
    //generateRemove(out);
    //generateClear(out);
    // generateSize(out);
    
    out.popDepth();
    out.println("};");
    */
    
    out.println();
    out.println("return " + var + ";");
    
    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  throw com.caucho.amber.AmberRuntimeException.create(e);");
    out.println("}");
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the size method.
   */
  private void generateSize(JavaWriter out)
    throws IOException
  {
    out.println("public int size()");
    out.println("{");
    out.pushDepth();

    out.println("if (__caucho_session == null || isValid())");
    out.println("  return super.size();");

    out.println("try {");
    out.pushDepth();
    
    out.println("__caucho_session.flush();");
    
    out.print("String sql=\"");
    
    out.print("SELECT count(*) FROM ");
    out.print(getSourceType().getName());
    out.print(" AS o ");
    
    out.print(" WHERE ");

    // getKeyColumn().generateRawMatchArgWhere("o");

    ArrayList<IdField> keys = getSourceType().getId().getKeys();
    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
	out.print(" AND ");
      
      out.print("o." + keys.get(i).getName());
      out.print("=?");
    }
    
    out.println("\";");
    out.println("com.caucho.amber.AmberQuery query;");
    out.println("query = __caucho_session.prepareQuery(sql);");

    out.println("int index = 1;");
    getSourceType().getId().generateSet(out, "query", "index", getSourceType().getName() + "__ResinExt.this");

    out.println("java.sql.ResultSet rs = query.executeQuery();");

    out.println("if (rs.next())");
    out.println("  return rs.getInt(1);");
    out.println("else");
    out.println("  return 0;");
    
    out.popDepth();
    out.println("} catch (java.sql.SQLException e) {");
    out.println("  throw com.caucho.amber.AmberRuntimeException.create(e);");
    out.println("}");

    out.popDepth();
    out.println("}");
  }
  
  /**
   * Generates the set property.
   */
  public void generateSetProperty(JavaWriter out)
    throws IOException
  {
    JMethod setter = getSetterMethod();

    if (setter == null)
      return;

    JClass []paramTypes = setter.getParameterTypes();
    
    out.println();
    out.print("public void " + setter.getName() + "(");
    out.print(getJavaTypeName() + " value)");
    out.println("{");
    out.pushDepth();
    out.popDepth();
    out.println("}");
  }    
  
  /**
   * Generates code for foreign entity create/delete
   */
  public void generateInvalidateForeign(JavaWriter out)
    throws IOException
  {
    Table table = getLinkColumns().getSourceTable();
    
    out.println("if (\"" + table.getName() + "\".equals(table)) {");
    out.pushDepth();

    String var = "_caucho_" + getGetterName();
      
    out.println("if (" + var + " != null)");
    out.println("  " + var + ".update();");
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the expire code
   *
   * ejb/06hi
   */
  public void generateExpire(JavaWriter out)
    throws IOException
  {
    String var = "_caucho_" + getGetterName();
    
    out.println(var + " = null;");
  }
}
