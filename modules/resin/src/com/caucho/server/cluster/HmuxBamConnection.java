/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
 * @author Sam
 */

package com.caucho.server.cluster;

import com.caucho.bam.AbstractActorStream;
import com.caucho.bam.ActorError;
import com.caucho.bam.ActorStream;
import com.caucho.config.ConfigException;
import com.caucho.hessian.io.ExtSerializerFactory;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterPort;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.ClusterStream;
import com.caucho.server.cluster.HmuxBamClient;
import com.caucho.server.cluster.ServerPool;
import com.caucho.server.hmux.HmuxRequest;
import com.caucho.server.resin.Resin;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.Serializable;

class HmuxBamConnection extends AbstractActorStream
{
  private static final L10N L = new L10N(HmuxBamConnection.class);

  private HmuxBamClient _client;
  private ClusterStream _stream;
  private HmuxBamClientCallback _callback;

  private String _jid;

  HmuxBamConnection(HmuxBamClient client, ClusterStream stream)
    throws IOException
  {
    _client = client;
    _stream = stream;

    WriteStream out = stream.getWriteStream();
    ReadStream in = stream.getReadStream();

    /*
    out.write(HmuxRequest.ADMIN_CONNECT);
    out.write(0);
    out.write(0);
    out.flush();
    */

    _callback = new HmuxBamClientCallback(client, this, in);

    _jid = _callback.getJid();
  }

  public ActorStream getBrokerStream()
  {
    throw new UnsupportedOperationException();
  }

  //
  // predicates
  //

  /**
   * Returns true if the connection is closed
   */
  public boolean isClosed()
  {
    return _stream == null;
  }

  //
  // ActorStream API
  //

  /**
   * Returns the client jid
   */
  public String getJid()
  {
    return "client";
  }
  
  //
  // messages
  //
  
  /**
   * Sends a message to an agent
   * 
   * @param to the target agent's JID
   * @param from the source agent's JID
   * @param value the message payload
   */
  public void message(String to, String from, Serializable value)
  {
    throw new UnsupportedOperationException(String.valueOf(this));
  }
  
  /**
   * Sends a message error to an agent
   * 
   * @param to the target agent's JID
   * @param from the source agent's JID
   * @param value the message payload
   * @param error the message error
   */
  public void messageError(String to,
			   String from,
			   Serializable value,
			   ActorError error)
  {
    throw new UnsupportedOperationException(String.valueOf(this));
  }

  //
  // queries (iq)
  //
  
  /**
   * Sends a query information call (get), returning true if this
   * handler understands the query class, and false if it does not.
   *
   * If queryGet returns true, the handler MUST send a
   * <code>queryResult</code> or <code>queryError</code> to the sender,
   * using the same <code>id</code>.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the target JID
   * @param from the source JID, used as the target for the response
   * @param query the query payload
   *
   * @return true if this handler understand the query, false otherwise
   */
  public void queryGet(long id,
			  String to,
			  String from,
			  Serializable query)
  {
    try {
      ClusterStream stream = getStream();

      synchronized (stream) {
	stream.queryGet(id, to, getJid(), query);
	stream.writeYield();
      }
    } catch (IOException e) {
      e.printStackTrace();
      
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Sends a query update request (set), returning true if this handler
   * understands the query class, and false if it does not.
   *
   * If querySet returns true, the handler MUST send a
   * <code>queryResult</code> or <code>queryError</code> to the sender,
   * using the same <code>id</code>.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the target JID
   * @param from the source JID, used as the target for the response
   * @param query the query payload
   *
   * @return true if this handler understand the query, false otherwise
   */
  public void querySet(long id,
			  String to,
			  String from,
			  Serializable query)
  {
    try {
      ClusterStream stream = getStream();

      synchronized (stream) {
	stream.querySet(id, to, getJid(), query);
	stream.writeYield();
      }
    } catch (IOException e) {
      e.printStackTrace();
      
      throw new RuntimeException(e);
    }
  }

  /**
   * Handles the query response from a corresponding queryGet or querySet.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the target JID
   * @param from the source JID, used as the target for the response
   * @param value the result payload
   */
  public void queryResult(long id,
			  String to,
			  String from,
			  Serializable value)
  {
    throw new UnsupportedOperationException(String.valueOf(this));
  }
  
  /**
   * Handles the query error from a corresponding queryGet or querySet.
   *
   * @param id the query identifier used to match requests with responses
   * @param to the target JID
   * @param from the source JID, used as the target for the response
   * @param query the query payload
   * @param error additional error information
   */
  public void queryError(long id,
			 String to,
			 String from,
			 Serializable query,
			 ActorError error)
  {
    throw new UnsupportedOperationException(String.valueOf(this));
  }

  //
  // presence
  //

  /**
   * Announces an agent's presence, e.g. for an IM user logging on
   * or changing their status text.
   */
  public void presence(String to,
		       String from,
		       Serializable data)
  {
    throw new UnsupportedOperationException(String.valueOf(this));
  }

  /**
   * Announces a logout of an agent.
   */
  public void presenceUnavailable(String to,
				  String from,
				  Serializable data)
  {
    throw new UnsupportedOperationException(String.valueOf(this));
  }

  /**
   * Presence forwarding announcement from a server to a client.
   */
  public void presenceProbe(String to,
			    String from,
			    Serializable data)
  {
    throw new UnsupportedOperationException(String.valueOf(this));
  }

  /**
   * A subscription request from a client
   */
  public void presenceSubscribe(String to,
				String from,
				Serializable data)
  {
    throw new UnsupportedOperationException(String.valueOf(this));
  }

  /**
   * A subscription response to a client
   */
  public void presenceSubscribed(String to,
				 String from,
				 Serializable data)
  {
    throw new UnsupportedOperationException(String.valueOf(this));
  }

  /**
   * An unsubscription request from a client
   */
  public void presenceUnsubscribe(String to,
				  String from,
				  Serializable data)
  {
    throw new UnsupportedOperationException(String.valueOf(this));
  }

  /**
   * A unsubscription response to a client
   */
  public void presenceUnsubscribed(String to,
				   String from,
				   Serializable data)
  {
    throw new UnsupportedOperationException(String.valueOf(this));
  }

  /**
   * An error response to a client
   */
  public void presenceError(String to,
			    String from,
			    Serializable data,
			    ActorError error)
  {
    throw new UnsupportedOperationException(String.valueOf(this));
  }

  //
  // Stream management
  //

  public ClusterStream getStream()
  {
    return _stream;
  }

  /**
   * Closes the connection
   */
  public void close()
  {
    HmuxBamClientCallback callback = _callback;
    _callback = null;
    
    ClusterStream stream = _stream;
    _stream = null;

    if (callback != null)
      callback.close();

    if (stream != null)
      stream.close();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _stream + "]";
  }
}

