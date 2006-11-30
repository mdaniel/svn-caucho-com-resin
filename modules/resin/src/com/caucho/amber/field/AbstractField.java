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
import com.caucho.amber.expr.PathExpr;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.table.Table;
import com.caucho.amber.type.AbstractStatefulType;
import com.caucho.amber.type.RelatedType;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassWrapper;
import com.caucho.bytecode.JField;
import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JType;
import com.caucho.config.ConfigException;
import com.caucho.java.JavaWriter;
import com.caucho.log.Log;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Configuration for a bean's property
 */
abstract public class AbstractField implements AmberField {
  private static final L10N L = new L10N(AbstractField.class);
  protected static final Logger log = Log.open(AbstractField.class);

  private AbstractStatefulType _sourceType;

  private String _name;

  private JType _javaType;

  private JMethod _getterMethod;
  private JMethod _setterMethod;

  private boolean _isLazy = true;

  private int _updateIndex;
  private int _loadGroupIndex = -1;

  AbstractField(AbstractStatefulType sourceType)
  {
    _sourceType = sourceType;
  }

  AbstractField(AbstractStatefulType sourceType, String name)
    throws ConfigException
  {
    this(sourceType);

    setName(name);
  }

  /**
   * Sets the name.
   */
  public void setName(String name)
    throws ConfigException
  {
    _name = name;

    if (! getSourceType().isFieldAccess()) {
      char ch = name.charAt(0);
      if (Character.isLowerCase(ch))
        name = Character.toUpperCase(ch) + name.substring(1);

      String getter = "get" + name;
      String setter = "set" + name;

      _getterMethod = AbstractStatefulType.getGetter(getBeanClass(), getter);

      if (_getterMethod == null) {
        getter = "is" + name;
        _getterMethod = AbstractStatefulType.getGetter(getBeanClass(), getter);
      }

      if (_getterMethod == null)
        throw new ConfigException(L.l("{0}: {1} has no matching getter.",
                                      getBeanClass().getName(), name));

      _javaType = _getterMethod.getGenericReturnType();

      _setterMethod = AbstractStatefulType.getSetter(getBeanClass(), setter);
    }
    else {
      JField field = AbstractStatefulType.getField(getBeanClass(), name);

      if (field == null)
        throw new ConfigException(L.l("{0}: {1} has no matching field.",
                                      getBeanClass().getName(), name));

      _javaType = field.getGenericType();
    }

    /*
      if (_setterMethod == null && ! isAbstract())
      throw new ConfigException(L.l("{0}: {1} has no matching setter.",
      getBeanClass().getName(), name));
    */
  }

  /**
   * Returns the field name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the java type.
   */
  protected void setJavaType(JType type)
  {
    _javaType = type;
  }

  /**
   * Sets the java type.
   */
  protected void setJavaType(Class type)
  {
    setJavaType(new JClassWrapper(type, getPersistenceUnit().getJClassLoader()));
  }

  /**
   * Returns the owning entity class.
   */
  public AbstractStatefulType getSourceType()
  {
    return _sourceType;
  }

  /**
   * Returns the amber manager.
   */
  public AmberPersistenceUnit getPersistenceUnit()
  {
    return getSourceType().getPersistenceUnit();
  }

  /**
   * Returns the bean class.
   */
  public JClass getBeanClass()
  {
    return getSourceType().getBeanClass();
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
   * Returns the table containing the field's columns.
   */
  public Table getTable()
  {
    return getEntitySourceType().getTable();
  }

  /**
   * Returns the property index.
   */
  public int getIndex()
  {
    return _updateIndex;
  }

  /**
   * Set the property index.
   */
  public void setIndex(int index)
  {
    _updateIndex = index;
  }

  /**
   * Returns the property's group index.
   */
  public int getLoadGroupIndex()
  {
    return _loadGroupIndex;
  }

  /**
   * Returns the load group mask.
   */
  public long getCreateLoadMask(int group)
  {
    int index = getLoadGroupIndex();

    if (64 * group <= index && index < 64 * (group + 1))
      return 1L << (index % 64);
    else
      return 0;
  }

  /**
   * Returns true for a lazy field.
   */
  public boolean isLazy()
  {
    return _isLazy;
  }

  /**
   * Set true for a lazy field.
   */
  public void setLazy(boolean isLazy)
  {
    _isLazy = isLazy;
  }

  /**
   * Returns the getter method.
   */
  public JMethod getGetterMethod()
  {
    return _getterMethod;
  }

  /**
   * Returns the getter name.
   */
  public String getGetterName()
  {
    if (getSourceType().isFieldAccess())
      return "__caucho_get_" + getName();
    else
      return _getterMethod.getName();
  }

  /**
   * Returns the getter name.
   */
  public String getJavaTypeName()
  {
    return getJavaType().getPrintName();
  }

  /**
   * Returns the Java code for the type.
   */
  private String getJavaTypeName(Class cl)
  {
    if (cl.isArray())
      return getJavaTypeName(cl.getComponentType()) + "[]";
    else
      return cl.getName();
  }

  /**
   * Returns the field's type
   */
  public JType getJavaType()
  {
    return _javaType;
  }

  /**
   * Returns the setter method.
   */
  public JMethod getSetterMethod()
  {
    return _setterMethod;
  }

  /**
   * Returns the setter name.
   */
  public String getSetterName()
  {
    if (getSourceType().isFieldAccess())
      return "__caucho_set_" + getName();
    else if (_setterMethod != null)
      return _setterMethod.getName();
    else
      return "set" + getGetterName().substring(3);
  }

  /**
   * Returns true if values are accessed by the fields.
   */
  public boolean isFieldAccess()
  {
    return getSourceType().isFieldAccess();
  }

  /**
   * Returns true if the methods are abstract.
   */
  public boolean isAbstract()
  {
    if (getSourceType().isFieldAccess())
      return true;
    else if (_getterMethod == null)
      return false;
    else
      return _getterMethod.isAbstract();
  }

  /**
   * Returns true if the field is cascadable.
   */
  public boolean isCascadable()
  {
    return false;
  }

  /**
   * Returns true if the methods are abstract.
   */
  public boolean isUpdateable()
  {
    return true;
  }

  /**
   * Initialize the field.
   */
  public void init()
    throws ConfigException
  {
    if (_loadGroupIndex < 0) {
      if (_isLazy)
        _loadGroupIndex = getEntitySourceType().nextLoadGroupIndex();
      else
        _loadGroupIndex = getEntitySourceType().getDefaultLoadGroupIndex();
    }
  }

  /**
   * Generates the post constructor initialization.
   */
  public void generatePostConstructor(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates any prologue.
   */
  public void generatePrologue(JavaWriter out, HashSet<Object> completedSet)
    throws IOException
  {
    if (isAbstract()) {
      out.println();
      out.print("public ");
      out.print(getJavaType().getPrintName());
      out.print(" " + getFieldName() + ";");
    }
  }

  /**
   * Generates the select clause for an entity load.
   */
  public String generateLoadSelect(Table table, String id)
  {
    return null;
  }

  /**
   * Generates the select clause.
   */
  public String generateSelect(String id)
  {
    return null;
  }

  /**
   * Generates the JPA QL select clause.
   */
  public String generateJavaSelect(String id)
  {
    return null;
  }

  /**
   * Generates the where clause.
   */
  public String generateWhere(String id)
  {
    return null;
  }

  /**
   * Generates the where clause.
   */
  public void generateUpdate(CharBuffer sql)
  {
  }

  /**
   * Generates loading cache
   */
  public void generateUpdate(JavaWriter out, String maskVar, String pstmt,
                             String index)
    throws IOException
  {
    int group = getIndex() / 64;
    long mask = 1L << getIndex() % 64;

    out.println();
    out.println("if ((" + maskVar + "_" + group + " & " + mask + "L) != 0) {");
    out.pushDepth();

    generateSet(out, pstmt, index);

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates loading code
   */
  public boolean hasLoadGroup(int index)
  {
    return index == _loadGroupIndex;
  }

  /**
   * Generates loading code
   */
  public int generateLoad(JavaWriter out, String rs,
                          String indexVar, int index)
    throws IOException
  {
    return index;
  }

  /**
   * Generates loading code
   */
  public int generateLoadEager(JavaWriter out, String rs,
                               String indexVar, int index)
    throws IOException
  {
    return index;
  }

  /**
   * Generates loading cache
   */
  public void generateLoadFromObject(JavaWriter out, String obj)
    throws IOException
  {
    if (getGetterMethod() == null || getSetterMethod() == null)
      return;

    String getter = getGetterName();

    String loadVar = "__caucho_loadMask_" + (getLoadGroupIndex() / 64);
    long loadMask = (1L << getLoadGroupIndex());

    out.println("if ((" + loadVar + " & " + loadMask + "L) != 0)");
    out.print("  ");

    out.println("  " + generateSuperSetter(generateGet(obj)) + ";");
  }

  /**
   * Generates loading cache
   */
  public void generateSet(JavaWriter out, String obj)
    throws IOException
  {
    out.println(generateSuperSetter(obj) + ";");
  }

  /**
   * Generates loading cache
   */
  public void generateUpdateFromObject(JavaWriter out, String obj)
    throws IOException
  {
    out.println(generateSuperSetter(generateGet(obj)) + ";");
  }

  /**
   * Generates the field getter.
   *
   * @param value the non-null value
   */
  public void generateGet(JavaWriter out, String value)
    throws IOException
  {
    out.print(generateGet(value));
  }

  /**
   * Returns the null value.
   */
  public String generateNull()
  {
    return "null";
  }

  /**
   * Generates the field getter.
   *
   * @param value the non-null value
   */
  public String generateGet(String obj)
  {
    if (obj == null)
      return generateNull();

    if (obj.equals("super"))
      return generateSuperGetter();
    else if (! isAbstract())
      return obj + "." + _getterMethod.getName() + "()";
    else if (_getterMethod != null)
      return obj + "." + _getterMethod.getName() + "()";
    else
      return obj + "." + getFieldName();
  }

  /**
   * Generates the field setter.
   *
   * @param value the non-null value
   */
  public String generateSet(String obj, String value)
  {
    if (obj.equals("super"))
      return generateSuperSetter(value);
    else if (isAbstract())
      return obj + "." + getFieldName() + " = " + value;
    else if (_setterMethod != null)
      return obj + "." + _setterMethod.getName() + "(" + value + ")";
    else
      return ""; // ejb/0gb9
  }

  /**
   * Returns the field name.
   */
  protected String getFieldName()
  {
    return "__amber_" + getName();
  }

  /**
   * Generates the insert.
   */
  public final String generateInsert()
  {
    return null;
  }

  /**
   * Generates the insert.
   */
  public void generateInsertColumns(ArrayList<String> columns)
  {
  }

  /**
   * Generates the get property.
   */
  public void generateGetProperty(JavaWriter out)
    throws IOException
  {

  }

  /**
   * Generates the set property.
   */
  public void generateSetProperty(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Returns the actual data.
   */
  public String generateSuperGetter()
  {
    /*
      if (isAbstract() || getGetterMethod() == null)
      return getFieldName();
      else
    */
    return("__caucho_super_get_" + getName() + "()");
  }

  /**
   * Sets the actual data.
   */
  public String generateSuperSetter(String value)
  {
    /*
      if (isAbstract() || getGetterMethod() == null)
      return(getFieldName() + " = " + value + ";");
      else
    */
    return "__caucho_super_set_" + getName() + "(" + value + ")";
  }

  /**
   * Generates the get property.
   */
  public void generateSuperGetter(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public final " + getJavaTypeName() + " __caucho_super_get_" + getName() + "()");
    out.println("{");
    out.pushDepth();

    if (isAbstract() || getGetterMethod() == null)
      out.println("return " + getFieldName() + ";");
    else
      out.println("return super." + getGetterName() + "();");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the set property.
   */
  public void generateSuperSetter(JavaWriter out)
    throws IOException
  {
    out.println();
    out.println("public final void __caucho_super_set_" + getName() + "(" + getJavaTypeName() + " v)");
    out.println("{");
    out.pushDepth();

    if (isAbstract() || getGetterMethod() == null)
      out.println(getFieldName() + " = v;");
    else if (getSetterMethod() != null)
      out.println("super." + getSetterMethod().getName() + "(v);");

    out.popDepth();
    out.println("}");
  }

  /**
   * Generates the table create.
   */
  public String generateCreateTableSQL(AmberPersistenceUnit manager)
  {
    return null;
  }

  /**
   * Generates the set clause.
   */
  public void generateSet(JavaWriter out, String pstmt, String index)
    throws IOException
  {
    generateSet(out, pstmt, index, "super");
  }

  /**
   * Generates the set clause for the insert clause.
   */
  public void generateInsertSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException
  {
    generateSet(out, pstmt, index, obj);
  }

  /**
   * Generates the set clause for the insert clause.
   */
  public void generateUpdateSet(JavaWriter out, String pstmt,
                                String index, String obj)
    throws IOException
  {
    generateSet(out, pstmt, index, obj);
  }

  /**
   * Updates the cached copy.
   */
  public void generateCopyUpdateObject(JavaWriter out,
                                       String dst, String src,
                                       int updateIndex)
    throws IOException
  {
    // commented out: jpa/0l03
    // if (getIndex() == updateIndex) {

    String value = generateGet(src);
    out.println(generateSet(dst, value) + ";");

    // }
  }

  /**
   * Updates the cached copy.
   */
  public void generateCopyLoadObject(JavaWriter out,
                                     String dst, String src,
                                     int loadIndex)
    throws IOException
  {
    // commented out: jpa/0l02
    // if (getLoadGroupIndex() == loadIndex) {

    String value = generateGet(src);
    out.println(generateSet(dst, value) + ";");

    // }
  }

  /**
   * Generates the set clause.
   */
  public void generateSet(JavaWriter out, String pstmt,
                          String index, String obj)
    throws IOException
  {
  }

  /**
   * Converts to an object.
   */
  public String toObject(String value)
  {
    return value;
  }

  /**
   * Links to the target.
   */
  public void link()
  {
  }

  /**
   * Generates the pre-delete code
   */
  public void generatePreDelete(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the delete foreign
   */
  public void generatePostDelete(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates the expire code
   */
  public void generateExpire(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Generates code for foreign entity create/delete
   */
  public void generateInvalidateForeign(JavaWriter out)
    throws IOException
  {
  }

  /**
   * Deletes the children
   */
  public void childDelete(AmberConnection aConn, Serializable primaryKey)
    throws SQLException
  {
  }

  /**
   * Generates code to convert to the type from the object.
   */
  public String generateCastFromObject(String value)
  {
    return value;
  }

  /**
   * Generates code to test the equals.
   */
  public String generateEquals(String leftBase, String value)
  {
    return leftBase + ".equals(" + value + ")";
  }

  /**
   * Creates the expression for the field.
   */
  public AmberExpr createExpr(QueryParser parser, PathExpr parent)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String toString()
  {
    return getClass().getName() + "[" + getName() + "]";
  }
}
