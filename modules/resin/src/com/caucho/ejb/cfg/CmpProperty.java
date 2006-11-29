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

import com.caucho.amber.field.IdField;
import com.caucho.amber.manager.AmberPersistenceUnit;
import com.caucho.amber.type.EntityType;
import com.caucho.bytecode.JClass;
import com.caucho.bytecode.JMethod;
import com.caucho.config.ConfigException;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;

/**
 * Configuraton for a cmp-field.
 */
public class CmpProperty {
  private static final L10N L = new L10N(CmpProperty.class);

  private EjbEntityBean _entity;

  private String _location;

  private String _name;
  private String _description;

  private boolean _isId;

  /**
   * Creates a new cmp-field
   *
   * @param entity the owning entity bean
   */
  public CmpProperty(EjbEntityBean entity)
  {
    _entity = entity;
  }

  /**
   * Returns the entity.
   */
  public EjbEntityBean getEntity()
  {
    return _entity;
  }

  /**
   * Returns the entity.
   */
  public EjbEntityBean getBean()
  {
    return _entity;
  }

  /**
   * Sets the location.
   */
  public void setConfigLocation(String filename, int line)
  {
    _location = filename + ":" + line + ": ";
  }

  /**
   * Sets the location.
   */
  public void setLocation(String location)
  {
    if (location != null)
      _location = location;
  }

  /**
   * Returns the location of the cmp-field in the config file.
   */
  public String getLocation()
  {
    if (_location != null)
      return _location;
    else
      return _entity.getLocation();
  }

  /**
   * Sets the cmp-field name.
   */
  public void setFieldName(String name)
  {
    _name = name;
  }

  /**
   * Returns the cmp-field name.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Sets true for an id.
   */
  public void setId(boolean id)
  {
    _isId = id;
  }

  /**
   * Gets true for an id.
   */
  public boolean isId()
  {
    return _isId;
  }

  /**
   * Sets the description.
   */
  public void setDescription(String description)
  {
    _description = description;
  }

  /**
   * Returns the getter.
   */
  public JMethod getGetter()
  {
    String methodName = ("get" +
			 Character.toUpperCase(_name.charAt(0)) +
			 _name.substring(1));
    
    return _entity.getMethod(methodName, new JClass[0]);
  }

  /**
   * Returns the setter.
   */
  public JMethod getSetter()
  {
    String methodName = ("set" +
			 Character.toUpperCase(_name.charAt(0)) +
			 _name.substring(1));

    JMethod getter = getGetter();

    if (getter != null)
      return _entity.getMethod(methodName,
			       new JClass[] { getter.getReturnType() });
    else
      return null;
  }

  /**
   * Amber creating the id field.
   */
  public IdField createId(AmberPersistenceUnit amberPersistenceUnit, EntityType type)
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
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
    return "CmpProperty[" + _name + "]";
  }
}
