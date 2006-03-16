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

import java.util.Collection;

/**
 * Represents composite data.
 */
public class ArrayType extends OpenType {
  private int dimension;
  private OpenType elementType;
  
  public ArrayType(int dimension, OpenType elementType)
    throws OpenDataException
  {
    super(arrayName(dimension, elementType.getClassName()),
	  arrayName(dimension, elementType.getTypeName()),
	  dimension + "-dimension array of " + elementType.getDescription());
    
    this.dimension = dimension;
    this.elementType = elementType;
  }

  private static String arrayName(int dimension, String type)
  {
    StringBuffer sb = new StringBuffer();

    for (int i = 0; i < dimension; i++)
      sb.append("[");

    sb.append(type);
    sb.append(";");

    return sb.toString();
  }

  /**
   * Returns the dimension of arrays.
   */
  public int getDimension()
  {
    return this.dimension;
  }

  /**
   * Returns the composite element type.
   */
  public OpenType getElementOpenType()
  {
    return this.elementType;
  }

  /**
   * Returns true if the obj is a value.
   */
  public boolean isValue(Object obj)
  {
    if (obj == null)
      return false;

    Class cl = obj.getClass();
    
    for (int i = dimension - 1; i >= 0; i--) {
      if (! cl.isArray())
	return false;

      cl = cl.getComponentType();
    }

    return true;
  }

  /**
   * Returns true for equality.
   */
  public boolean equals(Object o)
  {
    if (! (o instanceof ArrayType))
      return false;

    ArrayType type = (ArrayType) o;

    return (this.dimension == type.dimension &&
	    this.elementType.equals(type.elementType));
  }

  /**
   * Returns true for equality.
   */
  public int hashCode()
  {
    return this.dimension + this.elementType.hashCode();
  }

  public String toString()
  {
    return "ArrayType[]";
  }
}
