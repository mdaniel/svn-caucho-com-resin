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
 * Represents an int type.
 */
public final class IntegerPrimitiveType extends ConfigType<Integer>
{
  private static final L10N L = new L10N(IntegerPrimitiveType.class);
  
  public static final IntegerPrimitiveType TYPE = new IntegerPrimitiveType();
  
  private static final Integer ZERO = new Integer(0);
  
  /**
   * The IntegerPrimitiveType is a singleton
   */
  private IntegerPrimitiveType()
  {
  }
  
  /**
   * Returns the Java type.
   */
  @Override
  public Class<Integer> getType()
  {
    return int.class;
  }
  
  /**
   * Converts the string to a value of the type.
   */
  @Override
  public Object valueOf(String text)
  {
    if (text == null || text.length() == 0)
      return ZERO;
    else
      return Integer.valueOf(text);
  }
  
  /**
   * Converts the value to a value of the type.
   */
  public Object valueOf(Object value)
  {
    if (value instanceof Integer)
      return value;
    else if (value == null)
      return ZERO;
    else if (value instanceof String)
      return valueOf((String) value);
    else if (value instanceof Number)
      return new Integer(((Number) value).intValue());
    else if (value instanceof Boolean)
      return new Integer(Boolean.TRUE.equals(value) ? 1 : 0);
    else
      throw new ConfigException(L.l("'{0}' cannot be converted to an int",
                                    value));
  }
}
