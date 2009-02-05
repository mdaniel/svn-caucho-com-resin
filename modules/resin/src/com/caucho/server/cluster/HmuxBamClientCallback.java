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
import com.caucho.config.ConfigException;
import com.caucho.hessian.io.ExtSerializerFactory;
import com.caucho.hessian.io.Hessian2StreamingInput;
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

import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.*;

class HmuxBamClientCallback implements Runnable
{
  private static final L10N L = new L10N(HmuxBamClientCallback.class);
  private static final Logger log
    = Logger.getLogger(HmuxBamClientCallback.class.getName());

  private HmuxBamClient _client;
  private HmuxBamConnection _conn;
  private volatile ReadStream _is;
  private Hessian2StreamingInput _in;

  private ClassLoader _loader;

  private String _jid;

  HmuxBamClientCallback(HmuxBamClient client,
			HmuxBamConnection conn,
			ReadStream is)
    throws IOException
  {
    _client = client;
    _conn = conn;
    _is = is;

    int code = is.read();

    _jid = readStringValue(is);

    _loader = Thread.currentThread().getContextClassLoader();
    if (_loader == null)
      _loader = ClassLoader.getSystemClassLoader();

    Thread thread = new Thread(this);
    thread.setName("bam-client-" + _jid);
    thread.setDaemon(true);
    thread.start();
  }

  public String getJid()
  {
    return _jid;
  }

  //
  // runnable
  //

  public void run()
  {
    Thread thread = Thread.currentThread();

    thread.setContextClassLoader(_loader);
    
    ReadStream is;
    
    while ((is = _is) != null) {
      try {
	int ch = is.read();

	switch (ch) {
	case -1:
	  log.fine(this + " end of file");
	  close();
	  return;
	  
	case HmuxRequest.HMUX_EXIT:
	  log.fine(this + " -r: " + (char) ch + " exit");
	  close();
	  return;

	case HmuxRequest.HMTP_QUERY_RESULT:
	  readQueryResult(is);
	  break;

	case HmuxRequest.HMTP_QUERY_ERROR:
	  readQueryError(is);
	  break;

	case HmuxRequest.HMUX_YIELD:
	case HmuxRequest.HMUX_QUIT:
	  if (log.isLoggable(Level.FINE))
	    log.fine(this + " r-" + (char) ch);
	  break;
	  
	default:
	  System.out.println("CODE: " + (char) ch);
	  
	  log.fine(this + " unknown code " + (char) ch);
	  int len = (is.read() << 8) + is.read();
	  is.skip(len);
	  break;
	}
      } catch (Exception e) {
	close();

	return;
      }
    }
  }

  //
  // message reading
  //

  private void readQueryResult(ReadStream is)
    throws IOException
  {
    long id = readLongValue(is);
    String to = readString(is);
    String from = readString(is);

    Serializable query = (Serializable) readObject();

    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " r: HMTP queryResult("
		+ query + ", id=" + id
		+ " to=" + to + " from=" + from + ")");
    }

    _client.onQueryResult(id, to, from, query);
  }

  private void readQueryError(ReadStream is)
    throws IOException
  {
    long id = readLongValue(is);
    String to = readString(is);
    String from = readString(is);

    Serializable query = (Serializable) readObject();
    ActorError error = (ActorError) readObject();

    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " r: HMTP queryError("
		+ query + ", error=" + error + ", id=" + id
		+ " to=" + to + " from=" + from + ")");
    }

    _client.onQueryError(id, to, from, query, error);
  }

  public void close()
  {
    _conn = null;
    _is = null;
  }

  private String readString(ReadStream is)
    throws IOException
  {
    int code = is.read();

    return readStringValue(is);
  }

  private String readStringValue(ReadStream is)
    throws IOException
  {
    int len = (is.read() << 8) + is.read();
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++)
      sb.append((char) is.read());

    return sb.toString();
  }

  private long readLongValue(ReadStream is)
    throws IOException
  {
    int len = (is.read() << 8) + is.read();

    return (((is.read() & 0xffL) << 56)
	    + ((is.read() & 0xffL) << 48)
	    + ((is.read() & 0xffL) << 40)
	    + ((is.read() & 0xffL) << 32)
	    + ((is.read() & 0xffL) << 24)
	    + ((is.read() & 0xffL) << 16)
	    + ((is.read() & 0xffL) << 8)
	    + ((is.read() & 0xffL)));
  }

  private Object readObject()
    throws IOException
  {
    if (_in == null) {
      InputStream is = _is;

      /*
      if (log.isLoggable(Level.FINEST)) {
	is = new HessianDebugInputStream(is, log, Level.FINEST);
	is.startTop2();
      }
      */
	
      _in = new Hessian2StreamingInput(is);
    }

    return _in.readObject();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _conn + "]";
  }
}

