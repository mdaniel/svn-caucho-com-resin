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

import java.lang.reflect.Constructor;

/**
 * Information about an MBean constructor
 */
public class MBeanConstructorInfo extends MBeanFeatureInfo implements Cloneable {
  private MBeanParameterInfo []signature;
  
  /**
   * Constructor.
   *
   * @param description a description of the attribute
   * @param signature the signature
   */
  public MBeanConstructorInfo(String description, Constructor constructor)
  {
    super(constructor.getName(), description);

    Class []types = constructor.getParameterTypes();

    this.signature = new MBeanParameterInfo[types.length];

    for (int i = 0; i < types.length; i++) {
      MBeanParameterInfo paramInfo;

      paramInfo = new MBeanParameterInfo("a" + i, types[i].getName(), "");
      
      this.signature[i] = paramInfo;
    }
  }
  
  /**
   * Constructor.
   *
   * @param name the name of the attribute
   * @param description a description of the attribute
   * @param signature the signature
   */
  public MBeanConstructorInfo(String name, String description,
                              MBeanParameterInfo []signature)
  {
    super(name, description);

    this.signature = signature;
  }

  /**
   * Returns the signature of the constructor.
   */
  public MBeanParameterInfo []getSignature()
  {
    return this.signature;
  }

  /**
   * Returns a clone of the object.
   */
  public Object clone()
  {
    return this;
  }

  /**
   * Returns true if the features are identical.
   */
  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    MBeanConstructorInfo info = (MBeanConstructorInfo) o;

    if (! this.name.equals(info.name))
      return false;
    else if (this.signature.length != info.signature.length)
      return false;
    else {
      for (int i = this.signature.length - 1; i >= 0; i--) {
	if (! this.signature[i].equals(info.signature[i]))
	  return false;
      }

      return true;
    }
  }
}
