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

package com.caucho.hemp.service;

import com.caucho.config.*;
import com.caucho.hemp.manager.*;
import com.caucho.util.*;

import java.io.Serializable;
import java.util.*;
import java.util.logging.*;
import javax.annotation.*;
import javax.webbeans.*;

/**
 * Configuration for a service
 */
public class GenericService implements MessageListener, QueryListener {
  private static final L10N L = new L10N(GenericService.class);
  private static final Logger log
    = Logger.getLogger(GenericService.class.getName());
  
  private @In HempManager _manager;
  
  private String _name;
  private String _password;

  private HempSession _session;

  public void setName(String name)
  {
    _name = name;
  }

  public void setPassword(String password)
  {
    _password = password;
  }

  @PostConstruct
  private void init()
  {
    if (_name == null)
      throw new ConfigException(L.l("{0} requires a name",
				    getClass().getSimpleName()));

    _session = _manager.createSession(_name, _password);

    _session.setMessageListener(this);
    _session.setQueryListener(this);

    if (log.isLoggable(Level.FINE))
      log.fine(this + " init");
  }

  /**
   * Handles an incoming message
   */
  public void onMessage(String fromJid, String toJid, Serializable value)
  {
  }

  /**
   * Handles an incoming query
   */
  public Serializable onQuery(String fromJid, String toJid, Serializable query)
  {
    return null;
  }

  @PreDestroy
  private void destroy()
  {
    _session.close();

    if (log.isLoggable(Level.FINE))
      log.fine(this + " destroy");
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "]";
  }
}
