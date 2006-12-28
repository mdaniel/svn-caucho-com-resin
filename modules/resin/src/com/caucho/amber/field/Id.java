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

import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.table.Column;
import com.caucho.amber.type.*;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Configuration for a bean's field
 */
public class Id {
  private static final L10N L = new L10N(Id.class);
  protected static final Logger log = Log.open(Id.class);

  private RelatedType _ownerType;

  private ArrayList<IdField> _keys = new ArrayList<IdField>();
  private ArrayList<Column> _columns = new ArrayList<Column>();

  private EmbeddedIdField _embeddedIdField;

  public Id(RelatedType ownerType, ArrayList<IdField> keys)
  {
    _ownerType = ownerType;

    for (IdField key : keys) {
      addKey(key);
    }
  }

  public Id(RelatedType ownerType, IdField key)
  {
    _ownerType = ownerType;

    if (key instanceof EmbeddedIdField) {
      _embeddedIdField = (EmbeddedIdField) key;
    }
    else {
      // ejb/0623
      addKey(key);
    }
  }

  /**
   * Adds a new field to the id.
   */
  private void addKey(IdField key)
  {
    _keys.add(key);
    // ejb/0a04
    // Collections.sort(_keys, new AmberFieldCompare());

    // jpa/0ge2
    if (_ownerType instanceof MappedSuperclassType)
      return;

    _columns.addAll(key.getColumns());
    // Collections.sort(_columns, new ColumnCompare());

    for (Column column : key.getColumns()) {
      _ownerType.getTable().addIdColumn(column);
    }
  }

  private static ArrayList<IdField> createField(IdField field)
  {
    ArrayList<IdField> fields = new ArrayList<IdField>();
    fields.add(field);
    return fields;
  }

  /**
   * Returns the owner type.
   */
  public RelatedType getOwnerType()
  {
    return _ownerType;
  }

  /**
   * Returns all the column.
   */
  public ArrayList<Column> getColumns()
  {
    return _columns;
  }

  /**
   * Returns all the keys.
   */
  public ArrayList<IdField> getKeys()
  {
    return _keys;
  }

  /**
   * Returns all the keys.
   */
  public int getKeyCount()
  {
    return _columns.size();
  }

  /**
   * Returns the keys.
   */
  public IdField getKey()
  {
    return _keys.get(0);
  }

  /**
   * Returns the foreign type.
   */
  public String getForeignTypeName()
  {
    return _keys.get(0).getForeignTypeName();
  }

  /**
   * Initialize the id.
   */
  public void init()
    throws ConfigException
  {
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out,
                               HashSet<Object> completedSet,
                               String name)
    throws IOException
  {
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    for (int i = 0; i < _keys.size(); i++)
      _keys.get(i).generatePrologue(out, completedSet);
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
   * Returns the foreign type.
   */
  public int generateLoadForeign(JavaWriter out, String rs,
                                 String indexVar, int index,
                                 String name)
    throws IOException
  {
    if (_keys.size() > 1)
      throw new UnsupportedOperationException();

    return _keys.get(0).generateLoadForeign(out, rs, indexVar, index, name);
  }

  /**
   * Generates the select clause.
   */
  public String generateSelect(String id)
  {
    return getKey().generateSelect(id);
  }

  /**
   * Generates the JPA QL select clause.
   */
  public String generateJavaSelect(String id)
  {
    return getKey().generateJavaSelect(id);
  }

  /**
   * Generates the select clause.
   */
  public String generateLoadSelect(String id)
  {
    return null;
  }

  /**
   * Returns the key for the value
   */
  public String generateGetProperty(String value)
  {
    return getKey().generateGet(value);
  }

  /**
   * Returns the key for the value
   */
  public String generateGetProxyKey(String value)
  {
    return "((" + getForeignTypeName() + ") " + value + ".getPrimaryKey())";
  }

  /**
   * Generates loading cache
   */
  public void generateLoadFromObject(JavaWriter out, String obj)
    throws IOException
  {
    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      keys.get(i).generateLoadFromObject(out, obj);
    }
  }

  /**
   * Generates loading cache
   */
  public void generateSet(JavaWriter out, String obj)
    throws IOException
  {
    IdField key = getKey();

    key.generateSet(out, key.toValue(obj));
    // key.generateSet(out, key.getColumn().getType().generateCastFromObject(obj));
  }

  /**
   * Generates loading cache
   */
  public void generateUpdateFromObject(JavaWriter out, String obj)
    throws IOException
  {
    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      keys.get(i).generateUpdateFromObject(out, obj);
    }
  }

  /**
   * Generates the where clause.
   */
  public String generateMatchArgWhere(String id)
  {
    CharBuffer cb = new CharBuffer();

    boolean isFirst = true;

    if (_embeddedIdField == null) {
      for (IdField field : getKeys()) {
        for (Column column : field.getColumns()) {
          if (! isFirst)
            cb.append(" and ");
          isFirst = false;

          cb.append(column.generateMatchArgWhere(id));
        }
      }
    } else {
      HashMap<String, Column> columns
        = _embeddedIdField.getEmbeddedColumns();

      for (Map.Entry<String, Column> entry : columns.entrySet()) {
        Column column = entry.getValue();
        if (isFirst)
          isFirst = false;
        else
          cb.append(" and ");
        cb.append(column.generateMatchArgWhere(id));
      }
    }

    return cb.toString();
  }

  /**
   * Generates the where clause.
   */
  public String generateRawWhere(String id)
  {
    ArrayList<IdField> keys = getKeys();

    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
        cb.append(" and ");

      cb.append(keys.get(i).generateRawWhere(id));
    }

    return cb.close();
  }

  /**
   * Generates the where clause.
   */
  public String generateCreateTableSQL(AmberPersistenceUnit manager)
  {
    String sql = getKey().generateCreateTableSQL(manager);

    if (getKey().getGenerator() != null)
      return sql + " PRIMARY KEY auto_increment";
    else
      return sql + " PRIMARY KEY";
  }

  /**
   * Generates the set clause.
   */
  public void generateSetKey(JavaWriter out, String pstmt,
                             String index, String keyObject)
    throws IOException
  {
    IdField key = getKey();

    key.getType().generateSet(out, pstmt, index, keyObject);
  }

  /**
   * Generates the set clause.
   */
  public void generateSet(JavaWriter out, String pstmt,
                          String index, String value)
    throws IOException
  {
    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      keys.get(i).generateSet(out, pstmt, index, value);
    }
  }

  /**
   * Generates the set clause.
   */
  public void generateSet(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      keys.get(i).generateSet(out, pstmt, index);
    }
  }

  /**
   * Generates the set clause.
   */
  /*
    public String generateInsert()
    {
    String value = null;

    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
    String next = keys.get(i).generateInsert();

    if (value == null)
    value = next;
    else if (next == null) {
    }
    else
    value += ", " + next;
    }

    return value;
    }
  */

  /**
   * Generates the set clause.
   */
  public void generateSetInsert(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      keys.get(i).generateSetInsert(out, pstmt, index);
    }
  }

  /**
   * Generates code to convert to the type from the object.
   */
  public String generateCastFromObject(String value)
  {
    return value;
  }

  /**
   * Generates code for a match.
   */
  public void generateMatch(JavaWriter out, String key)
    throws IOException
  {
    IdField id = getKeys().get(0);

    out.println("return (" + id.generateEquals(id.generateSuperGetter(),
                                               id.toValue(key)) + ");");
  }

  /**
   * Generates code to test the equals.
   */
  public String generateEquals(String leftBase, String value)
  {
    ArrayList<IdField> keys = getKeys();

    String eq = "(";

    for (int i = 0; i < keys.size(); i++) {
      IdField key = keys.get(i);

      if (i != 0)
        eq += " && ";

      eq += key.generateEquals(leftBase, value);
    }

    return eq + ")";
  }

  /**
   * Generates the set clause.
   */
  public void generateCheckCreateKey(JavaWriter out)
    throws IOException
  {
    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      keys.get(i).generateCheckCreateKey(out);
    }
  }

  /**
   * Generates the set clause.
   */
  public void generateSetGeneratedKeys(JavaWriter out, String pstmt)
    throws IOException
  {
    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      keys.get(i).generateSetGeneratedKeys(out, pstmt);
    }
  }

  /**
   * Returns the embedded id field
   */
  public EmbeddedIdField getEmbeddedIdField()
  {
    return _embeddedIdField;
  }

  /**
   * Returns true if this is an @EmbeddedId
   */
  public boolean isEmbeddedId()
  {
    return _embeddedIdField != null;
  }

  /**
   * Generates code to convert to the object.
   */
  public String toObject(String value)
  {
    return getKey().toObject(value);
  }

  /**
   * Generates code to convert to the object.
   */
  public Object toObjectKey(long value)
  {
    // return getColumn().toObjectKey(value);
    return new Long(value);
  }

  /**
   * Generates code to convert to the object.
   */
  public Object getObject(ResultSet rs, int index)
    throws SQLException
  {
    return null;
  }
}
