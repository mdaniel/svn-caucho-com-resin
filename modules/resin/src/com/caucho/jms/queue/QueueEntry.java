package com.caucho.jms.queue;

import com.caucho.util.Alarm;

/**
 * Basic implementation of an entry in the Queue.
 */
public abstract class QueueEntry
{  
  private final int _priority;

  private final long _leaseExpire;

  private final String _msgId;

  QueueEntry _prev;
  QueueEntry _next;
  
  QueueEntry _nextPriority;

  private long _expiresTime;
  
  // True if the message has been read, but not yet committed
  private boolean _isRead;

  public QueueEntry(String msgId,
                    long leaseTimeout,
                    int priority,
                    long expiresTime)
  {
    if (msgId == null)
      throw new NullPointerException();
    
    _msgId = msgId;
    _leaseExpire = leaseTimeout + Alarm.getCurrentTime();
    _expiresTime = expiresTime;
    _priority = priority;
  }

  public String getMsgId()
  {
    return _msgId;
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
            + "[" + _msgId + ",pri=" + _priority + "]");
  }
  
  public abstract Object getPayload();
  
  public abstract void setPayload(Object object);
  
}
