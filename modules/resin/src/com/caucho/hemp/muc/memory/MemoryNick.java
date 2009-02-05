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
import com.caucho.bam.SimpleActor;
import com.caucho.xmpp.im.ImMessage;
import java.io.Serializable;
import java.util.*;
import java.util.logging.*;

/**
 * Multiuser chat room nick (xep-0045)
 */
public class MemoryNick extends SimpleActor
{
  private static final Logger log
    = Logger.getLogger(MemoryNick.class.getName());

  private static final String MUC_PERSISTENT_FEATURE
    = "muc_persistent";

  private final MemoryRoom _room;
  
  private String _userJid;

  private String _affiliation = "member";
  private String _role = "participant";

  public MemoryNick(MemoryRoom room, String jid)
  {
    _room = room;
    
    setJid(jid);
  }

  public String getUserJid()
  {
    return _userJid;
  }

  public String getAffiliation()
  {
    return _affiliation;
  }

  public String getRole()
  {
    return _role;
  }

  @Override
  public void message(String to, String from, Serializable value)
  {
    MemoryNick user = _room.getNick(from);

    if (user == null) {
      log.warning(this + " sendMessage unknown user from=" + from);
      return;
    }

    if (! (value instanceof ImMessage)) {
      log.fine(this + " sendMessage with unknown value from=" + from
	       + " value=" + value);
      return;
    }

    ImMessage msg = (ImMessage) value;

    // XXX: check for voice

    _room.getBrokerStream().message(getUserJid(), user.getJid(), msg);
  }

  @Override
  public void presence(String to, String from, Serializable data)
  {
    _userJid = from;
    
    _room.addPresence(this);
  }

  public MucUserPresence toPresenceData()
  {
    MucUserPresence presence = new MucUserPresence();

    presence.setAffiliation(getAffiliation());
    presence.setRole(getRole());

    return presence;
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[" + getJid()
	    + ",user=" + getUserJid() + "]");
  }
}
