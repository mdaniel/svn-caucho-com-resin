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

import java.lang.reflect.Constructor;

import javax.management.MBeanConstructorInfo;
import javax.management.MBeanParameterInfo;
import javax.management.Descriptor;
import javax.management.DescriptorAccess;

/**
 * Information about an MBean constructor
 *
 * <pre>
 * name               : operation name
 * descriptorType     : must be "operation"
 * role               : must be "constructor"
 * displayName        : human readable name of constructor
 * class              : class where method is defined (fully qualified)
 * visibility         : 1-4 where 1: always visible 4: rarely visible
 * presentationString : xml formatted string to describe how to
 *                      present operation
 * </pre>
 */
public class ModelMBeanConstructorInfo extends MBeanConstructorInfo
  implements DescriptorAccess {
  private Descriptor descriptor;
  
  /**
   * Constructor.
   *
   * @param description the constructor description
   * @param constructorMethod the constructor method
   */
  public ModelMBeanConstructorInfo(String description,
                                   Constructor constructorMethod)
  {
    super(description, constructorMethod);
  }
  
  /**
   * Constructor.
   *
   * @param description the constructor description
   * @param constructorMethod the constructor method
   * @param descriptor the descriptor
   */
  public ModelMBeanConstructorInfo(String description,
                                   Constructor constructorMethod,
                                   Descriptor descriptor)
  {
    super(description, constructorMethod);

    this.descriptor = descriptor;
  }
  
  /**
   * Constructor.
   *
   * @param name the constructor name
   * @param description the constructor description
   * @param signature the signature
   */
  public ModelMBeanConstructorInfo(String name,
                                   String description,
                                   MBeanParameterInfo []signature)
  {
    super(name, description, signature);
  }
  
  /**
   * Constructor.
   *
   * @param name the constructor name
   * @param description the constructor description
   * @param signature the signature
   * @param descriptor the descriptor
   */
  public ModelMBeanConstructorInfo(String name,
                                   String description,
                                   MBeanParameterInfo []signature,
                                   Descriptor descriptor)
  {
    super(name, description, signature);

    this.descriptor = descriptor;
  }

  /**
   * Returns a clone.
   */
  public Object clone()
  {
    return new ModelMBeanConstructorInfo(getName(), getDescription(),
                                         getSignature(), getDescriptor());
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
    return "ModelMBeanConstructorInfo[name=" + getName() + "]";
  }
}
