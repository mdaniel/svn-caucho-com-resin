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

import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.util.*;

/**
 * AMQ channel.
 */
public class AmqServerChannel extends AmqChannel {
  private static final Logger log
    = Logger.getLogger(AmqServerChannel.class.getName());

  private static final int BASIC_PUBLISH
    = (CLASS_BASIC << 8) + ID_BASIC_PUBLISH;

  private String _queue;
  private boolean _isPublishing;

  private int _contentType;
  
  AmqServerChannel(AmqConnection conn, int id)
  {
    super(conn);

    setId(id);
  }
  
  boolean doQueueDeclare(InputStream is)
    throws IOException
  {
    int ticket = _conn.readShort(is);
    _queue = _conn.readShortString(is);
    boolean passive = is.read() != 0;
    boolean durable = is.read() != 0;
    boolean exclusive = is.read() != 0;
    boolean autoDelete = is.read() != 0;
    HashMap<String,String> arguments = _conn.readTable(is);
    
    System.out.println("QUEUE: " + _queue);

    ByteBuffer packet = new ByteBuffer();

    packet.addShort(CLASS_QUEUE);
    packet.addShort(ID_QUEUE_DECLARE_OK);
    _conn.addShortString(packet, _queue);
    int msgCount = 0;
    packet.addInt(msgCount);
    int consumerCount = 0;
    packet.addInt(consumerCount);

    _conn.writePacket(FRAME_METHOD, getId(), packet);
    
    return true;
  }
  
  boolean doBasicPublish(InputStream is)
    throws IOException
  {
    int ticket = _conn.readShort(is);
    String exchange = _conn.readShortString(is);
    String routing = _conn.readShortString(is);
    boolean mandatory = is.read() != 0;
    boolean immediate = is.read() != 0;
    
    System.out.println("PUBLISH: " + exchange + " " + routing);
    
    _contentType = BASIC_PUBLISH;
    
    return true;
  }

  void doContentEnd(InputStream is)
    throws IOException
  {
    try {
      int contentType = _contentType;
      _contentType = 0;
    
      switch (contentType) {
      case BASIC_PUBLISH:
	Runnable action = new PublishAction(is);
	is = null;

	ThreadPool.getThreadPool().schedule(action);
	return;
      default:
	System.out.println("UNKNOWN: " + (contentType >> 8) + "." + (contentType & 0xffff));
	close();
	return;
      }
    } finally {
      if (is != null)
	is.close();
    }
  }

  class PublishAction implements Runnable {
    private InputStream _is;

    PublishAction(InputStream is)
    {
      _is = is;
    }
    
    public void run()
    {
      try {
	System.out.println("PUBLISH!");
      } finally {
	try {
	  _is.close();
	} catch (IOException e) {
	}
      }
    }
  }
}
