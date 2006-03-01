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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.security;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.security.*;

import javax.servlet.http.*;
import javax.servlet.*;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.xml.*;
import com.caucho.server.webapp.Application;

/**
 * Implements the "digest" auth-method.  Basic uses the
 * HTTP authentication with WWW-Authenticate and SC_UNAUTHORIZE.
 */
public class DigestLogin extends AbstractLogin {
  protected String _realm;
  
  /**
   * Sets the login realm.
   */
  public void setRealmName(String realm)
  {
    _realm = realm;
  }

  /**
   * Gets the realm.
   */
  public String getRealmName()
  {
    return _realm;
  }

  /**
   * Returns the authentication type.
   */
  public String getAuthType()
  {
    return "Digest";
  }
  
  /**
   * Logs a user in with a user name and a password.  Basic authentication
   * extracts the user and password from the authorization header.  If
   * the user/password is missing, authenticate will send a basic challenge.
   *
   * @param request servlet request
   * @param response servlet response, in case any cookie need sending.
   * @param application servlet application
   *
   * @return the logged in principal on success, null on failure.
   */
  public Principal authenticate(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext application)
    throws ServletException, IOException
  {
    Principal user;

    // If the user is already logged-in, return the user
    user = getAuthenticator().getUserPrincipal(request, response, application);
    if (user != null)
      return user;
    
    user = getDigestPrincipal(request, response, application);

    if (user != null)
      return user;

    sendDigestChallenge(response, application);
    
    return null;
  }
  
  /**
   * Returns the current user with the user name and password.
   *
   * @param request servlet request
   * @param response servlet response, in case any cookie need sending.
   * @param application servlet application
   *
   * @return the logged in principal on success, null on failure.
   */
  public Principal getUserPrincipal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    ServletContext application)
    throws ServletException
  {
    ServletAuthenticator auth = getAuthenticator();
    
    Principal user = auth.getUserPrincipal(request, response, application);

    if (user != null)
      return user;
    
    return getDigestPrincipal(request, response, application);
  }

  /**
   * Sends a challenge for basic authentication.
   */
  protected void sendDigestChallenge(HttpServletResponse res,
                                     ServletContext application)
    throws ServletException, IOException
  {
    String realm = getRealmName();
    if (realm == null)
      realm = "resin";

    CharBuffer cb = CharBuffer.allocate();
    Base64.encode(cb, getRandomLong(application));
    String nonce = cb.toString();
    cb.clear();
    cb.append("Digest ");
    cb.append("realm=\"");
    cb.append(realm);
    cb.append("\", qop=\"auth\", ");
    cb.append("nonce=\"");
    cb.append(nonce);
    cb.append("\"");

    res.setHeader("WWW-Authenticate", cb.close());
    
    res.sendError(res.SC_UNAUTHORIZED);
  }

  protected long getRandomLong(ServletContext application)
  {
    return RandomUtil.getRandomLong();
  }

  /**
   * Returns the principal from a basic authentication
   *
   * @param auth the authenticator for this application.
   */
  protected Principal getDigestPrincipal(HttpServletRequest request,
                                         HttpServletResponse response,
                                         ServletContext application)
    throws ServletException
  {
    String value = request.getHeader("authorization");
    if (value == null)
      return null;

    String username = null;
    String realm = null;
    String uri = null;
    String nonce = null;
    String cnonce = null;
    String nc = null;
    String qop = null;
    String digest = null;

    CharCursor cursor = new StringCharCursor(value);

    String key = scanKey(cursor);
    if (! "Digest".equalsIgnoreCase(key))
      return null;
      
    while ((key = scanKey(cursor)) != null) {
      value = scanValue(cursor);

      if (key.equals("username"))
        username = value;
      else if (key.equals("realm"))
        realm = value;
      else if (key.equals("uri"))
        uri = value;
      else if (key.equals("nonce"))
        nonce = value;
      else if (key.equals("response"))
        digest = value;
      else if (key.equals("cnonce"))
        cnonce = value;
      else if (key.equals("nc"))
        nc = value;
      else if (key.equals("qop"))
        qop = value;
    }

    byte []clientDigest = decodeDigest(digest);

    if (clientDigest == null || username == null ||
        uri == null || nonce == null)
      return null;

    ServletAuthenticator auth = getAuthenticator();
    Principal principal = auth.loginDigest(request, response, application,
                                           username, realm, nonce, uri,
                                           qop, nc, cnonce,
                                           clientDigest);
    
    if (log.isLoggable(Level.FINE))
      log.fine("digest: " + username + " -> " + principal);

    return principal;
  }

  protected byte []decodeDigest(String digest)
  {
    if (digest == null)
      return null;
    
    int len = (digest.length() + 1) / 2;
    byte []clientDigest = new byte[len];

    for (int i = 0; i + 1 < digest.length(); i += 2) {
      int ch1 = digest.charAt(i);
      int ch2 = digest.charAt(i + 1);

      int b = 0;
      if (ch1 >= '0' && ch1 <= '9')
        b += ch1 - '0';
      else if (ch1 >= 'a' && ch1 <= 'f')
        b += ch1 - 'a' + 10;

      b *= 16;
      
      if (ch2 >= '0' && ch2 <= '9')
        b += ch2 - '0';
      else if (ch2 >= 'a' && ch2 <= 'f')
        b += ch2 - 'a' + 10;

      clientDigest[i / 2] = (byte) b;
    }

    return clientDigest;
  }

  protected String scanKey(CharCursor cursor)
    throws ServletException
  {
    int ch;
    while (XmlChar.isWhitespace((ch = cursor.current())) || ch == ',') {
      cursor.next();
    }

    ch = cursor.current();
    if (ch == cursor.DONE)
      return null;
    
    if (! XmlChar.isNameStart(ch))
      throw new ServletException("bad key: " + (char) ch + " " + cursor);

    CharBuffer cb = CharBuffer.allocate();
    while (XmlChar.isNameChar(ch = cursor.read())) {
      cb.append((char) ch);
    }
    if (ch != cursor.DONE)
      cursor.previous();

    return cb.close();
  }

  protected String scanValue(CharCursor cursor)
    throws ServletException
  {
    int ch;
    skipWhitespace(cursor);

    ch = cursor.read();
    if (ch != '=')
      throw new ServletException("expected '='");

    skipWhitespace(cursor);
    
    CharBuffer cb = CharBuffer.allocate();
    
    ch = cursor.read();
    if (ch == '"')
      while ((ch = cursor.read()) != cursor.DONE && ch != '"')
        cb.append((char) ch);
    else {
      for (;
           ch != cursor.DONE && ch != ',' && ! XmlChar.isWhitespace(ch);
           ch = cursor.read())
        cb.append((char) ch);

      if (ch != cursor.DONE)
	cursor.previous();
    }

    return cb.close();
  }

  protected void skipWhitespace(CharCursor cursor)
  {
    while (XmlChar.isWhitespace(cursor.current())) {
      cursor.next();
    }
  }
}
