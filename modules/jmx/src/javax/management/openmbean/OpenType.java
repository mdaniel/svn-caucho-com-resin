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
public abstract class OpenType implements java.io.Serializable {
  public static final String []ALLOWED_CLASSNAMES;

  private String className;
  private String description;
  private String typeName;

  protected OpenType(String className, String typeName, String description)
    throws OpenDataException
  {
    this.className = className;
    this.typeName = typeName;
    this.description = description;
  }

  /**
   * Returns the class name.
   */
  public String getClassName()
  {
    return this.className;
  }

  /**
   * Returns the type name.
   */
  public String getTypeName()
  {
    return this.typeName;
  }

  /**
   * Returns the text description of this OpenType instance.
   */
  public String getDescription()
  {
    return this.description;
  }

  /**
   * Returns true if the value is an array.
   */
  public boolean isArray()
  {
    return this.className.startsWith("[");
  }

  /**
   * Returns true if the object is a value for the open type.
   */
  public abstract boolean isValue(Object obj);

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    return getTypeName().hashCode();
  }

  /**
   * Returns the string.
   */
  public String toString()
  {
    return getClass().getName() + "[" + getTypeName() + "]";
  }

  static {
    ALLOWED_CLASSNAMES = new String[] {
      "java.lang.Void",
      "java.lang.Boolean",
      "java.lang.Character",
      "java.lang.Byte",
      "java.lang.Short",
      "java.lang.Integer",
      "java.lang.Long",
      "java.lang.Float",
      "java.lang.Double",
      "java.lang.String",
      "java.math.BigDecimal",
      "java.math.BigInteger",
      "java.util.Date",
      "javax.management.ObjectName.Date",
      "javax.management.openmbean.CompositeData",
      "javax.management.openmbean.TabularData",
    };
  }
}
