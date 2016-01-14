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

import java.util.Properties;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.VfsOld;

/**
 * Represents a Properties type.
 */
public final class PropertiesType extends ConfigType
{
  private static final L10N L = new L10N(LocaleType.class);
  
  public static final PropertiesType TYPE = new PropertiesType();

  /**
   * The PropertiesType is a singleton
   */
  private PropertiesType()
  {
  }
  
  /**
   * Returns the Java type.
   */
  public Class getType()
  {
    return Properties.class;
  }
  
  /**
   * Creates a new instance of the type.
   */
  @Override
  public Object create(Object parent, NameCfg name)
  {
    throw new ConfigException(L.l("java.util.Properties syntax is a string in .properties file syntax like the following:\n  a=b\n  b=c"));
  }

  
  /**
   * Converts the string to a value of the type.
   */
  @Override
  public Object valueOf(String text)
  {
    if (text == null)
      return null;

    try {
      Properties props = new Properties();

      ReadStream is = VfsOld.openString(text);

      props.load(is);

      return props;
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
  }
  
  /**
   * Converts the value to a value of the type.
   */
  public Object valueOf(Object value)
  {
    if (value instanceof Properties)
      return value;
    else if (value == null)
      return null;
    else if (value instanceof String)
      return valueOf((String) value);
    else
      throw new ConfigException(L.l("'{0}' is not a valid Properties value.",
                                    value));
  }
}
