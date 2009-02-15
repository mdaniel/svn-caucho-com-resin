package com.caucho.jms.memory;

import com.caucho.jms.message.MessageImpl;
import com.caucho.jms.queue.QueueEntry;
import com.caucho.util.Alarm;

/**
 * Entry in a memory queue.
 *
 */
public class MemoryQueueEntry extends QueueEntry
{  

  private MessageImpl _payload;

  public MemoryQueueEntry(String msgId,
                        long leaseTimeout,
                        int priority,
                        long expiresTime,
                        MessageImpl payload)
  {
    
    super(msgId, leaseTimeout, priority, expiresTime);

    if (payload != null)
      _payload = payload;
  }
  
  public MessageImpl getPayload()
  {
    return _payload;
  }

  public void setPayload(Object payload)
  {
    _payload = (MessageImpl)payload;
  }

}
