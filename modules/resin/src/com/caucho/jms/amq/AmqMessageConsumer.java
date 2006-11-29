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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jms.amq;

import com.caucho.jms.session.MessageConsumerImpl;
import com.caucho.jms.session.SessionImpl;
import com.caucho.util.L10N;

import javax.jms.JMSException;
import java.util.logging.Logger;

/**
 * Represents an AMQ queue consumer.
 */
public class AmqMessageConsumer extends MessageConsumerImpl
{
  private static final Logger log
    = Logger.getLogger(AmqMessageConsumer.class.getName());
  private static final L10N L = new L10N(AmqMessageConsumer.class);
  
  private AmqQueue _queue;

  public AmqMessageConsumer(SessionImpl session, String messageSelector,
			    AmqQueue queue, boolean isTopic)
    throws JMSException
  {
    super(session, messageSelector, queue, isTopic);
    
    _queue = queue;
  }

  /**
   * Closes the consumer.
   */
  public void close()
    throws JMSException
  {
  }

  /**
   * Returns a printable view of the queue.
   */
  public String toString()
  {
    return "AmqMessageConsumer[" + _queue + "]";
  }
}

