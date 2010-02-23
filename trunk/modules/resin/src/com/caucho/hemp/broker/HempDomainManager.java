/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.hemp.broker;

import com.caucho.bam.*;
import com.caucho.hemp.*;
import com.caucho.loader.*;
import com.caucho.server.cluster.*;
import com.caucho.server.host.*;
import com.caucho.server.resin.*;
import com.caucho.util.*;
import java.util.*;
import java.util.logging.*;
import java.lang.ref.*;
import java.io.Serializable;


/**
 * Domain manager for foreign domains
 */
public class HempDomainManager extends DomainManager
{
  private static final Logger log
    = Logger.getLogger(HempDomainManager.class.getName());
  private static final L10N L = new L10N(HempDomainManager.class);
  
  // domains
  private final HashMap<String,WeakReference<ActorStream>> _domainMap
    = new HashMap<String,WeakReference<ActorStream>>();

  public HempDomainManager()
  {
  }

  public void addDomain(String name, ActorStream domain)
  {
    synchronized (_domainMap) {
      _domainMap.put(name, new WeakReference<ActorStream>(domain));
    }

    if (log.isLoggable(Level.FINER))
      log.finer(this + " add " + domain + " as '" + name + "'");
  }

  public ActorStream removeDomain(String name)
  {
    WeakReference<ActorStream> domainRef = null;
    
    synchronized (_domainMap) {
      domainRef = _domainMap.remove(name);
    }

    if (domainRef != null) {
      if (log.isLoggable(Level.FINER))
	log.finer(this + " remove " + name);
      
      return domainRef.get();
    }
    else
      return null;
  }

  public ActorStream findDomain(String name)
  {
    WeakReference<ActorStream> domainRef = null;
    
    synchronized (_domainMap) {
      domainRef = _domainMap.get(name);
    }

    if (domainRef != null)
      return domainRef.get();

    Server server = Server.getCurrent();

    if (server == null)
      return null;
    
    Host host = server.getHost(name, 5222);

    /*
    if (host == null)
      return null;

    ActorStream domain = host.getBamDomain();

    synchronized (_domainMap) {
      _domainMap.put(name, new WeakReference<ActorStream>(domain));
    }
    */

    return null;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
