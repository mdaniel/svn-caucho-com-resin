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

import com.caucho.v5.config.*;
import com.caucho.v5.util.*;

/**
 * Represents a byte or Byte type.
 */
public final class ByteType extends ConfigType
{
  private static final L10N L = new L10N(IntegerType.class);
  
  public static final ByteType TYPE = new ByteType();
  
  private static final Byte ZERO = new Byte((byte) 0);

  /**
   * The ByteType is a singleton
   */
  private ByteType()
  {
  }
  
  /**
   * Returns the Java type.
   */
  public Class getType()
  {
    return Byte.class;
  }
  
  /**
   * Converts the string to a value of the type.
   */
  public Object valueOf(String text)
  {
    if (text == null || text.length() == 0)
      return null;
    else
      return Byte.valueOf(text);
  }
  
  /**
   * Converts the value to a value of the type.
   */
  public Object valueOf(Object value)
  {
    if (value instanceof Byte)
      return value;
    else if (value == null)
      return null;
    else if (value instanceof String)
      return valueOf((String) value);
    else if (value instanceof Number)
      return new Byte(((Number) value).byteValue());
    else if (value instanceof Boolean)
      return new Byte(((Boolean) value) ? (byte) 1 : 0);
    else
      throw new ConfigException(L.l("'{0}' cannot be converted to a Byte",
                                    value));
  }
}
