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

package com.caucho.ejb.cfg;

import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JClassLoader;

import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;

import com.caucho.config.ConfigException;

import com.caucho.jdbc.JdbcMetaData;

import com.caucho.amber.AmberManager;

import com.caucho.amber.type.Type;
import com.caucho.amber.type.EntityType;

import com.caucho.amber.field.IdField;
import com.caucho.amber.field.KeyPropertyField;

import com.caucho.amber.table.Column;

/**
 * Configuraton for a cmp-field.
 */
public class CmpField extends CmpProperty {
  private static final L10N L = new L10N(CmpProperty.class);

  private String _sqlColumn;
  private String _abstractSQLType;
  private String _sqlType;
  private boolean _isAutoGenerate = true;

  private JClass _javaType = JClass.STRING;

  /**
   * Creates a new cmp-field
   *
   * @param entity the owning entity bean
   */
  public CmpField(EjbEntityBean entity)
  {
    super(entity);
  }

  /**
   * Returns the SQL column name.
   */
  public String getSQLColumn()
  {
    return _sqlColumn;
  }

  /**
   * Sets the SQL column name.
   */
  public void setSQLColumn(String sqlColumn)
  {
    _sqlColumn = sqlColumn;
  }

  /**
   * Returns the SQL type name.
   */
  public String getSQLType()
  {
    return _sqlType;
  }

  /**
   * Sets the SQL type name.
   */
  public void setSQLType(String sqlType)
  {
    _sqlType = sqlType;
  }

  /**
   * Returns the abstract SQL type name.
   */
  public String getAbstractSQLType()
  {
    return _abstractSQLType;
  }

  /**
   * Sets the abstract SQL type name.
   */
  public void setAbstractSQLType(String sqlType)
  {
    _abstractSQLType = sqlType;
  }

  /**
   * Sets the Java type.
   */
  public void setJavaType(JClass javaType)
  {
    //_javaType = new JClassWrapper(javaType);
    _javaType = javaType;
  }

  /**
   * Returns the Java type.
   */
  public JClass getJavaType()
  {
    return _javaType;
  }

  /**
   * Set true for auto-generation.
   */
  public void setAutoGenerate(boolean isAutoGenerate)
  {
    _isAutoGenerate = isAutoGenerate;
  }

  /**
   * true for auto-generation.
   */
  public boolean isAutoGenerate()
  {
    return _isAutoGenerate;
  }

  /**
   * Initialize the field.
   */
  public void init()
    throws ConfigException
  {
    String name = getName();

    String getterName = ("get" +
			 Character.toUpperCase(name.charAt(0)) +
			 name.substring(1));
	
    JMethod getter = getEntity().getMethod(getEntity().getEJBClassWrapper(),
					  getterName,
					   new JClass[0]);

    if (getter == null)
      throw new ConfigException(L.l("{0}: '{1}' is an unknown cmp-field.  cmp-fields must have matching getter methods.",
				    getEntity().getEJBClass().getName(),
				    name));
    else if (! getter.isPublic()) {
      throw new ConfigException(L.l("{0}: '{1}' must be public.  cmp-fields getters must be public.",
				    getEntity().getEJBClass().getName(),
				    getter.getFullName()));
    }
    else if (! getter.isAbstract() && ! getEntity().isAllowPOJO()) {
      throw new ConfigException(L.l("{0}: '{1}' must be abstract.  cmp-fields getters must be abstract.",
				    getEntity().getEJBClass().getName(),
				    getter.getFullName()));
    }
    else if (getter.getExceptionTypes().length != 0) {
      throw new ConfigException(L.l("{0}: '{1}' must not throw {2}.  Container managed fields and relations must not throw exceptions.",
				    getEntity().getEJBClass().getName(),
				    getter.getFullName(),
				    getter.getExceptionTypes()[0].getName()));
    }

    _javaType = getter.getReturnType();

    if ("void".equals(_javaType.getName())) {
      throw new ConfigException(L.l("{0}: '{1}' must not return void.  CMP fields must not return void.",
				    getEntity().getEJBClass().getName(),
				    getName()));
    }
    else if (_javaType.isAssignableTo(EJBLocalObject.class)) {
      throw new ConfigException(L.l("{0}: '{1}' must not return an EJB interface.  CMP fields must return concrete values.",
				    getEntity().getEJBClass().getName(),
				    getName()));
    }
    else if (_javaType.isAssignableTo(EJBObject.class)) {
      throw new ConfigException(L.l("{0}: '{1}' must not return an EJB interface.  CMP fields must return concrete values.",
				    getEntity().getEJBClass().getName(),
				    getName()));
    }
    
    String setterName = ("set" +
			 Character.toUpperCase(name.charAt(0)) +
			 name.substring(1));
	
  JMethod setter = getEntity().getMethod(getEntity().getEJBClassWrapper(),
					  setterName,
					  new JClass[] { getter.getReturnType() });

    if (setter == null) {
    }
    else if (! setter.isPublic()) {
      throw new ConfigException(L.l("{0}: '{1}' must be public.  cmp-fields setters must be public.",
				    getEntity().getEJBClass().getName(),
				    setter.getFullName()));
    }
    else if (! "void".equals(setter.getReturnType().getName())) {
      throw new ConfigException(L.l("{0}: '{1}' must return void.  cmp-fields setters must return void.",
				    getEntity().getEJBClass().getName(),
				    setter.getFullName()));
    }
    else if (! setter.isAbstract() && ! getEntity().isAllowPOJO()) {
      throw new ConfigException(L.l("{0}: '{1}' must be abstract.  cmp-fields setters must be abstract.",
				    getEntity().getEJBClass().getName(),
				    setter.getFullName()));
    }
    else if (setter.getExceptionTypes().length != 0) {
      throw new ConfigException(L.l("{0}: '{1}' must not throw {2}.  Container managed fields and relations must not throw exceptions.",
				    getEntity().getEJBClass().getName(),
				    setter.getFullName(),
				    setter.getExceptionTypes()[0].getName()));
    }

    if (_sqlColumn == null)
      _sqlColumn = toSqlName(getName());
  }

  /**
   * Amber creating the id field.
   */
  public IdField createId(AmberManager amberManager, EntityType type)
    throws ConfigException
  {
    String fieldName = getName();
    String sqlName = getSQLColumn();

    if (sqlName == null)
      sqlName = toSqlName(fieldName);
      
    JClass dataType = getJavaType();

    if (dataType == null)
      throw new NullPointerException(L.l("'{0}' is an unknown field",
					 fieldName));

    Type amberType = amberManager.createType(dataType);
    Column column = type.getTable().createColumn(sqlName, amberType);

    KeyPropertyField idField = new KeyPropertyField(type, fieldName, column);

    if (! isAutoGenerate()) {
    }
    else if ("int".equals(dataType.getName()) ||
	"long".equals(dataType.getName()) ||
	"java.lang.Integer".equals(dataType.getName()) ||
	"java.lang.Long".equals(dataType.getName())) {
      JdbcMetaData metaData = amberManager.getMetaData();

      if (metaData.supportsIdentity()) {
	idField.setGenerator("identity");
	column.setGeneratorType("identity");
      }
      else if (metaData.supportsSequences()) {
	idField.setGenerator("sequence");
	column.setGeneratorType("sequence");

	String name = type.getTable().getName() + "_cseq";

	type.setGenerator(idField.getName(), amberManager.createSequenceGenerator(name, 10));
      }
      else {
	// XXX: should try table
      }
    }

    return idField;
  }

  static String toSqlName(String name)
  {
    CharBuffer cb = new CharBuffer();

    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);

      if (! Character.isUpperCase(ch))
	cb.append(ch);
      else if (i > 0 && ! Character.isUpperCase(name.charAt(i - 1))) {
	cb.append("_");
	cb.append(Character.toLowerCase(ch));
      }
      else if (i + 1 < name.length() &&
	       ! Character.isUpperCase(name.charAt(i + 1))) {
	cb.append("_");
	cb.append(Character.toLowerCase(ch));
      }
      else
	cb.append(Character.toLowerCase(ch));
    }

    return cb.toString();
  }

  public String toString()
  {
    return "CmpField[" + getName() + "]";
  }
}
