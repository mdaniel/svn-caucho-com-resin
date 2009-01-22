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

import com.caucho.bam.BamError;
import com.caucho.bam.BamService;
import com.caucho.bam.BamStream;
import com.caucho.config.ConfigException;
import com.caucho.hemp.packet.*;
import com.caucho.server.resin.*;
import com.caucho.server.util.*;
import com.caucho.util.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.lang.ref.*;
import java.io.Serializable;

/**
 * Queue of hmtp packets
 */
public class MemoryQueueServiceFilter implements BamService, Runnable
{
  private static final Logger log
    = Logger.getLogger(MemoryQueueServiceFilter.class.getName());
  private static final L10N L = new L10N(MemoryQueueServiceFilter.class);

  private final Executor _executor = ScheduledThreadPool.getLocal();
  private final ClassLoader _loader
    = Thread.currentThread().getContextClassLoader();
  
  private final BamService _service;
  private final BamStream _brokerStream;

  private int _threadSemaphore;

  private Packet []_queue = new Packet[32];
  private int _head;
  private int _tail;

  public MemoryQueueServiceFilter(BamService service,
				  BamStream brokerStream,
				  int threadMax)
  {
    if (service == null)
      throw new NullPointerException();

    if (brokerStream == null)
      throw new NullPointerException();
    
    _service = service;
    _brokerStream = brokerStream;
    _threadSemaphore = threadMax;
  }
  
  /**
   * Returns the service's jid
   */
  public String getJid()
  {
    return _service.getJid();
  }
  
  /**
   * Returns the service's jid
   */
  public void setJid(String jid)
  {
  }

  /**
   * Sends a message
   */
  public void message(String to, String from, Serializable value)
  {
    enqueue(new Message(to, from, value));
  }

  /**
   * Sends a message
   */
  public void messageError(String to,
			       String from,
			       Serializable value,
			       BamError error)
  {
    enqueue(new MessageError(to, from, value, error));
  }

  /**
   * Query an entity
   */
  public boolean queryGet(long id,
			      String to,
			      String from,
			      Serializable query)
  {
    enqueue(new QueryGet(id, to, from, query));
    
    return true;
  }

  /**
   * Query an entity
   */
  public boolean querySet(long id,
			      String to,
			      String from,
			      Serializable query)
  {
    enqueue(new QuerySet(id, to, from, query));
    
    return true;
  }

  /**
   * Query an entity
   */
  public void queryResult(long id,
			      String to,
			      String from,
			      Serializable value)
  {
    enqueue(new QueryResult(id, to, from, value));
  }

  /**
   * Query an entity
   */
  public void queryError(long id,
			     String to,
			     String from,
			     Serializable query,
			     BamError error)
  {
    enqueue(new QueryError(id, to, from, query, error));
  }

  /**
   * Presence
   */
  public void presence(String to, String from, Serializable data)
  {
    enqueue(new Presence(to, from, data));
  }

  /**
   * Presence unavailable
   */
  public void presenceUnavailable(String to,
				      String from,
				      Serializable data)
  {
    enqueue(new PresenceUnavailable(to, from, data));
  }

  /**
   * Presence probe
   */
  public void presenceProbe(String to,
			        String from,
			        Serializable data)
  {
    enqueue(new PresenceProbe(to, from, data));
  }

  /**
   * Presence subscribe
   */
  public void presenceSubscribe(String to,
				    String from,
				    Serializable data)
  {
    enqueue(new PresenceSubscribe(to, from, data));
  }

  /**
   * Presence subscribed
   */
  public void presenceSubscribed(String to,
				     String from,
				     Serializable data)
  {
    enqueue(new PresenceSubscribed(to, from, data));
  }

  /**
   * Presence unsubscribe
   */
  public void presenceUnsubscribe(String to,
				      String from,
				      Serializable data)
  {
    enqueue(new PresenceUnsubscribe(to, from, data));
  }
  
  /**
   * Presence unsubscribed
   */
  public void presenceUnsubscribed(String to,
				       String from,
				       Serializable data)
  {
    enqueue(new PresenceUnsubscribed(to, from, data));
  }

  /**
   * Presence error
   */
  public void presenceError(String to,
			        String from,
			        Serializable data,
			        BamError error)
  {
    enqueue(new PresenceError(to, from, data, error));
  }

  public BamStream getAgentStream()
  {
    return _service.getAgentStream();
  }
  
  /**
   * Requests that an agent with the given jid be started. 
   */
  public boolean startAgent(String jid)
  {
    return _service.startAgent(jid);
  }

  /**
   * Requests that an agent with the given jid be stopped. 
   */
  public boolean stopAgent(String jid)
  {
    return _service.stopAgent(jid);
  }

  /**
   * Called when an agent logs in
   */
  public void onAgentStart(String jid)
  {
    _service.onAgentStart(jid);
  }

  /**
   * Called when an agent logs out
   */
  public void onAgentStop(String jid)
  {
    _service.onAgentStop(jid);
  }
  
  /**
   * Returns a filter for outbound calls, i.e. filtering messages to the agent.
   */
  public BamStream getAgentFilter(BamStream stream)
  {
    return _service.getAgentFilter(stream);
  }

  /**
   * Returns a filter for inbound calls, i.e. filtering messages to the broker
   * from the agent.
   */
  public BamStream getBrokerFilter(BamStream stream)
  {
    return _service.getBrokerFilter(stream);
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
    synchronized (this) {
      if (_head == _tail) {
	_threadSemaphore++;
	
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
	
	packet.dispatch(getAgentStream(), _brokerStream);
	
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
    return getClass().getSimpleName() + "[" + _service + "]";
  }
}
