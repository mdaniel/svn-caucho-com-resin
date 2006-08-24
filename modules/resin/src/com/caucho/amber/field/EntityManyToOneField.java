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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.field;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;

import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.java.JavaWriter;

import com.caucho.amber.AmberRuntimeException;

import com.caucho.amber.type.Type;
import com.caucho.amber.type.EntityType;

import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.Table;
import com.caucho.amber.table.Column;

import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.expr.ManyToOneExpr;
import com.caucho.amber.query.QueryParser;

/**
 * Represents a many-to-one link pointing to an entity.
 */
public class EntityManyToOneField extends AbstractField {
  private static final L10N L = new L10N(EntityManyToOneField.class);
  private static final Logger log = Log.open(EntityManyToOneField.class);

  private LinkColumns _linkColumns;

  private EntityType _targetType;

  private int _targetLoadIndex;

  private DependentEntityOneToOneField _targetField;
  private PropertyField _aliasField;

  private boolean _isInsert = true;
  private boolean _isUpdate = true;

  private boolean _isSourceCascadeDelete;
  private boolean _isTargetCascadeDelete;

  public EntityManyToOneField(EntityType entityType, String name)
    throws ConfigException
  {
    super(entityType, name);
  }

  public EntityManyToOneField(EntityType entityType)
  {
    super(entityType);
  }

  /**
   * Sets the target type.
   */
  public void setType(Type targetType)
  {
    if (! (targetType instanceof EntityType))
      throw new AmberRuntimeException(L.l("many-to-one requires an entity target at '{0}'",
                                          targetType));

    _targetType = (EntityType) targetType;
  }

  /**
   * Returns the target type.
   */
  public EntityType getEntityType()
  {
    return _targetType;
  }

  /**
   * Returns the foreign type.
   */
  public String getForeignTypeName()
  {
    //return ((KeyColumn) getColumn()).getType().getForeignTypeName();
    return getEntityType().getForeignTypeName();
  }

  /**
   * Set true if deletes cascade to the target.
   */
  public void setTargetCascadeDelete(boolean isCascadeDelete)
  {
    _isTargetCascadeDelete = isCascadeDelete;
  }

  /**
   * Set true if deletes cascade to the source.
   */
  public void setSourceCascadeDelete(boolean isCascadeDelete)
  {
    _isSourceCascadeDelete = isCascadeDelete;
  }

  /**
   * Set true if deletes cascade to the target.
   */
  public boolean isTargetCascadeDelete()
  {
    return _isTargetCascadeDelete;
  }

  /**
   * Set true if deletes cascade to the source.
   */
  public boolean isSourceCascadeDelete()
  {
    return _isSourceCascadeDelete;
  }

  /**
   * Sets the join columns.
   */
  public void setLinkColumns(LinkColumns linkColumns)
  {
    _linkColumns = linkColumns;
  }

  /**
   * Gets the columns.
   */
  public LinkColumns getLinkColumns()
  {
    return _linkColumns;
  }

  /**
   * Sets the target field.
   */
  public void setTargetField(DependentEntityOneToOneField field)
  {
    _targetField = field;
  }

  /**
   * Sets any alias field.
   */
  public void setAliasField(PropertyField alias)
  {
    _aliasField = alias;
  }

  /**
   * Initializes the field.
   */
  public void init()
    throws ConfigException
  {
    super.init();

    Id id = getEntityType().getId();
    ArrayList<Column> keys = id.getColumns();

    if (_linkColumns == null) {
      ArrayList<ForeignColumn> columns = new ArrayList<ForeignColumn>();

      for (int i = 0; i < keys.size(); i++) {
        Column key = keys.get(i);

        String name;

        if (keys.size() == 1)
          name = getName();
        else
          name = getName() + "_" + key.getName();

        columns.add(getSourceType().getTable().createForeignColumn(name, key));
      }

      _linkColumns = new LinkColumns(getSourceType().getTable(),
                                     _targetType.getTable(),
                                     columns);
    }

    if (getSourceType().getId() != null) {
      // resolve any alias
      for (AmberField field : getSourceType().getId().getKeys()) {
        if (field instanceof PropertyField) {
          PropertyField prop = (PropertyField) field;

          for (ForeignColumn column : _linkColumns.getColumns()) {
            if (prop.getColumn().getName().equals(column.getName()))
              _aliasField = prop;
          }
        }
      }
    }

    _targetLoadIndex = getSourceType().nextLoadGroupIndex();

    _linkColumns.setTargetCascadeDelete(isTargetCascadeDelete());
    _linkColumns.setSourceCascadeDelete(isSourceCascadeDelete());
  }

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return new ManyToOneExpr(parent, _linkColumns);
  }

  /**
   * Gets the column corresponding to the target field.
   */
  public ForeignColumn getColumn(Column targetColumn)
  {
    return _linkColumns.getSourceColumn(targetColumn);
  }

  /**
   * Generates the insert.
   */
  public void generateInsertColumns(ArrayList<String> columns)
  {
    if (_isInsert && _aliasField == null)
      _linkColumns.generateInsert(columns);
  }

  /**
   * Generates the select clause.
   */
  public String generateLoadSelect(Table table, String id)
  {
    if (_aliasField != null)
      return null;

    if (_linkColumns.getSourceTable() != table)
      return null;
    else
      return _linkColumns.generateSelectSQL(id);
  }

  /**
   * Generates the select clause.
   */
  public String generateSelect(String id)
  {
    if (_aliasField != null)
      return null;

    return _linkColumns.generateSelectSQL(id);
  }

  /**
   * Generates the update set clause
   */
  public void generateUpdate(CharBuffer sql)
  {
    if (_aliasField != null)
      return;

    if (_isUpdate) {
      sql.append(_linkColumns.generateUpdateSQL());
    }
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    super.generatePrologue(out, completedSet);

    out.println();

    Id id = getEntityType().getId();

    out.println("private transient " + id.getForeignTypeName() + " __caucho_" + getName() + ";");

    if (_aliasField == null) {
      id.generatePrologue(out, completedSet, getName());
    }
  }

  /**
   * Generates the linking for a join
   */
  public void generateJoin(CharBuffer cb,
                           String sourceTable,
                           String targetTable)
  {
    cb.append(_linkColumns.generateJoin(sourceTable, targetTable));
  }

  /**
   * Generates loading code
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    if (_aliasField != null)
      return index;

    out.print("__caucho_" + getName() + " = ");

    index = getEntityType().getId().generateLoadForeign(out, rs,
                                                        indexVar, index,
                                                        getName());

    out.println(";");

    /*
    // ejb/0a06
    String proxy = "aConn.loadProxy(\"" + getEntityType().getName() + "\", __caucho_" + getName() + ")";

    proxy = "(" + getEntityType().getProxyClass().getName() + ") " + proxy;

    out.println(generateSuperSetter(proxy) + ";");
    */
    out.println(generateSuperSetter("null") + ";");

    int group = _targetLoadIndex / 64;
    long mask = (1L << (_targetLoadIndex % 64));

    //out.println("__caucho_loadMask_" + group + " &= ~" + mask + "L;");

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

      int group = _targetLoadIndex / 64;
      long mask = (1L << (_targetLoadIndex % 64));
      String loadVar = "__caucho_loadMask_" + group;

      out.println(loadVar + " |= " + mask + "L;");

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
    String javaType = getJavaTypeName();

    int group = _targetLoadIndex / 64;
    long mask = (1L << (_targetLoadIndex % 64));
    String loadVar = "__caucho_loadMask_" + group;

    out.println();
    out.println("public " + javaType + " " + getGetterName() + "()");
    out.println("{");
    out.pushDepth();

    out.println();
    out.print("if (__caucho_session != null && ");
    out.println("(" + loadVar + " & " + mask + "L) == 0) {");
    out.pushDepth();

    String index = "_" + (_targetLoadIndex / 64);
    index += "_" + (1L << (_targetLoadIndex % 64));

    if (_aliasField == null) {
      out.println("__caucho_load_" + getLoadGroupIndex() + "(__caucho_session);");
    }

    out.println(loadVar + " |= " + mask + "L;");

    generateLoadProperty(out, index, "__caucho_session");

    out.println("return v"+index+";");

    out.popDepth();
    out.println("}");

    out.println("else");
    out.println("  return " + generateSuperGetter() + ";");

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

    if (_targetField != null) {
      out.println("java.util.Map map_" + index + " = new java.util.HashMap();");
      out.println("map_" + index + ".put(\"" + _targetField.getName() + "\", this);");
      out.println();
    }

    out.print(javaType + " v"+index+" = (" + javaType + ") "+session+".find(" + javaType + ".class, ");

    if (_aliasField == null) {
      out.print("__caucho_" + getName());
    }
    else {
      out.print(_aliasField.generateGet("super"));
    }

    if (_targetField == null)
      out.println(");");
    else
      out.println(", map_" + index + ");");

    out.println(generateSuperSetter("v" + index) + ";");
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
      // order matters: ejb/06gc
      String var = "__caucho_" + getName();
      out.println(generateAccessor(dst, var) + " = " + generateAccessor(src, var) + ";");

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
    // jpa/0o05
    // if (getLoadGroupIndex() == updateIndex) {

    // order matters: ejb/06gc
    String var = "__caucho_" + getName();
    out.println(generateAccessor(dst, var) + " = " + generateAccessor(src, var) + ";");
    // jpa/0o05
    if (! dst.equals("super")) { // || isLazy())) {
      out.println("((" + getSourceType().getInstanceClassName() + ") " + dst + ")." +
                  generateSuperSetter(generateSuperGetter()) + ";");
    }

    // jpa/0o05
    // }

    if (_targetLoadIndex == updateIndex) { // ejb/0h20
      String value = generateGet(src);
      out.println(generateSet(dst, value) + ";");
    }

    /*
      if (_targetLoadIndex == updateIndex) {
      // ejb/0a06
      String value = generateGet(src);
      out.println(generateSet(dst, value) + ";");
      }
    */
  }

  private String generateAccessor(String src, String var)
  {
    if (src.equals("super"))
      return var;
    else
      return "((" + getSourceType().getInstanceClassName() + ") " + src + ")." + var;
  }

  /**
   * Generates the set property.
   */
  public void generateSetProperty(JavaWriter out)
    throws IOException
  {
    // ejb/06gc - updates with EJB 2.0

    Id id = getEntityType().getId();
    String var = "__caucho_" + getName();

    String keyType = getEntityType().getId().getForeignTypeName();

    out.println();
    out.println("public void " + getSetterName() + "(" + getJavaTypeName() + " v)");
    out.println("{");
    out.pushDepth();

    int group = getLoadGroupIndex() / 64;
    long loadMask = (1L << (getLoadGroupIndex() % 64));
    String loadVar = "__caucho_loadMask_" + group;

    if (_aliasField == null) {
      out.println("if ((" + loadVar + " & " + loadMask + "L) == 0 && __caucho_session != null) {");
      out.println("  __caucho_load_0(__caucho_session);");
      out.println("}");

      out.println();
      out.println("if (v == null) {");
      out.println("  if (" + var + " == null) {");
      out.println("    " + generateSuperSetter("null") + ";");
      out.println("    return;");
      out.println("  }");

      out.println();
      out.println("  " + var + " = null;");
      out.println("} else {");
      out.pushDepth();
      out.print(keyType + " key = ");

      EntityType targetType = getEntityType();

      if (targetType.isEJBProxy(getJavaTypeName())) {
        // To handle EJB local objects.
        out.print(id.generateGetProxyKey("v"));
      }
      else {
        String v = "((" + getEntityType().getInstanceClassName()+ ") v)";

        out.print(id.toObject(id.generateGetProperty(v)));
      }

      out.println(";");

      out.println();
      out.println("if (key.equals(" + var + ")) {");
      out.println("  " + generateSuperSetter("v") + ";");
      out.println("  return;");
      out.println("}");

      out.println();
      out.println(var + " = key;");

      out.popDepth();
      out.println("}");

      out.println();
      out.println(generateSuperSetter("v") + ";");
      out.println();
      out.println("if (__caucho_session != null) {");
      out.pushDepth();

      String dirtyVar = "__caucho_dirtyMask_" + (getIndex() / 64);
      long dirtyMask = (1L << (getIndex() % 64));

      out.println(dirtyVar + " |= " + dirtyMask + "L;");
      out.println(loadVar + " |= " + loadMask + "L;");
      out.println("__caucho_session.update(this);");

      out.println("__caucho_session.addCompletion(__caucho_home.createManyToOneCompletion(\"" + getName() + "\", this, v));");

      out.println();

      out.popDepth();
      out.println("}");
    }
    else {
      out.println("throw new IllegalStateException(\"aliased field cannot be set\");");
    }

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the set clause.
   */
  public void generateSet(JavaWriter out, String pstmt,
                          String index, String source)
    throws IOException
  {
    if (_aliasField != null)
      return;

    if (source == null) {
      throw new NullPointerException();
    }

    String var = "__caucho_" + getName();

    if (! source.equals("this") && ! source.equals("super"))
      var = source + "." + var;

    out.println("if (" + var + " != null) {");
    out.pushDepth();

    Id id = getEntityType().getId();
    ArrayList<IdField> keys = id.getKeys();

    if (keys.size() == 1) {
      IdField key = keys.get(0);

      key.getType().generateSet(out, pstmt, index, key.getType().generateCastFromObject(var));
    }
    else {
      for (int i = 0; i < keys.size(); i++) {
        IdField key = keys.get(i);

        key.getType().generateSet(out, pstmt, index, key.generateGetKeyProperty(var));
      }
    }

    out.popDepth();
    out.println("} else {");
    out.pushDepth();

    for (int i = 0; i < keys.size(); i++) {
      IdField key = keys.get(i);

      key.getType().generateSetNull(out, pstmt, index);
    }

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates loading cache
   */
  public void generateUpdateFromObject(JavaWriter out, String obj)
    throws IOException
  {
    String var = "__caucho_" + getName();

    out.println(var + " = " + obj + "." + var + ";");
  }

  /**
   * Generates code for foreign entity create/delete
   */
  public void generateInvalidateForeign(JavaWriter out)
    throws IOException
  {
    out.println("if (\"" + _targetType.getTable().getName() + "\".equals(table)) {");
    out.pushDepth();

    String loadVar = "__caucho_loadMask_" + (_targetLoadIndex / 64);

    out.println(loadVar + " = 0L;");
    out.popDepth();
    out.println("}");
  }

  /**
   * Generates any pre-delete code
   */
  public void generatePreDelete(JavaWriter out)
    throws IOException
  {
    if (! isTargetCascadeDelete())
      return;

    String var = "caucho_" + getName();

    out.println(getJavaTypeName() + " " + var + " = " + getGetterName() + "();");
  }

  /**
   * Generates any pre-delete code
   */
  public void generatePostDelete(JavaWriter out)
    throws IOException
  {
    if (! isTargetCascadeDelete())
      return;

    String var = "caucho_" + getName();

    out.println("if (" + var + " != null) {");
    out.println("  try {");
    // out.println("    __caucho_session.delete(" + var + ");");
    out.println("    " + var + ".remove();");
    out.println("  } catch (Exception e) {");
    out.println("    throw com.caucho.amber.AmberRuntimeException.create(e);");
    out.println("  }");
    out.println("}");
  }
}
