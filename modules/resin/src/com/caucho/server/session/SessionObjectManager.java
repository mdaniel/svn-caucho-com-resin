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

package com.caucho.server.session;

import com.caucho.hessian.io.*;
import com.caucho.server.cluster.*;
import java.io.*;

/**
 * Persistence object manager for sessions
 */
public final class SessionObjectManager implements ObjectManager
{
  private final SessionManager _sessionManager;

  /**
   * Creates and initializes a new session object manager
   */
  public SessionObjectManager(SessionManager sessionManager)
  {
    _sessionManager = sessionManager;
  }
  /**
   * Returns the maximum idle time.
   */
  public long getMaxIdleTime()
  {
    return _sessionManager.getMaxIdleTime();
  }

  /**
   * Loads the session.
   *
   * @param in the input stream containing the serialized session
   * @param obj the session object to be deserialized
   */
  public void load(InputStream is, Object obj)
    throws IOException
  {
    SessionImpl session = (SessionImpl) obj;

    if (_sessionManager.isHessianSerialization()) {
      Hessian2Input in = new Hessian2Input(is);

      session.load(in);

      in.close();
    }
    else {
      ObjectInputStream in = new DistributedObjectInputStream(is);

      session.load(in);

      in.close();
    }
  }

  /**
   * Checks if the session is empty.
   */
  public boolean isEmpty(Object obj)
  {
    SessionImpl session = (SessionImpl) obj;

    return session.isEmpty();
  }

  /**
   * Saves the session.
   */
  public void store(OutputStream os, Object obj)
    throws IOException
  {
    SessionImpl session = (SessionImpl) obj;

    if (_sessionManager.isHessianSerialization()) {
      Hessian2Output out = new Hessian2Output(os);

      session.store(out);

      out.close();
    }
    else {
      ObjectOutputStream out = new ObjectOutputStream(os);

      session.store(out);

      out.close();
    }
  }

  /**
   * Notification from the cluster.
   */
  public void notifyUpdate(String objectId)
  {
  }
      
  /**
   * Notifies an object has been removed.
   */
  public void notifyRemove(String objectId)
    throws IOException
  {
    _sessionManager.notifyRemove(objectId);
  }

  static class DistributedObjectInputStream extends ObjectInputStream {
    private ClassLoader _loader;
    
    DistributedObjectInputStream(InputStream is)
      throws IOException
    {
      super(is);

      Thread thread = Thread.currentThread();
      _loader = thread.getContextClassLoader();
    }

    @Override
    protected Class resolveClass(ObjectStreamClass v)
      throws IOException, ClassNotFoundException
    {
      String name = v.getName();

      return Class.forName(name, false, _loader);
    }
  }
}
