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

package javax.management;

import java.lang.reflect.Method;

/**
 * Information about an MBean attribute.
 */
public class MBeanAttributeInfo extends MBeanFeatureInfo implements Cloneable {
  private String type;
  private boolean isReadable;
  private boolean isWritable;
  private boolean isIs;
  
  /**
   * Constructor.
   *
   * @param name the name of the attribute
   * @param description a description of the attribute
   * @param getter the get method
   * @param setter the set method
   */
  public MBeanAttributeInfo(String name, String description,
                            Method getter, Method setter)
    throws IntrospectionException
  {
    super(name, description);

    this.isReadable = getter != null;
    this.isWritable = setter != null;
    this.isIs = isReadable && getter.getName().startsWith("is");

    if (getter != null)
      type = getter.getReturnType().getName();
    else
      type = setter.getParameterTypes()[0].getName();
  }

  /**
   * Constructor.
   *
   * @param name the name of the attribute
   * @param type the type of the attribute
   * @param description a description of the attribute
   * @param getter the get method
   * @param setter the set method
   */
  public MBeanAttributeInfo(String name, String type, String description,
                            boolean isReadable, boolean isWritable,
                            boolean isIs)
  {
    super(name, description);

    this.type = type;
    this.isReadable = isReadable;
    this.isWritable = isWritable;
    this.isIs = isIs;
  }

  /**
   * Returns the className of the attribute.
   */
  public String getType()
  {
    return this.type;
  }

  /**
   * Returns true if the attribute is readable.
   */
  public boolean isReadable()
  {
    return this.isReadable;
  }

  /**
   * Returns true if the attribute is writable
   */
  public boolean isWritable()
  {
    return this.isWritable;
  }

  /**
   * Returns true if the attribute has an "is" getter.
   */
  public boolean isIs()
  {
    return this.isIs;
  }
}
