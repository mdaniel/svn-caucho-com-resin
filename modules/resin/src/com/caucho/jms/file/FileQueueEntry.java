/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jms.file;

import java.lang.ref.*;

import java.io.Serializable;

import com.caucho.util.Alarm;

/**
 * Entry in a file queue
 */
public class FileQueueEntry
{
  private final long _id;
  private final int _priority;

  private final long _leaseExpire;

  private final String _msgId;

  FileQueueEntry _prev;
  FileQueueEntry _next;
  
  FileQueueEntry _nextPriority;

  private long _expiresTime;

  private SoftReference<Serializable> _payload;

  // True if the message has been read, but not yet committed
  private boolean _isRead;

  public FileQueueEntry(long id,
			String msgId,
			long leaseTimeout,
			int priority,
			long expiresTime)
  {
    if (msgId == null)
      throw new NullPointerException();
    
    _id = id;
    _msgId = msgId;
    _leaseExpire = leaseTimeout + Alarm.getCurrentTime();
    _expiresTime = expiresTime;
    _priority = priority;
  }

  public FileQueueEntry(long id,
			String msgId,
			long leaseTimeout,
			int priority,
			long expiresTime,
			Serializable payload)
  {
    if (msgId == null)
      throw new NullPointerException();
    
    _id = id;
    _msgId = msgId;
    _leaseExpire = leaseTimeout + Alarm.getCurrentTime();
    _priority = priority;
    _expiresTime = expiresTime;

    if (payload != null)
      _payload = new SoftReference<Serializable>(payload);
  }
  
  public long getId()
  {
    return _id;
  }

  public String getMsgId()
  {
    return _msgId;
  }

  public Serializable getPayload()
  {
    SoftReference<Serializable> ref = _payload;

    if (ref != null)
      return ref.get();
    else
      return null;
  }

  public void setPayload(Serializable payload)
  {
    _payload = new SoftReference<Serializable>(payload);
  }

  /**
   * Returns true if we can get a lease to this entry
   */
  public boolean isLease()
  {
    return _leaseExpire < Alarm.getCurrentTime();
  }

  public boolean isRead()
  {
    return _isRead;
  }

  public void setRead(boolean isRead)
  {
    _isRead = isRead;
  }
  
  public int getPriority()
  {
    return _priority;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[" + _id + "," + _msgId
	    + ",pri=" + _priority + "]");
  }
}

