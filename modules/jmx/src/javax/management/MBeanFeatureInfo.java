/*
 * Copyright (c) 1998-2002 Caucho Technology -- all rights reserved
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

/**
 * Information about an MBean feature.
 */
public class MBeanFeatureInfo implements java.io.Serializable {
  protected String name;
  protected String description;
  
  /**
   * Constructor.
   *
   * @param name the name of the feature
   * @param description a description of the feature
   */
  public MBeanFeatureInfo(String name,
                          String description)
  {
    this.name = name;
    this.description = description;
  }

  /**
   * Returns the name of the feature.
   */
  public String getName()
  {
    return this.name;
  }

  /**
   * Returns the description of the feature
   */
  public String getDescription()
  {
    return this.description;
  }

  /**
   * Returns true if the features are identical.
   */
  public int hashCode()
  {
    return this.name.hashCode();
  }

  /**
   * Returns true if the features are identical.
   */
  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    MBeanFeatureInfo info = (MBeanFeatureInfo) o;

    return (this.name.equals(info.name));
  }
}
