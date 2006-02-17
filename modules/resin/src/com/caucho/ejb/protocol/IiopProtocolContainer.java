/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.protocol;

import java.io.*;
import java.util.*;
import java.rmi.*;

import java.sql.*;

import javax.ejb.*;
import javax.sql.*;
import javax.naming.*;
import javax.transaction.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.config.ConfigException;

import com.caucho.ejb.*;

import com.caucho.iiop.IiopContext;

/**
 * Server containing all the EJBs for a given configuration.
 *
 * <p>Each protocol will extend the container to override Handle creation.
 */
public class IiopProtocolContainer extends ProtocolContainer {
  private static final L10N L = new L10N(IiopProtocolContainer.class);

  private IiopContext _context;

  private IiopProtocolContainer(IiopContext context)
  {
    _context = context;
  }
  
  /**
   * Creates the IIOP protocol server if IIOP is available.
   */
  public static IiopProtocolContainer createProtocolContainer()
  {
    IiopContext context = IiopContext.getLocalContext();

    if (context != null)
      return new IiopProtocolContainer(context);
    else
      return null;
  }

  public String getName()
  {
    return "iiop";
  }

  /**
   * Exports a server to iiop.
   */
  public void addServer(AbstractServer server)
  {
    if (server.getHomeObject() == null)
      return;

    EjbIiopRemoteService service = new EjbIiopRemoteService(server);

    // XXX: check logic with getJNDIName
    _context.setService(server.getEJBName(), service);

    if (server.getJndiName() != null)
      _context.setService(server.getJndiName(), service);
  }

  /**
   * Removes a server from iiop.
   */
  public void removeServer(AbstractServer server)
  {
    if (server.getHomeObject() == null)
      return;

    _context.removeService(server.getEJBName());
    
    if (server.getJndiName() != null)
      _context.removeService(server.getJndiName());
  }
  
  protected HandleEncoder createHandleEncoder(AbstractServer server,
                                              Class primaryKeyClass)
    throws ConfigException
  {
    if (_urlPrefix != null)
      return new HandleEncoder(server, _urlPrefix + server.getEJBName());
    else
      return new HandleEncoder(server, server.getEJBName());
  }

  /**
   * Returns the skeleton
   */
  public Skeleton getSkeleton(String uri, String queryString)
    throws Exception
  {
    throw new UnsupportedOperationException();
  }
}
