/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.message.common;

import com.caucho.message.MessagePropertiesFactory;


/**
 * message factory
 */
public class AbstractMessageFactory<T> 
  extends AbstractQueueSender<T>
  implements MessagePropertiesFactory<T>
{
  private boolean _isDurable;
  private int _priority = -1;
  private long _ttl = -1;
  private boolean _isFirstAcquirer = true;
  
  // properties
  private Object _messageId; // type=message-id
  private String _userId; // type=binary
  private String _to; // type=address
  private String _subject;
  private String _replyTo; // type=address
  private Object _correlationId; // type=message-id
  private String _contentType; // type=contentType
  private String _contentEncoding; // type=symbol
  private long _expiryTime = -1; // type=timestamp
  private long _creationTime = -1; // type=timestamp
  private String _groupId;
  private long _groupSequence = -1; // type=sequence
  private String _replyToGroupId;

  @Override
  public void setDurable(boolean isDurable)
  {
    _isDurable = isDurable;
  }
  
  @Override
  public boolean isDurable()
  {
    return _isDurable;
  }

  @Override
  public int getPriority()
  {
    return _priority;
  }
  
  @Override
  public void setPriority(int priority)
  {
    _priority = priority;
  }

  @Override
  public long getTimeToLive()
  {
    return _ttl;
  }
  
  @Override
  public void setTimeToLive(long ttl)
  {
    _ttl = ttl;
  }

  @Override
  public boolean isFirstAcquirer()
  {
    return _isFirstAcquirer;
  }

  @Override
  public void setFirstAcquirer(boolean isFirst)
  {
    _isFirstAcquirer = isFirst;
  }
  
  //
  // properties
  //
  
  public Object getMessageId()
  {
    return _messageId;
  }
  
  public void setMessageId(Object value)
  {
    _messageId = value;
  }
  
  public String getUserId()
  {
    return _userId;
  }
  
  public void setUserId(String value)
  {
    _userId = value;
  }
  
  @Override
  public String getTo()
  {
    return _to;
  }
  
  @Override
  public void setTo(String to)
  {
    _to = to;
  }

  @Override
  public String getSubject()
  {
    return _subject;
  }

  @Override
  public void setSubject(String subject)
  {
    _subject = subject;
  }
  
  @Override
  public String getReplyTo()
  {
    return _replyTo;
  }
  
  @Override
  public void setReplyTo(String value)
  {
    _replyTo = value;
  }
  
  @Override
  public Object getCorrelationId()
  {
    return _correlationId;
  }
  
  @Override
  public void setCorrelationId(Object value)
  {
    _correlationId = value;
  }
  
  @Override
  public String getContentType()
  {
    return _contentType;
  }
  
  @Override
  public void setContentType(String value)
  {
    _contentType = value;
  }
  
  @Override
  public String getContentEncoding()
  {
    return _contentEncoding;
  }
  
  @Override
  public void setContentEncoding(String value)
  {
    _contentEncoding = value;
  }
  
  @Override
  public long getExpiryTime()
  {
    return _expiryTime;
  }
  
  @Override
  public void setExpiryTime(long value)
  {
    _expiryTime = value;
  }
  
  @Override
  public long getCreationTime()
  {
    return _creationTime;
  }
  
  @Override
  public void setCreationTime(long value)
  {
    _creationTime = value;
  }
  
  @Override
  public String getGroupId()
  {
    return _groupId;
  }
  
  @Override
  public void setGroupId(String value)
  {
    _groupId = value;
  }
  
  @Override
  public long getGroupSequence()
  {
    return _groupSequence;
  }
  
  @Override
  public void setGroupSequence(long value)
  {
    _groupSequence = value;
  }
  
  @Override
  public String getReplyToGroupId()
  {
    return _replyToGroupId;
  }
  
  @Override
  public void setReplyToGroupId(String value)
  {
    _replyToGroupId = value;
  }

  @Override
  protected boolean offerMicros(MessagePropertiesFactory<T> factory, 
                                T value,
                                long timeoutMicros)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void close()
  {
  }
}
