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

import java.util.Iterator;
import java.util.Map;

/**
 * Envelope for the amqp value.
 */
public interface AmqpEnvelope {
  //
  // delivery annotations
  //
  
  public Object getDeliveryAnnotation(String name);
  
  public Iterator<Map.Entry<String,Object>> getDeliveryAnnotations();
  
  //
  // message annotations
  //
  
  public Object getMessageAnnotation(String name);
  
  public Iterator<Map.Entry<String,Object>> getMessageAnnotations();
  
  //
  // properties
  //
  
  public Object getMessageId();
  
  public String getUserId();
  
  public String getTo();
  
  public String getSubject();
  
  public String getReplyTo();
  
  public Object getCorrelationId();
  
  public String getContentType();
  
  public String getContentEncoding();
  
  public long getExpiryTime();
  
  public long getCreationTime();
  
  public String getGroupId();
  
  public long getGroupSequence();
  
  public String getReplyToGroupId();
  
  public Object getProperty(String name);
  
  public Iterator<Map.Entry<String,Object>> getProperties();
  
  //
  // value
  //
  
  public Object getValue();

  
  //
  // footers
  //
  
  public Object getFooter(String name);
  
  public Iterator<Map.Entry<String,Object>> getFooters();
}
