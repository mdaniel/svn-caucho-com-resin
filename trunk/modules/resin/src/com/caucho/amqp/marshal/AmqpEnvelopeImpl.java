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

package com.caucho.amqp.marshal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.caucho.util.NullIterator;

/**
 * Envelope for the amqp value.
 */
public class AmqpEnvelopeImpl implements AmqpEnvelope {
  // delivery annotations
  
  private HashMap<String,Object> _deliveryAnnotationMap;
  
  // message annotations
  
  private HashMap<String,Object> _messageAnnotationMap;
  
  // properties
  
  private Object _messageId;
  private String _userId;
  private String _to;
  private String _subject;
  private String _replyTo;
  
  private Object _correlationId;
  private String _contentType;
  private String _contentEncoding;
  
  private long _expiryTime = -1;
  private long _creationTime = -1;
  private String _groupId;
  private long _groupSequence = -1;
  private String _replyToGroupId;
  
  // app properties
  
  private HashMap<String,Object> _propertyMap;
  
  private Object _value;
  
  // message annotations
  
  private HashMap<String,Object> _footerMap;

  //
  // delivery annotations
  //
  
  @Override
  public Object getDeliveryAnnotation(String name)
  {
    if (_deliveryAnnotationMap != null) {
      return _deliveryAnnotationMap.get(name);
    }
    else {
      return null;
    }
  }
  
  public void setDeliveryAnnotation(String name, Object value)
  {
    if (_deliveryAnnotationMap == null) {
      _deliveryAnnotationMap = new HashMap<String,Object>();
    }
    
    _deliveryAnnotationMap.put(name, value);
  }
  
  @Override
  public Iterator<Map.Entry<String,Object>> getDeliveryAnnotations()
  {
    if (_deliveryAnnotationMap != null) {
      return _deliveryAnnotationMap.entrySet().iterator();
    }
    else {
      return NullIterator.create();
    }
  }
  
  //
  // message annotations
  //
  
  @Override
  public Object getMessageAnnotation(String name)
  {
    if (_messageAnnotationMap != null) {
      return _messageAnnotationMap.get(name);
    }
    else {
      return null;
    }
  }
  
  public void setMessageAnnotation(String name, Object value)
  {
    if (_messageAnnotationMap == null) {
      _messageAnnotationMap = new HashMap<String,Object>();
    }
    
    _messageAnnotationMap.put(name, value);
  }
  
  @Override
  public Iterator<Map.Entry<String,Object>> getMessageAnnotations()
  {
    if (_messageAnnotationMap != null) {
      return _messageAnnotationMap.entrySet().iterator();
    }
    else {
      return NullIterator.create();
    }
  }

  public void setMessageId(Object messageId)
  {
    _messageId = messageId;
  }
  
  @Override
  public Object getMessageId()
  {
    return _messageId;
  }
  
  public void setUserId(String value)
  {
    _userId = value;
  }
  
  @Override
  public String getUserId()
  {
    return _userId;
  }
  
  public void setTo(String value)
  {
    _to = value;
  }
  
  @Override
  public String getTo()
  {
    return _to;
  }
  
  public void setSubject(String subject)
  {
    _subject = subject;
  }
  
  public String getSubject()
  {
    return _subject;
  }
  
  public void setReplyTo(String value)
  {
    _replyTo = value;
  }
  
  @Override
  public String getReplyTo()
  {
    return _replyTo;
  }
  
  public void setCorrelationId(Object value)
  {
    _correlationId = value;
  }
  
  @Override
  public Object getCorrelationId()
  {
    return _correlationId;
  }
  
  public void setContentType(String value)
  {
    _contentType = value;
  }
  
  @Override
  public String getContentType()
  {
    return _contentType;
  }
  
  public void setContentEncoding(String value)
  {
    _contentEncoding = value;
  }
  
  @Override
  public String getContentEncoding()
  {
    return _contentEncoding;
  }
  
  public void setExpiryTime(long value)
  {
    _expiryTime = value;
  }
  
  @Override
  public long getExpiryTime()
  {
    return _expiryTime;
  }
  
  public void setCreationTime(long value)
  {
    _creationTime = value;
  }
  
  @Override
  public long getCreationTime()
  {
    return _creationTime;
  }

  public void setGroupId(String value)
  {
    _groupId = value;
  }

  @Override
  public String getGroupId()
  {
    return _groupId;
  }

  public void setGroupSequence(long value)
  {
    _groupSequence = value;
  }

  @Override
  public long getGroupSequence()
  {
    return _groupSequence;
  }

  public void setReplyToGroupId(String value)
  {
    _replyToGroupId = value;
  }

  @Override
  public String getReplyToGroupId()
  {
    return _replyToGroupId;
  }
  
  //
  // application properties
  //
  
  @Override
  public Object getProperty(String name)
  {
    if (_propertyMap != null) {
      return _propertyMap.get(name);
    }
    else {
      return null;
    }
  }
  
  public void setProperty(String name, Object value)
  {
    if (_propertyMap == null) {
      _propertyMap = new HashMap<String,Object>();
    }
    
    _propertyMap.put(name, value);
  }
  
  @Override
  public Iterator<Map.Entry<String,Object>> getProperties()
  {
    if (_propertyMap != null) {
      return _propertyMap.entrySet().iterator();
    }
    else {
      return NullIterator.create();
    }
  }
  
  //
  // value
  //

  public void setValue(Object value)
  {
    _value = value;
  }
  
  @Override
  public Object getValue()
  {
    return _value;
  }
  
  //
  // footers
  //
  
  @Override
  public Object getFooter(String name)
  {
    if (_footerMap != null) {
      return _footerMap.get(name);
    }
    else {
      return null;
    }
  }
  
  public void setFooter(String name, Object value)
  {
    if (_footerMap == null) {
      _footerMap = new HashMap<String,Object>();
    }
    
    _footerMap.put(name, value);
  }
  
  @Override
  public Iterator<Map.Entry<String,Object>> getFooters()
  {
    if (_footerMap != null) {
      return _footerMap.entrySet().iterator();
    }
    else {
      return NullIterator.create();
    }
  }
}
