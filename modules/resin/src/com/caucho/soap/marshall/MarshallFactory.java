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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.soap.marshall;

import java.util.*;
import java.math.*;

/**
 * Factory for creating marshall instances
 */
public class MarshallFactory {
  /**
   * Returns the deserializer for the given type.
   */
  public Marshall createDeserializer(Class type)
  {
    throw new UnsupportedOperationException(type.getName());
  }
  
  /**
   * Returns the serializer for the given type.
   */
  public Marshall createSerializer(Class type)
  {
    if (String.class.equals(type))
      return StringMarshall.MARSHALL;
    if (Map.class.equals(type))
      return MapMarshall.MARSHALL;
    if (Double.class.equals(type))
      return DoubleMarshall.MARSHALL;
    if (Float.class.equals(type))
      return FloatMarshall.MARSHALL;
    if (Long.class.equals(type))
      return LongMarshall.MARSHALL;
    if (BigDecimal.class.equals(type))
      return BigDecimalMarshall.MARSHALL;
    if (List.class.equals(type))
      return ListMarshall.MARSHALL;
    if (Date.class.equals(type))
      return DateMarshall.MARSHALL;
    if (byte[].class.equals(type))
      return ByteArrayMarshall.MARSHALL;
    if (Object[].class.isAssignableFrom(type))
      return ArrayMarshall.MARSHALL;
    else
      throw new UnsupportedOperationException(type.getName());
  }
}


