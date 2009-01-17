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

import com.caucho.bam.BamStream;
import com.caucho.bam.BamError;

import com.caucho.hemp.*;
import com.caucho.util.*;
import java.io.Serializable;
import java.util.logging.*;

/**
 * Handles packets sent to the connection
 */
class HempConnectionAgentStream implements BamStream
{
  private static final Logger log
    = Logger.getLogger(HempConnectionAgentStream.class.getName());
  
  private static final L10N L = new L10N(HempConnectionAgentStream.class);

  private final HempConnectionImpl _conn;
  private final String _jid;

  private BamStream _agentStream;

  HempConnectionAgentStream(HempConnectionImpl conn)
  {
    _conn = conn;
    _jid = conn.getJid();
  }
  
  /**
   * Returns the agent's jid
   */
  public String getJid()
  {
    return _jid;
  }

  //
  // handler registration
  //

  /**
   * Registers the handler
   */
  void setAgentStream(BamStream handler)
  {
    _agentStream = handler;
  }

  /**
   * Registers the handler
   */
  BamStream getAgentStream()
  {
    return _agentStream;
  }

  //
  // message handling
  //

  /**
   * Forwards the message
   */
  public void message(String to, String from, Serializable value)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendMessage (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.message(to, from, value);
  }

  /**
   * Forwards the message
   */
  public void messageError(String to,
			       String from,
			       Serializable value,
			       BamError error)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendMessageError (no handler) to=" + to
		 + " from=" + from + " error=" + error);
      return;
    }
    
    handler.messageError(to, from, value, error);
  }

  //
  // Query/RPC handling
  //

  /**
   * Forwards the message
   */
  public boolean queryGet(long id,
			      String to,
			      String from,
			      Serializable query)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendQueryGet (no handler) to=" + to + " from=" + from);
      return false;
    }
    
    if (handler.queryGet(id, to, from, query))
      return true;

    BamError error = 
      new BamError(BamError.TYPE_CANCEL,
	            BamError.FEATURE_NOT_IMPLEMENTED,
		    "unknown query: " + query.getClass().getName());
    
     _conn.getBrokerStream().queryError(id, from, to, query, error);
   
    return true;
  }

  /**
   * Forwards the message
   */
  public boolean querySet(long id,
		            String to,
		            String from,
		            Serializable query)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendQuerySet (no handler) to=" + to + " from=" + from);
      return false;
    }
    
    if (handler.querySet(id, to, from, query))
      return true;

    BamError error =
      new BamError(BamError.TYPE_CANCEL,
		    BamError.FEATURE_NOT_IMPLEMENTED,
		    "unknown query: " + query.getClass().getName());
    
    _conn.getBrokerStream().queryError(id, from, to, query, error);

    return true;
  }

  /**
   * Result from the message
   */
  public void queryResult(long id,
		            String to,
		            String from,
		            Serializable value)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (_conn.onQueryResult(id, to, from, value))
	return;
      
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendQueryResult (no handler) to=" + to + " from=" + from);
      return;
    }

    handler.queryResult(id, to, from, value);
  }

  /**
   * Error from the message
   */
  public void queryError(long id,
		           String to,
		           String from,
		           Serializable query,
		           BamError error)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (_conn.onQueryError(id, to, from, query, error))
	return;
      
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendQueryError (no handler) to=" + to + " from=" + from);
      return;
    }

    handler.queryError(id, to, from, query, error);
  }

  //
  // presence handling
  //

  /**
   * Forwards the presence
   */
  public void presence(String to, String from, Serializable data)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresence (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.presence(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void presenceProbe(String to,
				String from,
				Serializable data)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceProbe (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.presenceProbe(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void presenceUnavailable(String to,
				      String from,
				      Serializable data)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceUnavailable (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.presenceUnavailable(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void presenceSubscribe(String to,
				    String from,
				    Serializable data)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceSubscribe (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.presenceSubscribe(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void presenceSubscribed(String to,
				     String from,
				     Serializable data)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceSubscribed (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.presenceSubscribed(to, from, data);
  }
  
  /**
   * Forwards the presence
   */
  public void presenceUnsubscribe(String to,
				      String from,
				      Serializable data)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceUnsubscribe (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.presenceUnsubscribe(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void presenceUnsubscribed(String to,
				       String from,
				       Serializable data)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceUnsubscribed (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.presenceUnsubscribed(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void presenceError(String to,
				String from,
				Serializable data,
				BamError error)
  {
    BamStream handler = _agentStream;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceError (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.presenceError(to, from, data, error);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
