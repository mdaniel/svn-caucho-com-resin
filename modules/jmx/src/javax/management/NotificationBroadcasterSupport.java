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

import java.util.ArrayList;

/**
 * Represents an MBean which supports broadcast events
 */
public class NotificationBroadcasterSupport
  implements NotificationEmitter {
  // array list with the listeners
  private ArrayList _listenerList;
  
  /**
   * Adds a new listener.
   *
   * @param listener the new listener
   * @param filter a filter for the listener
   * @param handback the handback
   */
  public void addNotificationListener(NotificationListener listener,
                                      NotificationFilter filter,
                                      Object handback)
    throws IllegalArgumentException
  {
    if (_listenerList == null)
      _listenerList = new ArrayList();

    _listenerList.add(new Listener(listener, filter, handback));
  }
  
  /**
   * Removes a listener.
   *
   * @param listener the listener to remove
   */
  public void removeNotificationListener(NotificationListener listener)
    throws ListenerNotFoundException
  {
    if (_listenerList == null)
      return;
    
    for (int i = _listenerList.size() - 1; i >= 0; i--) {
      Listener callback = (Listener) _listenerList.get(i);

      if (callback._listener == listener)
        _listenerList.remove(i);
    }
  }
  
  /**
   * Removes a listener.
   *
   * @param listener the listener to remove
   */
  public void removeNotificationListener(NotificationListener listener,
					 NotificationFilter filter,
					 Object handback)
    throws ListenerNotFoundException
  {
    if (_listenerList == null)
      return;
    
    for (int i = _listenerList.size() - 1; i >= 0; i--) {
      Listener callback = (Listener) _listenerList.get(i);

      if (callback._listener == listener)
        _listenerList.remove(i);
    }
  }

  /**
   * Returns information about the notification sent.
   */
  public MBeanNotificationInfo []getNotificationInfo()
  {
    return new MBeanNotificationInfo[0];
  }

  /**
   * Send a notification.
   */
  public void sendNotification(Notification notification)
  {
    if (_listenerList == null)
      return;
    
    for (int i = 0; i < _listenerList.size(); i++) {
      Listener callback = (Listener) _listenerList.get(i);

      if (callback._filter == null ||
          callback._filter.isNotificationEnabled(notification)) {
	handleNotification(callback._listener,
			   notification,
			   callback._handback);
      }
    }
  }

  protected void handleNotification(NotificationListener listener,
				    Notification notif,
				    Object handback)
  {
    listener.handleNotification(notif, handback);
  }

  /**
   * Represents a single listener.
   */
  static class Listener {
    NotificationListener _listener;
    NotificationFilter _filter;
    Object _handback;

    Listener(NotificationListener listener,
             NotificationFilter filter,
             Object handback)
    {
      _listener = listener;
      _filter = filter;
      _handback = handback;
    }
  }
}
