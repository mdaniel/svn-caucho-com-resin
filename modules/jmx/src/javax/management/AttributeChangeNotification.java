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
 * Represents a notification event.
 */
public class AttributeChangeNotification extends Notification {
  public static final String ATTRIBUTE_CHANGE = "jmx.attribute.change";

  private String attributeName;
  private String attributeType;
  private Object newValue;
  private Object oldValue;
  
  /**
   * Create a notification object.
   *
   * @param source the mbean for the application
   * @param sequenceNumber the notification sequence
   * @param timeStamp the notification timestamp
   * @param message the notification message
   * @param attributeName the name of the attribute that changed
   * @param attributeType the name of the attribute that changed
   * @param oldValue the old value of the attribute
   * @param newValue the new value of the attribute
   */
  public AttributeChangeNotification(Object source,
                                     long sequenceNumber,
                                     long timeStamp,
                                     String message,
                                     String attributeName,
                                     String attributeType,
                                     Object oldValue,
                                     Object newValue)
  {
    super(ATTRIBUTE_CHANGE, source, sequenceNumber, timeStamp, message);
    
    this.attributeName = attributeName;
    this.attributeType = attributeType;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  /**
   * Returns the name of the attribute which changed.
   */
  public String getAttributeName()
  {
    return this.attributeName;
  }

  /**
   * Returns the type of the attribute which changed.
   */
  public String getAttributeType()
  {
    return this.attributeType;
  }

  /**
   * Returns the old attribute value.
   */
  public Object getOldValue()
  {
    return this.oldValue;
  }

  /**
   * Returns the new attribute value.
   */
  public Object getNewValue()
  {
    return this.newValue;
  }
}
