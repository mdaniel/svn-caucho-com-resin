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

package com.caucho.hemp.muc.memory;

import com.caucho.xmpp.muc.MucUserPresence;
import com.caucho.xmpp.im.ImMessage;
import com.caucho.bam.*;
import com.caucho.hemp.broker.GenericService;
import java.io.Serializable;
import java.util.*;
import java.util.logging.*;

/**
 * Multiuser chat room (xep-0045)
 */
public class MemoryRoom extends GenericService
{
  private static final Logger log
    = Logger.getLogger(MemoryRoom.class.getName());

  private static final String MUC_PERSISTENT_FEATURE
    = "muc_persistent";

  private HashMap<String,MemoryNick> _nicknameMap
    = new HashMap<String,MemoryNick>();

  private ArrayList<MemoryNick> _users
    = new ArrayList<MemoryNick>();

  private MemoryNick []_userArray = new MemoryNick[0];

  @Override
  protected String getDiscoCategory()
  {
    return "conference";
  }

  @Override
  protected String getDiscoType()
  {
    return "text";
  }

  @Override
  protected void getDiscoFeatureNames(ArrayList<String> featureNames)
  {
    super.getDiscoFeatureNames(featureNames);
    
    featureNames.add(MUC_PERSISTENT_FEATURE);
  }

  @Override
  public boolean startAgent(String jid)
  {
    synchronized (_nicknameMap) {
      MemoryNick nick = _nicknameMap.get(jid);

      if (nick == null) {
        nick = new MemoryNick(this, jid);
        getBroker().addService(nick);

        _nicknameMap.put(jid, nick);
      }

      return true;
    }
  }

  @Message
  public void handleImMessage(String to, String from, ImMessage msg)
  {
    MemoryNick nick = getNick(from);

    if (nick == null) {
      log.warning(this + " sendMessage unknown user from=" + from);
      return;
    }

    if (! "groupchat".equals(msg.getType())) {
      log.fine(this + " sendMessage expects 'groupchat' at type='"
	       + msg.getType() + "' from='" + from + "'");
      return;
    }

    // XXX: check for voice

    MemoryNick []users = _userArray;
    for (MemoryNick user : users) {
      getBrokerStream().message(user.getUserJid(), nick.getJid(), msg);
    }
  }

  public MemoryNick getNick(String from)
  {
    MemoryNick []users = _userArray;
    for (MemoryNick user : users) {
      if (user.getUserJid().equals(from))
	return user;
    }

    return null;
  }

  public BamStream getBrokerStream()
  {
    return super.getBrokerStream();
  }

  protected void addPresence(MemoryNick nick)
  {
    synchronized (_users) {
      if (! _users.contains(nick)) {
	_users.add(nick);
	_userArray = new MemoryNick[_users.size()];
	_users.toArray(_userArray);

	if (log.isLoggable(Level.FINE))
	  log.fine(this + " addPresence " + nick);
      }
    }

    MemoryNick []users = _userArray;

    // send information to the user about the current users
    for (MemoryNick user : users) {
      if (user == nick)
	continue;

      MucUserPresence presenceData = user.toPresenceData();

      getBrokerStream().presence(nick.getUserJid(), user.getJid(),
                                 presenceData);
    }

    // send presence about the new user to the current users
    for (MemoryNick user : users) {
      if (user == nick)
	continue;

      MucUserPresence presenceData = user.toPresenceData();

      getBrokerStream().presence(user.getUserJid(), nick.getJid(),
                                 presenceData);
    }

    // send presence about the user to itself
    MucUserPresence presenceData = nick.toPresenceData();
    presenceData.setStatus(new int[] { 110 });

    getBrokerStream().presence(nick.getUserJid(), nick.getJid(),
                               presenceData);
  }
}
