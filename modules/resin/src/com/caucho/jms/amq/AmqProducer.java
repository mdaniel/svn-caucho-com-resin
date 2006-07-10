/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jms.amq;

import java.io.*;
import javax.jms.*;

import com.caucho.util.*;

import com.caucho.jms.*;
import com.caucho.jms.message.*;
import com.caucho.jms.session.*;

/**
 * A basic message producer.
 */
public class AmqProducer extends MessageProducerImpl
{
  private static final L10N L = new L10N(MessageProducer.class);

  protected AmqQueue _queue;
  private AmqClientChannel _channel;

  public AmqProducer(SessionImpl session, AmqQueue queue)
  {
    super(session, queue);

    _queue = queue;
  }
  
  /**
   * Sends a message to the destination
   *
   * @param destination the destination the message should be send to
   * @param message the message to send
   * @param deliveryMode the delivery mode
   * @param priority the priority
   * @param timeToLive how long the message should live
   */
  public void send(Destination destination,
                   Message message,
                   int deliveryMode,
                   int priority,
                   long timeToLive)
    throws JMSException
  {
    /*
    if (destination != _queue)
      throw new UnsupportedOperationException("can't handle non-local " + destination + " " + _queue);
    */

    try {
      AmqClientChannel channel = getChannel();

      TextMessage msg = (TextMessage) message;

      byte []data = msg.getText().getBytes();
    
      channel.publish(data.length, new ByteArrayInputStream(data));
    } catch (IOException e) {
      throw new JMSExceptionWrapper(e);
    }
  }

  private InputStream messageToInputStream(Message message)
  {
    return null;
  }
  
  private AmqClientChannel getChannel()
    throws IOException
  {
    if (_channel == null) {
      _channel = _queue.openChannel();
    }
    
    return _channel;
  }

  /**
   * Closes the producer.
   */
  public void close()
    throws JMSException
  {
    AmqChannel channel = _channel;
    _channel = null;

    if (channel != null)
      channel.close();
  }
}

