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

import com.caucho.hemp.packet.*;
import com.caucho.bam.BamStream;
import com.caucho.bam.BamError;
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
public class HempMemoryQueue implements BamStream, Runnable
{
  private static final Logger log
    = Logger.getLogger(HempMemoryQueue.class.getName());
  private static final L10N L = new L10N(HempMemoryQueue.class);

  private static long _gid;

  // how long the thread should wait for a new request before exiting
  private long _queueIdleTimeout = 1000L;
  
  private final Executor _executor = ScheduledThreadPool.getLocal();
  private final ClassLoader _loader
    = Thread.currentThread().getContextClassLoader();
  private final BamStream _agentStream;
  private final BamStream _brokerStream;

  private int _threadSemaphore;

  private Packet []_queue = new Packet[32];
  private int _head;
  private int _tail;

  private String _name;

  private volatile boolean _isClosed;

  public HempMemoryQueue(BamStream agentStream,
			 BamStream brokerStream)
  {
    this(null, agentStream, brokerStream);
  }

  public HempMemoryQueue(String name,
			 BamStream agentStream,
			 BamStream brokerStream)
  {
    if (agentStream == null)
      throw new NullPointerException();

    if (brokerStream == null)
      throw new NullPointerException();
    
    _agentStream = agentStream;
    _brokerStream = brokerStream;
    _threadSemaphore = 1;

    if (name == null)
      name = _agentStream.getJid();

    _name = name;
  }
  
  /**
   * Returns the agent's jid
   */
  public String getJid()
  {
    return _agentStream.getJid();
  }

  /**
   * Returns true if a message is available.
   */
  public boolean isPacketAvailable()
  {
    return _head != _tail;
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
    if (from == null) {
      Thread.dumpStack();
      throw new NullPointerException();
    }
    
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

  protected BamStream getAgentStream()
  {
    return _agentStream;
  }

  protected void enqueue(Packet packet)
  {
    if (log.isLoggable(Level.FINEST)) {
      int size = (_head - _tail + _queue.length) % _queue.length;
      log.finest(this + " enqueue(" + size + ") " + packet);
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

      notifyAll();

      if (_threadSemaphore > 0 && ! _isClosed) {
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
	try {
	  if (! _isClosed)
	    wait(_queueIdleTimeout);
	} catch (Exception e) {
	  log.log(Level.FINEST, e.toString(), e);
	}

	if (_head == _tail) {
	  _threadSemaphore++;
	  
	  return null;
	}
      }
      
      if (_isClosed)
	return null;

      Packet packet = _queue[_tail];
      
      _tail = (_tail + 1) % _queue.length;

      return packet;
    }
  }

  public void run()
  {
    Packet packet;

    Thread thread = Thread.currentThread();

    String name = _name;
    
    thread.setName(name + "-" + _gid++);

    thread.setContextClassLoader(_loader);

    while ((packet = dequeue()) != null) {
      boolean isValid = false;
      
      try {
	if (log.isLoggable(Level.FINEST))
	  log.finest(this + " dequeue " + packet);
	
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

  public void close()
  {
    _isClosed = true;

    synchronized (this) {
      notifyAll();
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _agentStream + "]";
  }
}
