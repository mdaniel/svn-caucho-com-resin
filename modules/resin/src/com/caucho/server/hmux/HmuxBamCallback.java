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

package com.caucho.server.hmux;

import com.caucho.hessian.io.*;
import com.caucho.bam.*;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.Server;
import com.caucho.server.connection.AbstractHttpRequest;
import com.caucho.server.connection.Connection;
import com.caucho.server.connection.HttpServletRequestImpl;
import com.caucho.server.connection.HttpServletResponseImpl;
import com.caucho.server.dispatch.DispatchServer;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.dispatch.InvocationDecoder;
import com.caucho.server.http.InvocationKey;
import com.caucho.server.port.ServerRequest;
import com.caucho.server.webapp.ErrorPageManager;
import com.caucho.util.*;
import com.caucho.vfs.ClientDisconnectException;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StreamImpl;
import com.caucho.vfs.WriteStream;

import java.io.*;
import java.net.InetAddress;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HmuxBamCallback extends AbstractActorStream
{
  private static final L10N L = new L10N(HmuxBamCallback.class);
  private static final Logger log
    = Logger.getLogger(HmuxBamCallback.class.getName());

  private HmuxRequest _request;

  public HmuxBamCallback(HmuxRequest request)
  {
    _request = request;
  }

  @Override
  public String getJid()
  {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public ActorStream getBrokerStream()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public void queryError(long id,
			 String to,
			 String from,
			 Serializable query,
			 ActorError error)
  {
    try {
      _request.writeHmtpQueryError(id, to, from, query, error);
      _request.writeFlush();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  @Override
  public void queryResult(long id,
			  String to,
			  String from,
			  Serializable query)
  {
    try {
      _request.writeHmtpQueryResult(id, to, from, query);
      _request.writeFlush();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void close()
  {
    _request = null;
  }
}
