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

import java.lang.reflect.Method;

/**
 * Information about an MBean operation
 */
public class MBeanOperationInfo extends MBeanFeatureInfo implements Cloneable {
  /**
   * Indicates that the operation is read-like.
   */
  public static final int INFO = 0;
  /**
   * Indicates that the operation is write-like
   */
  public static final int ACTION = 1;
  /**
   * Indicates that the operation is read-write-like
   */
  public static final int ACTION_INFO = 2;
  /**
   * Indicates that the operation is unknown
   */
  public static final int UNKNOWN = 3;
  
  private MBeanParameterInfo []signature;
  private String type;
  private int impact;
  
  /**
   * Operation.
   *
   * @param description a description of the operation
   * @param method the method
   */
  public MBeanOperationInfo(String description, Method method)
  {
    super(method.getName(), description);

    this.type = method.getReturnType().getName();
    this.impact = UNKNOWN;

    Class []param = method.getParameterTypes();

    signature = new MBeanParameterInfo[param.length];
    for (int i = 0; i < param.length; i++) {
      signature[i] = new MBeanParameterInfo("a" + i,
                                            param[i].getName(),
                                            param[i].getName());
    }
  }
  
  /**
   * Operation.
   *
   * @param name the name of the attribute
   * @param description a description of the attribute
   * @param signature the signature
   */
  public MBeanOperationInfo(String name, String description,
                            MBeanParameterInfo []signature,
                            String type,
                            int impact)
  {
    super(name, description);

    this.signature = signature;
    this.type = type;
    this.impact = impact;
  }

  /**
   * Returns the signature of the operation.
   */
  public MBeanParameterInfo []getSignature()
  {
    return this.signature;
  }

  /**
   * Returns the return type.
   */
  public String getReturnType()
  {
    return this.type;
  }

  /**
   * Returns the method's impact.
   */
  public int getImpact()
  {
    return this.impact;
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

    MBeanOperationInfo info = (MBeanOperationInfo) o;

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
