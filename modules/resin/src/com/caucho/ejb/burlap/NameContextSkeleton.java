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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.burlap;

import com.caucho.burlap.io.BurlapInput;
import com.caucho.burlap.io.BurlapOutput;
import com.caucho.ejb.protocol.EjbProtocolManager;
import com.caucho.ejb.protocol.Skeleton;
import com.caucho.ejb.server.AbstractServer;
import com.caucho.services.name.NameServerRemote;

import javax.ejb.EJBHome;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for any bean skeleton capable of handling a Burlap request.
 *
 * <p/>Once selected, the calling servlet will dispatch the request through
 * the <code>_service</code> call.  After parsing the request headers,
 * <code>_service</code> calls the generated entry <code>_execute</code>
 * to execute the request.
 */
public class NameContextSkeleton extends Skeleton {
  protected static final Logger log
    = Logger.getLogger(NameContextSkeleton.class.getName());

  private BurlapProtocol _protocol;
  private String _prefix;

  NameContextSkeleton(BurlapProtocol protocol, String prefix)
  {
    _protocol = protocol;
    _prefix = prefix;
  }

  /**
   * Services the request.
   */
  public void _service(InputStream is, OutputStream os)
    throws Exception
  {
    BurlapInput in = new BurlapReader(is);
    BurlapOutput out = new BurlapWriter(os);

    in.startCall();

    String method = in.getMethod();

    try {
      if (method.equals("lookup")
	  || method.equals("lookup_string")
	  || method.equals("lookup_1"))
        executeLookup(in, out);
      else if (method.equals("list"))
        executeList(in, out);
      else
        executeUnknown(method, in, out);
    } catch (BurlapProtocolException e) {
      throw e;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      out.startReply();
      out.writeFault("ServiceException", e.getMessage(), e);
      out.completeReply();
    }
  }

  private void executeLookup(BurlapInput in, BurlapOutput out)
    throws Throwable
  {
    String name = in.readString();
    in.completeCall();

    EjbProtocolManager container = _protocol.getProtocolManager();
    
    AbstractServer server = container.getServerByEJBName(name);

    if (server != null) {
      EJBHome home = server.getEJBHome();
      
      out.startReply();
      
      out.writeObject(home);

      out.completeReply();
    }
    else if (container.getRemoteChildren(name) != null) {
      out.startReply();

      String serverId = _prefix + name;
      if (serverId.startsWith("/"))
        serverId = serverId.substring(1);
      
      out.writeRemote(NameServerRemote.class.getName(),
                      _protocol.getURLPrefix() + serverId);
      
      out.completeReply();
    }
    else {
      out.startReply();

      out.writeNull();
      out.completeReply();
    }
  }

  private void executeList(BurlapInput in, BurlapOutput out)
    throws Throwable
  {
    in.completeCall();

    EjbProtocolManager container = _protocol.getProtocolManager();
    
    AbstractServer server = container.getServerByEJBName(_prefix);

    ArrayList children;
    
    if (server != null) {
      EJBHome home = server.getEJBHome();
      
      out.startReply();
      
      out.writeNull();

      out.completeReply();
    }
    else if ((children = container.getRemoteChildren(_prefix)) != null) {
      out.startReply();
      
      out.writeObject(children.toArray(new String[children.size()]));
      
      out.completeReply();
    }
    else {
      out.startReply();

      out.writeNull();
      out.completeReply();
    }
  }

  /**
   * Executes an unknown method.
   *
   * @param method the method name to match.
   * @param in the burlap input stream
   * @param out the burlap output stream
   */
  protected void executeUnknown(String method,
                                BurlapInput in, BurlapOutput out)
    throws Exception
  {
    if (method.equals("_burlap_getAttribute")) {
      String key = in.readString();
      in.completeCall();

      out.startReply();

      if ("java.api.class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else if ("java.home.class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else if ("java.object.class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else if ("home-class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else if ("remote-class".equals(key))
        out.writeString(NameServerRemote.class.getName());
      else
        out.writeNull();
      
      out.completeReply();
    }
    else {
      out.startReply();
      out.writeFault("NoMethod", "no such method: " + method, null);
      out.completeReply();
    }
  }
}
