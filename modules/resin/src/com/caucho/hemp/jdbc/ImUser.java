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

package com.caucho.hemp.jdbc;

import com.caucho.bam.SimpleActor;
import com.caucho.xmpp.im.RosterQuery;
import com.caucho.xmpp.im.RosterItem;
import com.caucho.xmpp.disco.DiscoInfoQuery;
import com.caucho.xmpp.disco.DiscoIdentity;
import com.caucho.xmpp.disco.DiscoFeature;
import com.caucho.bam.ActorStream;
import java.io.*;
import java.util.*;
import java.util.logging.*;


/**
 * Resource representing an IM user
 */
public class ImUser extends SimpleActor
{
  private static final Logger log
    = Logger.getLogger(ImUser.class.getName());

  private JdbcServiceManager _manager;
  private ActorStream _broker;

  private long _dbId;

  private ArrayList<String> _jidList
    = new ArrayList<String>();

  private ArrayList<RosterItem> _rosterList
    = new ArrayList<RosterItem>();
  
  private String []_jids = new String[0];
  
  public ImUser()
  {
  }
  
  public ImUser(JdbcServiceManager manager, long dbId, String jid)
  {
    if (manager == null)
      throw new NullPointerException("server may not be null");

    _dbId = dbId;
    
    setJid(jid);

    _manager = manager;

    _broker = manager.getBroker().getBrokerStream();
  }

  public String []getJids()
  {
    return _jids;
  }

  /**
   * Creates an inbound filter
   */
  @Override
  public ActorStream getBrokerFilter(ActorStream stream)
  {
    return new ImBrokerFilter(stream, this);
  }

  @Override
  public void onChildStart(String jid)
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
  public void onChildStop(String jid)
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
      _broker.message(jid, from, value);
    }
  }
  
  @Override
  public void queryGet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    if (query instanceof DiscoInfoQuery) {
      DiscoInfoQuery info = new DiscoInfoQuery(getDiscoIdentity(),
					       getDiscoFeatures());
      
      _broker.queryResult(id, from, to, info);
    }
    else if (query instanceof RosterQuery) {
      _broker.queryResult(id, from, to, new RosterQuery(getRoster()));
    }

    // XXX:
  }
  
  @Override
  public void querySet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    if (query instanceof RosterQuery) {
      RosterQuery roster = (RosterQuery) query;

      querySetRoster(id, to, from, roster);
    }
  }

  /**
   * forwards presence to logged in resources
   */
  @Override
  public void presence(String to, String from, Serializable data)
  {
    String []jids = _jids;

    for (String jid : jids) {
      _broker.presence(jid, from, data);
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
      _broker.presenceProbe(jid, from, data);
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
      _broker.presenceUnavailable(jid, from, data);
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
	_broker.presence(item.getJid(), getJid(), data);
      }
      
      if ("to".equals(subscription)
	  || "both".equals(subscription)) {
	_broker.presenceProbe(item.getJid(), getJid(), data);
      }
    }
    
    for (String jid : _jids) {
      if (! jid.equals(from)) {
	_broker.presence(jid, from, data);
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
      _broker.presenceSubscribe(jids[0], from, data);
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
	_broker.presenceSubscribed(jids[0], from, data);
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

      InputStream is = null;

      synchronized (_rosterList) {
	is = _manager.serialize(_rosterList);
      }

      _manager.putData(getJid(), "jabber:im:roster", is);
    }
    
    querySetResources(roster);

    _broker.queryResult(id, from, to, null);

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
    RosterItem oldItem = findRoster(item.getJid());

    if (oldItem == null) {
      RosterItem newItem = new RosterItem(item.getJid(), item.getGroup());

      synchronized (_rosterList) {
	_rosterList.add(newItem);
      }
    }
  }

  /**
   * remove a roster item
   */
  protected void removeRoster(RosterItem item)
  {
  }

  protected RosterItem findRoster(String jid)
  {
    synchronized (_rosterList) {
      for (int i = 0; i < _rosterList.size(); i++) {
	RosterItem item = _rosterList.get(i);

	if (item.getJid().equals(jid))
	  return item;
      }
    }

    return null;
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
    Serializable data = _manager.getData(getJid(), "jabber:im:roster");

    if (data != null)
      _rosterList = (ArrayList<RosterItem>) data;
    
    RosterItem []rosterList = new RosterItem[_rosterList.size()];
    _rosterList.toArray(rosterList);
    
    return rosterList;
  }

  /**
   * Updates resources with the given query
   */
  protected void querySetResources(Serializable query)
  {
  }
}
