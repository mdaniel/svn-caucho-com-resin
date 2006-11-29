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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.cfg;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;

/**
 * Configuraton for a cmp-relation.
 */
public class CmpRelation {
  private static final L10N L = new L10N(CmpRelation.class);

  private EjbConfig _config;

  private String _location = "";

  private String _name;
  private String _sqlTable;
  
  private CmpRelationRole _sourceRole;
  private CmpRelationRole _targetRole;

  private int _roleCount;

  /**
   * Creates a new cmp-relation
   */
  public CmpRelation(EjbConfig config)
  {
    _config = config;
    _sourceRole = new CmpRelationRole(this);
    _targetRole = new CmpRelationRole(this);

    _sourceRole.setTarget(_targetRole);
    _targetRole.setTarget(_sourceRole);
  }

  /**
   * Creates a new cmp-relation
   */
  public CmpRelation()
  {
    _sourceRole = new CmpRelationRole(this);
    _targetRole = new CmpRelationRole(this);

    _sourceRole.setTarget(_targetRole);
    _targetRole.setTarget(_sourceRole);
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
   * Returns the relation name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets the relation name.
   */
  public void setName(String name)
  {
    _name = name;
  }

  /**
   * Sets the relation name.
   */
  public void setEJBRelationName(String name)
  {
    _name = name;
  }

  /**
   * Returns the SQL table for a three-table relation.
   */
  public String getSQLTable()
  {
    return _sqlTable;
  }

  /**
   * Sets the SQL tarble for a three-table relation.
   */
  public void setSQLTable(String sqlTable)
  {
    _sqlTable = sqlTable;
  }

  /**
   * Returns the source role.
   */
  public CmpRelationRole getSourceRole()
  {
    if (_sourceRole.getFieldName() == null &&
        _targetRole.getFieldName() != null)
      return _targetRole;
    else
      return _sourceRole;
  }

  /**
   * Returns the target role.
   */
  public CmpRelationRole getTargetRole()
  {
    if (_sourceRole.getFieldName() == null &&
        _targetRole.getFieldName() != null)
      return _sourceRole;
    else
      return _targetRole;
  }

  /**
   * Returns the source ejb name.
   */
  public String getSourceEJB()
  {
    return getSourceRole().getEJBName();
  }

  /**
   * Sets the source ejb.
   */
  public void setSourceEJB(String sourceEJB)
  {
    _sourceRole.setEJBName(sourceEJB);
  }

  /**
   * Returns the source field name.
   */
  public String getSourceField()
  {
    return getSourceRole().getFieldName();
  }

  /**
   * Sets the source field.
   */
  public void setSourceField(String sourceField)
  {
    _sourceRole.setFieldName(sourceField);
  }

  /**
   * Returns the source cascade-delete property.
   */
  public boolean getSourceCascadeDelete()
  {
    return getSourceRole().getCascadeDelete();
  }

  /**
   * Sets the source cascade-delete property.
   */
  public void setSourceCascadeDelete(boolean sourceCascadeDelete)
  {
    _sourceRole.setCascadeDelete(sourceCascadeDelete);
  }

  /**
   * Returns the source multiplicity property.
   */
  public String getSourceMultiplicity()
  {
    return getSourceRole().getMultiplicity();
  }

  /**
   * Sets the source multiplicity property.
   */
  public void setSourceMultiplicity(String sourceMultiplicity)
    throws ConfigException
  {
    _sourceRole.setMultiplicity(sourceMultiplicity);
  }

  /**
   * Returns the source sql columns.
   */
  public SqlRelation []getSourceSQLColumns()
  {
    return getSourceRole().getSQLColumns();
  }

  /**
   * Add a source sql columns.
   */
  public void addSourceSQLColumn(String sqlColumn, String references)
  {
    _sourceRole.addSQLColumn(sqlColumn, references);
  }

  /**
   * Creates a target sql column.
   */
  public CmpRelationRole.SqlColumn createSourceSqlColumn()
  {
    return _sourceRole.createSqlColumn();
  }

  /**
   * Returns the source order-by property.
   */
  public String getSourceOrderBy()
  {
    return getSourceRole().getOrderBy();
  }

  /**
   * Sets the source order-by property.
   */
  public void setSourceOrderBy(String sourceOrderBy)
  {
    _sourceRole.setOrderBy(sourceOrderBy);
  }

  /**
   * Returns the target ejb name.
   */
  public String getTargetEJB()
  {
    return getTargetRole().getEJBName();
  }

  /**
   * Sets the target ejb.
   */
  public void setTargetEJB(String targetEJB)
  {
    _targetRole.setEJBName(targetEJB);
  }

  /**
   * Returns the target field name.
   */
  public String getTargetField()
  {
    return getTargetRole().getFieldName();
  }

  /**
   * Sets the target field.
   */
  public void setTargetField(String targetField)
  {
    _targetRole.setFieldName(targetField);
  }

  /**
   * Returns the target cascade-delete property.
   */
  public boolean getTargetCascadeDelete()
  {
    return getTargetRole().getCascadeDelete();
  }

  /**
   * Sets the target cascade-delete property.
   */
  public void setTargetCascadeDelete(boolean targetCascadeDelete)
  {
    _targetRole.setCascadeDelete(targetCascadeDelete);
  }

  /**
   * Returns the target multiplicity property.
   */
  public String getTargetMultiplicity()
  {
    return getTargetRole().getMultiplicity();
  }

  /**
   * Sets the target multiplicity property.
   */
  public void setTargetMultiplicity(String targetMultiplicity)
    throws ConfigException
  {
    _targetRole.setMultiplicity(targetMultiplicity);
  }

  /**
   * Returns the target sql columns.
   */
  public SqlRelation []getTargetSQLColumns()
  {
    return getTargetRole().getSQLColumns();
  }

  /**
   * Add a target sql columns.
   */
  public void addTargetSQLColumn(String sqlColumn, String references)
  {
    _targetRole.addSQLColumn(sqlColumn, references);
  }

  /**
   * Creates a target sql column.
   */
  public CmpRelationRole.SqlColumn createTargetSqlColumn()
  {
    return _targetRole.createSqlColumn();
  }

  /**
   * Returns the target order-by property.
   */
  public String getTargetOrderBy()
  {
    return getTargetRole().getOrderBy();
  }

  /**
   * Sets the target order-by property.
   */
  public void setTargetOrderBy(String targetOrderBy)
  {
    _targetRole.setOrderBy(targetOrderBy);
  }

  /**
   * Configures the ejb-relationship-role
   */
  public CmpRelationRole createEjbRelationshipRole()
    throws ConfigException
  {
    _roleCount++;
    
    if (_roleCount == 1)
      return _sourceRole;
    else if (_roleCount == 2)
      return _targetRole;
    else
      throw new ConfigException(L.l("ejb-relation requires two ejb-relationship-role elements"));
  }

  /**
   * Merges the relation.
   */
  public void merge(CmpRelation newRel)
    throws ConfigException
  {
    if (_sqlTable == null)
      _sqlTable = newRel.getSQLTable();

    _sourceRole.merge(newRel.getSourceRole());
    _targetRole.merge(newRel.getTargetRole());
  }

  /**
   * Initialization sanity checks.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (getSourceEJB() == null)
      throw new ConfigException(L.l("ejb-relation needs a source EJB."));
    
    if (getTargetEJB() == null)
      throw new ConfigException(L.l("ejb-relation needs a target EJB."));

    if (getSourceField() == null)
      throw new ConfigException(L.l("ejb-relation needs a source field."));
  }
    
  /**
   * Returns true if this is the same relation.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof CmpRelation))
      return false;

    CmpRelation relation = (CmpRelation) o;

    return _sourceRole.equals(relation._sourceRole);
  }
}
