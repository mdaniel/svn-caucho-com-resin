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

/**
 * API for a MBeanServer
 */
public class MBeanServerDelegate extends NotificationBroadcasterSupport
  implements MBeanServerDelegateMBean {
  private static final MBeanNotificationInfo []_notifications;
  
  /**
   * Return the MBean server agent id.
   */
  public String getMBeanServerId()
  {
    return "Resin-User-JMX";
  }
  
  /**
   * Return the specification name.
   */
  public String getSpecificationName()
  {
    return "Java Management Extension";
  }
  
  /**
   * Return the specification vendor.
   */
  public String getSpecificationVendor()
  {
    return "Sun Microsystems";
  }
  
  /**
   * Return the specification version.
   */
  public String getSpecificationVersion()
  {
    return "1.2 Maintenance Release";
  }
  
  /**
   * Return the implementation name.
   */
  public String getImplementationName()
  {
    return "Resin";
  }
  
  /**
   * Return the implementation vendor.
   */
  public String getImplementationVendor()
  {
    return "Caucho Technology";
  }
  
  /**
   * Return the implementation version.
   */
  public String getImplementationVersion()
  {
    return "3.0";
  }

  /**
   * Returns the notification info.
   */
  public MBeanNotificationInfo []getNotificationInfo()
  {
    return _notifications;
  }

  static {
    _notifications = new MBeanNotificationInfo[] {
      new MBeanNotificationInfo(new String[] {"JMX.mbean.registered",
					      "JMX.mbean.unregistered"},
				MBeanServerNotification.class.getName(),
				"MBeanServer notifications")
    };
  }
}
