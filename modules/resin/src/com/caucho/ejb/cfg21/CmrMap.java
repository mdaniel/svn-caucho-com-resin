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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.cfg21;

import com.caucho.ejb.cfg.*;
import com.caucho.ejb.cfg21.CmrManyToOne;
import com.caucho.amber.field.AmberField;
import com.caucho.amber.field.EntityMapField;
import com.caucho.amber.field.IdField;
import com.caucho.amber.type.EntityType;
import com.caucho.bytecode.JMethodWrapper;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

/**
 * map relation
 */
public class CmrMap extends CmrRelation {
  private static final L10N L = new L10N(CmrMap.class);

  private EjbEntityBean _targetBean;
  private CmrManyToOne _idRel;
  private ApiMethod _mapMethod;

  /**
   * Creates a new cmr map
   */
  public CmrMap(EjbEntityBean sourceBean,
		String fieldName,
		EjbEntityBean targetBean,
		CmrManyToOne idRel)
    throws ConfigException
  {
    super(sourceBean);

    setFieldName(fieldName);

    _targetBean = targetBean;
    _idRel = idRel;
  }

  /**
   * Sets the map method.
   */
  public void setMapMethod(ApiMethod method)
  {
    _mapMethod = method;
  }

  /**
   * Gets the map method.
   */
  public ApiMethod getMapMethod()
  {
    return _mapMethod;
  }

  /**
   * Returns the index name.
   */
  public String getIndexName()
  {
    EntityType type = _targetBean.getEntityType();

    for (IdField key : type.getId().getKeys()) {
      if (! key.getName().equals(_idRel.getName()))
	return key.getName();
    }
    
    throw new IllegalStateException();
  }

  /**
   * Returns the rel name.
   */
  public String getIdName()
  {
    return _idRel.getName();
  }

  /**
   * Returns the target bean.
   */
  public EjbEntityBean getTargetBean()
  {
    return _targetBean;
  }

  /**
   * Create any bean methods.
   */
  public EjbMethod createGetter(EjbView view,
				ApiMethod apiMethod,
				ApiMethod implMethod)
    throws ConfigException
  {
    return new EjbMapMethod(view, apiMethod, implMethod, this);
  }

  /**
   * Creates the amber type.
   */
  public AmberField assembleAmber(EntityType type)
    throws ConfigException
  {
    EntityMapField field = new EntityMapField(type);

    field.setName(getName());
    field.setMapMethod(new JMethodWrapper(_mapMethod.getMethod()));

    field.setTargetType(_targetBean.getEntityType());

    EntityType sourceType = _targetBean.getEntityType();
    for (IdField key : sourceType.getId().getKeys()) {
      if (key.getName().equals(_idRel.getName())) {
	field.setId(key);
      }
      else {
	field.setIndex(key);
      }
    }
    

    return field;
  }
}
