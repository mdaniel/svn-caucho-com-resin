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
import java.util.Map;
import java.util.TreeMap;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.attribute.AttributeConfig;
import com.caucho.v5.config.attribute.EntryAttribute;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.util.L10N;

/**
 * Represents an introspected bean type for configuration.
 */
public class MapType extends ConfigType<Map<?,?>>
{
  private static final L10N L = new L10N(MapType.class);

  private final Class<Map<?,?>> _mapClass;
  private final Class<?> _instanceClass;

  private final EntryAttribute _entryAttribute;

  public MapType()
  {
    this(TreeMap.class);
  }

  public MapType(Class mapClass)
  {
    _mapClass = mapClass;

    if (! _mapClass.isInterface()
        && Modifier.isAbstract(_mapClass.getModifiers())) {
      _instanceClass = _mapClass;
    }
    else
      _instanceClass = TreeMap.class;

    _entryAttribute = new EntryAttribute();
  }

  /**
   * Returns the given type.
   */
  @Override
  public Class<Map<?,?>> getType()
  {
    return _mapClass;
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
    return _entryAttribute;
  }
  
  /**
   * Converts the string to the given value.
   */
  public Object valueOf(String text)
  {
    throw new ConfigException(L.l("Can't convert to '{0}' from '{1}'.",
                                  _mapClass.getName(), text));
  }
}
