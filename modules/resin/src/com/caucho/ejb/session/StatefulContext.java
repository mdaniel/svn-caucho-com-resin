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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.session;

import javax.ejb.TimerService;

import com.caucho.config.*;
import com.caucho.config.xml.XmlConfigContext;
import com.caucho.ejb.*;
import com.caucho.ejb.server.AbstractServer;
import com.caucho.util.*;

/**
 * Abstract base class for an session context
 */
abstract public class StatefulContext extends AbstractSessionContext {
  private static final L10N L = new L10N(StatefulContext.class);
    
  private transient StatefulManager _server;

  public StatefulContext(StatefulManager server)
  {
    assert(server != null);

    _server = server;
  }

  /**
   * Returns the server which owns this bean.
   */
  public StatefulManager getStatefulManager()
  {
    return _server;
  }

  /**
   * Returns the server which owns this bean.
   */
  @Override
  public AbstractServer getServer()
  {
    return _server;
  }
  
  public StatefulProvider getProvider(Class api)
  {
    return null;
  }
  
  /**
   * Returns the timer service.
   */
  @Override
  public TimerService getTimerService()
    throws IllegalStateException
  {
    throw new IllegalStateException("Stateful session beans cannot call SessionContext.getTimerService()");
  }

  /**
   * Returns the new instance for EJB 3.0
   */
  protected Object _caucho_newInstance()
  {
    return null;
  }

  /**
   * Returns the new instance for EJB 3.0
   */
  protected Object _caucho_newInstance(XmlConfigContext env)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the new instance for EJB 2.1
   */
  protected Object _caucho_newInstance21()
  {
    return null;
  }

  /**
   * Returns the new remote instance for EJB 3.0
   */
  protected Object _caucho_newRemoteInstance()
  {
    return null;
  }

  /**
   * Returns the new remote instance for EJB 2.1
   */
  protected Object _caucho_newRemoteInstance21()
  {
    return null;
  }
}
