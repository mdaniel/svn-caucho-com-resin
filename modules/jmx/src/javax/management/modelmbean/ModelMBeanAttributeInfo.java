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

package javax.management.modelmbean;

import java.lang.reflect.Method;

import javax.management.MBeanAttributeInfo;
import javax.management.DescriptorAccess;
import javax.management.Descriptor;
import javax.management.IntrospectionException;

/**
 * The info for an mbean attribute.
 *
 * <pre>
 * name           : attribute name  
 * descriptorType : must be "attribute"   
 * value          : current value for attribute 
 * default        : default value for attribute 
 * displayName    : name of attribute to be used in displays 
 * getMethod      : name of operation descriptor for get method  
 * setMethod      : name of operation descriptor for set method 
 * protocolMap    : object which implements the ProtocolMap interface:
 *                  map of protocol names and protocol hints 
 * persistPolicy  : Update|OnTime|NoMoreOftenThan|Always|Never  
 * persistPeriod  : seconds - frequency of persist cycle. Used when
 *                  persistPolicy is"OnTime" or "NoMoreOftenThan".  
 * currencyTimeLimit : how long value is valid, <0 never, =0 always, >0 seconds  
 * lastUpdatedTimeStamp : when value was set  
 * iterable       : T - object value supports Iterable interface,
 *                  F - does not support Iterable interface       
 * visibility     : 1-4 where 1: always visible 4: rarely visible
 * presentationString : xml formatted string to allow presentation of data
 * </pre>
 */
public class ModelMBeanAttributeInfo extends MBeanAttributeInfo
  implements DescriptorAccess, Cloneable {
  private Descriptor descriptor;
  
  /**
   * Constructor.
   *
   * @param name the name of the attribute
   * @param description a description of the attribute
   * @param getter the get method
   * @param setter the set method
   */
  public ModelMBeanAttributeInfo(String name, String description,
                                 Method getter, Method setter)
    throws IntrospectionException
  {
    super(name, description, getter, setter);
    
    this.descriptor = new DescriptorSupport();
    if (getter != null)
      this.descriptor.setField("getMethod", getter.getName());
    if (setter != null)
      this.descriptor.setField("setMethod", setter.getName());
  }
  
  /**
   * Constructor.
   *
   * @param name the name of the attribute
   * @param description a description of the attribute
   * @param getter the get method
   * @param setter the set method
   * @param descriptor the descriptor for the attribute
   */
  public ModelMBeanAttributeInfo(String name, String description,
                                 Method getter, Method setter,
                                 Descriptor descriptor)
    throws IntrospectionException
  {
    super(name, description, getter, setter);

    this.descriptor = descriptor;
  }

  /**
   * Constructor.
   *
   * @param name the name of the attribute
   * @param type the type of the attribute
   * @param description a description of the attribute
   * @param isReadable true if the attribute is readable
   * @param isWriteable true if the attribute is writeable
   * @param isIs true if the attribute is a boolean
   */
  public ModelMBeanAttributeInfo(String name, String type, String description,
                                 boolean isReadable, boolean isWritable,
                                 boolean isIs)
  {
    super(name, type, description, isReadable, isWritable, isIs);

    this.descriptor = new DescriptorSupport();
  }

  /**
   * Constructor.
   *
   * @param name the name of the attribute
   * @param type the type of the attribute
   * @param description a description of the attribute
   * @param isReadable true if the attribute is readable
   * @param isWriteable true if the attribute is writeable
   * @param descriptor the descriptor for the attribute
   */
  public ModelMBeanAttributeInfo(String name, String type, String description,
                                 boolean isReadable, boolean isWritable,
                                 boolean isIs, Descriptor descriptor)
  {
    super(name, type, description, isReadable, isWritable, isIs);

    this.descriptor = descriptor;
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
   * Returns a clone for the info
   */
  public Object clone()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a string value.
   */
  public String toString()
  {
    return "ModelMBeanAttributeInfo[name=" + getName() + "]";
  }
}

  
