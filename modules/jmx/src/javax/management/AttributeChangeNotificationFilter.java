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

import java.util.Vector;

/**
 * Represents a filter for an attribute change notification event.
 */
public class AttributeChangeNotificationFilter
  implements NotificationFilter, java.io.Serializable {
  private Vector enabledAttributes;
  
  /**
   * Create an AttributeChangeNotification filter.
   */
  public AttributeChangeNotificationFilter()
  {
    enabledAttributes = new Vector();
  }
  
  /**
   * Disables all attributesn.
   *
   * @param name the attribute to disable.
   */
  public void disableAllAttributes()
  {
    enabledAttributes.clear();
  }
  
  /**
   * Disable an attribute.
   *
   * @param name the attribute to disable.
   */
  public void disableAttribute(String name)
  {
    enabledAttributes.remove(name);
  }
  
  /**
   * Enable an attribute.
   *
   * @param name the attribute to enable.
   */
  public void enableAttribute(String name)
  {
    if (! enabledAttributes.contains(name))
      enabledAttributes.add(name);
  }
  
  /**
   * Return the enabled attibute names.
   *
   * @return the attribute name vector.
   */
  public Vector getEnabledAttributes()
  {
    return enabledAttributes;
  }

  /**
   * Returns true if this attribute is on we're interested in.
   */
  public boolean isNotificationEnabled(Notification notification)
  {
    if (! (notification instanceof AttributeChangeNotification))
      return false;

    AttributeChangeNotification change =
      (AttributeChangeNotification) notification;

    return enabledAttributes.contains(change.getAttributeName());
  }
}
