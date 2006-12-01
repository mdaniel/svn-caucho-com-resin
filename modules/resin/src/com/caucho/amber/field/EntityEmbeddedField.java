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

package com.caucho.amber.field;

import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.expr.EmbeddedExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.EmbeddableType;
import com.caucho.amber.type.RelatedType;
import com.caucho.amber.type.Type;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration for a bean's embedded field
 */
public class EntityEmbeddedField extends AbstractField {
  private static final L10N L = new L10N(PropertyField.class);
  protected static final Logger log = Log.open(PropertyField.class);

  private HashMap<String, Column> _columns;
  private HashMap<String, String> _fieldNameByColumn;
  private EmbeddableType _type;

  private boolean _isInsert = true;
  private boolean _isUpdate = true;

  private boolean _isEmbeddedId = false;

  public EntityEmbeddedField(RelatedType relatedType,
                             String name)
    throws ConfigException
  {
    super(relatedType, name);
  }

  public EntityEmbeddedField(RelatedType relatedType)
  {
    super(relatedType);
  }

  public EmbeddableType getEmbeddableType()
  {
    return (EmbeddableType) _type;
  }

  /**
   * Sets the result type.
   */
  public void setType(EmbeddableType type)
  {
    _type = type;
  }

  /**
   * Sets the result type.
   */
  public Type getType()
  {
    return _type;
  }

  /**
   * Returns the table containing the field's columns.
   */
  public Table getTable()
  {
    // Assume all columns belong to the same table
    for (Map.Entry<String, Column> entry : _columns.entrySet()) {
      Column column = entry.getValue();
      return column.getTable();
    }

    return null;
  }

  /**
   * Sets the columns.
   */
  public void setEmbeddedColumns(HashMap<String, Column> columns)
  {
    _columns = columns;
  }

  /**
   * Gets the columns.
   */
  public HashMap<String, Column> getEmbeddedColumns()
  {
    return _columns;
  }

  /**
   * Sets the field name by column mapping.
   */
  public void setFieldNameByColumn(HashMap<String, String> fieldNameByColumn)
  {
    _fieldNameByColumn = fieldNameByColumn;
  }

  /**
   * Gets the field name by column mapping.
   */
  public HashMap<String, String> getFieldNameByColumn()
  {
    return _fieldNameByColumn;
  }

  /**
   * Returns true if the property is an @EmbeddedId.
   */
  public boolean isEmbeddedId()
  {
    return _isEmbeddedId;
  }

  /**
   * Set true if the property is an @EmbeddedId.
   */
  public void setEmbeddedId(boolean isEmbeddedId)
  {
    _isEmbeddedId = isEmbeddedId;
  }

  /**
   * Set true if the property should be saved on an insert.
   */
  public void setInsert(boolean isInsert)
  {
    _isInsert = isInsert;
  }

  /**
   * Set true if the property should be saved on an update.
   */
  public void setUpdate(boolean isUpdate)
  {
    _isUpdate = isUpdate;
  }

  /**
   * Initializes the property.
   */
  public void init()
    throws ConfigException
  {
    super.init();

    if (getEmbeddedColumns() == null)
      throw new IllegalStateException(L.l("columns must be set before init"));
  }

  /**
   * Returns the null value.
   */
  public String generateNull()
  {
    return getType().generateNull();
  }

  /**
   * Generates the set property.
   */
  public void generateGetProperty(JavaWriter out)
    throws IOException
  {
    if (! isFieldAccess() && getGetterMethod() == null)
      return;

    out.println();
    out.println("public " + getJavaTypeName() + " " + getGetterName() + "()");
    out.println("{");
    out.pushDepth();

    out.println("if (__caucho_session != null)");
    out.println("  __caucho_load_" + getLoadGroupIndex() + "(__caucho_session);");
    out.println();
    out.println("return " + generateSuperGetter() + ";");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the set property.
   */
  public void generateSetProperty(JavaWriter out)
    throws IOException
  {
    if (! isFieldAccess() && (getGetterMethod() == null ||
                              getSetterMethod() == null && ! isAbstract()))
      return;

    out.println();
    out.println("public void " + getSetterName() + "(" + getJavaTypeName() + " v)");
    out.println("{");
    out.pushDepth();

    if (! _isUpdate) {
      out.println("if (__caucho_session == null)");
      out.println("  " + generateSuperSetter("v") + ";");
    }
    else {
      out.println(getJavaTypeName() + " oldValue = " + generateSuperGetter() + ";");

      int maskGroup = getLoadGroupIndex() / 64;
      String loadVar = "__caucho_loadMask_" + maskGroup;

      long mask = 1L << (getLoadGroupIndex() % 64);

      if (getJavaTypeName().equals("java.lang.String")) {
        out.println("if ((oldValue == v || v != null && v.equals(oldValue)) && (" + loadVar + " & " + mask + "L) != 0L)");
        out.println("  return;");
      }
      else {
        out.println("if (oldValue == v && (" + loadVar + " & " + mask + "L) != 0)");
        out.println("  return;");
      }

      out.println(generateSuperSetter("v") + ";");

      int dirtyGroup = getIndex() / 64;
      String dirtyVar = "__caucho_dirtyMask_" + dirtyGroup;

      long dirtyMask = 1L << (getIndex() % 64);

      out.println();
      out.println("long oldMask = " + dirtyVar + ";");
      out.println(dirtyVar + " |= " + dirtyMask + "L;");
      out.println();
      out.println("if (__caucho_session != null && oldMask == 0)");
      out.println("  __caucho_session.update(this);");
    }

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the select clause.
   */
  public String generateLoadSelect(Table table, String id)
  {
    if (getTable() != table)
      return null;
    else
      return generateSelect(id);
  }

  /**
   * Generates the select clause.
   */
  public String generateSelect(String id)
  {
    StringBuilder sb = new StringBuilder();

    boolean isFirst = true;

    for (Map.Entry<String, Column> entry : _columns.entrySet()) {
      Column column = entry.getValue();
      if (isFirst)
        isFirst = false;
      else
        sb.append(", ");
      sb.append(column.generateSelect(id));
    }

    return sb.toString();
  }

  /**
   * Generates the where clause.
   */
  public String generateWhere(String id)
  {
    StringBuilder sb = new StringBuilder();

    for (Map.Entry<String, Column> entry : _columns.entrySet()) {
      Column column = entry.getValue();
      sb.append(column.generateSelect(id));
    }

    return sb.toString();
  }

  /**
   * Generates the insert.
   */
  public void generateInsertColumns(ArrayList<String> columns)
  {
    if (_isInsert) {
      for (Map.Entry<String, Column> entry : _columns.entrySet()) {
        Column column = entry.getValue();
        columns.add(column.getName());
      }
    }
  }

  /**
   * Generates the update set clause
   */
  public void generateUpdate(CharBuffer sql)
  {
    if (_isUpdate) {
      for (Map.Entry<String, Column> entry : _columns.entrySet()) {
        Column column = entry.getValue();
        sql.append(column.generateUpdateSet());
      }
    }

    /*
      sql.append(getColumn());
      sql.append("=?");
    */
  }

  /**
   * Generates the set clause for the insert clause.
   */
  public void generateInsertSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException
  {
    if (_isInsert)
      generateSet(out, pstmt, index, obj);
    else if (getLoadGroupIndex() != 0) {
      int groupIndex = getLoadGroupIndex();
      int group = groupIndex / 64;
      long groupMask = 1L << (groupIndex % 64);
      out.println("__caucho_loadMask_" + group + " &= ~" + groupMask + "L;");
    }
  }

  /**
   * Generates the set clause for the insert clause.
   */
  public void generateUpdateSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException
  {
    if (_isUpdate)
      generateSet(out, pstmt, index, obj);
  }

  /**
   * Generates the set clause.
   */
  public void generateSet(JavaWriter out, String pstmt,
                          String index, String obj)
    throws IOException
  {
    if (! isFieldAccess() && getGetterMethod() == null)
      return;

    for (Map.Entry<String, Column> entry : _columns.entrySet()) {
      Column column = entry.getValue();

      String getter = _fieldNameByColumn.get(column.getName());

      EmbeddableType embeddableType = getEmbeddableType();

      if (! embeddableType.isFieldAccess())
        getter = "get" + Character.toUpperCase(getter.charAt(0)) +
          getter.substring(1) + "()";

      out.println("if (" + generateGet(obj) + " == null) {");
      out.pushDepth();

      out.println(pstmt + ".setNull(index++, java.sql.Types.NULL);");

      out.popDepth();
      out.println("} else");
      out.pushDepth();
      column.generateSet(out, pstmt, index, generateGet(obj)+"."+getter);
      out.popDepth();
    }
  }

  /**
   * Generates get property.
   */
  public void generateGetPrimaryKey(CharBuffer cb)
  {
    if (! isFieldAccess() && getGetterMethod() == null)
      return;

    String thisGetter = generateGet("this");

    EmbeddableType embeddableType = getEmbeddableType();

    ArrayList<AmberField> fields = embeddableType.getFields();
    for (int i = 0; i < fields.size(); i++) {
      if (i != 0)
        cb.append(", ");

      AmberField field = fields.get(i);

      String getter = field.getName();

      if (! embeddableType.isFieldAccess()) {
        getter = "get" + Character.toUpperCase(getter.charAt(0)) +
          getter.substring(1) + "()";
      }

      cb.append(thisGetter + "." + getter);
    }
  }

  /**
   * Generates loading code
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    /*
      if (getSetterMethod() == null)
      return index;
    */

    String var = "amber_ld_embedded" + index;

    out.print(getJavaTypeName());
    out.println(" " + var + " = new "+getJavaTypeName()+"();");

    for (Map.Entry<String, Column> entry : _columns.entrySet()) {
      Column column = entry.getValue();
      out.print(column.getType().getJavaTypeName());
      out.print(" amber_ld" + index + " = ");
      index = column.generateLoad(out, rs, indexVar, index);
      out.println(";");

      String setter = _fieldNameByColumn.get(column.getName());

      EmbeddableType embeddableType = getEmbeddableType();

      if (embeddableType.isFieldAccess()) {
        out.println(var + "." + setter + " = amber_ld" + (index-1) + ";");
      }
      else {
        out.println(var + ".set" + Character.toUpperCase(setter.charAt(0)) +
                    setter.substring(1) + "(amber_ld" + (index-1) + ");");
      }
    }

    out.println(generateSuperSetter(var) + ";");

    // out.println("__caucho_loadMask |= " + (1L << getIndex()) + "L;");

    return index;
  }

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return new EmbeddedExpr(parent, _type, _columns, _fieldNameByColumn);
  }
}
