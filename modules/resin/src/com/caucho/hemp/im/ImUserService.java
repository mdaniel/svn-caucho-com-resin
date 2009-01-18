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

import com.caucho.bam.BamStream;
import com.caucho.bam.SimpleBamService;
import com.caucho.xmpp.im.RosterQuery;
import com.caucho.xmpp.im.RosterItem;
import com.caucho.xmpp.disco.DiscoInfoQuery;
import com.caucho.xmpp.disco.DiscoIdentity;
import com.caucho.xmpp.disco.DiscoFeature;
import java.io.Serializable;
import java.util.*;
import java.util.logging.*;


/**
 * Resource representing an IM user
 */
public class ImUserService extends SimpleBamService
{
  private static final Logger log
    = Logger.getLogger(ImUserService.class.getName());
  
  private ArrayList<String> _jidList
    = new ArrayList<String>();
  
  private String []_jids = new String[0];
  
  public ImUserService()
  {
  }
  
  public ImUserService(String jid)
  {
    setJid(jid);
  }

  public String []getJids()
  {
    return _jids;
  }

  /**
   * Creates an inbound filter
   */
  @Override
  public BamStream getBrokerFilter(BamStream stream)
  {
    return new ImBrokerFilter(stream, this);
  }

  @Override
  public void onAgentStart(String jid)
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

  @Override
  public void onAgentStop(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " logout(" + jid + ")");
    
    synchronized (_jidList) {
      _jidList.remove(jid);

      _jids = new String[_jidList.size()];
      _jidList.toArray(_jids);
    }
  }

  @Override
  public void message(String to, String from, Serializable value)
  {
    String []jids = _jids;

    for (String jid : jids) {
      // XXX: is the "to" correct?
      getBrokerStream().message(jid, from, value);
    }
  }
  
  @Override
  public boolean queryGet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    if (query instanceof DiscoInfoQuery) {
      DiscoInfoQuery info = new DiscoInfoQuery(getDiscoIdentity(),
					       getDiscoFeatures());
      
      getBrokerStream().queryResult(id, from, to, info);
      
      return true;
    }
    else if (query instanceof RosterQuery) {
      getBrokerStream().queryResult(id, from, to, new RosterQuery(getRoster()));

      return true;
    }
    
    return false;
  }
  
  @Override
  public boolean querySet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    if (query instanceof RosterQuery) {
      RosterQuery roster = (RosterQuery) query;

      return querySetRoster(id, to, from, roster);
    }
    
    return false;
  }

  /**
   * forwards presence to logged in resources
   */
  @Override
  public void presence(String to, String from, Serializable data)
  {
    String []jids = _jids;

    for (String jid : jids) {
      getBrokerStream().presence(jid, from, data);
    }
  }

  /**
   * forwards presence to logged in resources
   */
  @Override
  public void presenceProbe(String to, String from, Serializable data)
  {
    String []jids = _jids;

    for (String jid : jids) {
      getBrokerStream().presenceProbe(jid, from, data);
    }
  }

  /**
   * forwards presence to logged in resources
   */
  @Override
  public void presenceUnavailable(String to, String from,
				      Serializable data)
  {
    String []jids = _jids;

    for (String jid : jids) {
      getBrokerStream().presenceUnavailable(jid, from, data);
    }
  }

  /**
   * Presence from self
   */
  public void sendPresence(String from, Serializable data)
  {
    for (RosterItem item : getRoster()) {
      String subscription = item.getSubscription();

      if ("from".equals(subscription)
	  || "both".equals(subscription)) {
	getBrokerStream().presence(item.getJid(), getJid(), data);
      }
      
      if ("to".equals(subscription)
	  || "both".equals(subscription)) {
	getBrokerStream().presenceProbe(item.getJid(), getJid(), data);
      }
    }
    
    for (String jid : _jids) {
      if (! jid.equals(from)) {
	getBrokerStream().presence(jid, from, data);
      }
    }
  }

  @Override
  public void presenceSubscribe(String to,
				    String from,
				    Serializable data)
  {
    if (! rosterSubscribeFrom(to, from, data)) {
      log.fine(this + " sendPresenceSubscribe denied from=" + from);

      return;
    }
    
    String []jids = _jids;

    if (jids.length > 0) {
      getBrokerStream().presenceSubscribe(jids[0], from, data);
    }
    else {
      log.fine(this + " onPresenceSubscribe to=" + to);
    }
  }

  @Override
  public void presenceSubscribed(String to,
				     String from,
				     Serializable data)
  {
    // complete subscription from the target
    if (rosterSubscribedFrom(to, from, data)) {
      String []jids = _jids;

      if (jids.length > 0) {
	getBrokerStream().presenceSubscribed(jids[0], from, data);
      }
    }
  }

  private boolean querySetRoster(long id,
				 String to,
				 String from,
				 RosterQuery roster)
  {
    String user = from;
    int p = from.indexOf('/');
    if (p > 0)
      user = from.substring(0, p);

    if (! user.equals(to)) {
      if (log.isLoggable(Level.FINE)) {
	log.fine(this + " roster set with non-matching to='"
		 + to + "' from='" + from + "'");
      }
	
      return false;
    }
      
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " roster set to='" + to + "' from='" + from + "'");
    }

    if (roster.getItems() != null) {
      for (RosterItem item : roster.getItems()) {
	if ("remove".equals(item.getSubscription()))
	  removeRoster(item);
	else
	  updateRoster(item);

      }
    }
    
    querySetResources(roster);

    getBrokerStream().queryResult(id, from, to, null);

    return true;
  }

  /**
   * Returns the disco identity of the resource
   */
  protected DiscoIdentity []getDiscoIdentity()
  {
    return new DiscoIdentity[] {
      new DiscoIdentity("foo", "bar"),
    };
  }

  /**
   * Returns the disco features of the resource
   */
  protected DiscoFeature []getDiscoFeatures()
  {
    return new DiscoFeature[] {
      new DiscoFeature(DiscoInfoQuery.class.getName()),
    };
  }

  /**
   * add/update a roster item
   */
  protected void updateRoster(RosterItem item)
  {
  }

  /**
   * remove a roster item
   */
  protected void removeRoster(RosterItem item)
  {
  }

  /**
   * client requests a subscription to this resource
   */
  protected boolean rosterSubscribeTo(String to,
				      String from,
				      Serializable data)
  {
    return true;
  }

  /**
   * this resource requests a subscription from the target
   */
  protected boolean rosterSubscribeFrom(String to,
					String from,
					Serializable data)
  {
    return true;
  }

  /**
   * subscribe to a roster item
   */
  protected boolean rosterSubscribedTo(String to,
				     String from,
				     Serializable data)
  {
    return true;
  }

  /**
   * add a subscription from a roster item
   */
  protected boolean rosterSubscribedFrom(String to,
					 String from,
					 Serializable data)
  {
    return true;
  }

  /**
   * Gets the roster items
   */
  protected RosterItem []getRoster()
  {
    return new RosterItem [0];
  }

  /**
   * Updates resources with the given query
   */
  protected void querySetResources(Serializable query)
  {
  }
}
