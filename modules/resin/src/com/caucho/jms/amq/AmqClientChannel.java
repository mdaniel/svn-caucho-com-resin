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

import com.caucho.util.ByteBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AMQ channel.
 */
public class AmqClientChannel extends AmqChannel {
  private static final Logger log
    = Logger.getLogger(AmqClientChannel.class.getName());

  private String _queue;
  private boolean _isQueue;

  private long _publishCount;
  
  AmqClientChannel(AmqConnection conn)
  {
    super(conn);
  }

  boolean openQueue(String queueName)
    throws IOException
  {
    ByteBuffer packet = new ByteBuffer();

    packet.addShort(CLASS_QUEUE);
    packet.addShort(ID_QUEUE_DECLARE);
    int ticket = 0;
    packet.addShort(ticket);
    _conn.addShortString(packet, queueName);
    boolean passive = false;
    packet.add(passive ? 1 : 0);
    boolean durable = false;
    packet.add(durable ? 1 : 0);
    boolean exclusive = false;
    packet.add(exclusive ? 1 : 0);
    boolean autoDelete = false;
    packet.add(autoDelete ? 1 : 0);
    _conn.addTable(packet, null);

    _conn.writePacket(FRAME_METHOD, getId(), packet);

    synchronized (this) {
      if (_isQueue)
	return true;
      else if (_isClosed)
	return false;
      
      try {
	this.wait(10000);
      } catch (Exception e) {
	log.log(Level.FINER, e.toString(), e);
      }
    }

    return _isQueue;
  }

  boolean publish(long length, InputStream data)
    throws IOException
  {
    ByteBuffer packet = new ByteBuffer();

    packet.addShort(CLASS_BASIC);
    packet.addShort(ID_BASIC_PUBLISH);
    int ticket = 0;
    packet.addShort(ticket);
    String exchange = "test";
    _conn.addShortString(packet, exchange);
    String routing = "test-router";
    _conn.addShortString(packet, routing);
    boolean mandatory = false;
    packet.add(mandatory ? 1 : 0);
    boolean immediate = false;
    packet.add(immediate ? 1 : 0);

    _conn.writePacket(FRAME_METHOD, getId(), packet);

    synchronized (this) {
      _publishCount++;
    }

    packet.clear();
    packet.addShort(CLASS_BASIC);
    int weight = 0;
    packet.addShort(weight);
    packet.addLong(length);
    System.out.println("LENGTH: " + length);
    int propFlags = 0;
    packet.addShort(propFlags);

    _conn.writePacket(FRAME_HEADER, getId(), packet);
    _conn.writeData(getId(), length, data);

    try {
      Thread.sleep(2000);
    } catch (Throwable e) {
    }

    return true;
  }

  boolean waitOpen()
  {
    synchronized (this) {
      if (_isOpen)
	return true;
      else if (_isClosed)
	return false;
      
      try {
	this.wait(10000);
      } catch (Exception e) {
	log.log(Level.FINER, e.toString(), e);
      }
    }

    return _isOpen;
  }

  boolean doOpenOk(InputStream is)
  {
    synchronized (this) {
      if (! _isClosed)
	_isOpen = true;

      this.notifyAll();

    }

    return true;
  }

  boolean doQueueDeclareOk(InputStream is)
    throws IOException
  {
    String queue = _conn.readShortString(is);
    int messageCount = _conn.readInt(is);
    int consumerCount = _conn.readInt(is);

    System.out.println("OK! " + queue);

    synchronized (this) {
      if (! _isClosed)
	_isQueue = true;

      this.notifyAll();
    }

    return true;
  }
}
