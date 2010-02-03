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

package com.caucho.ejb.session;

import com.caucho.ejb.*;
import com.caucho.ejb.protocol.*;
import com.caucho.ejb.server.AbstractServer;

import java.util.logging.*;
import java.io.*;
import java.rmi.*;
import javax.ejb.*;

/**
 * Abstract base class for a 3.0 session object
 */
abstract public class StatelessObject<T>
{
  private static final Logger log
    = Logger.getLogger(StatelessObject.class.getName());
  
  protected final StatelessManager<T> _server;
  protected final Class<T> _api;

  protected StatelessObject(StatelessManager<T> server, Class<T> api)
  {
    _server = server;
    _api = api;
  }

  /**
   * Returns the stateless server.
   */
  public StatelessManager<T> getStatelessManager()
  {
    return _server;
  }

  /**
   * Returns the stateless server.
   */
  public AbstractServer<T> getServer()
  {
    return _server;
  }

  /**
   * Returns the server.
   */
  public AbstractServer<T> __caucho_getServer()
  {
    return getServer();
  }

  /**
   * The home id is null.
   */
  public String __caucho_getId()
  {
    return "::ejb:stateless";
  }

  /**
   * Serialize the HomeSkeletonWrapper in place of this object.
   *
   * @return the matching skeleton wrapper.
   */
  public Object writeReplace() throws ObjectStreamException
  {
    throw new UnsupportedOperationException(getClass().getName());
    // XXX:return _server.getObjectHandle(this, _api);
  }
}
