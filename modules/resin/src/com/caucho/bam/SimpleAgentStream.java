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

package com.caucho.bam;

import java.util.logging.*;
import java.util.concurrent.atomic.*;

import java.io.Serializable;

/**
 * Simple connections will receive packets using the SimpleBamClientStream
 */
public class SimpleBamClientStream implements BamStream
{
  private static final Logger log
    = Logger.getLogger(SimpleBamClientStream.class.getName());

  private final BamSkeleton _skeleton;
  private AbstractBamConnection _conn;

  public SimpleBamClientStream()
  {
    _skeleton = BamSkeleton.getBamSkeleton(getClass());
  }

  protected void setBamConnection(AbstractBamConnection bamConnection)
  {
    _conn = bamConnection;
  }

  /**
   * Returns the jid of the connection
   */
  public String getJid()
  {
    return _conn.getJid();
  }

  protected BamSkeleton getBamSkeleton()
  {
    return _skeleton;
  }
  
  /**
   * Returns the broker stream
   */
  public BamStream getBrokerStream()
  {
    return _conn.getBrokerStream();
  }
  
  /**
   * Callback to handle messages
   * 
   * @param to the target JID
   * @param from the source JID
   * @param value the message payload
   */
  public void message(String to, String from, Serializable value)
  {
    _skeleton.dispatchMessage(_conn, to, from, value);
  }
  
  /**
   * Callback to handle messages
   * 
   * @param to the target JID
   * @param from the source JID
   * @param value the message payload
   */
  public void messageError(String to,
			   String from,
			   Serializable value,
			   BamError error)
  {
    _skeleton.dispatchMessageError(_conn, to, from, value, error);
  }
  
  public boolean queryGet(long id,
			  String to,
			  String from,
			  Serializable query)
  {
    if (_skeleton.dispatchQueryGet(_conn, id, to, from, query)) {
      return true;
    }
    else {
      String msg = (this + " unknown queryGet " + query
		    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
    
      BamError error = new BamError(BamError.TYPE_CANCEL,
				    BamError.FEATURE_NOT_IMPLEMENTED,
				    msg);
				    
      getBrokerStream().queryError(id, from, to, query, error);
      
      if (log.isLoggable(Level.FINE)) {
	log.fine(msg);
      }

      return true;
    }
  }
  
  public boolean querySet(long id,
			  String to,
			  String from,
			  Serializable query)
  {
    if (_skeleton.dispatchQuerySet(_conn, id, to, from, query)) {
      return true;
    }
    else {
      String msg = (this + " unknown querySet " + query
		    + " {id:" + id + ", to:" + to + ", from:" + from + "}");
    
      BamError error = new BamError(BamError.TYPE_CANCEL,
				    BamError.FEATURE_NOT_IMPLEMENTED,
				    msg);
				    
      getBrokerStream().queryError(id, from, to, query, error);
      
      if (log.isLoggable(Level.FINE)) {
	log.fine(msg);
      }

      return true;
    }
  }
  
  public void queryResult(long id,
			  String to,
			  String from,
			  Serializable value)
  {
    if (! _skeleton.dispatchQueryResult(_conn, id, to, from, value)) {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " queryResult " + value + " {id:" + id
		  + ", to:" + to + ", from:" + from + "}");
      }

      _conn.onQueryResult(id, to, from, value);
    }
  }
  
  public void queryError(long id,
			 String to,
			 String from,
			 Serializable value,
			 BamError error)
  {
    if (! _skeleton.dispatchQueryError(this, id, to, from, value, error)) {
      if (log.isLoggable(Level.FINER)) {
	log.finer(this + " queryError " + error + " " + value + " {id:" + id
		  + ", from:" + from + "}");
      }
	
      _conn.onQueryError(id, to, from, value, error);
    }
  }
  
  /**
   * General presence, for clients announcing availability
   */
  public void presence(String to,
		       String from,
		       Serializable value)
  {
    _skeleton.dispatchPresence(_conn, to, from, value);
  }

  /**
   * General presence, for clients announcing unavailability
   */
  public void presenceUnavailable(String to,
				    String from,
				    Serializable value)
  {
    _skeleton.dispatchPresenceUnavailable(_conn, to, from, value);
  }

  /**
   * Presence probe from the server to a client
   */
  public void presenceProbe(String to,
			    String from,
			    Serializable value)
  {
    _skeleton.dispatchPresenceProbe(_conn, to, from, value);
  }

  /**
   * A subscription request from a client
   */
  public void presenceSubscribe(String to,
				  String from,
				  Serializable value)
  {
    _skeleton.dispatchPresenceSubscribe(_conn, to, from, value);
  }

  /**
   * A subscription response to a client
   */
  public void presenceSubscribed(String to,
				 String from,
				 Serializable value)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " presenceSubscribed to=" + to + " from=" + from
		+ " value=" + value);
    }

    _skeleton.dispatchPresenceSubscribed(_conn, to, from, value);
  }

  /**
   * An unsubscription request from a client
   */
  public void presenceUnsubscribe(String to,
				  String from,
				  Serializable value)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " presenceUnsubscribe to=" + to + " from=" + from
		+ " value=" + value);
    }

    _skeleton.dispatchPresenceUnsubscribe(_conn, to, from, value);
  }

  /**
   * A unsubscription response to a client
   */
  public void presenceUnsubscribed(String to,
				   String from,
				   Serializable value)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " presenceUnsubscribed to=" + to + " from=" + from
		+ " value=" + value);
    }

    _skeleton.dispatchPresenceUnsubscribed(_conn, to, from, value);
  }

  /**
   * An error response to a client
   */
  public void presenceError(String to,
			    String from,
			    Serializable value,
			    BamError error)
  {
    _skeleton.dispatchPresenceError(_conn, to, from, value, error);
  }

  /**
   * Close the stream
   */
  public void close()
  {
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
