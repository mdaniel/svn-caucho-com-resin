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

package javax.management.modelmbean;

import java.lang.reflect.Method;

import javax.management.MBeanOperationInfo;
import javax.management.Descriptor;
import javax.management.DescriptorAccess;
import javax.management.MBeanParameterInfo;


/**
 * Information about an MBean operation
 */
public class ModelMBeanOperationInfo extends MBeanOperationInfo
  implements DescriptorAccess {
  private Descriptor descriptor;
  
  /**
   * Operation.
   *
   * @param description a description of the operation
   * @param method the method
   */
  public ModelMBeanOperationInfo(String description, Method method)
  {
    super(description, method);
  }
  
  /**
   * Operation.
   *
   * @param description a description of the operation
   * @param method the method
   * @param descriptor the descriptor
   */
  public ModelMBeanOperationInfo(String description, Method method,
                                 Descriptor descriptor)
  {
    super(description, method);

    this.descriptor = descriptor;
  }
  
  /**
   * Operation.
   *
   * @param name the name of the attribute
   * @param description a description of the attribute
   * @param signature the signature
   */
  public ModelMBeanOperationInfo(String name, String description,
                                 MBeanParameterInfo []signature,
                                 String type,
                                 int impact)
  {
    super(name, description, signature, type, impact);
  }
  
  /**
   * Operation.
   *
   * @param name the name of the attribute
   * @param description a description of the attribute
   * @param signature the signature
   */
  public ModelMBeanOperationInfo(String name, String description,
                                 MBeanParameterInfo []signature,
                                 String type,
                                 int impact,
                                 Descriptor descriptor)
  {
    super(name, description, signature, type, impact);

    this.descriptor = descriptor;
  }

  /**
   * Returns a clong.
   */
  public Object clone()
  {
    return new ModelMBeanOperationInfo(getName(), getDescription(),
                                       getSignature(), getReturnType(), getImpact(),
                                       getDescriptor());
  }

  /**
   * Returns the descriptor.
   */
  public Descriptor getDescriptor()
  {
    return descriptor;
  }

  /**
   * Sets the descriptor.
   */
  public void setDescriptor(Descriptor descriptor)
  {
    this.descriptor = descriptor;
  }

  /**
   * Returns a printable version.
   */
  public String toString()
  {
    return "ModelMBeanOperationInfo[name=" + getName() + "]";
  }
}
