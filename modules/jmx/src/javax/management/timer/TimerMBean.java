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

package javax.management.timer;

import java.util.Date;
import java.util.Vector;

import javax.management.*;

/**
 * Timer interface for MBeans.
 */
public interface TimerMBean {
  /**
   * Start the timer.
   */
  public void start();
  
  /**
   * Stop the timer.
   */
  public void stop();

  /**
   * Adds a notification for the timer.
   *
   * @param type the notification type
   * @param message the notification message
   * @param userData user data for the notification
   * @param date the start date for the notification
   * @param period how often the notification should be repeated.
   * @param nbOccurences how many notifications should be sent.
   * @param fixedRate fixed rate or fixed delay
   */
  public Integer addNotification(String type,
                                 String message,
                                 Object userData,
                                 Date date,
                                 long period,
                                 long nbOccurences,
				 boolean fixedRate)
    throws IllegalArgumentException;

  /**
   * Adds a notification for the timer.
   *
   * @param type the notification type
   * @param message the notification message
   * @param userData user data for the notification
   * @param date the start date for the notification
   * @param period how often the notification should be repeated.
   * @param nbOccurences how many notifications should be sent.
   */
  public Integer addNotification(String type,
                                 String message,
                                 Object userData,
                                 Date date,
                                 long period,
                                 long nbOccurences)
    throws IllegalArgumentException;

  /**
   * Adds a notification for the timer.
   *
   * @param type the notification type
   * @param message the notification message
   * @param userData user data for the notification
   * @param date the start date for the notification
   * @param period how often the notification should be repeated.
   */
  public Integer addNotification(String type,
                                 String message,
                                 Object userData,
                                 Date date,
                                 long period)
    throws IllegalArgumentException;

  /**
   * Adds a notification for the timer.
   *
   * @param type the notification type
   * @param message the notification message
   * @param userData user data for the notification
   * @param date the start date for the notification
   * @param period how often the notification should be repeated.
   */
  public Integer addNotification(String type,
                                 String message,
                                 Object userData,
                                 Date date)
    throws IllegalArgumentException;

  /**
   * Removes a notification for the timer.
   *
   * @param id the notification to remove
   */
  public void removeNotification(Integer id)
    throws InstanceNotFoundException;

  /**
   * Removes notifications for the timer.
   *
   * @param type the notification types remove
   */
  public void removeNotifications(String type)
    throws InstanceNotFoundException;

  /**
   * Removes all notifications for the timer.
   */
  public void removeAllNotifications();

  /**
   * Returns the number of timer notifications.
   */
  public int getNbNotifications();

  /**
   * Returns a list of notification ids.
   */
  public Vector getAllNotificationIDs();

  /**
   * Returns a list of notification ids matching the given type.
   */
  public Vector getNotificationIDs(String type);

  /**
   * Returns the notification type for a timer.
   */
  public String getNotificationType(Integer id);

  /**
   * Returns the notification message for a timer.
   */
  public String getNotificationMessage(Integer id);

  /**
   * Returns the notification user data for a timer.
   */
  public Object getNotificationUserData(Integer id);

  /**
   * Returns the notification date for a timer.
   */
  public Date getDate(Integer id);

  /**
   * Returns the notification period for a timer.
   */
  public Long getPeriod(Integer id);

  /**
   * Returns the notification occurences for a timer.
   */
  public Long getNbOccurences(Integer id);

  /**
   * Returns true if the timer is fixed-rate.
   */
  public Boolean getFixedRate(Integer id);

  /**
   * Returns true if the timer sends past notifications.
   */
  public boolean getSendPastNotifications();

  /**
   * Set true if the timer sends past notifications.
   */
  public void setSendPastNotifications(boolean value);

  /**
   * Return true if the timer is active.
   */
  public boolean isActive();

  /**
   * Return true if there are no notifications.
   */
  public boolean isEmpty();
}
