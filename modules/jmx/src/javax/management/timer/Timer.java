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
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Date;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.management.*;

import com.caucho.jmx.JobManager;

/**
 * Timer interface for MBeans.
 */
public class Timer extends NotificationBroadcasterSupport
  implements TimerMBean, MBeanRegistration {
  private final static Logger log = Logger.getLogger(Timer.class.getName());
  
  public final static long ONE_SECOND = 1000L;
  public final static long ONE_MINUTE = 60 * ONE_SECOND;
  public final static long ONE_HOUR = 60 * ONE_MINUTE;
  public final static long ONE_DAY = 24 * ONE_HOUR;
  public final static long ONE_WEEK = 7 * ONE_DAY;

  private int _notificationId;

  private ArrayList _notifications = new ArrayList();
  private ArrayList _runJobs = new ArrayList();

  private boolean _isSendPastNotifications;

  private long _sequenceNumber;

  private boolean _isActive;

  /**
   * The job task.
   */
  private Job _job;

  public Timer()
  {
    _job = new Job(this);
  }
  
  /**
   * Start the timer.
   */
  public void start()
  {
    if (_isActive)
      return;
    // should check for old tasks
    _isActive = true;

    queue();
  }
  
  /**
   * Stop the timer.
   */
  public void stop()
  {
    _isActive = false;
    cancelTimer();
  }

  private void cancelTimer()
  {
    JobManager.dequeue(_job);
  }

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
                                 long nbOccurences,
				 boolean fixedRate)
    throws IllegalArgumentException
  {
    Integer value;
    
    synchronized (this) {
      TimerJob job = new TimerJob(generateId(), type, message,
				  userData, date, period, nbOccurences,
				  fixedRate);
      
      _notifications.add(job);

      value = new Integer(job.getId());
    }
    
    queue();

    return value;
  }

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
    throws IllegalArgumentException
  {
    return addNotification(type, message, userData, date,
			   period, nbOccurences, true);
  }

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
    throws IllegalArgumentException
  {
    return addNotification(type, message, userData, date,
			   period, Long.MAX_VALUE / 2, true);
  }

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
    throws IllegalArgumentException
  {
    return addNotification(type, message, userData, date, 0, 1, true);
  }

  /**
   * generates the id.
   */
  private int generateId()
  {
    synchronized (this) {
      return _notificationId++;
    }
  }

  /**
   * Removes a notification for the timer.
   *
   * @param id the notification to remove
   */
  public void removeNotification(Integer idObj)
    throws InstanceNotFoundException
  {
    int id = idObj.intValue();
    
    synchronized (this) {
      for (int i = _notifications.size() - 1; i >= 0; i--) {
	TimerJob job = (TimerJob) _notifications.get(i);

	if (job.getId() == id) {
	  _notifications.remove(i);
	  return;
	}
      }
    }
    
    throw new InstanceNotFoundException(id + " is an unknown timer notification");
  }

  /**
   * Removes notifications for the timer.
   *
   * @param type the notification types remove
   */
  public void removeNotifications(String type)
    throws InstanceNotFoundException
  {
    synchronized (this) {
      for (int i = _notifications.size() - 1; i >= 0; i--) {
	TimerJob job = (TimerJob) _notifications.get(i);

	if (job.getType().equals(type))
	  _notifications.remove(i);
      }
    }
  }

  /**
   * Removes all notifications for the timer.
   */
  public void removeAllNotifications()
  {
    synchronized (this) {
      _notifications.clear();
      _notificationId = 0;
      
      cancelTimer();
    }
  }

  /**
   * Returns the number of timer notifications.
   */
  public int getNbNotifications()
  {
    return _notifications.size();
  }

  /**
   * Returns a list of notification ids.
   */
  public Vector getAllNotificationIDs()
  {
    Vector ids = new Vector();

    synchronized (this) {
      for (int i = _notifications.size() - 1; i >= 0; i--) {
	TimerJob job = (TimerJob) _notifications.get(i);

	ids.add(new Integer(job.getId()));
      }
    }

    return ids;
  }

  /**
   * Returns a list of notification ids matching the given type.
   */
  public Vector getNotificationIDs(String type)
  {
    Vector ids = new Vector();

    synchronized (this) {
      for (int i = _notifications.size() - 1; i >= 0; i--) {
	TimerJob job = (TimerJob) _notifications.get(i);

	if (job.getType().equals(type))
	  ids.add(new Integer(job.getId()));
      }
    }

    return ids;
  }

  /**
   * Returns the notification type for a timer.
   */
  public String getNotificationType(Integer id)
  {
    TimerJob job = getNotification(id);

    if (job == null)
      return null;
    
    return job.getType();
  }

  /**
   * Returns the notification message for a timer.
   */
  public String getNotificationMessage(Integer id)
  {
    TimerJob job = getNotification(id);

    if (job == null)
      return null;
    
    return job.getMessage();
  }

  /**
   * Returns the notification user data for a timer.
   */
  public Object getNotificationUserData(Integer id)
  {
    TimerJob job = getNotification(id);

    if (job == null)
      return null;
    
    return job.getUserData();
  }

  /**
   * Returns the notification date for a timer.
   */
  public Date getDate(Integer id)
  {
    TimerJob job = getNotification(id);

    if (job == null)
      return null;
    
    return job.getDate();
  }

  /**
   * Returns the notification period for a timer.
   */
  public Long getPeriod(Integer id)
  {
    TimerJob job = getNotification(id);

    if (job == null)
      return null;
    
    return new Long(job.getPeriod());
  }

  /**
   * Returns the notification occurences for a timer.
   */
  public Long getNbOccurences(Integer id)
  {
    TimerJob job = getNotification(id);

    if (job == null)
      return null;
    
    return new Long(job.getNbOccurences());
  }

  /**
   * Returns true if the timer sends past notifications.
   */
  public boolean getSendPastNotifications()
  {
    return _isSendPastNotifications;
  }

  /**
   * Set true if the timer sends past notifications.
   */
  public void setSendPastNotifications(boolean value)
  {
    _isSendPastNotifications = value;
  }

  /**
   * Returns true if the timer is fixed-rate.
   */
  public Boolean getFixedRate(Integer id)
  {
    TimerJob job = getNotification(id);

    if (job == null)
      return null;
    
    return new Boolean(job.getFixedRate());
  }

  /**
   * Removes a notification for the timer.
   *
   * @param id the notification to remove
   */
  private TimerJob getNotification(Integer idObj)
  {
    int id = idObj.intValue();
    
    synchronized (this) {
      for (int i = _notifications.size() - 1; i >= 0; i--) {
	TimerJob job = (TimerJob) _notifications.get(i);

	if (job.getId() == id)
	  return job;
      }
    }

    return null;
  }

  /**
   * Return true if the timer is active.
   */
  public boolean isActive()
  {
    return _isActive;
  }

  /**
   * Return true if there are no notifications.
   */
  public boolean isEmpty()
  {
    return _notifications.size() == 0;
  }
  
  /**
   * Called before the registration.
   *
   * @param server the mbean server to be registered
   * @param name the client's name to be registered
   *
   * @return the name the object wans the be registered as
   */
  public ObjectName preRegister(MBeanServer server,
                                ObjectName name)
    throws Exception
  {
    return name;
  }
  
  /**
   * Called after the registration.
   *
   * @param registrationDone true if the registration was successful.
   */
  public void postRegister(Boolean registrationDone)
  {
  }
  
  /**
   * Called before deregistration.
   */
  public void preDeregister()
    throws Exception
  {
    stop();
  }
  
  /**
   * Called after the deregistration.
   */
  public void postDeregister()
  {
  }

  private void queue()
  {
    if (! _isActive)
      return;
    
    run();
    
    long time = nextTime();

    if (time < Long.MAX_VALUE)
      JobManager.queueAbsolute(_job, time);
  }
  
  /**
   * Returns the next time.
   */
  long nextTime()
  {
    long now = JobManager.getCurrentTime();
    
    long nextTime = Long.MAX_VALUE;

    synchronized (this) {
      for (int i = _notifications.size() - 1; i >= 0; i--) {
	TimerJob job = (TimerJob) _notifications.get(i);

	long next = job.nextTime();

	if (next == Long.MAX_VALUE)
	  _notifications.remove(i);
	else if (next < nextTime)
	  nextTime = next;
      }
    }

    return nextTime;
  }

  /**
   * Runs the timer.
   */
  void run()
  {
    _runJobs.clear();

    long now = JobManager.getCurrentTime();

    synchronized (this) {
      for (int i = _notifications.size() - 1; i >= 0; i--) {
	TimerJob job = (TimerJob) _notifications.get(i);

	long nextTime = job.nextTime();

	if (Long.MAX_VALUE / 2 <= nextTime)
	  _notifications.remove(i);
	else if (nextTime <= now) {
	  _runJobs.add(job);
	  job.setLastTime(now);
	  if (Long.MAX_VALUE / 2 <= job.nextTime())
	    _notifications.remove(i);
	}
      }
    }

    for (int i = _runJobs.size() - 1; i >= 0; i--) {
      TimerJob job = (TimerJob) _runJobs.get(i);

      TimerNotification notif;

      notif = new TimerNotification(job.getType(), this,
				    _sequenceNumber++, now, job.getMessage(),
				    new Integer(job.getId()));
      sendNotification(notif);
    }
  }

  static class TimerJob {
    private int _id;
    private String _type;
    private String _message;
    private Object _userData;
    private Date _date;
    private long _startDate;
    private long _period;
    private long _nbOccurences;
    private boolean _fixedRate;

    private long _lastTime;

    TimerJob(int id,
	     String type, String message, Object userData,
	     Date date, long period, long nbOccurences, boolean fixedRate)
    {
      _id = id;
      _type = type;
      _message = message;
      _userData = userData;
      _date = date;
      _startDate = date.getTime();
      _period = period;
      _nbOccurences = nbOccurences;
      _fixedRate = fixedRate;

      _lastTime = JobManager.getCurrentTime();
    }

    int getId()
    {
      return _id;
    }

    String getType()
    {
      return _type;
    }

    String getMessage()
    {
      return _message;
    }

    Object getUserData()
    {
      return _userData;
    }

    Date getDate()
    {
      return _date;
    }

    long getPeriod()
    {
      return _period;
    }

    long getNbOccurences()
    {
      return _nbOccurences;
    }

    boolean getFixedRate()
    {
      return _fixedRate;
    }

    long getLastTime()
    {
      return _lastTime;
    }

    void setLastTime(long lastTime)
    {
      _lastTime = lastTime;
    }

    long nextTime()
    {
      if (_lastTime < _startDate)
	return _startDate;
      else if (_nbOccurences < 0 || Long.MAX_VALUE / 2 <= _nbOccurences)
	return _lastTime + _period;
      else if (_lastTime < _startDate + _period * _nbOccurences)
	return _lastTime + _period;
      else
	return Long.MAX_VALUE;
    }
  }

  static class Job extends TimerTask {
    private Timer _timer;

    Job(Timer timer)
    {
      _timer = timer;
    }
    
    public void run()
    {
      try {
	_timer.run();
      } finally {
	try {
	  if (_timer.isActive()) {
	    long nextTime = _timer.nextTime();

	    if (nextTime < Long.MAX_VALUE / 2)
	      JobManager.queueAbsolute(this, nextTime);
	  }
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }
    }
  }
}
