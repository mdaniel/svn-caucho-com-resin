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

import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;
import javax.management.Notification;
import javax.management.Attribute;
import javax.management.AttributeChangeNotification;

import javax.management.MBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.ListenerNotFoundException;

/**
 * Notification broadcasters for the mbean.
 */
public interface ModelMBeanNotificationBroadcaster
  extends NotificationBroadcaster {
  /**
   * Sends a notification.
   *
   * @param notification object to be passed to the listener.
   */
  public void sendNotification(Notification notification)
    throws MBeanException, RuntimeOperationsException;
  
  /**
   * Sends notification text.
   *
   * The type will be jmx.modelmbean.general
   *
   * @param text text of notification to be passed to the listener.
   */
  public void sendNotification(String text)
    throws MBeanException, RuntimeOperationsException;
  
  /**
   * Sends an attribute change notification
   *
   * @param notification notification to be passed to the listener.
   */
  public void
    sendAttributeChangeNotification(AttributeChangeNotification notification)
    throws MBeanException, RuntimeOperationsException;
  
  /**
   * Sends an attribute change notification
   *
   * The type will be jmx.attribute.change
   *
   * @param oldValue the old attribute binding.
   * @param newValue the new attribute binding.
   */
  public void sendAttributeChangeNotification(Attribute oldValue,
                                              Attribute newValue)
    throws MBeanException, RuntimeOperationsException;
  
  /**
   * Adds a new listener
   *
   * @param listener the new listener
   * @param attributeName the attribute to listen
   * @param handback the handback
   */
  public void
    addAttributeChangeNotificationListener(NotificationListener listener,
					   String attributeName,
					   Object handBack)
    throws MBeanException, RuntimeOperationsException;
  
  /**
   * Remove a listener
   *
   * @param listener the new listener
   * @param attributeName the attribute to listen
   */
  public void
    removeAttributeChangeNotificationListener(NotificationListener listener,
					      String attributeName)
    throws MBeanException, RuntimeOperationsException,
	   ListenerNotFoundException;
}

  
