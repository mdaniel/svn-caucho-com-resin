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

import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.java.JavaWriter;

import com.caucho.amber.type.Type;
import com.caucho.amber.type.EntityType;

import com.caucho.amber.table.Table;
import com.caucho.amber.table.LinkColumns;

import com.caucho.amber.query.AmberExpr;
import com.caucho.amber.query.PathExpr;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.query.ManyToOneExpr;
import com.caucho.amber.query.OneToManyExpr;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JType;

/**
 * Configuration for a bean's field
 */
public class EntityManyToManyField extends AssociationField {
  private static final L10N L = new L10N(EntityManyToManyField.class);
  protected static final Logger log = Log.open(EntityManyToManyField.class);

  private EntityType _targetType;

  private Table _associationTable;

  private LinkColumns _sourceLink;
  private LinkColumns _targetLink;

  public EntityManyToManyField(EntityType entityType, String name)
    throws ConfigException
  {
    super(entityType, name);
  }

  public EntityManyToManyField(EntityType entityType)
  {
    super(entityType);
  }

  /**
   * Sets the target type.
   */
  public void setType(Type targetType)
  {
    _targetType = (EntityType) targetType;

    super.setType(targetType);
  }

  /**
   * Returns the target type.
   */
  public EntityType getTargetType()
  {
    return _targetType;
  }

  /**
   * Returns the association table
   */
  public Table getAssociationTable()
  {
    return _associationTable;
  }

  /**
   * Sets the association table
   */
  public void setAssociationTable(Table table)
  {
    _associationTable = table;
  }

  /**
   * Adds a column from the association table to the source side.
   */
  public void setSourceLink(LinkColumns link)
  {
    _sourceLink = link;
  }

  /**
   * Returns the source link.
   */
  public LinkColumns getSourceLink()
  {
    return _sourceLink;
  }

  /**
   * Adds a column from the association table to the target side.
   */
  public void setTargetLink(LinkColumns link)
  {
    _targetLink = link;
  }

  /**
   * Returns the target link.
   */
  public LinkColumns getTargetLink()
  {
    return _targetLink;
  }

  /**
   * Initializes the field.
   */
  public void init()
    throws ConfigException
  {
    // XXX: might not have cascade delete if there's an associated entity
    
    _targetLink.setSourceCascadeDelete(true);
    _sourceLink.setSourceCascadeDelete(true);
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
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return new ManyToOneExpr(new OneToManyExpr(parser, parent, _sourceLink),
			     _targetLink);
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
    return getTargetType().getId().generateSelect(id);
  }

  /**
   * Generates the select clause.
   */
  public String generateTargetLoadSelect(String id)
  {
    CharBuffer cb = CharBuffer.allocate();
    
    cb.append(getTargetType().getId().generateLoadSelect(id));

    String value = getTargetType().generateLoadSelect(id);

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

    JType type = getJavaType();
    JType []paramArgs = type.getActualTypeArguments();
    JType param = paramArgs.length > 0 ? paramArgs[0] : null;
    
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
    
    out.print("SELECT o." + getName());
    out.print(" FROM " + getSourceType().getName() + " o");
    out.print(" WHERE ");
    out.print(getSourceType().getId().generateRawWhere("o"));
    
    out.println("\";");
    out.println("com.caucho.amber.AmberQuery query;");
    out.println("query = __caucho_session.prepareQuery(sql);");

    out.println("int index = 1;");
    getSourceType().getId().generateSet(out, "query", "index", "this");

    out.print(var + " = new com.caucho.amber.collection.CollectionImpl");
    
    if (param != null)
      out.print("<" + param.getPrintName() + ">");
    
    out.println("(query);");

    /*
    out.pushDepth();

    generateAdd(out);
    generateRemove(out);
    generateClear(out);
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

    generateAmberAdd(out);
    generateAmberRemove(out);
    generateAmberRemoveTargetAll(out);
  }
  

  /**
   * Generates the set property.
   */
  private void generateAdd(JavaWriter out)
    throws IOException
  {
    JType type = getJavaType();
    JType []paramArgs = type.getActualTypeArguments();
    String gType = paramArgs.length > 0 ? paramArgs[0].getPrintName() : "Object";
    
    out.println("public boolean add(" + gType + " o)");
    out.println("{");
    out.pushDepth();

    String ownerType = getSourceType().getInstanceClassName();

    out.println("if (! (o instanceof " + ownerType + "))");
    out.println("  throw new java.lang.IllegalArgumentException((o == null ? \"null\" : o.getClass().getName()) + \" must be a " + ownerType + "\");");

    out.println(ownerType + " bean = (" + ownerType + ") o;");

    // XXX: makePersistent

    /*
    ArrayList<Column> keyColumns = getKeyColumns();
    for (int i = 0; i < keyColumns.size(); i++) {
      Column column = keyColumns.get(i);
      AbstractProperty prop = column.getProperty();
	
	
      if (prop != null) {
	out.println("bean." + prop.getSetterName() + "(" + ownerType + "__ResinExt.this);");
      }
    }
    */

    out.println("return true;");
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the set property.
   */
  private void generateRemove(JavaWriter out)
    throws IOException
  {
    JType type = getJavaType();
    JType []paramArgs = type.getActualTypeArguments();
    String gType = paramArgs.length > 0 ? paramArgs[0].getPrintName() : "Object";
    
    out.println("public boolean remove(" + gType + " o)");
    out.println("{");
    out.pushDepth();

    String ownerType = getSourceType().getInstanceClassName();

    out.println("if (! (o instanceof " + ownerType + "))");
    out.println("  throw new java.lang.IllegalArgumentException((o == null ? \"null\" : o.getClass().getName()) + \" must be a " + ownerType + "\");");

    out.println(ownerType + " bean = (" + ownerType + ") o;");

    // XXX: makePersistent

    /*
    ArrayList<Column> keyColumns = getKeyColumns();
    for (int i = 0; i < keyColumns.size(); i++) {
      Column column = keyColumns.get(i);
      AbstractProperty prop = column.getProperty();
	
      if (prop != null) {
	out.println("bean." + prop.getSetterName() + "(null);");
      }
    }
    */

    out.println("return true;");
    
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the clear method.
   */
  private void generateClear(JavaWriter out)
    throws IOException
  {
    out.println("public void clear()");
    out.println("{");
    out.pushDepth();

    out.println("if (__caucho_session != null) {");
    out.pushDepth();

    out.println("try {");
    out.pushDepth();
    
    out.println("__caucho_session.flush();");
    
    out.print("String sql=\"");
    
    out.print("UPDATE ");
    out.print(getSourceType().getName());
    out.print(" SET ");
    /*
    ArrayList<Column> columns = getKeyColumns();
    for (int i = 0; i < columns.size(); i++) {
      if (i != 0)
	out.print(", ");
      
      out.print(columns.get(i).getName());
      out.print("=null");
    }
    */
    
    out.print(" WHERE ");

    /*
    for (int i = 0; i < columns.size(); i++) {
      if (i != 0)
	out.print(" AND ");
      
      out.print(columns.get(i).getName());
      out.print("=?");
    }
    */
    
    out.println("\";");
    out.println("com.caucho.amber.AmberQuery query;");
    out.println("query = __caucho_session.prepareQuery(sql);");

    String ownerType = getSourceType().getInstanceClassName();
    
    out.println("int index = 1;");
    getSourceType().getId().generateSet(out, "query", "index", ownerType + ".this");

    out.println("query.executeUpdate();");

    out.println("super.clear();");
    
    out.popDepth();
    out.println("} catch (java.sql.SQLException e) {");
    out.println("  throw com.caucho.amber.AmberRuntimeException.create(e);");
    out.println("}");

    out.popDepth();
    out.println("} else {");
    out.println("  super.clear();");
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

    /*
    ArrayList<Column> columns = getKeyColumns();
    for (int i = 0; i < columns.size(); i++) {
      if (i != 0)
	out.print(" AND ");
      
      out.print("o." + columns.get(i).getName());
      out.print("=?");
    }
    */
    
    out.println("\";");
    out.println("com.caucho.amber.AmberQuery query;");
    out.println("query = __caucho_session.prepareQuery(sql);");

    out.println("int index = 1;");
    getSourceType().getId().generateSet(out, "query", getSourceType().getName() + "__ResinExt.this", "index");

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
  public void generateAmberAdd(JavaWriter out)
    throws IOException
  {
    String targetType = getTargetType().getProxyClass().getName();
    
    out.println();
    out.println("public boolean" +
		" __amber_" + getGetterName() + "_add(Object o)");
    out.println("{");
    out.pushDepth();
    
    out.println("if (! (o instanceof " + targetType + "))");
    out.println("  return false;");

    out.println();
    out.println(targetType + " v = (" + targetType + ") o;");
    out.println();
    out.println("if (__caucho_session == null)");
    out.println("  return false;");

    out.println();
    out.print("String sql = \"INSERT INTO ");
    out.print(_associationTable.getName() + " (");

    out.print(_sourceLink.generateSelectSQL(null));
    
    out.print(", ");

    out.print(_targetLink.generateSelectSQL(null));
    
    out.print(") VALUES (");

    int count = (getSourceType().getId().getKeyCount() +
		 getTargetType().getId().getKeyCount());
    
    for (int i = 0; i < count; i++) {
      if (i != 0)
	out.print(", ");

      out.print("?");
    }
    out.println(")\";");

    out.println();
    out.println("try {");
    out.pushDepth();

    out.println("java.sql.PreparedStatement pstmt = __caucho_session.prepareInsertStatement(sql);");

    out.println("int index = 1;");
    getSourceType().getId().generateSet(out, "pstmt", "index", "this");
    getTargetType().getId().generateSet(out, "pstmt", "index", "v");
    
    out.println("if (pstmt.executeUpdate() == 1) {");
    out.pushDepth();
    out.println("__caucho_session.addCompletion(new com.caucho.amber.entity.TableInvalidateCompletion(\"" + _targetLink.getSourceTable().getName() + "\"));");
    out.println("return true;");
    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("}");
    
    out.println("return false;");
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the remove property.
   */
  public void generateAmberRemove(JavaWriter out)
    throws IOException
  {
    String targetType = getTargetType().getProxyClass().getName();
    
    out.println();
    out.println("public boolean" +
		" __amber_" + getGetterName() + "_remove(Object o)");
    out.println("{");
    out.pushDepth();
    
    out.println("if (! (o instanceof " + targetType + "))");
    out.println("  return false;");

    out.println();
    out.println(targetType + " v = (" + targetType + ") o;");
    out.println();
    out.println("if (__caucho_session == null)");
    out.println("  return false;");

    out.println();
    out.print("String sql = \"DELETE FROM ");
    out.print(_associationTable.getName() + " WHERE ");

    out.print(_sourceLink.generateMatchArgSQL(null));

    out.print(" AND ");

    out.print(_targetLink.generateMatchArgSQL(null));

    out.println("\";");

    out.println();
    out.println("try {");
    out.pushDepth();

    out.println("java.sql.PreparedStatement pstmt = __caucho_session.prepareStatement(sql);");

    out.println("int index = 1;");
    getSourceType().getId().generateSet(out, "pstmt", "index", "this");
    getTargetType().getId().generateSet(out, "pstmt", "index", "v");
    
    out.println("if (pstmt.executeUpdate() == 1) {");
    out.pushDepth();
    out.println("__caucho_session.addCompletion(new com.caucho.amber.entity.TableInvalidateCompletion(\"" + _targetLink.getSourceTable().getName() + "\"));");
    out.println("return true;");
    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("}");
    
    out.println("return false;");
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the remove property.
   */
  public void generateAmberRemoveTargetAll(JavaWriter out)
    throws IOException
  {
    String targetType = getTargetType().getProxyClass().getName();
    
    out.println();
    out.println("public boolean" +
		" __amber_" + getGetterName() + "_remove_target(Object o)");
    out.println("{");
    out.pushDepth();
    
    out.println("if (! (o instanceof " + targetType + "))");
    out.println("  return false;");

    out.println();
    out.println(targetType + " v = (" + targetType + ") o;");
    out.println();
    out.println("if (__caucho_session == null)");
    out.println("  return false;");

    out.println();
    out.print("String sql = \"DELETE FROM ");
    out.print(_associationTable.getName() + " WHERE ");

    out.print(_targetLink.generateMatchArgSQL(null));

    out.println("\";");

    out.println();
    out.println("try {");
    out.pushDepth();

    out.println("java.sql.PreparedStatement pstmt = __caucho_session.prepareStatement(sql);");

    out.println("int index = 1;");
    getTargetType().getId().generateSet(out, "pstmt", "index", "v");
    
    out.println("if (pstmt.executeUpdate() == 1)");
    out.println("  return true;");

    out.popDepth();
    out.println("} catch (Exception e) {");
    out.println("  __caucho_log.log(java.util.logging.Level.FINE, e.toString(), e);");
    out.println("}");
    
    out.println("return false;");
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

    JType type = getGetterMethod().getGenericReturnType();
    
    out.println();
    out.print("public void " + setter.getName() + "(");
    out.print(type.getPrintName() + " value)");
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
    out.println("if (\"" + _sourceLink.getSourceTable().getName() + "\".equals(table)) {");
    out.pushDepth();

    generateExpire(out);
    
    out.popDepth();
    out.println("}");
  }
  
  /**
   * Generates code for the object expire
   */
  public void generateExpire(JavaWriter out)
    throws IOException
  {
    String var = "_caucho_" + getGetterName();
      
    out.println("if (" + var + " != null)");
    out.println("  " + var + ".update();");
  }
}
