/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.cfg;

import com.caucho.amber.field.AbstractField;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import java.util.Collection;

/**
 * Configuration for a cmp-relation.
 */
public class CmpRelationRole {
  private static L10N L = new L10N(CmpRelationRole.class);

  private CmpRelation _relation;
  private CmpRelationRole _target;
  
  private String _location;
  
  private String _ejbName;
  private String _fieldName;
  
  private boolean _cascadeDelete;
  private String _multiplicity;
  
  private SqlRelation []_sqlColumns = new SqlRelation[0];
  private String _orderBy;

  private AbstractField _amberField;
  private Class _javaType;

  private boolean _isImplicit;

  /**
   * Creates a new cmp-relation
   */
  public CmpRelationRole(CmpRelation relation)
  {
    _relation = relation;
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
  }

  /**
   * Gets the owning relation.
   */
  public CmpRelation getRelation()
  {
    return _relation;
  }

  /**
   * Gets the target
   */
  public CmpRelationRole getTarget()
  {
    return _target;
  }

  /**
   * Sets the target
   */
  public void setTarget(CmpRelationRole target)
  {
    _target = target;
  }

  /**
   * Sets the location.
   */
  public void setConfigLocation(String filename, int line)
  {
    _location = filename + ":" + line + ": ";
  }

  /**
   * Gets the location.
   */
  public String getLocation()
  {
    return _location;
  }

  /**
   * Sets the ejb-relationship-role-name
   */
  public void setEjbRelationshipRoleName(String name)
  {
  }

  /**
   * Returns the source ejb name.
   */
  public String getEJBName()
  {
    return _ejbName;
  }

  /**
   * Sets the ejb-name.
   */
  public void setEJBName(String ejbName)
  {
    _ejbName = ejbName;
  }

  /**
   * Returns the field name.
   */
  public String getFieldName()
  {
    return _fieldName;
  }

  /**
   * Sets the field name.
   */
  public void setFieldName(String fieldName)
  {
    _fieldName = fieldName;
  }

  /**
   * Returns the cascade-delete property.
   */
  public boolean getCascadeDelete()
  {
    return _cascadeDelete;
  }

  /**
   * Sets the cascade-delete property.
   */
  public void setCascadeDelete(boolean cascadeDelete)
  {
    _cascadeDelete = cascadeDelete;
  }

  /**
   * Returns the multiplicity property.
   */
  public String getMultiplicity()
  {
    return _multiplicity;
  }

  /**
   * Sets the source multiplicity property.
   */
  public void setMultiplicity(String multiplicity)
    throws ConfigException
  {
    _multiplicity = multiplicity;

    if (multiplicity == null ||
        ! multiplicity.equals("One") &&
        ! multiplicity.equals("Many"))
      throw new ConfigException(L.l("'{0}' is an unknown multiplicity.  'One' and 'Many' are the only allowed values.", multiplicity));
  }

  /**
   * Returns the source sql columns.
   */
  public SqlRelation []getSQLColumns()
  {
    return _sqlColumns;
  }

  /**
   * Add a sql columns.
   */
  public void addSQLColumn(String sqlColumn, String references)
  {
    SqlRelation relation = new SqlRelation(_fieldName);

    relation.setSQLColumn(sqlColumn);
    relation.setReferences(references);

    if (_sqlColumns == null)
      _sqlColumns = new SqlRelation[] { relation };
    else {
      SqlRelation []newColumns = new SqlRelation[_sqlColumns.length + 1];
      System.arraycopy(_sqlColumns, 0, newColumns, 0, _sqlColumns.length);

      newColumns[_sqlColumns.length] = relation;
      _sqlColumns = newColumns;
    }
  }

  public SqlColumn createSqlColumn()
  {
    return new SqlColumn();
  }

  /**
   * Returns the order-by property.
   */
  public String getOrderBy()
  {
    return _orderBy;
  }

  /**
   * Sets the order-by property.
   */
  public void setOrderBy(String orderBy)
  {
    _orderBy = orderBy;
  }

  /**
   * Returns true if the role is implicit.
   */
  public boolean isImplicit()
  {
    return _isImplicit;
  }

  /**
   * Sets true if the role is implicit.
   */
  public void setImplicit(boolean isImplicit)
  {
    _isImplicit = isImplicit;
  }

  /**
   * Sets the Java type.
   */
  public void setJavaType(Class cl)
  {
    _javaType = cl;
  }

  /**
   * Sets the amber type.
   */
  public void setAmberField(AbstractField field)
  {
    _amberField = field;
  }

  /**
   * Gets the amber type.
   */
  public AbstractField getAmberField()
  {
    return _amberField;
  }

  /**
   * Return true for collections.
   */
  public boolean isCollection()
  {
    return _javaType != null && Collection.class.isAssignableFrom(_javaType);
  }

  /**
   * Merges with the target.
   */
  public void merge(CmpRelationRole newRole)
  {
    if (_sqlColumns.length == 0)
      _sqlColumns = newRole.getSQLColumns();
  }

  // EJB config
  
  /**
   * Sets the relationship-role-source.
   */
  public RoleSource createRelationshipRoleSource()
  {
    return new RoleSource();
  }
  
  /**
   * Sets the relationship-role-source.
   */
  public CmrField createCmrField()
  {
    return new CmrField();
  }

  /**
   * Returns true if this is the same relation.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof CmpRelationRole))
      return false;

    CmpRelationRole role = (CmpRelationRole) o;

    if (! _ejbName.equals(role._ejbName))
      return false;

    if (_fieldName == null || role._fieldName == null)
      return _fieldName == role._fieldName;
    
    if (! _fieldName.equals(role._fieldName))
      return false;

    return true;
  }

  public class RoleSource {
    public void setEJBName(String name)
    {
      CmpRelationRole.this.setEJBName(name);
    }
  }

  public class CmrField {
    public void setCmrFieldName(String name)
    {
      CmpRelationRole.this.setFieldName(name);
    }
    
    public void setCmrFieldType(String name)
    {
      // XXX: CmpRelationRole.this.setFieldName(name);
    }

    public SqlColumn createSqlColumn()
    {
      return new SqlColumn();
    }
  }

  public class SqlColumn {
    private String _value;
    private String _references;

    public void setReferences(String references)
    {
      _references = references;
    }

    public void setValue(String value)
    {
      _value = value;
    }

    public void addText(String value)
    {
      _value = value;
    }

    @PostConstruct
    public void init()
    {
      CmpRelationRole.this.addSQLColumn(_value, _references);
    }
  }
}
