/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib;

import java.util.Map;
import java.util.HashMap;

import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;
import com.caucho.util.Alarm;

import com.caucho.java.WorkDir;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Callback;
import com.caucho.quercus.env.SessionArrayValue;
import com.caucho.quercus.env.SessionCallback;

/**
 * PHP class information
 */
public class QuercusSessionModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusSessionModule.class);
  private static final Logger log
    = Logger.getLogger(QuercusSessionModule.class.getName());

  private static final HashMap<String,StringValue> _iniMap
    = new HashMap<String,StringValue>();

  /**
   * Returns the default quercus.ini values.
   */
  public Map<String,StringValue> getDefaultIni()
  {
    return _iniMap;
  }

  /**
   * Returns and/or sets the value of session.cache_limiter, affecting the
   * cache related headers that are sent as a result of a call to
   * {@link #session_start(Env)}.
   *
   * If the optional parameter is not supplied, this function simply returns the existing value.
   * If the optional parameter is supplied, the returned value
   * is the old value that was set before the new value is applied.
   *
   * Valid values are "nocache" (the default), "private", "private_no_expire",
   * and "public". If a value other than these values is supplied, then a warning is produced
   * and no cache related headers will be sent to the client.
   */
  public Value session_cache_limiter(Env env, @Optional String newValue)
  {
    Value value = env.getIni("session.cache_limiter");

    if (newValue == null || "".equals(newValue)) // XXX: php/1k16
      return value;

    env.setIni("session.cache_limiter", newValue);

    return value;
  }

  public Value session_cache_expire(Env env, @Optional String newValue)
  {
    Value value = env.getIni("session.cache_expire");

    if (newValue == null || newValue.length() == 0)
      return value;

    env.setIni("session.cache_expire", newValue);

    return value;
  }

  /**
   * Start the session
   */
  public boolean session_destroy(Env env)
    throws Throwable
  {
    SessionArrayValue session = env.getSession();

    if (session == null)
      return false;

    env.destroySession(session.getId());

    return true;
  }

  /**
   * Returns the session id
   */
  public String session_id(Env env)
  {
    Value sessionIdValue = (Value) env.getSpecialValue("caucho.session_id");

    if (sessionIdValue != null)
      return sessionIdValue.toString();
    else
      return "";
  }

  /**
   * Returns the object's class name
   */
  public Value session_name(Env env, @Optional String newValue)
  {
    Value value = env.getIni("session.name");

    if (newValue != null && ! newValue.equals(""))
      env.setIni("session.name", newValue);

    return value;
  }

  /**
   * Sets the session cookie parameters
   */
  public Value session_set_cookie_params(Env env,
                                         long lifetime,
                                         @Optional String path,
                                         @Optional String domain,
                                         @Optional boolean secure)
    throws Throwable
  {
    env.setIni("session.cookie_lifetime", String.valueOf(lifetime));

    if (path != null)
      env.setIni("session.cookie_path", path);

    if (domain != null)
      env.setIni("session.cookie_domain", domain);

    env.setIni("session.cookie_secure", secure ? "1" : "0");

    return NullValue.NULL;
  }

  /**
   * Sets the session save handler
   */
  public boolean session_set_save_handler(Env env,
					  Callback open,
					  Callback close,
					  Callback read,
					  Callback write,
					  Callback directory,
					  Callback gc)

    throws Throwable
  {
    SessionCallback cb = new SessionCallback(open,
					     close,
					     read,
					     write,
					     directory,
					     gc);

    env.setSessionCallback(cb);

    return true;
  }

  /**
   * Start the session
   */
  public boolean session_start(Env env)
    throws Throwable
  {
    if (env.getSession() != null) {
      env.notice(L.l("session has already been started"));
      return true;
    }

    SessionCallback callback = env.getSessionCallback();

    Value sessionIdValue = (Value) env.getSpecialValue("caucho.session_id");
    String sessionId = null;

    final HttpServletResponse response = env.getResponse();

    if (sessionIdValue != null && ! sessionIdValue.isNull()) {
      sessionId = sessionIdValue.toString();
    }
    else {
      String cookieName = env.getIni("session.name").toString();

      if (callback != null)
	callback.open(env, WorkDir.getLocalWorkDir().getPath(), cookieName);

      Cookie []cookies = env.getRequest().getCookies();

      for (int i = 0; cookies != null && i < cookies.length; i++) {
        if (cookies[i].getName().equals(cookieName)) {
          sessionId = cookies[i].getValue();
        }
      }

      if (sessionId == null) {
        sessionId = generateSessionId(env);

        Cookie cookie = new Cookie(cookieName, sessionId);
        cookie.setVersion(1);

        if (response.isCommitted()) {
          env.warning(L.l("cannot send session cookie because response is committed"));
        }
        else {
          Value path = env.getIni("session.cookie_path");
          cookie.setPath(path.toString());

          Value maxAge = env.getIni("session.cookie_lifetime");
          if (maxAge.toInt() != 0)
            cookie.setMaxAge(maxAge.toInt());

          Value domain = env.getIni("session.cookie_domain");
          cookie.setDomain(domain.toString());

          Value secure = env.getIni("session.cookie_secure");
          cookie.setSecure(secure.toBoolean());

          response.addCookie(cookie);
        }
      }
    }

    env.setSpecialValue("caucho.session_id", new StringValue(sessionId));

    if (response.isCommitted())
      env.warning(L.l("cannot send session cache limiter headers because response is committed"));
    else {
      Value cacheLimiterValue = env.getIni("session.cache_limiter");
      String cacheLimiter = String.valueOf(cacheLimiterValue);

      Value cacheExpireValue = env.getIni("session.cache_expire");
      int cacheExpire = cacheExpireValue.toInt() * 60;

      if ("nocache".equals(cacheLimiter)) {
        response.setHeader("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
        response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
        response.addHeader("Pragma", "no-cache");
      }
      else if ("private".equals(cacheLimiter)) {
        response.setHeader("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
        response.addHeader("Cache-Control", "private, max-age=" + cacheExpire + ", pre-check=" + cacheExpire);
        response.setDateHeader("Last-Modified", env.getLastModified());
      }
      else if ("private_no_expire".equals(cacheLimiter)) {
        response.addHeader("Cache-Control", "private, max-age=" + cacheExpire + ", pre-check=" + cacheExpire);
        response.setDateHeader("Last-Modified", env.getLastModified());
      }
      else if ("public".equals(cacheLimiter)) {
        response.setDateHeader("Expires", Alarm.getCurrentTime());
        response.addHeader("Cache-Control", "public, max-age=" + cacheExpire);
        response.setDateHeader("Last-Modified", env.getLastModified());
      }
    }

    env.createSession(sessionId);

    return true;
  }

  private static String generateSessionId(Env env)
  {
    StringBuilder sb = new StringBuilder();

    long random = RandomUtil.getRandomLong();
    long date = Alarm.getCurrentTime();

    for (int i = 0; i < (64 + 5) / 6; i++) {
      sb.append(encode(random >> (i * 6)));
    }

    for (int i = 0; i < 8; i++) {
      sb.append(encode(date >> (i * 6)));
    }

    return sb.toString();
  }

  /**
   * Converts an integer to a printable character
   */
  private static char encode(long code)
  {
    code = code & 0x3f;

    if (code < 26)
      return (char) ('a' + code);
    else if (code < 52)
      return (char) ('A' + code - 26);
    else if (code < 62)
      return (char) ('0' + code - 52);
    else if (code == 62)
      return '_';
    else
      return '-';
  }

  static {
    addIni(_iniMap, "session.save_path", "", PHP_INI_ALL);
    addIni(_iniMap, "session.name", "PHPSESSID", PHP_INI_ALL);
    addIni(_iniMap, "session.save_handler", "files", PHP_INI_ALL);
    addIni(_iniMap, "session.auto_start", "0", PHP_INI_ALL);
    addIni(_iniMap, "session.gc_probability_start", "1", PHP_INI_ALL);
    addIni(_iniMap, "session.gc_divisor", "100", PHP_INI_ALL);
    addIni(_iniMap, "session.gc_maxlifetime", "1440", PHP_INI_ALL);
    addIni(_iniMap, "session.serialize_handler", "quercus", PHP_INI_ALL);
    addIni(_iniMap, "session.cookie_lifetime", "0", PHP_INI_ALL);
    addIni(_iniMap, "session.cookie_path", "/", PHP_INI_ALL);
    addIni(_iniMap, "session.cookie_domain", "", PHP_INI_ALL);
    addIni(_iniMap, "session.cookie_secure", "", PHP_INI_ALL);
    addIni(_iniMap, "session.use_cookies", "1", PHP_INI_ALL);
    addIni(_iniMap, "session.use_only_cookies", "0", PHP_INI_ALL);
    addIni(_iniMap, "session.referer_check", "", PHP_INI_ALL);
    addIni(_iniMap, "session.entropy_file", "", PHP_INI_ALL);
    addIni(_iniMap, "session.entropy_length", "0", PHP_INI_ALL);
    addIni(_iniMap, "session.cache_limiter", "nocache", PHP_INI_ALL);
    addIni(_iniMap, "session.cache_expire", "180", PHP_INI_ALL);
    addIni(_iniMap, "session.use_trans_sid", "0", PHP_INI_ALL);
    addIni(_iniMap, "session.bug_compat_42", "1", PHP_INI_ALL);
    addIni(_iniMap, "session.bug_compat_warn", "1", PHP_INI_ALL);
    addIni(_iniMap, "session.hash_function", "0", PHP_INI_ALL);
    addIni(_iniMap, "session.hash_bits_per_character", "4", PHP_INI_ALL);
    addIni(_iniMap, "user_rewriter.tags", "a=href,area=href,frame=src,form=,fieldset=", PHP_INI_ALL);
  }
}
