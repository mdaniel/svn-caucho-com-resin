/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.config.type;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.util.L10N;

/**
 * Represents an introspected bean type for configuration.
 */
public class ListType extends ConfigType
{
  private static final L10N L = new L10N(ListType.class);
  private static final Logger log
    = Logger.getLogger(ListType.class.getName());

  private final Class<?> _listClass;
  private final Class<?> _instanceClass;

  public ListType()
  {
    this(ArrayList.class);
  }

  public ListType(Class<?> listClass)
  {
    _listClass = listClass;

    if (! _listClass.isInterface()
        && Modifier.isAbstract(_listClass.getModifiers())) {
      _instanceClass = _listClass;
    }
    else
      _instanceClass = ArrayList.class;
  }

  /**
   * Returns the given type.
   */
  public Class<?> getType()
  {
    return _listClass;
  }

  @Override
  public String getTypeName()
  {
    return getType().getName() + "[]";
  }

  /**
   * Creates a new instance
   */
  public Object create(Object parent)
  {
    try {
      return _instanceClass.newInstance();
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }

  /**
   * Returns the attribute based on the given name.
   */
  public AttributeConfig getAttribute(NameCfg name)
  {
    AttributeConfig attr = TypeFactoryConfig.getFactory().getListAttribute(name);
    
    return attr;
  }
  
  /**
   * Converts the string to the given value.
   */
  public Object valueOf(String text)
  {
    if ("".equals(text.trim()))
      return null;
    
    throw new ConfigException(L.l("Can't convert to '{0}' from '{1}'.",
                                  _listClass.getName(), text));
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _listClass.getName() + "]";
  }
}
