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

package com.caucho.amqp.io;

import java.io.IOException;

/**
 * The message properties header.
 */
public final class MessageProperties extends AmqpAbstractComposite {
  private Object _messageId; // messageid
  
  // private String _messageId; // type=message-id
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
  
  public Object getMessageId()
  {
    return _messageId;
  }
  
  public void setMessageId(Object messageId)
  {
    _messageId = messageId;
  }
  
  public String getUserId()
  {
    return _userId;
  }
  
  public void setUserId(String userId)
  {
    _userId = userId;
  }
  
  public void setTo(String to)
  {
    _to = to;
  }
  
  public String getTo()
  {
    return _to;
  }
  
  public String getSubject()
  {
    return _subject;
  }
  
  public void setSubject(String subject)
  {
    _subject = subject;
  }
  
  public String getReplyTo()
  {
    return _replyTo;
  }
  
  public void setReplyTo(String replyTo)
  {
    _replyTo = replyTo;
  }
  
  public Object getCorrelationId()
  {
    return _correlationId;
  }
  
  public void setCorrelationId(Object correlationId)
  {
    _correlationId = correlationId;
  }
  
  public String getContentType()
  {
    return _contentType;
  }
  
  public void setContentType(String contentType)
  {
    _contentType = contentType;
  }
  
  public String getContentEncoding()
  {
    return _contentEncoding;
  }
  
  public void setContentEncoding(String contentEncoding)
  {
    _contentEncoding = contentEncoding;
  }
  
  public long getExpiryTime()
  {
    return _expiryTime;
  }
  
  public void setExpiryTime(long expiryTime)
  {
    _expiryTime = expiryTime;
  }
  
  public long getCreationTime()
  {
    return _creationTime;
  }
  
  public void setCreationTime(long creationTime)
  {
    _creationTime = creationTime;
  }
  
  public String getGroupId()
  {
    return _groupId;
  }
  
  public void setGroupId(String groupId)
  {
    _groupId = groupId;
  }
  
  public long getGroupSequence()
  {
    return _groupSequence;
  }
  
  public void setGroupSequence(long seq)
  {
    _groupSequence = seq;
  }
  
  public String getReplyToGroupId()
  {
    return _replyToGroupId;
  }
  
  public void setReplyToGroupId(String replyToGroupId)
  {
    _replyToGroupId = replyToGroupId;
  }
  
  @Override
  public long getDescriptorCode()
  {
    return ST_MESSAGE_PROPERTIES;
  }
  
  @Override
  public MessageProperties createInstance()
  {
    return new MessageProperties();
  }
  
  @Override
  public void readBody(AmqpReader in, int count)
    throws IOException
  {
    _messageId = in.readObject();
    _userId = in.readString();
    _to = in.readString();
    _subject = in.readString();
    _replyTo = in.readString();
    _correlationId = in.readString();
    _contentType = in.readSymbol();
    _contentEncoding = in.readSymbol();
    _expiryTime = in.readLong();
    _creationTime = in.readLong();
    _groupId = in.readString();
    
    _groupSequence = in.readLong();
    if (in.isNull()) {
      _groupSequence = -1;
    }
    _replyToGroupId = in.readString();
  }
  
  @Override
  public int writeBody(AmqpWriter out)
    throws IOException
  {
    out.writeObject(_messageId);
    out.writeString(_userId); // binary
    out.writeString(_to);
    out.writeString(_subject);
    out.writeString(_replyTo);
    out.writeObject(_correlationId);
    
    out.writeSymbol(_contentType);
    out.writeSymbol(_contentEncoding);
    out.writeUint((int) (_expiryTime / 1000));
    out.writeUint((int) (_creationTime / 1000));
    
    out.writeString(_groupId);
    
    if (_groupSequence >= 0) {
      out.writeUint((int) (_groupSequence));
    }
    else {
      out.writeNull();
    }
    
    out.writeString(_replyToGroupId);
    
    return 13;
  }
}
