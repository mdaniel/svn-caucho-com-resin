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

package com.caucho.hemp.im;

import com.caucho.hmpp.HmppBroker;
import com.caucho.hmpp.HmppResource;
import com.caucho.hmpp.disco.*;
import com.caucho.hmpp.spi.HmppServer;
import com.caucho.hmpp.AbstractResource;
import java.io.Serializable;
import java.util.*;
import java.util.logging.*;


/**
 * Resource representing an IM user
 */
public class ImResource extends AbstractResource
{
  private static final Logger log
    = Logger.getLogger(ImResource.class.getName());
  
  private HmppServer _server;

  private ArrayList<String> _jidList
    = new ArrayList<String>();
  
  private String []_jids = new String[0];
  
  public ImResource()
  {
  }
  
  public ImResource(HmppServer server, String jid)
  {
    if (server == null)
      throw new NullPointerException("server may not be null");
    
    setJid(jid);

    _server = server;
  }

  public void onLogin(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " login(" + jid + ")");
    
    synchronized (_jidList) {
      if (! _jidList.contains(jid))
	_jidList.add(jid);

      _jids = new String[_jidList.size()];
      _jidList.toArray(_jids);
    }
  }

  public void onLogout(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " logout(" + jid + ")");
    
    synchronized (_jidList) {
      _jidList.remove(jid);

      _jids = new String[_jidList.size()];
      _jidList.toArray(_jids);
    }
  }

  public void onMessage(String to, String from, Serializable value)
  {
    String []jids = _jids;

    for (String jid : jids) {
      // XXX: is the "to" correct?
      _server.sendMessage(jid, from, value);
    }
  }
  
  public boolean onQueryGet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    if (query instanceof DiscoInfoQuery) {
      DiscoInfoQuery info = new DiscoInfoQuery(getDiscoIdentity(),
					       getDiscoFeatures());
      
      _server.queryResult(id, from, to, info);
      
      return true;
    }
    
    return false;
  }

  protected DiscoIdentity []getDiscoIdentity()
  {
    return new DiscoIdentity[] {
      new DiscoIdentity("foo", "bar"),
    };
  }

  protected DiscoFeature []getDiscoFeatures()
  {
    return new DiscoFeature[] {
      new DiscoFeature(DiscoInfoQuery.class.getName()),
    };
  }
}
