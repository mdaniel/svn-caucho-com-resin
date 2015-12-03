/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 * @author Emil Ong
 */

package com.caucho.quercus.lib.session;

import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.*;
import com.caucho.quercus.servlet.api.QuercusHttpServletRequest;
import com.caucho.server.cluster.*;
import com.caucho.server.distcache.*;
import com.caucho.server.webapp.*;
import com.caucho.server.session.*;
import com.caucho.util.*;

import java.io.*;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * Stripped down version of com.caucho.server.session.SessionManager,
 * customized to PHP instead of J2EE sessions.
 */
public class ProSessionManager extends QuercusSessionManager
{
  static protected final L10N L = new L10N(ProSessionManager.class);
  static protected final Logger log
    = Logger.getLogger(ProSessionManager.class.getName());

  private WebApp _webApp;
  // private StoreManager _storeManager;
  private SessionManager _sessionManager;
  // private Store _store;

  public ProSessionManager(QuercusContext quercus, String contextId)
  {
    super(quercus);

    _webApp = WebApp.getLocal();

    if (_webApp != null)
      _sessionManager = _webApp.getSessionManager();

    if (_sessionManager != null)
      setSessionTimeout(_sessionManager.getSessionTimeout());

    ServletService server = ServletService.getCurrent();

    /*
    if (server != null)
      _storeManager = server.getStore();

    if (_storeManager != null) {
      ObjectManager objectManager = null;

      String hostName = _webApp.getHostName();
      String contextPath = _webApp.getContextPath();

      if (hostName == null || hostName.equals(""))
	hostName = "default";

      String storeId = hostName + contextPath + "-PHP";

      _store = _storeManager.createStore(storeId, objectManager);
    }
    */
  }

  /**
   * Creates a pseudo-random session id.  If there's an old id and the
   * group matches, then use it because different applications on the
   * same matchine should use the same cookie.
   */
  @Override
  public String createSessionId(Env env)
  {
    if (_sessionManager == null)
      return super.createSessionId(env);

    String id;

    do {
      QuercusHttpServletRequest request = env.getRequest();

      id = _sessionManager.createSessionIdImpl(request.toRequest(HttpServletRequest.class));
    } while (getSession(env, id, 0) != null);

    if (id == null || id.equals(""))
      throw new RuntimeException();

    return id;
  }

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
  public void load(InputStream in, Object obj)
    throws IOException
  {
    SessionArrayValue session = (SessionArrayValue) obj;

    session.load(Env.getInstance(), in);
  }

  /**
   * Checks if the session is empty.
   */
  public boolean isEmpty(Object obj)
  {
    SessionArrayValue session = (SessionArrayValue) obj;

    return session.isEmpty();
  }

  /**
   * Notification from the cluster.
   */
  public void notifyRemove(Object id)
  {
    SessionArrayValue session = _sessions.remove((String) id);

    if (session != null)
      session.invalidate();
  }

  /**
   * Notification from the cluster.
   */
  public void notifyUpdate(Object id)
  {
  }

  /**
   * Saves the session.
   */
  public void store(OutputStream out, Object obj)
    throws IOException
  {
    SessionArrayValue session = (SessionArrayValue) obj;

    session.store(Env.getInstance(), out);
  }
}
