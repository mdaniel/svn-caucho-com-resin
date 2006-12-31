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
import com.caucho.amber.expr.DependentEntityOneToOneExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.*;
import com.caucho.amber.type.*;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

import javax.persistence.CascadeType;

/**
 * Represents a many-to-one link pointing to an entity.
 */
public class DependentEntityOneToOneField extends CascadableField {
  private static final L10N L = new L10N(DependentEntityOneToOneField.class);
  protected static final Logger log = Log.open(DependentEntityOneToOneField.class);

  private EntityManyToOneField _targetField;
  private long _targetLoadIndex;
  private boolean _isCascadeDelete;

  public DependentEntityOneToOneField(RelatedType relatedType,
                                      String name)
    throws ConfigException
  {
    super(relatedType, name, null);
  }

  /**
   * Sets the target field.
   */
  public void setTargetField(EntityManyToOneField targetField)
  {
    _targetField = targetField;
  }

  /**
   * Sets the target field.
   */
  public EntityManyToOneField getTargetField()
  {
    return _targetField;
  }

  /**
   * Returns the source type as
   * entity or mapped-superclass.
   */
  public RelatedType getEntitySourceType()
  {
    return (RelatedType) getSourceType();
  }

  /**
   * Returns the target type as
   * entity or mapped-superclass.
   */
  public RelatedType getEntityTargetType()
  {
    return (RelatedType) _targetField.getSourceType();
  }

  /**
   * Returns the target type.
   */
  public Type getType()
  {
    return getEntityTargetType();
  }

  /**
   * Returns the foreign type.
   */
  public String getForeignTypeName()
  {
    //return ((KeyColumn) getColumn()).getType().getForeignTypeName();
    return getEntityTargetType().getForeignTypeName();
  }

  /**
   * Sets the column.
   */
  public void setColumn(Column column)
  {
    throw new IllegalStateException();
  }

  /**
   * Sets the cascade-delete property.
   */
  public void setCascadeDelete(boolean isCascadeDelete)
  {
    _isCascadeDelete = isCascadeDelete;
  }

  /**
   * Returns the cascade-delete property.
   */
  public boolean isCascadeDelete()
  {
    return _isCascadeDelete;
  }

  public void init()
    throws ConfigException
  {
    super.init();

    _targetLoadIndex = getEntitySourceType().nextLoadGroupIndex();
  }

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return new DependentEntityOneToOneExpr(parent,
                                           _targetField.getLinkColumns());
  }

  /**
   * Gets the column corresponding to the target field.
   */
  public ForeignColumn getColumn(IdField targetField)
  {
    /*
      EntityColumn entityColumn = (EntityColumn) getColumn();

      ArrayList<ForeignColumn> columns = entityColumn.getColumns();

      Id id = getEntityTargetType().getId();
      ArrayList<IdField> keys = id.getKeys();

      for (int i = 0; i < keys.size(); i++ ){
      if (keys.get(i) == targetField)
      return columns.get(i);
      }
    */

    return null;
  }

  /**
   * Generates the flush check for this child.
   */
  public boolean generateFlushCheck(JavaWriter out)
    throws IOException
  {
    String getter = generateSuperGetter();

    out.println("if (" + getter + " != null) {");
    out.pushDepth();
    out.println("int state = ((com.caucho.amber.entity.Entity) " + getter + ").__caucho_getEntityState();");

    // jpa/0s2d
    out.println("if (this.__caucho_state < com.caucho.amber.entity.Entity.P_DELETING");
    out.println("    && (state <= com.caucho.amber.entity.Entity.P_NEW || ");
    out.println("        state >= com.caucho.amber.entity.Entity.P_DELETED))");

    String errorString = ("(\"amber flush: unable to flush " +
                          getEntitySourceType().getName() + "[\" + __caucho_getPrimaryKey() + \"] "+
                          "with non-managed dependent relationship one-to-one to "+
                          getEntityTargetType().getName() + "\")");

    out.println("  throw new IllegalStateException" + errorString + ";");
    out.popDepth();
    out.println("}");

    return true;
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    super.generatePrologue(out, completedSet);

    out.println();

    Id id = getEntityTargetType().getId();

    id.generatePrologue(out, completedSet, getName());
  }

  /**
   * Generates the linking for a join
   */
  public void generateJoin(CharBuffer cb,
                           String sourceTable,
                           String targetTable)
  {
    LinkColumns linkColumns = _targetField.getLinkColumns();

    cb.append(linkColumns.generateJoin(sourceTable, targetTable));
  }

  /**
   * Generates loading code
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    if (isLazy()) {
      out.println(generateSuperSetter("null") + ";");

      String loadVar = "__caucho_loadMask_" + (_targetLoadIndex / 64);
      long loadMask = (1L << _targetLoadIndex);

      out.println(loadVar + " &= ~" + loadMask + "L;");
    }

    // return index + 1;
    return index;
  }

  /**
   * Generates loading code
   */
  public int generateLoadEager(JavaWriter out, String rs,
                               String indexVar, int index)
    throws IOException
  {
    if (! isLazy()) {

      String loadVar = "__caucho_loadMask_" + (_targetLoadIndex / 64);
      long loadMask = 1L << (_targetLoadIndex % 64);

      out.println(loadVar + " |= " + loadMask + "L;");

      String javaType = getJavaTypeName();

      out.println("if ((preloadedProperties == null) || (! preloadedProperties.containsKey(\"" + getName() + "\"))) {");
      out.pushDepth();

      String indexS = "_" + (_targetLoadIndex / 64);
      indexS += "_" + (1L << (_targetLoadIndex % 64));

      generateLoadProperty(out, indexS, "aConn");

      out.popDepth();
      out.println("} else {");
      out.println("  " + generateSuperSetter("(" + javaType + ") preloadedProperties.get(\"" + getName() + "\")") + ";");
      out.println("}");
    }

    return index;
  }

  /**
   * Generates the set property.
   */
  public void generateGetProperty(JavaWriter out)
    throws IOException
  {
    String loadVar = "__caucho_loadMask_" + (_targetLoadIndex / 64);
    long loadMask = 1L << (_targetLoadIndex % 64);

    String index = "_" + (_targetLoadIndex / 64);
    index += "_" + (1L << (_targetLoadIndex % 64));

    String javaType = getJavaTypeName();

    out.println();
    out.println("public " + javaType + " " + getGetterName() + "()");
    out.println("{");
    out.pushDepth();

    out.print("if (__caucho_session != null && ");
    out.println("(" + loadVar + " & " + loadMask + "L) == 0) {");
    out.pushDepth();
    out.println("__caucho_load_" + getLoadGroupIndex() + "(__caucho_session);");
    out.println(loadVar + " |= " + loadMask + "L;");

    generateLoadProperty(out, index, "__caucho_session");

    /*
      out.print(javaType + " v = (" + javaType + ") __caucho_session.loadProxy(\"" + getEntityTargetType().getName() + "\", ");
      out.println("__caucho_field_" + getName() + ", true);");
    */

    out.println("return v"+index+";");
    out.popDepth();
    out.println("}");
    out.println("else {");
    out.println("  return " + generateSuperGetter() + ";");
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the set property.
   */
  public void generateLoadProperty(JavaWriter out,
                                   String index,
                                   String session)
    throws IOException
  {
    String javaType = getJavaTypeName();

    out.println(javaType + " v"+index+" = null;");

    out.println("try {");
    out.pushDepth();

    out.print("String sql"+index+" = \"");
    out.print("SELECT o." + getName() +
              " FROM " + getEntitySourceType().getName() + " o" +
              " WHERE ");

    ArrayList<IdField> sourceKeys = getEntitySourceType().getId().getKeys();
    for (int i = 0; i < sourceKeys.size(); i++) {
      if (i != 0)
        out.print(" and ");

      IdField key = sourceKeys.get(i);

      out.print("o." + key.getName() + "=?");
    }
    out.println("\";");

    out.println("com.caucho.amber.AmberQuery query"+index+" = "+session+".prepareQuery(sql"+index+");");

    out.println("int index"+index+" = 1;");

    getEntitySourceType().getId().generateSet(out, "query"+index, "index"+index, "super");

    out.println("v"+index+" = (" + javaType + ") query"+index+".getSingleResult();");

    out.popDepth();
    out.println("} catch (java.sql.SQLException e) {");
    out.println("  throw new RuntimeException(e);");
    out.println("}");

    out.println(generateSuperSetter("v"+index) + ";");
  }

  /**
   * Updates the cached copy.
   */
  public void generateCopyUpdateObject(JavaWriter out,
                                       String dst, String src,
                                       int updateIndex)
    throws IOException
  {
    if (getIndex() == updateIndex) {
      String value = generateGet(src);
      out.println(generateSet(dst, value) + ";");
    }
  }

  /**
   * Updates the cached copy.
   */
  public void generateCopyLoadObject(JavaWriter out,
                                     String dst, String src,
                                     int updateIndex)
    throws IOException
  {
    if (getLoadGroupIndex() == updateIndex) {
      String value = generateGet(src);
      out.println(generateSet(dst, value) + ";");
    }
  }

  /**
   * Generates the set property.
   */
  public void generateSetProperty(JavaWriter out)
    throws IOException
  {
    Id id = getEntityTargetType().getId();

    String keyType = getEntityTargetType().getId().getForeignTypeName();

    out.println();
    out.println("public void " + getSetterName() + "(" + getJavaTypeName() + " v)");
    out.println("{");
    out.pushDepth();

    out.println(generateSuperSetter("v") + ";");
    out.println("if (__caucho_session != null) {");
    out.pushDepth();

    String updateVar = "__caucho_updateMask_" + (_targetLoadIndex / 64);
    long updateMask = (1L << _targetLoadIndex);

    out.println(updateVar + " |= " + updateMask + "L;");
    out.println("__caucho_session.update(this);");
    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the set clause.
   */
  public void generateSet(JavaWriter out, String pstmt, String index)
    throws IOException
  {
  }

  /**
   * Generates loading cache
   */
  public void generateUpdateFromObject(JavaWriter out, String obj)
    throws IOException
  {
  }

  /**
   * Generates code for foreign entity create/delete
   */
  public void generateInvalidateForeign(JavaWriter out)
    throws IOException
  {
    // Table table = getEntityTargetType().getTable();

    AmberPersistenceUnit persistenceUnit = getSourceType().getPersistenceUnit();

    String className = getJavaType().getName();
    EntityType entity = persistenceUnit.getEntity(className);

    // jpa/0ge4
    Table table = entity.getTable();

    out.println("if (\"" + table.getName() + "\".equals(table)) {");
    out.pushDepth();
    String loadVar = "__caucho_loadMask_" + (_targetLoadIndex / 64);
    out.println(loadVar + " = 0;");
    out.popDepth();
    out.println("}");
  }
}
