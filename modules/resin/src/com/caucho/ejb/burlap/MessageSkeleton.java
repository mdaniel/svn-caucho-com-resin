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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.burlap;

import java.io.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.ejb.*;

import javax.jms.Destination;
import javax.jms.Session;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Connection;
import javax.jms.JMSException;

import com.caucho.util.CharBuffer;

import com.caucho.ejb.*;

import com.caucho.ejb.protocol.Skeleton;

import com.caucho.ejb.message.MessageServer;

import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import com.caucho.log.Log;

import com.caucho.burlap.io.*;

import com.caucho.services.message.MessageSender;

/**
 * Base class for any bean skeleton capable of handling an BURLAP-RPC request.
 *
 * <p/>Once selected, the calling servlet will dispatch the request through
 * the <code>_service</code> call.  After parsing the request headers,
 * <code>_service</code> calls the generated entry <code>_execute</code>
 * to execute the request.
 */
public class MessageSkeleton extends Skeleton {
  protected static Logger log = Log.open(MessageSkeleton.class);

  protected MessageServer _server;
  protected Connection _connection;

  protected Destination _destination;

  protected Session _session;
  
  protected MessageProducer _sender;

  MessageSkeleton(MessageServer server)
    throws JMSException
  {
    _server = server;
    _destination = server.getDestination();
    _connection = server.getJMSConnection();

    _session = _connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    _sender = _session.createProducer(server.getDestination());
  }

  /**
   * Sets the owning server.
   */
  void _setServer(AbstractServer server)
  {
    _server = (MessageServer) server;
  }

  /**
   * Sets the underlying object manipulated by the skeleton.
   */
  protected void _setObject(Object obj)
  {
  }

  /**
   * Services the request.
   *
   * <pre>
   * send(HashMap header, Object data)
   * </pre>
   *
   * @param rawIs the raw input stream from the servlet request
   * @param rawOs the raw output stream to the servlet response
   */
  public void _service(InputStream rawIs, OutputStream rawOs)
    throws Exception
  {
    BurlapInput in = new BurlapInput(rawIs);
    BurlapOutput out = new BurlapWriter(rawOs);

    try {
      in.startCall();
      
      String method = in.getMethod();

      if (! "send".equals(method))
        throw new IOException("no such method: " + method);
      
      HashMap headers = (HashMap) in.readObject();
      Object data = in.readObject();

      in.completeCall();

      if (_destination instanceof MessageSender) {
        ((MessageSender) _destination).send(headers, data);
      }
      else {
        Message message = null;

        if (data instanceof String) {
          message = _session.createTextMessage((String) data);
        }
        else
          message = _session.createObjectMessage((Serializable) data);

	_sender.send(message);
      }

      out.startReply();
      out.writeNull();
      out.completeReply();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      out.startReply();
      out.writeFault("SystemFault", String.valueOf(e), e);
      out.completeReply();
    }
  }

  protected void _execute(CharBuffer method,
                          BurlapInput in,
                          BurlapOutput out)
    throws Exception
  {
  }
}


