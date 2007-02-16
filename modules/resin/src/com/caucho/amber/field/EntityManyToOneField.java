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

import com.caucho.amber.AmberRuntimeException;
import com.caucho.amber.cfg.*;
import com.caucho.amber.expr.AmberExpr;
import com.caucho.amber.expr.ManyToOneExpr;
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.Column;
import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.*;
import com.caucho.bytecode.JAnnotation;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import javax.persistence.CascadeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Represents a many-to-one link pointing to an entity.
 */
public class EntityManyToOneField extends CascadableField {
  private static final L10N L = new L10N(EntityManyToOneField.class);
  private static final Logger log = Log.open(EntityManyToOneField.class);

  private LinkColumns _linkColumns;

  private RelatedType _targetType;

  private int _targetLoadIndex;

  private DependentEntityOneToOneField _targetField;
  private PropertyField _aliasField;

  private boolean _isInsert = true;
  private boolean _isUpdate = true;

  private boolean _isSourceCascadeDelete;
  private boolean _isTargetCascadeDelete;

  private Object _joinColumnsAnn[];
  private HashMap<String, JoinColumnConfig> _joinColumnMap = null;

  public EntityManyToOneField(RelatedType relatedType,
                              String name,
                              CascadeType[] cascadeType)
    throws ConfigException
  {
    super(relatedType, name, cascadeType);
  }

  public EntityManyToOneField(RelatedType relatedType,
                              String name)
    throws ConfigException
  {
    this(relatedType, name, null);
  }

  public EntityManyToOneField(RelatedType relatedType)
  {
    super(relatedType);
  }

  /**
   * Sets the target type.
   */
  public void setType(Type targetType)
  {
    if (! (targetType instanceof RelatedType))
      throw new AmberRuntimeException(L.l("many-to-one requires an entity target at '{0}'",
                                          targetType));

    _targetType = (RelatedType) targetType;
  }

  /**
   * Returns the source type as
   * entity or mapped-superclass.
   */
  public RelatedType getRelatedType()
  {
    return (RelatedType) getSourceType();
  }

  /**
   * Returns the target type as
   * entity or mapped-superclass.
   */
  public RelatedType getEntityTargetType()
  {
    return _targetType;
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
   * Sets the join column annotations.
   */
  public void setJoinColumns(Object joinColumnsAnn[])
  {
    _joinColumnsAnn = joinColumnsAnn;
  }

  /**
   * Gets the join column annotations.
   */
  public Object[] getJoinColumns()
  {
    return _joinColumnsAnn;
  }

  /**
   * Sets the join column map.
   */
  public void setJoinColumnMap(HashMap<String, JoinColumnConfig> joinColumnMap)
  {
    _joinColumnMap = joinColumnMap;
  }

  /**
   * Gets the join column map.
   */
  public HashMap<String, JoinColumnConfig> getJoinColumnMap()
  {
    return _joinColumnMap;
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
    init(getRelatedType());
  }

  /**
   * Initializes the field.
   */
  public void init(RelatedType relatedType)
    throws ConfigException
  {
    Table sourceTable = relatedType.getTable();

    if (sourceTable == null || ! relatedType.getPersistenceUnit().isJPA()) {
      // jpa/0ge3, ejb/0602
      super.init();
      _targetLoadIndex = relatedType.getLoadGroupIndex();
      return;
    }

    int n = 0;

    if (_joinColumnMap != null)
      n = _joinColumnMap.size();

    ArrayList<ForeignColumn> foreignColumns = new ArrayList<ForeignColumn>();

    RelatedType parentType = _targetType;

    ArrayList<Column> targetIdColumns = _targetType.getId().getColumns();

    while (targetIdColumns.size() == 0) {

      parentType = parentType.getParentType();

      if (parentType == null)
        break;

      targetIdColumns = parentType.getId().getColumns();
    }

    for (Column keyColumn : targetIdColumns) {

      String columnName;

      if (getRelatedType().getPersistenceUnit().isJPA())
        columnName = getName().toUpperCase() + '_' + keyColumn.getName();
      else {
        // ejb/0602
        columnName = keyColumn.getName();
      }

      boolean nullable = true;
      boolean unique = false;

      if (n > 0) {
        JoinColumnConfig joinColumn;

        if (n == 1) {
          joinColumn = (JoinColumnConfig) _joinColumnMap.values().toArray()[0];
        } else
          joinColumn = _joinColumnMap.get(keyColumn.getName());

        if (joinColumn != null) {
          columnName = joinColumn.getName();

          nullable = joinColumn.getNullable();
          unique = joinColumn.getUnique();
        }
      }
      else {
        JAnnotation joinAnn
          = BaseConfigIntrospector.getJoinColumn(_joinColumnsAnn,
                                                 keyColumn.getName());

        if (joinAnn != null) {
          columnName = joinAnn.getString("name");

          nullable = joinAnn.getBoolean("nullable");
          unique = joinAnn.getBoolean("unique");
        }
      }

      ForeignColumn foreignColumn;

      foreignColumn = sourceTable.createForeignColumn(columnName, keyColumn);

      foreignColumn.setNotNull(! nullable);
      foreignColumn.setUnique(unique);

      foreignColumns.add(foreignColumn);
    }

    LinkColumns linkColumns = new LinkColumns(sourceTable,
                                              _targetType.getTable(),
                                              foreignColumns);

    setLinkColumns(linkColumns);

    super.init();

    Id id = getEntityTargetType().getId();
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

        columns.add(sourceTable.createForeignColumn(name, key));
      }

      _linkColumns = new LinkColumns(relatedType.getTable(),
                                     _targetType.getTable(),
                                     columns);
    }

    if (relatedType.getId() != null) {
      // resolve any alias
      for (AmberField field : relatedType.getId().getKeys()) {
        if (field instanceof PropertyField) {
          PropertyField prop = (PropertyField) field;

          for (ForeignColumn column : _linkColumns.getColumns()) {
            if (prop.getColumn().getName().equals(column.getName()))
              _aliasField = prop;
          }
        }
      }
    }

    _targetLoadIndex = relatedType.getLoadGroupIndex(); // nextLoadGroupIndex();

    _linkColumns.setTargetCascadeDelete(isTargetCascadeDelete());
    _linkColumns.setSourceCascadeDelete(isSourceCascadeDelete());
  }

  /**
   * Generates the post constructor initialization.
   */
  public void generatePostConstructor(JavaWriter out)
    throws IOException
  {
    out.println(getSetterName() + "(" + generateSuperGetter() + ");");
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

    if (_linkColumns == null) {
      // jpa/0ge3
      return null;
    }

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

    Id id = getEntityTargetType().getId();

    out.println("protected transient " + id.getForeignTypeName() + " __caucho_field_" + getName() + ";");

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

    out.print("__caucho_field_" + getName() + " = ");

    index = getEntityTargetType().getId().generateLoadForeign(out, rs,
                                                              indexVar, index,
                                                              getName());

    out.println(";");

    /*
    // ejb/0a06
    String proxy = "aConn.loadProxy(\"" + getEntityTargetType().getName() + "\", __caucho_field_" + getName() + ")";

    proxy = "(" + getEntityTargetType().getProxyClass().getName() + ") " + proxy;

    out.println(generateSuperSetter(proxy) + ";");
    */

    // commented out jpa/0l40
    // out.println(generateSuperSetter("null") + ";");

    int group = _targetLoadIndex / 64;
    long mask = (1L << (_targetLoadIndex % 64));

    //out.println("__caucho_loadMask_" + group + " &= ~" + mask + "L;");

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

      // commented out jpa/0l40
      // out.println(loadVar + " |= " + mask + "L;");

      String javaType = getJavaTypeName();

      // jpa/0o05
      String indexS = "_" + group + "_" + index;

      generateLoadProperty(out, indexS, "aConn");
    }

    return ++index;
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
    out.println("public " + javaType + " __caucho_item_" + getGetterName() + "(com.caucho.amber.manager.AmberConnection aConn)");
    out.println("{");
    out.pushDepth();

    out.println("if (aConn != null) {");
    out.pushDepth();

    String index = "_" + (_targetLoadIndex / 64);
    index += "_" + (1L << (_targetLoadIndex % 64));

    if (_aliasField == null) {
      out.println("__caucho_load_" + getLoadGroupIndex() + "(aConn);");
    }

    out.println(loadVar + " |= " + mask + "L;");

    generateLoadProperty(out, index, "aConn");

    out.println("return v"+index+";");

    out.popDepth();
    out.println("}");

    out.println("return null;");

    out.popDepth();
    out.println("}");

    out.println();
    out.println("public " + javaType + " " + getGetterName() + "()");
    out.println("{");
    out.pushDepth();

    // jpa/0h07: detached entity fields must not be loaded.
    out.println("if (__caucho_session != null && __caucho_item != null) {");
    out.pushDepth();

    // ejb/06h0
    String extClassName = getRelatedType().getInstanceClassName(); // getRelatedType().getName() + "__ResinExt";
    out.println(extClassName + " item = (" + extClassName + ") __caucho_item.getEntity();");

    out.println("item.__caucho_item_" + getGetterName() + "(__caucho_session);");

    generateCopyLoadObject(out, "super", "item", getLoadGroupIndex());

    // out.println("__caucho_loadMask_" + group + " |= " + mask + "L;");
    // out.println("__caucho_loadMask_" + group + " |= item.__caucho_loadMask_" + group + ";"); // mask + "L;");

    out.println("__caucho_loadMask_" + group + " |= item.__caucho_loadMask_" + group + " & " + mask + "L;");

    out.popDepth();
    out.println("}");

    out.println("else");
    out.pushDepth();
    out.print("if (__caucho_session != null && ");
    out.println("(" + loadVar + " & " + mask + "L) == 0) {");
    out.pushDepth();

    out.println("return __caucho_item_" + getGetterName() + "(__caucho_session);");

    out.popDepth();
    out.println("}");
    out.popDepth();

    out.println();
    out.println("return " + generateSuperGetter() + ";");

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

    // jpa/0o04
    out.println(session + ".addEntity(this);");

    // ejb/06h0
    if (isAbstract()) {
      String proxy = "aConn.loadProxy(\"" + getEntityTargetType().getName() + "\", __caucho_field_" + getName() + ")";

      proxy = getEntityTargetType().getProxyClass().getName() + " v" + index + " = (" + getEntityTargetType().getProxyClass().getName() + ") " + proxy + ";";

      out.println(proxy);
      out.println(generateSuperSetter("v" + index) + ";");
    }
    else {
      String targetType = _targetType.getBeanClass().getName();
      out.print(targetType + " v"+index+" = (" + targetType + ") "+session+".find(" + targetType + ".class, ");

      if (_aliasField == null) {
        out.print("__caucho_field_" + getName());
      }
      else {
        out.print(_aliasField.generateGet("super"));
      }

      out.println(");");

      out.println(generateSuperSetter("v" + index) + ";");
    }
  }

  /**
   * Updates the cached copy.
   */
  public void generateCopyUpdateObject(JavaWriter out,
                                       String dst, String src,
                                       int updateIndex)
    throws IOException
  {
    // jpa/0s29
    // if (getIndex() == updateIndex) {

    // order matters: ejb/06gc
    String var = "__caucho_field_" + getName();
    out.println(generateAccessor(dst, var) + " = " + generateAccessor(src, var) + ";");

    String value = generateGet(src);
    out.println(generateSet(dst, value) + ";");

    // }
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
    String var = "__caucho_field_" + getName();
    out.println(generateAccessor(dst, var) + " = " + generateAccessor(src, var) + ";");
    // jpa/0o05
    if (! dst.equals("super")) { // || isLazy())) {
      out.println("((" + getRelatedType().getInstanceClassName() + ") " + dst + ")." +
                  generateSuperSetter(generateSuperGetter()) + ";");
    }

    // jpa/0o05
    // }

    // commented out: jpa/0s29
    // if (_targetLoadIndex == updateIndex) { // ejb/0h20

    String value = generateGet(src);
    out.println(generateSet(dst, value) + ";");

    // }

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
      return "((" + getRelatedType().getInstanceClassName() + ") " + src + ")." + var;
  }

  /**
   * Generates the set property.
   */
  public void generateSetProperty(JavaWriter out)
    throws IOException
  {
    // ejb/06gc - updates with EJB 2.0

    Id id = getEntityTargetType().getId();
    String var = "__caucho_field_" + getName();

    String keyType = getEntityTargetType().getId().getForeignTypeName();

    out.println();
    out.println("public void " + getSetterName() + "(" + getJavaTypeName() + " v)");
    out.println("{");
    out.pushDepth();

    int group = getLoadGroupIndex() / 64;
    long loadMask = (1L << (getLoadGroupIndex() % 64));
    String loadVar = "__caucho_loadMask_" + group;

    if (_aliasField == null) {
      out.println("if ((" + loadVar + " & " + loadMask + "L) == 0 && __caucho_session != null) {");
      // ejb/0602
      out.println("  __caucho_load_" + group + "(__caucho_session);");
      out.println();
      // jpa/0j5f
      out.println("  if (__caucho_session.isInTransaction())");
      out.println("    __caucho_session.makeTransactional((com.caucho.amber.entity.Entity) this);");
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

      RelatedType targetType = getEntityTargetType();

      if (targetType.isEJBProxy(getJavaTypeName())) {
        // To handle EJB local objects.
        out.print(id.generateGetProxyKey("v"));
      }
      else {
        String v = "((" + getEntityTargetType().getInstanceClassName()+ ") v)";

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

      out.println("__caucho_session.update((com.caucho.amber.entity.Entity) this);");

      out.println("__caucho_session.addCompletion(__caucho_home.createManyToOneCompletion(\"" + getName() + "\", (com.caucho.amber.entity.Entity) this, v));");

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
   * Generates the flush check for this child.
   */
  public boolean generateFlushCheck(JavaWriter out)
    throws IOException
  {
    // ejb/06bi
    if (! getRelatedType().getPersistenceUnit().isJPA())
      return false;

    String getter = generateSuperGetter();

    out.println("if (" + getter + " != null) {");
    out.pushDepth();

    String relatedEntity = "((com.caucho.amber.entity.Entity) " + getter + ")";
    out.println("com.caucho.amber.entity.EntityState state = " + relatedEntity + ".__caucho_getEntityState();");

    // jpa/0j5e as a negative test.
    out.println("if (" + relatedEntity + ".__caucho_getConnection() == null) {");
    out.pushDepth();

    // jpa/0j5c as a positive test.
    out.println("if (! __caucho_state.isManaged())");

    String errorString = ("(\"amber flush: unable to flush " +
                          getRelatedType().getName() + "[\" + __caucho_getPrimaryKey() + \"] "+
                          "with non-managed dependent relationship many-to-one to "+
                          getEntityTargetType().getName() + "\")");

    out.println("  throw new IllegalStateException" + errorString + ";");

    out.popDepth();
    out.println("}");

    out.popDepth();
    out.println("}");

    return true;
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

    String var = "__caucho_field_" + getName();

    if (! source.equals("this") && ! source.equals("super"))
      var = source + "." + var;

    if (! (isAbstract() && getRelatedType().getPersistenceUnit().isJPA())) {
      // jpa/1004, ejb/06bi
      out.println("if (" + var + " != null) {");
    }
    else {
      // jpa/0j58: avoids breaking FK constraints.

      // The "one" end in the many-to-one relationship.
      String amberVar = getFieldName();

      out.println("com.caucho.amber.entity.EntityState " + amberVar + "_state = (" + var + " == null) ? ");
      out.println("com.caucho.amber.entity.EntityState.TRANSIENT : ");
      out.println("((com.caucho.amber.entity.Entity) " + amberVar + ").");
      out.println("__caucho_getEntityState();");

      out.println("if (" + amberVar + "_state.isTransactional()) {");
    }

    out.pushDepth();

    Id id = getEntityTargetType().getId();
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
    String var = "__caucho_field_" + getName();

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

    String var = "caucho_field_" + getName();

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

    String var = "caucho_field_" + getName();

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
