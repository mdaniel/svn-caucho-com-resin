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

package com.caucho.management.server;

import com.caucho.jmx.*;

/**
 * Administration for the SessionManager for a WebApp.
 *
 * The JMX name looks like:
 * <pre>
 *  resin:type=SessionManager,WebApp=/wiki,Host=caucho.com
 * </pre>
 */
@Description("The session manager for a web-app")
public interface SessionManagerMXBean extends ManagedObjectMXBean
{
  //
  // Hierarchy Attributes
  //
  
  /**
   * Returns the owning WebApp.
   */
  @Description("The owning WebApp for this session manager")
  public WebAppMXBean getWebApp();

  /**
   * Returns the ObjectName for the persistent store.
   */
  @Description("Returns the persistent store mbean")
  public PersistentStoreMXBean getPersistentStore();

  //
  // Configuration attributes
  //

  /**
   * True if the session should be serialized for storage, even if
   * no attributes in the session have been set.
   */
  @Description("If true, serialize the session on each request, even if no attributes have been set")
  public boolean isAlwaysSaveSession();

  /**
   * If true, the server's cluster index is appended to the cookie value.
   */
  @Description("If true, append the server's cluster index to the cookie value")
  public boolean isCookieAppendServerIndex();

  /**
   * The host domain used for session cookies
   */
  @Description("The host domain used for session cookies")
  public String getCookieDomain();

  /**
   * True if the cookie should only be used for http, not https requests.
   */
  @Description("True if the cookie should only be used for http, not https requests")
  public boolean isCookieHttpOnly();

  /**
   * The length of the generated cookie
   */
  @Description("The length of the generated cookie")
  public long getCookieLength();

  /**
   * The session cookie max-age sent to the browser.
   */
  @Description("The session cookie max-age sent to the browser")
  @Units("milliseconds")
  public long getCookieMaxAge();

  /**
   * The cookie name used for sessions.
   */
  @Description("The cookie name for servlet sessions")
  public String getCookieName();

  /**
   * The session cookie port sent to the client browser.
   */
  @Description("The session cookie port sent to the browser")
  public String getCookiePort();

  /**
   * True if the session cookie should only be sent on a secure connection.
   */
  @Description("The session cookie should only be sent on a secure connection")
  public boolean isCookieSecure();

  /**
   * The cookie version sent to the browser.
   */
  @Description("The cookie version sent to the browser")
  public int getCookieVersion();

  /**
   * True if the server reads and writes cookies
   */
  @Description("If session cookies are enabled")
  public boolean isEnableCookies();

  /**
   * (discouraged).  True if the URL-rewriting is enabled.
   * In general, URL-rewriting should be avoided as a security risk.
   */
  @Description("(discouraged).  If URL-rewriting is enabled.  URL-rewriting should be avoided as a security risk.")
  public boolean isEnableURLRewriting();

  /**
   * True if persistent sessions should ignore serialization errors.
   */
  @Description("True if persistent sessions should ignore serialization errors.")
  public boolean isIgnoreSerializationErrors();

  /**
   * True if the session should be invalidated only after listeners are
   * called.
   */
  @Description("True if the session should be invalidated only after listeners are called")
  public boolean isInvalidateAfterListener();

  /**
   * True if session-id should be reused if no session exists.  This
   * should generally be true for web-app consistence.
   */
  @Description("True if the session-id should be reused if no session exists to match the cookie.  This should generally be true to ensure web-app session consistency")
  public boolean isReuseSessionId();
    
  /**
   * Returns the session save-mode.
   */
  @Description("Configured session persistence mode.  The session save-mode is one of: " +
	       "before-headers, after-request, on-shutdown")
  public String getSaveMode();
    
  /**
   * The maximum number of sessions in memory.  The number
   * of persistent sessions may be larger.
   */
  @Description("Configured maximum number of sessions in memory.  The number of persistent sessions may be larger")
  public int getSessionMax();
    
  /**
   * The maximum time an idle session will be saved.  session-timeout affects
   * persistent sessions.
   */
  @Description("Configured maximum time an idle session will be saved in milliseconds.  SessionTimeout affects persistent sessions")
  @Units("milliseconds")
  public long getSessionTimeout();

  //
  // statistics
  //

  /**
   * Returns the count of active sessions.
   */
  public long getSessionActiveCount();

  /**
   * Returns the count of sessions created
   */
  public long getSessionCreateCount();

  /**
   * Returns the count of sessions invalidated
   */
  public long getSessionInvalidateCount();

  /**
   * Returns the count of sessions timeout
   */
  public long getSessionTimeoutCount();
}
