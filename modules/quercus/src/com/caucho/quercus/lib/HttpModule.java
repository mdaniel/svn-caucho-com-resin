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

package com.caucho.quercus.lib;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import com.caucho.util.L10N;
import com.caucho.util.Alarm;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.Reference;

import com.caucho.quercus.env.*;

/**
 * PHP HTTP functions
 */
public class HttpModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(HttpModule.class);

  /**
   * Adds a header.
   */
  public static Value header(Env env, String header,
                             @Optional("true") boolean replace,
                             @Optional long httpResponseCode)
    throws IOException
  {
    HttpServletResponse res = env.getResponse();

    int len = header.length();

    if (header.startsWith("HTTP/")) {
      int p = header.indexOf(' ');
      int status = 0;
      int ch;

      for (; p < len && header.charAt(p) == ' '; p++) {
      }

      for (; p < len && '0' <= (ch = header.charAt(p)) && ch <= '9'; p++) {
	status = 10 * status + ch - '0';
      }

      for (; p < len && header.charAt(p) == ' '; p++) {
      }

      if (status > 0) {
	res.setStatus(status, header.substring(p));

	return NullValue.NULL;
      }
    }

    int p = header.indexOf(':');

    if (p > 0) {
      String key = header.substring(0, p).trim();
      String value = header.substring(p + 1).trim();

      if (key.equalsIgnoreCase("Location"))
        res.sendRedirect(value);
      else if (replace)
        res.setHeader(key, value);
      else
        res.addHeader(key, value);

      if (key.equalsIgnoreCase("Content-Type")) {
	env.getOut().setEncoding(res.getCharacterEncoding());
      }
    }

    return NullValue.NULL;
  }

  /**
   * Return true if the headers have been sent.
   */
  public static boolean headers_sent(Env env,
                                     @Optional @Reference Value file,
                                     @Optional @Reference Value line)
    throws IOException
  {
    HttpServletResponse res = env.getResponse();

    return res.isCommitted();
  }

  /**
   * Sets a cookie
   */
  public static boolean setcookie(Env env,
                                  String name,
                                  @Optional String value,
                                  @Optional long expire,
                                  @Optional String path,
                                  @Optional String domain,
                                  @Optional boolean secure)
    throws IOException
  {
    if (value == null || value.equals(""))
      value = "";

    StringBuffer sb = new StringBuffer();
    int len = value.length();
    for (int i = 0; i < len; i++) {
      char ch = value.charAt(i);

      switch (ch) {
      case '%': case ';': case ':': case '{':  case '}':
      case ' ': case '\t': case '\n': case '\r':
      case '"': case '\'':
        {
          sb.append('%');

          int d = (ch / 16) & 0xf;
          if (d < 10)
            sb.append((char) ('0' + d));
          else
            sb.append((char) ('A' + d - 10));

          d = ch & 0xf;
          if (d < 10)
            sb.append((char) ('0' + d));
          else
            sb.append((char) ('A' + d - 10));

          break;
        }

      default:
        sb.append(ch);
        break;
      }
    }

    Cookie cookie = new Cookie(name, sb.toString());

    if (expire > 0)
      cookie.setMaxAge((int) (expire - Alarm.getCurrentTime() / 1000));

    if (path != null && ! path.equals(""))
      cookie.setPath(path);

    if (domain != null && ! domain.equals(""))
      cookie.setDomain(domain);

    if (secure)
      cookie.setSecure(true);

    env.getResponse().addCookie(cookie);

    return true;
  }

  /**
   * Returns the decoded string.
   */
  // XXX: see duplicate in env
  public static String urldecode(String s)
  {
    int len = s.length();
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      if (ch == '%' && i + 2 < len) {
        int d1 = s.charAt(i + 1);
        int d2 = s.charAt(i + 2);

        int v = 0;

        if ('0' <= d1 && d1 <= '9')
          v = 16 * (d1 - '0');
        else if ('a' <= d1 && d1 <= 'f')
          v = 16 * (d1 - 'a' + 10);
        else if ('A' <= d1 && d1 <= 'F')
          v = 16 * (d1 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        if ('0' <= d2 && d2 <= '9')
          v += (d2 - '0');
        else if ('a' <= d2 && d2 <= 'f')
          v += (d2 - 'a' + 10);
        else if ('A' <= d2 && d2 <= 'F')
          v += (d2 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        i += 2;
        sb.append((char) v);
      }
      else
        sb.append(ch);
    }

    return sb.toString();
  }
}

