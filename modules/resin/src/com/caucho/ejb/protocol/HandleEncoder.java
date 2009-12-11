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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.protocol;

import com.caucho.ejb.server.AbstractServer;
import com.caucho.util.Base64;
import com.caucho.util.CharBuffer;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

import java.util.logging.Logger;

/**
 * Encodes and decodes handles.
 */
public class HandleEncoder {
  private static final L10N L = new L10N(HandleEncoder.class);
  private static final Logger log
    = Logger.getLogger(HandleEncoder.class.getName());

  private final String _serverId;
  private AbstractServer _server;

  public HandleEncoder(String serverId)
  {
    _serverId = serverId;
  }

  public HandleEncoder(AbstractServer server, String serverId)
  {
    this(serverId);

    setServer(server);
  }

  public String getServerId()
  {
    return _serverId;
  }

  protected void setServer(AbstractServer server)
  {
    _server = server;
  }

  protected AbstractServer getServer()
  {
    return _server;
  }

  /**
   * Creates a home handle given the server id.
   */
  public AbstractHomeHandle createHomeHandle()
  {
    if (_server != null) {
      try {
        return new HomeHandleImpl(_server.getEJBHome(), _serverId);
      } catch (Throwable e) {
      }
    }

    return new HomeHandleImpl(_serverId);
  }

  /**
   * Converts the primary key to a URL.
   */
  public String getURL()
  {
    return _serverId;
  }

  /**
   * Converts the primary key to a URL.
   */
  public String getURL(String primaryKey)
  {
    return _serverId + "?id=" + primaryKey;
  }

  /**
   * Creates a handle given the server id and the object id.
   */
  public AbstractHandle createHandle(String objectId)
  {
    return new HandleImpl(_serverId, objectId);
  }

  /**
   * Creates a random string key.
   */
  public String createRandomStringKey()
  {
    long id = RandomUtil.getRandomLong();
    
    CharBuffer cb = new CharBuffer();
    Base64.encode(cb, id);
    for (int i = 1; i < cb.length(); i++) {
      if (cb.charAt(i) == '/')
        cb.setCharAt(i, '-');
    }
    
    return cb.toString();
  }

  /**
   * Encodes the primary key as a string.
   */
  protected String encodePrimaryKey(Object primaryKey)
  {
    if (_server != null)
      return _server.encodeId(primaryKey);
    else
      return String.valueOf(primaryKey);
  }

  public Object objectIdToKey(Object id)
  {
    return id;
  }
}
