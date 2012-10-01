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

import com.caucho.amqp.io.AmqpWriter;
import com.caucho.amqp.io.MessageProperties;
import com.caucho.message.MessagePropertiesFactory;


/**
 * encoding a message
 */
abstract public class AbstractMessageEncoder<T> implements AmqpMessageEncoder<T>
{
  @Override
  public boolean isDurable(MessagePropertiesFactory<T> factory, T value)
  {
    return factory.isDurable();
  }
  
  @Override
  public int getPriority(MessagePropertiesFactory<T> factory, T value)
  {
    return factory.getPriority();
  }
  
  @Override
  public long getTimeToLive(MessagePropertiesFactory<T> factory, T value)
  {
    return factory.getTimeToLive();
  }
  
  @Override
  public boolean isFirstAcquirer(MessagePropertiesFactory<T> factory, T value)
  {
    return factory.isFirstAcquirer();
  }
  
  @Override
  public String getContentType(T value)
  {
    return null;
  }
  
  @Override
  public void encode(AmqpWriter out, 
                     MessagePropertiesFactory<T> factory,
                     T value)
    throws IOException
  {
    // header taken care of prior to call
    
    encodeDeliveryAnnotations(out, factory, value);
    
    encodeMessageAnnotations(out, factory, value);
    
    encodeApplicationProperties(out, factory, value);
    
    encodeData(out, value);
    
    encodeFooters(out, factory, value);
  }
  
  protected void 
  encodeDeliveryAnnotations(AmqpWriter out, 
                            MessagePropertiesFactory<T> factory,
                            T value)
    throws IOException
  {
  }
  
  protected void 
  encodeMessageAnnotations(AmqpWriter out, 
                           MessagePropertiesFactory<T> factory,
                           T value)
    throws IOException
  {
  }
  
  protected void encodeProperties(AmqpWriter out, 
                                  MessagePropertiesFactory<T> factory,
                                  T value)
    throws IOException
  {
    String contentType = getContentType(value);
      
    if (contentType != null) {
      MessageProperties properties = new MessageProperties();
        
      properties.setContentType(contentType);
        
      properties.write(out);
    }
  }
  
  protected void 
  encodeApplicationProperties(AmqpWriter out, 
                              MessagePropertiesFactory<T> factory,
                              T value)
    throws IOException
  {
    
  }

  @Override
  abstract public void encodeData(AmqpWriter out, T value)
    throws IOException;
  
  
  protected void 
  encodeFooters(AmqpWriter out, 
                MessagePropertiesFactory<T> factory,
                T value)
    throws IOException
  {
  }
}
