/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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

package javax.management.openmbean;

import java.io.ObjectStreamException;

/**
 * Represents composite data.
 */
public class SimpleType extends OpenType {
  public static final SimpleType VOID = create("java.lang.Void");
  public static final SimpleType BOOLEAN = create("java.lang.Boolean");
  public static final SimpleType CHARACTER = create("java.lang.Character");
  public static final SimpleType BYTE = create("java.lang.Byte");
  public static final SimpleType SHORT = create("java.lang.Short");
  public static final SimpleType INTEGER = create("java.lang.Integer");
  public static final SimpleType LONG = create("java.lang.Long");
  public static final SimpleType FLOAT = create("java.lang.Float");
  public static final SimpleType DOUBLE = create("java.lang.Double");
  public static final SimpleType STRING = create("java.lang.String");
  public static final SimpleType BIGDECIMAL = create("java.math.BigDecimal");
  public static final SimpleType BIGINTEGER = create("java.math.BigInteger");
  public static final SimpleType DATE = create("java.lang.util");
  public static final SimpleType OBJECTNAME =
    create("javax.management.ObjectName");
  
  private SimpleType(String type)
    throws OpenDataException
  {
    super(type, type, type);
  }

  public boolean isValue(Object obj)
  {
    if (obj == null)
      return false;

    return obj.getClass().getName().equals(getTypeName());
  }

  /**
   * Returns true for equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof SimpleType))
      return false;

    SimpleType simple = (SimpleType) o;

    return getTypeName().equals(simple.getTypeName());
  }

  /**
   * Returns the hashCode.
   */
  public int hashCode()
  {
    return getTypeName().hashCode();
  }

  public Object readResolve()
    throws ObjectStreamException
  {
    String typeName = getTypeName();

    if (VOID.getTypeName().equals(typeName))
      return VOID;
    else if (BOOLEAN.getTypeName().equals(typeName))
      return BOOLEAN;
    else if (BYTE.getTypeName().equals(typeName))
      return BYTE;
    else if (SHORT.getTypeName().equals(typeName))
      return SHORT;
    else if (INTEGER.getTypeName().equals(typeName))
      return INTEGER;
    else if (LONG.getTypeName().equals(typeName))
      return LONG;
    else if (FLOAT.getTypeName().equals(typeName))
      return FLOAT;
    else if (DOUBLE.getTypeName().equals(typeName))
      return DOUBLE;
    else if (STRING.getTypeName().equals(typeName))
      return STRING;
    else if (BIGDECIMAL.getTypeName().equals(typeName))
      return BIGDECIMAL;
    else if (BIGINTEGER.getTypeName().equals(typeName))
      return BIGINTEGER;
    else if (DATE.getTypeName().equals(typeName))
      return DATE;
    else if (OBJECTNAME.getTypeName().equals(typeName))
      return OBJECTNAME;
    else
      return this;
  }

  private static SimpleType create(String type)
  {
    try {
      return new SimpleType(type);
    } catch (Throwable e) {
      e.printStackTrace();

      return null;
    }
  }
}
