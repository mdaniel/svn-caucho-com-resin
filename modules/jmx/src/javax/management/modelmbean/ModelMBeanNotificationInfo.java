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

import javax.management.MBeanNotificationInfo;
import javax.management.Descriptor;
import javax.management.DescriptorAccess;

/**
 * Information about an MBean notification
 *
 * <pre>
 * name : notification name
 * descriptorType : must be "notification"
 * severity : 1-5 where 1: fatal 2: severe 3: error 4: warn 5: info
 * messageID : unique key for message text (to allow translation,analysis)
 * messageText : text of notification
 * log : T - log message F - do not log message
 * logfile : string fully qualified file name appropriate for operating system
 * visibility : 1-4 where 1: always visible 4: rarely visible
 * presentationString : xml formatted string to allow presentation of data
 * </pre>
 */
public class ModelMBeanNotificationInfo extends MBeanNotificationInfo
  implements DescriptorAccess {
  private Descriptor descriptor;
  
  /**
   * Notification.
   *
   * @param info information about a notification
   * @param method the method
   */
  public ModelMBeanNotificationInfo(ModelMBeanNotificationInfo info)
  {
    super(info.getNotifTypes(), info.getName(), info.getDescription());

    descriptor = info.getDescriptor();
  }
  
  /**
   * Notification.
   *
   * @param notifTypes the notification types
   * @param name the notification name
   * @param description the description
   */
  public ModelMBeanNotificationInfo(String []notifTypes,
                                    String name,
                                    String description)
  {
    super(notifTypes, name, description);
  }
  
  /**
   * Notification.
   *
   * @param notifTypes the notification types
   * @param name the notification name
   * @param description the description
   * @param descriptor the descriptor
   */
  public ModelMBeanNotificationInfo(String []notifTypes,
                                    String name,
                                    String description,
                                    Descriptor descriptor)
  {
    super(notifTypes, name, description);

    this.descriptor = descriptor;
  }

  /**
   * Returns a clong.
   */
  public Object clone()
  {
    return new ModelMBeanNotificationInfo(this);
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
    return "ModelMBeanNotificationInfo[name=" + getName() + "]";
  }
}
