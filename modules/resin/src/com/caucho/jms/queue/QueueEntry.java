package com.caucho.jms.queue;

import com.caucho.util.Alarm;
import java.io.Serializable;

/**
 * Basic implementation of an entry in the Queue.
 */
public abstract class QueueEntry
{  
  private final int _priority;

  private final long _leaseExpire;

  private final String _msgId;

  QueueEntry _next;
  
  QueueEntry _nextPriority;

  private long _expiresTime;

  private Serializable _payload;
  
  // True if the message has been read, but not yet committed
  private long _readSequence;

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
    return _readSequence != 0;
  }
  
  public long getReadSequence()
  {
    return _readSequence;
  }

  public void setReadSequence(long readSequence)
  {
    _readSequence = readSequence;
  }  

  public void rollback()
  {
    
  }
  
  public int getPriority()
  {
    return _priority;
  }

  public Serializable readPayload()
  {
    return getPayload();
  }
  
  public final Serializable getPayload()
  {
    return _payload;
  }
  
  public final void setPayload(Serializable payload)
  {
    _payload = payload;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _msgId + ",pri=" + _priority + "]");
  }
}
