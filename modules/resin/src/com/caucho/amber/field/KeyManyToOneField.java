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

import com.caucho.amber.type.Type;
import com.caucho.amber.type.EntityType;

import com.caucho.amber.table.ForeignColumn;
import com.caucho.amber.table.LinkColumns;
import com.caucho.amber.table.Column;

import com.caucho.amber.query.AmberExpr;
import com.caucho.amber.query.PathExpr;
import com.caucho.amber.query.KeyManyToOneExpr;
import com.caucho.amber.query.QueryParser;

/**
 * Configuration for a bean's field
 */
public class KeyManyToOneField extends EntityManyToOneField implements IdField {
  private static final L10N L = new L10N(KeyManyToOneField.class);
  protected static final Logger log = Log.open(KeyManyToOneField.class);

  // fields
  private ArrayList<KeyPropertyField> _idFields =
    new ArrayList<KeyPropertyField>();
  
  // use field accessors to get key values.
  private boolean _isKeyField;
  
  public KeyManyToOneField(EntityType entityType, String name)
    throws ConfigException
  {
    super(entityType, name);
  }

  public KeyManyToOneField(EntityType entityType,
			   String name,
			   LinkColumns columns)
    throws ConfigException
  {
    super(entityType, name);

    setLinkColumns(columns);

    setSourceCascadeDelete(true);
  }

  /**
   * Gets the generator.
   */
  public String getGenerator()
  {
    return null;
  }

  public Type getType()
  {
    return getEntityType();
  }

  public Column getColumn()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Set true if key fields are accessed through fields.
   */
  public void setKeyField(boolean isKeyField)
  {
    _isKeyField = isKeyField;
  }

  /**
   * Returns the foreign type name.
   */
  public String getForeignTypeName()
  {
    return getJavaTypeName();
  }

  /**
   * Set true if deletes cascade to the target.
   */
  public boolean isTargetCascadeDelete()
  {
    return false;
  }

  /**
   * Set true if deletes cascade to the source.
   */
  public boolean isSourceCascadeDelete()
  {
    return true;
  }

  /**
   * Initialize the field.
   */
  public void init()
    throws ConfigException
  {
    super.init();

    ArrayList<IdField> keys = getEntityType().getId().getKeys();

    ArrayList<ForeignColumn> columns = getLinkColumns().getColumns();

    for (int i = 0; i < keys.size(); i++) {
      IdField key = keys.get(i);
      ForeignColumn column = columns.get(i);

      KeyPropertyField field;
      field = new IdentifyingKeyPropertyField(getSourceType(), column);

      _idFields.add(field);
    }
  }

  /**
   * Returns the component count.
   */
  public int getComponentCount()
  {
    return getEntityType().getId().getKeyCount();
  }

  /**
   * Returns columns
   */
  public ArrayList<Column> getColumns()
  {
    ArrayList<Column> columns = new ArrayList<Column>();

    columns.addAll(getLinkColumns().getColumns());

    return columns;
  }

  /**
   * Returns the identifying field matching the target's id.
   */
  public KeyPropertyField getIdField(IdField field)
  {
    ArrayList<IdField> keys = getEntityType().getId().getKeys();

    if (_idFields.size() != keys.size()) {
      try {
	init();
      } catch (Exception e) {
	throw new RuntimeException(e);
      }
    }

    for (int i = 0; i < keys.size(); i++) {
      if (keys.get(i) == field)
	return _idFields.get(i);
    }

    throw new IllegalStateException(field.toString());
  }

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    return new KeyManyToOneExpr(parent, this);
  }

  /**
   * Returns the where code
   */
  public String generateMatchArgWhere(String id)
  {
    return getLinkColumns().generateMatchArgSQL(id);
  }

  /**
   * Returns the where code
   */
  public String generateRawWhere(String id)
  {
    CharBuffer cb = new CharBuffer();

    String prefix = id + "." + getName();

    ArrayList<IdField> keys = getEntityType().getId().getKeys();
    
    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
	cb.append(" and ");

      cb.append(keys.get(i).generateRawWhere(prefix));
    }
    
    return cb.toString();
  }

  /**
   * Generates the property getter for an EJB proxy
   *
   * @param value the non-null value
   */
  public String generateGetProxyProperty(String value)
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates the linking for a join
   */
  public void generateJoin(CharBuffer cb, String table1, String table2)
  {
    cb.append(getLinkColumns().generateJoin(table1, table2));
  }

  /**
   * Gets the column corresponding to the target field.
   */
  public ForeignColumn getColumn(Column key)
  {
    return getLinkColumns().getSourceColumn(key);
  }

  /**
   * Returns the foreign type.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
				 String indexVar, int index)
    throws IOException
  {
    return generateLoadForeign(out, rs, indexVar, index,
			       getForeignTypeName().replace('.', '_'));
  }

  /**
   * Returns the actual data.
   */
  public String generateSuperGetter()
  {
    if (isAbstract() ||	getGetterMethod() == null)
      return getFieldName();
    else
      return getGetterMethod().getName() + "()";
  }

  /**
   * Sets the actual data.
   */
  public String generateSuperSetter(String value)
  {
    if (isAbstract() ||	getGetterMethod() == null || getSetterMethod() == null)
      return(getFieldName() + " = " + value + ";");
    else
      return getSetterMethod().getName() + "(" + value + ")";
  }

  /**
   * Returns the foreign type.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
				 String indexVar, int index,
				 String name)
    throws IOException
  {
    out.print("(" + getForeignTypeName() + ") ");
    
    out.print("aConn.loadProxy(\"" + getEntityType().getName() + "\", ");
    index = getEntityType().getId().generateLoadForeign(out, rs, indexVar, index,
						getName());
    
    out.println(");");

    return index;
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    super.generatePrologue(out, completedSet);
    
    if (isAbstract()) {
      out.println();
      
      out.println();
      out.println("public " + getJavaTypeName() + " " + getGetterName() + "()");
      out.println("{");
      out.println("  return " + getFieldName() + ";");
      out.println("}");
      
      out.println();
      out.println("public void " + getSetterName() + "(" + getJavaTypeName() + " v)");
      out.println("{");
      out.println("  " + getFieldName() + " = v;");
      out.println("}");
    }
  }

  /**
   * Generates the set clause.
   */
  public void generateSet(JavaWriter out, String pstmt,
			  String index, String value)
    throws IOException
  {
    ArrayList<ForeignColumn> columns = getLinkColumns().getColumns();
    
    Id id = getEntityType().getId();
    ArrayList<IdField> keys = id.getKeys();

    String prop = value != null ? generateGet(value) : null;
    for (int i = 0; i < columns.size(); i++ ){
      IdField key = keys.get(i);
      ForeignColumn column = columns.get(i);

      column.generateSet(out, pstmt, index, key.generateGet(prop));
    }
  }

  /**
   * Generates the set clause.
   */
  public void generateSet(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    String var = getFieldName();

    Id id = getEntityType().getId();
    ArrayList<IdField> keys = id.getKeys();

    for (int i = 0; i < keys.size(); i++) {
      IdField key = keys.get(i);
      
      key.getType().generateSet(out, pstmt, index, key.generateGet(var));
    }
  }

  /**
   * Generates the set clause.
   */
  public void generateSetInsert(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    String value = generateSuperGetter();
    
    out.println("if (" + getEntityType().generateIsNull(value) + ") {");
    out.pushDepth();

    getEntityType().generateSetNull(out, pstmt, index);

    out.popDepth();
    out.println("} else {");
    out.pushDepth();

    generateSet(out, pstmt, index);

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the setter for a key property
   */
  public String generateSetKeyProperty(String key, String value)
    throws IOException
  {
    if (_isKeyField)
      return key + "." + getName() + " = " + value;
    else
      return generateSet(key, value);
  }

  /**
   * Generates the getter for a key property
   */
  public String generateGetKeyProperty(String key)
    throws IOException
  {
    if (_isKeyField)
      return key + "." + getName();
    else
      return generateGet(key);
  }

  /**
   * Generates the set clause.
   */
  public void generateSetGeneratedKeys(JavaWriter out, String pstmt)
    throws IOException
  {
  }

  /**
   * Generates the set clause.
   */
  public void generateCheckCreateKey(JavaWriter out)
    throws IOException
  {
    out.println("if (" + generateSuperGetter() + " == null)");
    out.println("  throw new com.caucho.amber.AmberException(\"primary key must not be null on creation.  " + getGetterName() + "() must not return null.\");");
  }

  /**
   * Returns a test for null.
   */
  public String generateIsNull(String value)
  {
    return  "(" + value + " == null)";
  }

  /**
   * Converts from an object.
   */
  public String toValue(String value)
  {
    return "((" + getJavaTypeName() + ") " + value + ")";
  }
}
