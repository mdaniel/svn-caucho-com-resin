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
public class Notification extends java.util.EventObject {
  // The classname of the MBean for the notification.
  private String type;
  // The notification sequence number
  private long sequenceNumber;
  // The timestamp of the notification
  private long timeStamp;
  // The notification message
  private String message;
  // UserData
  private Object userData;
  
  /**
   * Create a notification object.
   *
   * @param type the classname of the object 
   * @param source the source of the notification
   * @param sequenceNumber the notification sequence
   * @param timeStamp the notification timestamp
   * @param message the notification message
   */
  public Notification(String type,
                      Object source,
                      long sequenceNumber,
                      long timeStamp,
                      String message)
  {
    super(source);
    
    this.type = type;
    this.sequenceNumber = sequenceNumber;
    this.timeStamp = timeStamp;
    this.message = message;
  }
  
  /**
   * Create a notification object.
   *
   * @param type the classname of the object 
   * @param source the source of the notification
   * @param sequenceNumber the notification sequence
   * @param timeStamp the notification timestamp
   */
  public Notification(String type,
                      Object source,
                      long sequenceNumber,
                      long timeStamp)
  {
    super(source);
    
    this.type = type;
    this.sequenceNumber = sequenceNumber;
    this.timeStamp = timeStamp;
  }
  
  /**
   * Create a notification object.
   *
   * @param type the classname of the object 
   * @param source the source of the notification
   * @param sequenceNumber the notification sequence
   */
  public Notification(String type,
                      Object source,
                      long sequenceNumber)
  {
    super(source);
    
    this.type = type;
    this.sequenceNumber = sequenceNumber;
  }
  /**
   * Create a notification object.
   *
   * @param type the classname of the object 
   * @param source the source of the notification
   * @param sequenceNumber the notification sequence
   * @param message the notification message
   */
  public Notification(String type,
                      Object source,
                      long sequenceNumber,
                      String message)
  {
    super(source);
    
    this.type = type;
    this.sequenceNumber = sequenceNumber;
    this.message = message;
  }

  /**
   * Sets the message.
   */
  public String getMessage()
  {
    return message;
  }

  /**
   * Returns the type.
   */
  public String getType()
  {
    return type;
  }

  /**
   * Returns the sequence number.
   */
  public long getSequenceNumber()
  {
    return sequenceNumber;
  }

  /**
   * Returns the timestamp.
   */
  public long getTimeStamp()
  {
    return timeStamp;
  }

  /**
   * Returns the userData.
   */
  public Object getUserData()
  {
    return userData;
  }

  /**
   * Sets the type.
   */
  public void setType(String type)
  {
    this.type = type;
  }

  /**
   * Sets the source.
   */
  public void setSource(Object source)
  {
    this.source = source;
  }

  /**
   * Sets the sequence number.
   */
  public void setSequenceNumber(long sequenceNumber)
  {
    this.sequenceNumber = sequenceNumber;
  }

  /**
   * Sets the timestamp.
   */
  public void setTimeStamp(long timeStamp)
  {
    this.timeStamp = timeStamp;
  }

  /**
   * Sets the userData.
   */
  public void setUserData(Object userData)
  {
    this.userData = userData;
  }
}
