/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jms.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Enumeration;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.IOException;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;

import javax.jms.Message;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.jms.JMSException;

import javax.sql.DataSource;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.CharBuffer;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.ByteToChar;

import com.caucho.jms.AbstractDestination;
import com.caucho.jms.JMSExceptionWrapper;

import com.caucho.jms.selector.Selector;

import com.caucho.jms.message.MessageImpl;
import com.caucho.jms.message.TextMessageImpl;
import com.caucho.jms.message.BytesMessageImpl;
import com.caucho.jms.message.StreamMessageImpl;
import com.caucho.jms.message.ObjectMessageImpl;
import com.caucho.jms.message.MapMessageImpl;

import com.caucho.jms.session.MessageConsumerImpl;
import com.caucho.jms.session.SessionImpl;

/**
 * Represents a memory topic consumer.
 */
public class MemoryTopicConsumer extends MessageConsumerImpl
  implements TopicSubscriber {
  static final Logger log = Log.open(MemoryTopicConsumer.class);
  static final L10N L = new L10N(MemoryTopicConsumer.class);

  private MemoryTopic _topic;
  private MemoryQueue _queue;

  private int _consumerId;

  private boolean _autoAck;

  public MemoryTopicConsumer(SessionImpl session, String messageSelector,
			     MemoryTopic topic)
    throws JMSException
  {
    this(session, messageSelector, topic, null);
  }

  public MemoryTopicConsumer(SessionImpl session, String messageSelector,
			     MemoryTopic topic, String name)
    throws JMSException
  {
    super(session, messageSelector, topic, false);
    
    _topic = topic;

    if (session.getAcknowledgeMode() == session.AUTO_ACKNOWLEDGE ||
	session.getAcknowledgeMode() == session.DUPS_OK_ACKNOWLEDGE)
      _autoAck = true;

    if (name != null)
      _queue = topic.createDurableSubscriber(name);
    else
      _queue = topic.createSubscriberQueue();

    _queue.addListener(this);
  }

  /**
   * Returns the topic.
   */
  public Topic getTopic()
  {
    return _topic;
  }

  /**
   * Receives a message from the topic.
   */
  protected MessageImpl receiveImpl()
    throws JMSException
  {
    // purgeExpiredConsumers();
    // _topic.purgeExpiredMessages();

    return _queue.receive(_selector, 1, true);
  }

  /**
   * Acknowledges all received messages from the session.
   */
  public void acknowledge()
    throws JMSException
  {
    if (_autoAck)
      return;
  }

  /**
   * Rollback all received messages from the session.
   */
  public void rollback()
    throws JMSException
  {
    if (_autoAck)
      return;
  }

  /**
   * Closes the consumer.
   */
  public void close()
  {
    _topic.removeSubscriber(_queue);
  }

  /**
   * Returns a printable view of the topic.
   */
  public String toString()
  {
    return "MemoryTopicConsumer[" + _topic + "," + _consumerId + "]";
  }
}

