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

package com.caucho.jms.xmpp;

import com.caucho.jms.hub.*;
import com.caucho.server.connection.Connection;
import com.caucho.server.port.*;

import com.caucho.webbeans.manager.*;

import java.util.*;
import java.util.logging.*;
import javax.annotation.*;

/*
 * XMPP protocol server
 */
public class XmppProtocol extends Protocol
{
  private static final Logger log
    = Logger.getLogger(XmppProtocol.class.getName());
  
  private ClassLoader _loader;

  private HashMap<String,XmppPubSubLeaf> _pubSubMap
    = new HashMap<String,XmppPubSubLeaf>();
  
  public XmppProtocol()
  {
    setProtocolName("xmpp");

    _loader = Thread.currentThread().getContextClassLoader();
    Thread.dumpStack();
  }

  ClassLoader getClassLoader()
  {
    return _loader;
  }

  @PostConstruct
  public void init()
  {
    WebBeansContainer.create().addSingleton(this);
  }

  /**
   * Returns an new xmpp connection
   */
  @Override
  public ServerRequest createRequest(Connection connection)
  {
    return new XmppRequest(this, (TcpConnection) connection);
  }

  //
  // pub-sub stuff
  //
  
  public XmppPubSubLeaf createNode(String name)
  {
    synchronized (_pubSubMap) {
      XmppPubSubLeaf leaf = _pubSubMap.get(name);

      if (leaf == null) {
	if (log.isLoggable(Level.FINE))
	  log.fine(this + " create pub-sub node " + name);
	
	leaf = new XmppPubSubLeaf(name);
	_pubSubMap.put(name, leaf);
      }

      return leaf;
    }
  }
  
  public XmppPubSubLeaf getNode(String name)
  {
    synchronized (_pubSubMap) {
      return _pubSubMap.get(name);
    }
  }
}
