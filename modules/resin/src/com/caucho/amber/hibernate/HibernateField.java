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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.hibernate;

import com.caucho.bytecode.JMethod;
import com.caucho.bytecode.JClass;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.amber.type.Type;
import com.caucho.amber.type.EntityType;

import com.caucho.amber.field.AbstractField;

/**
 * configuration for an entity
 */
public class HibernateField {
  private static final L10N L = new L10N(HibernateField.class);

  private EntityType _type;
  private AbstractField _field;
  
  private Type _resultType;

  HibernateField(EntityType type)
  {
    _type = type;
  }

  protected void setField(AbstractField field)
  {
    _field = field;
  }

  public void setName(String name)
    throws ConfigException
  {
    _field.setName(name);
  }

  public void setType(String type)
    throws ConfigException
  {
    _resultType = _type.getAmberManager().createType(type);
  }

  public Type getType()
  {
    return _resultType;
  }

  EntityType getOwnerType()
  {
    return _type;
  }

  public void init()
    throws ConfigException
  {
    /*
    if (_field.getName() == null) {
      throw new ConfigException(L.l("'name' missing for property.  Properties need a name attribute."));
    }
    */
    
    if (_resultType == null) {
      JMethod method = _field.getGetterMethod();
      if (method != null) {
	JClass resultClass = method.getReturnType();
	_resultType = _type.getAmberManager().createType(resultClass);
      }
      else {
	_resultType = _type.getAmberManager().createType("string");
      }
    }
  }
}
