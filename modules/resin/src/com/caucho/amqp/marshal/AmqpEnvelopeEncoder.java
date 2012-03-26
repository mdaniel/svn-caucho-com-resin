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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.caucho.amqp.io.AmqpConstants;
import com.caucho.amqp.io.AmqpWriter;
import com.caucho.amqp.io.MessageAnnotations;
import com.caucho.amqp.io.MessageAppProperties;
import com.caucho.amqp.io.MessageDeliveryAnnotations;
import com.caucho.amqp.io.MessageFooter;
import com.caucho.amqp.io.MessageProperties;
import com.caucho.message.MessagePropertiesFactory;


/**
 * Encodes a message as an envelope.
 */
public class AmqpEnvelopeEncoder extends AbstractMessageEncoder<AmqpEnvelope>
  implements AmqpMessageEncoder<AmqpEnvelope>
{
  public static final AmqpEnvelopeEncoder ENCODER = new AmqpEnvelopeEncoder();
  
  //
  // delivery annotations
  //

  @Override
  protected void
  encodeDeliveryAnnotations(AmqpWriter out, 
                            MessagePropertiesFactory<AmqpEnvelope> factory,
                            AmqpEnvelope envelope)
    throws IOException
  {
    Iterator<Map.Entry<String,Object>> envIter = envelope.getDeliveryAnnotations();
    // Iterator<Map.Entry<String,Object>> envIter = envelope.getProperties();
    
    if (envIter.hasNext()) {
      MessageDeliveryAnnotations annotations = new MessageDeliveryAnnotations();

      annotations.putAll(envIter);
      
      annotations.write(out);
    }
  }
  
  //
  // message annotations
  //

  @Override
  protected void
  encodeMessageAnnotations(AmqpWriter out, 
                           MessagePropertiesFactory<AmqpEnvelope> factory,
                           AmqpEnvelope envelope)
    throws IOException
  {
    Iterator<Map.Entry<String,Object>> envIter = envelope.getMessageAnnotations();
    // Iterator<Map.Entry<String,Object>> envIter = envelope.getProperties();
    
    if (envIter.hasNext()) {
      MessageAnnotations annotations = new MessageAnnotations();

      annotations.putAll(envIter);
      
      annotations.write(out);
    }
  }

  protected Object getMessageId(MessagePropertiesFactory<AmqpEnvelope> factory,
                                AmqpEnvelope envelope)
  {
    Object messageId = envelope.getMessageId();
    
    if (messageId == null) {
      messageId = factory.getMessageId();
    }
    
    return messageId;
  }
  
  protected String getUserId(MessagePropertiesFactory<AmqpEnvelope> factory,
                             AmqpEnvelope envelope)
  {
    String userId = envelope.getUserId();
    
    if (userId == null) {
      userId = factory.getUserId();
    }
    
    return userId;
  }
  
  protected String getTo(MessagePropertiesFactory<AmqpEnvelope> factory,
                             AmqpEnvelope envelope)
  {
    String to = envelope.getTo();
    
    if (to == null) {
      to = factory.getTo();
    }
    
    return to;
  }

  protected String getSubject(MessagePropertiesFactory<AmqpEnvelope> factory,
                              AmqpEnvelope envelope)
  {
    String subject = envelope.getSubject();
    
    if (subject == null) {
      subject = factory.getSubject();
    }
    
    return subject;
  }
  
  protected String getReplyTo(MessagePropertiesFactory<AmqpEnvelope> factory,
                              AmqpEnvelope envelope)
  {
    String replyTo = envelope.getReplyTo();
    
    if (replyTo == null) {
      replyTo = factory.getReplyTo();
    }
    
    return replyTo;
  }
  
  protected Object getCorrelationId(MessagePropertiesFactory<AmqpEnvelope> factory,
                                    AmqpEnvelope envelope)
  {
    Object value = envelope.getCorrelationId();
    
    if (value == null) {
      value = factory.getCorrelationId();
    }
    
    return value;
  }
  
  protected String getContentType(MessagePropertiesFactory<AmqpEnvelope> factory,
                                  AmqpEnvelope envelope)
  {
    String value = envelope.getContentType();
    
    if (value == null) {
      value = factory.getContentType();
    }
    
    if (value == null) {
      value = "text/plain";
    }
    
    return value;
  }
  
  protected String getContentEncoding(MessagePropertiesFactory<AmqpEnvelope> factory,
                                      AmqpEnvelope envelope)
  {
    String value = envelope.getContentEncoding();
    
    if (value == null) {
      value = factory.getContentEncoding();
    }
    
    return value;
  }
  
  protected long getExpiryTime(MessagePropertiesFactory<AmqpEnvelope> factory,
                               AmqpEnvelope envelope)
  {
    long value = envelope.getExpiryTime();
    
    if (value <= 0) {
      value = factory.getExpiryTime();
    }
    
    return value;
  }
  
  protected long getCreationTime(MessagePropertiesFactory<AmqpEnvelope> factory,
                                 AmqpEnvelope envelope)
  {
    long value = envelope.getCreationTime();
    
    if (value <= 0) {
      value = factory.getCreationTime();
    }
    
    return value;
  }

  protected String getGroupId(MessagePropertiesFactory<AmqpEnvelope> factory,
                              AmqpEnvelope envelope)
  {
    String value = envelope.getGroupId();
    
    if (value == null) {
      value = factory.getGroupId();
    }
    
    return value;
  }
  
  protected long getGroupSequence(MessagePropertiesFactory<AmqpEnvelope> factory,
                                 AmqpEnvelope envelope)
  {
    long value = envelope.getGroupSequence();
    
    if (value < 0) {
      value = factory.getGroupSequence();
    }
    
    return value;
  }

  protected String getReplyToGroupId(MessagePropertiesFactory<AmqpEnvelope> factory,
                              AmqpEnvelope envelope)
  {
    String value = envelope.getReplyToGroupId();
    
    if (value == null) {
      value = factory.getReplyToGroupId();
    }
    
    return value;
  }
  
  @Override
  public String getContentType(AmqpEnvelope value)
  {
    return "text/plain";
  }

  @Override
  protected void encodeProperties(AmqpWriter out, 
                                  MessagePropertiesFactory<AmqpEnvelope> factory,
                                  AmqpEnvelope envelope)
    throws IOException
  {
    MessageProperties properties = new MessageProperties();
    boolean isProperties = false;

    Object messageId = getMessageId(factory, envelope);
    if (messageId != null) {
      isProperties = true;
      properties.setMessageId(messageId);
    }

    String userId = getUserId(factory, envelope);
    if (userId != null) {
      isProperties = true;
      properties.setUserId(userId);
    }

    String to = getTo(factory, envelope);
    if (to != null) {
      isProperties = true;
      properties.setTo(to);
    }
    
    String subject = getSubject(factory, envelope);
    if (subject != null) {
      isProperties = true;
      properties.setSubject(subject);
    }

    String replyTo = getReplyTo(factory, envelope);
    if (replyTo != null) {
      isProperties = true;
      properties.setReplyTo(replyTo);
    }

    Object correlationId = getCorrelationId(factory, envelope);
    if (correlationId != null) {
      isProperties = true;
      properties.setCorrelationId(correlationId);
    }
    
    String contentType = getContentType(factory, envelope);
    if (contentType != null) {
      isProperties = true;
      properties.setContentType(contentType);
    }
    
    String contentEncoding = getContentEncoding(factory, envelope);
    if (contentEncoding != null) {
      isProperties = true;
      properties.setContentEncoding(contentEncoding);
    }
    
    long expiryTime = getExpiryTime(factory, envelope);
    if (expiryTime > 0) {
      isProperties = true;
      properties.setExpiryTime(expiryTime);
    }
    
    long creationTime = getCreationTime(factory, envelope);
    if (creationTime > 0) {
      isProperties = true;
      properties.setCreationTime(creationTime);
    }
    
    String groupId = getGroupId(factory, envelope);
    if (groupId != null) {
      isProperties = true;
      properties.setGroupId(groupId);
    }
    
    long groupSequence = getGroupSequence(factory, envelope);
    if (groupSequence >= 0) {
      isProperties = true;
      properties.setGroupSequence(groupSequence);
    }
    
    String replyToGroupId = getReplyToGroupId(factory, envelope);
    if (replyToGroupId != null) {
      isProperties = true;
      properties.setReplyToGroupId(replyToGroupId);
    }
    
    if (isProperties) {
      properties.write(out);
    }
  }

  @Override
  protected void
  encodeApplicationProperties(AmqpWriter out, 
                              MessagePropertiesFactory<AmqpEnvelope> factory,
                              AmqpEnvelope envelope)
    throws IOException
  {
    Iterator<Map.Entry<String,Object>> envIter = envelope.getProperties();
    // Iterator<Map.Entry<String,Object>> envIter = envelope.getProperties();
    
    if (envIter.hasNext()) {
      MessageAppProperties properties = new MessageAppProperties();

      properties.putAll(envIter);
      
      properties.write(out);
    }
  }

  @Override
  public void encodeData(AmqpWriter out, AmqpEnvelope envelope)
    throws IOException
  {
    out.writeDescriptor(AmqpConstants.ST_MESSAGE_VALUE);
    out.writeString(String.valueOf(envelope.getValue()));
  }
  
  
  //
  // message annotations
  //

  @Override
  protected void
  encodeFooters(AmqpWriter out, 
                MessagePropertiesFactory<AmqpEnvelope> factory,
                AmqpEnvelope envelope)
    throws IOException
  {
    Iterator<Map.Entry<String,Object>> envIter = envelope.getFooters();
    // Iterator<Map.Entry<String,Object>> envIter = envelope.getProperties();
    
    if (envIter.hasNext()) {
      MessageFooter footers = new MessageFooter();

      footers.putAll(envIter);
      
      footers.write(out);
    }
  }

}
