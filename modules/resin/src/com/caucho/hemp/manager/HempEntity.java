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

package com.caucho.hemp.manager;

import java.util.*;
import java.lang.ref.*;
import java.io.Serializable;

import com.caucho.hemp.*;
import com.caucho.server.resin.*;
import com.caucho.util.*;

/**
 * Entity
 */
class HempEntity {
  private static final L10N L = new L10N(HempEntity.class);
  
  private final HempManager _manager;
  private final String _uid;
  
  private final ArrayList<WeakReference<HempSession>> _sessionList
    = new ArrayList<WeakReference<HempSession>>();

  HempEntity(HempManager manager, String uid)
  {
    _manager = manager;
    _uid = uid;
  }

  void addSession(HempSession session)
  {
    synchronized (_sessionList) {
      _sessionList.add(new WeakReference<HempSession>(session));
    }
  }

  void removeSession(HempSession session)
  {
    synchronized (_sessionList) {
      for (int i = _sessionList.size() - 1; i >= 0; i--) {
	WeakReference<HempSession> sessionRef = _sessionList.get(i);
	HempSession hempSession = sessionRef.get();

	if (hempSession == null || session == hempSession)
	  _sessionList.remove(i);
      }
    }
  }

  void onMessage(String fromJid, String toJid, Serializable value)
  {
    HempSession []sessionArray = null;
    
    synchronized (_sessionList) {
      if (_sessionList.size() == 0)
	return;

      sessionArray = new HempSession[_sessionList.size()];
      
      for (int i = _sessionList.size() - 1; i >= 0; i--) {
	WeakReference<HempSession> sessionRef = _sessionList.get(i);
	HempSession hempSession = sessionRef.get();

	if (hempSession != null)
	  sessionArray[i] = hempSession;
	else
	  _sessionList.remove(i);
      }
    }

    for (HempSession session : sessionArray) {
      if (session != null)
	session.onMessage(fromJid, toJid, value);
    }
  }

  Serializable onQuery(String fromJid, String toJid, Serializable query)
  {
    HempSession []sessionArray = null;
    
    synchronized (_sessionList) {
      if (_sessionList.size() == 0)
	return null;

      sessionArray = new HempSession[_sessionList.size()];
      
      for (int i = _sessionList.size() - 1; i >= 0; i--) {
	WeakReference<HempSession> sessionRef = _sessionList.get(i);
	HempSession hempSession = sessionRef.get();

	if (hempSession != null)
	  sessionArray[i] = hempSession;
	else
	  _sessionList.remove(i);
      }
    }

    for (HempSession session : sessionArray) {
      if (session != null) {
	Serializable result = session.onQuery(fromJid, toJid, query);

	if (result != null)
	  return result;
      }
    }

    throw new RuntimeException(L.l("'{0}' is an unknown query", query));
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _uid + "]";
  }
}
