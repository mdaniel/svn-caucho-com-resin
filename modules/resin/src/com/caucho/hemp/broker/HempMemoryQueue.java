/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.hemp.broker;

import com.caucho.hmpp.*;
import com.caucho.hmpp.packet.*;
import com.caucho.hmpp.spi.*;
import com.caucho.server.resin.*;
import com.caucho.server.util.*;
import com.caucho.util.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.lang.ref.*;
import java.io.Serializable;

/**
 * Queue of hmpp packets
 */
public class HempMemoryQueue implements HmppStream, Runnable {
  private static final Logger log
    = Logger.getLogger(HempMemoryQueue.class.getName());
  private static final L10N L = new L10N(HempMemoryQueue.class);

  private final Executor _executor = ScheduledThreadPool.getLocal();
  private final ClassLoader _loader
    = Thread.currentThread().getContextClassLoader();
  private final HmppStream _target;

  private int _threadSemaphore;

  private Packet []_queue = new Packet[32];
  private int _head;
  private int _tail;

  public HempMemoryQueue(HmppStream target)
  {
    _target = target;
    _threadSemaphore = 1;
  }

  /**
   * Sends a message
   */
  public void sendMessage(String to, String from, Serializable value)
  {
    enqueue(new Message(to, from, value));
  }

  /**
   * Query an entity
   */
  public boolean sendQueryGet(long id, String to, String from, Serializable query)
  {
    return true;
  }

  /**
   * Query an entity
   */
  public boolean sendQuerySet(long id, String to, String from, Serializable query)
  {
    return true;
  }

  /**
   * Query an entity
   */
  public void sendQueryResult(long id, String to, String from, Serializable value)
  {
  }

  /**
   * Query an entity
   */
  public void sendQueryError(long id,
			 String to,
			 String from,
			 Serializable query,
			 HmppError error)
  {
  }

  /**
   * Presence
   */
  public void sendPresence(String to, String from, Serializable []data)
  {
  }

  /**
   * Presence unavailable
   */
  public void sendPresenceUnavailable(String to,
				      String from,
				      Serializable []data)
  {
  }

  /**
   * Presence probe
   */
  public void sendPresenceProbe(String to,
			        String from,
			        Serializable []data)
  {
  }

  /**
   * Presence subscribe
   */
  public void sendPresenceSubscribe(String to,
				    String from,
				    Serializable []data)
  {
  }

  /**
   * Presence subscribed
   */
  public void sendPresenceSubscribed(String to,
				     String from,
				     Serializable []data)
  {
  }

  /**
   * Presence unsubscribe
   */
  public void sendPresenceUnsubscribe(String to,
				      String from,
				      Serializable []data)
  {
  }
  
  /**
   * Presence unsubscribed
   */
  public void sendPresenceUnsubscribed(String to,
				       String from,
				       Serializable []data)
  {
  }

  /**
   * Presence error
   */
  public void sendPresenceError(String to,
			        String from,
			        Serializable []data,
			        HmppError error)
  {
  }

  protected HmppStream getStream()
  {
    return _target;
  }

  protected void enqueue(Packet packet)
  {
    if (log.isLoggable(Level.FINER)) {
      int size = (_head - _tail + _queue.length) % _queue.length;
      log.finer(this + " enqueue(" + size + ") " + packet);
    }

    boolean isStartThread = false;
    
    synchronized (this) {
      int next = (_head + 1) % _queue.length;

      if (next == _tail) {
	Packet []extQueue = new Packet[_queue.length + 32];
	
	int i = 0;
	for (int tail = _tail;
	     tail != _head;
	     tail = (tail + 1) % _queue.length) {
	  extQueue[i++] = _queue[tail];
	}
	
	_queue = extQueue;

	_tail = 0;
	_head = i;
	next = _head + 1;
      }

      _queue[_head] = packet;
      _head = next;

      if (_threadSemaphore > 0) {
	_threadSemaphore--;
	isStartThread = true;
      }
    }
    
    if (isStartThread) {
      _executor.execute(this);
    }
  }

  protected Packet dequeue()
  {
    boolean isStartThread = false;
    
    synchronized (this) {
      if (_head == _tail) {
	_threadSemaphore--;
	
	return null;
      }

      Packet packet = _queue[_tail];
      
      _tail = (_tail + 1) % _queue.length;

      return packet;
    }
  }

  public void run()
  {
    Packet packet;

    Thread.currentThread().setContextClassLoader(_loader);
    
    while ((packet = dequeue()) != null) {
      boolean isValid = false;
      
      try {
	if (log.isLoggable(Level.FINER))
	  log.finer(this + " dequeue " + packet);
	
	packet.dispatch(getStream());
	
	isValid = true;
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
	isValid = true;
      } finally {
	// fix semaphore in case of thread death
	if (! isValid) {
	  synchronized (this) {
	    _threadSemaphore++;
	  }
	}
      }
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _target + "]";
  }
}
