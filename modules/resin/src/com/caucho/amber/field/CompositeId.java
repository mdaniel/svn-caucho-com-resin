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

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;

import java.util.logging.Logger;

import com.caucho.bytecode.JClass;

import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;

import com.caucho.log.Log;

import com.caucho.java.JavaWriter;

import com.caucho.amber.manager.AmberPersistenceUnit;

import com.caucho.amber.type.EntityType;


/**
 * Configuration for a bean's field
 */
public class CompositeId extends Id {
  private static final L10N L = new L10N(CompositeId.class);
  protected static final Logger log = Log.open(CompositeId.class);

  private JClass _keyClass;

  public CompositeId(EntityType ownerType, ArrayList<IdField> keys)
  {
    super(ownerType, keys);
  }

  /**
   * Sets the foreign key type.
   */
  public void setKeyClass(JClass keyClass)
  {
    _keyClass = keyClass;

    getOwnerType().addDependency(keyClass);
  }

  /**
   * Returns the foreign type.
   */
  public String getForeignTypeName()
  {
    if (_keyClass != null)
      return _keyClass.getName();
    else
      return getOwnerType().getName();
  }

  /**
   * Returns the foreign type.
   */
  public String getForeignMakeKeyName()
  {
    return getOwnerType().getName().replace('.', '_').replace('/', '_');    
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    super.generatePrologue(out, completedSet);
    
    generatePrologue(out, completedSet, getForeignMakeKeyName());
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out,
			       HashSet<Object> completedSet,
			       String name)
    throws IOException
  {
    generatePrologueMake(out, completedSet);
    generatePrologueLoad(out, completedSet);
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologueMake(JavaWriter out,
				   HashSet<Object> completedSet)
    throws IOException
  {
    String makeName = "__caucho_make_key_" + getForeignMakeKeyName();

    if (completedSet.contains(makeName))
      return;

    completedSet.add(makeName);
    
    out.println();
    out.print("private static ");
    out.print(getForeignTypeName() + " " + makeName);
    out.print("(");

    ArrayList<IdField> keys = getKeys();
    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
	out.print(", ");

      IdField key = keys.get(i);
      
      out.print(key.getJavaTypeName() + " a" + i);
    }
    out.println(")");
    out.println("{");
    out.pushDepth();
    out.println(getForeignTypeName() + " key = new " + getForeignTypeName() + "();");

    for (int i = 0; i < keys.size(); i++) {
      IdField key = keys.get(i);
      
      out.println(key.generateSetKeyProperty("key", "a" + i) + ";");
    }
    
    out.println("return key;");

    out.popDepth();
    out.println("}");
  }
  
  /**
   * Generates any prologue.
   */
  public void generatePrologueLoad(JavaWriter out, 
				   HashSet<Object> completedSet)
    throws IOException
  {
    String loadName = "__caucho_load_key_" + getForeignMakeKeyName();

    if (completedSet.contains(loadName))
      return;

    completedSet.add(loadName);

    out.println();
    out.print("private static ");
    out.print(getForeignTypeName() + " " + loadName);
    out.println("(com.caucho.amber.connection.AmberConnection aConn, java.sql.ResultSet rs, int index)");
    out.println("  throws java.sql.SQLException");
    
    out.println("{");
    out.pushDepth();

    int index = 0;
    ArrayList<IdField> keys = getKeys();
    for (int i = 0; i < keys.size(); i++) {
      IdField key = keys.get(i);

      String javaType = key.getJavaTypeName();
      out.print(javaType + " a" + i + " = (" + javaType + ") ");
      index = key.getType().generateLoad(out, "rs", "index", index);
      out.println(";");

      out.println("if (rs.wasNull())");
      out.println("  return null;");
    }
    
    out.println(getForeignTypeName() + " key = new " + getForeignTypeName() + "();");

    for (int i = 0; i < keys.size(); i++) {
      out.println(keys.get(i).generateSetKeyProperty("key", "a" + i) + ";");
    }
    
    out.println("return key;");

    out.popDepth();
    out.println("}");
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
    out.print("__caucho_load_key_" + getForeignMakeKeyName());
    out.print("(aConn, " + rs + ", " + indexVar + " + " + index + ")");

    ArrayList<IdField> keys = getKeys();

    index += keys.size();
    
    return index;
  }

  /**
   * Generates the select clause.
   */
  public String generateSelect(String id)
  {
    ArrayList<IdField> keys = getKeys();

    CharBuffer cb = CharBuffer.allocate();
    
    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
	cb.append(", ");

      cb.append(keys.get(i).generateSelect(id));
    }

    return cb.close();
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
    CharBuffer cb = CharBuffer.allocate();
    
    cb.append("__caucho_make_key_" + getForeignMakeKeyName());
    cb.append("(");

    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
	cb.append(", ");

      cb.append(keys.get(i).generateGet(value));
    }

    cb.append(")");

    return cb.close();
  }

  /**
   * Returns the key for the value
   */
  public String generateGetProxyProperty(String value)
  {
    CharBuffer cb = CharBuffer.allocate();
    
    cb.append("__caucho_make_key_" + getForeignMakeKeyName());
    cb.append("(");

    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
	cb.append(", ");

      cb.append(keys.get(i).generateGetProxyProperty(value));
    }

    cb.append(")");

    return cb.close();
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
    ArrayList<IdField> keys = getKeys();

    out.println("if (" + obj + " != null) {");
    out.pushDepth();
    
    out.println(getForeignTypeName() + " " + obj + "_key = (" + getForeignTypeName() + ") " + obj + ";");

    for (int i = 0; i < keys.size(); i++) {
      IdField key = keys.get(i);
      
      key.generateSet(out, key.generateGetKeyProperty(obj + "_key"));
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
    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      keys.get(i).generateUpdateFromObject(out, obj);
    }
  }

  /**
   * Generates the where clause.
   */
  public String generateWhere(String id)
  {
    ArrayList<IdField> keys = getKeys();

    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < keys.size(); i++) {
      if (i != 0)
	cb.append(" AND ");

      cb.append(keys.get(i).generateWhere(id));
    }

    return cb.close();
  }

  /**
   * Generates the where clause.
   */
  public String generateCreateTableSQL(AmberPersistenceUnit manager)
  {
    return null;
  }

  /**
   * Generates the set clause.
   */
  public void generateSetKey(JavaWriter out, String pstmt,
			     String obj, String index)
    throws IOException
  {
    generateSet(out, pstmt, obj, index);
  }

  /**
   * Generates the set clause.
   */
  public void generateSet(JavaWriter out, String pstmt,
			  String obj, String index)
    throws IOException
  {
    ArrayList<IdField> keys = getKeys();

    for (int i = 0; i < keys.size(); i++) {
      keys.get(i).generateSet(out, pstmt, obj, index);
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
    out.println("return " + generateEquals("super", key) + ";");
  }

  /**
   * Generates code to test the equals.
   */
  public String generateEquals(String leftBase, String value)
  {
    return leftBase + ".equals(" + value + ")";
  }
    /**
     * Generates the set clause.
     */
   public void generateCheckCreateKey(JavaWriter out)
     throws IOException
    {
    }
  
    /**
     * Generates the set clause.
     */
  /*
   public void generateSetGeneratedKeys(JavaWriter out, String pstmt)
      throws IOException
    {
    }
  */

  /**
   * Generates code to convert to the object.
   */
  public String toObject(String value)
  {
    return value;
  }
}
