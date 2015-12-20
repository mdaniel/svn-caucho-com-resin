/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.dispatch;

import java.io.IOException;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.util.L10N;

/**
 * Decodes invocation URI for a servlet.
 */
public class InvocationDecoderServlet
  extends InvocationDecoder<InvocationServlet>
{
  private static final L10N L = new L10N(InvocationDecoderServlet.class);

  private String _sessionCookie = "JSESSIONID";
  private String _sslSessionCookie;
  
  // The URL-encoded session suffix
  private String _sessionSuffix = ";jsessionid=";

  // The URL-encoded session prefix
  private String _sessionPrefix;
  
  /**
   * Sets the session cookie
   */
  public void setSessionCookie(String cookie)
  {
    _sessionCookie = cookie;
  }

  /**
   * Gets the session cookie
   */
  public String getSessionCookie()
  {
    return _sessionCookie;
  }
  
  /**
   * Sets the SSL session cookie
   */
  public void setSSLSessionCookie(String cookie)
  {
    _sslSessionCookie = cookie;
  }

  /**
   * Gets the SSL session cookie
   */
  public String getSSLSessionCookie()
  {
    if (_sslSessionCookie != null)
      return _sslSessionCookie;
    else
      return _sessionCookie;
  }

  /**
   * Sets the session url prefix.
   */
  public void setSessionURLPrefix(String prefix)
  {
    _sessionSuffix = prefix;
  }

  /**
   * Gets the session url prefix.
   */
  public String getSessionURLPrefix()
  {
    return _sessionSuffix;
  }

  /**
   * Sets the alternate session url prefix.
   */
  public void setAlternateSessionURLPrefix(String prefix)
    throws ConfigException
  {
    if (! prefix.startsWith("/"))
      prefix = '/' + prefix;

    if (prefix.lastIndexOf('/') > 0)
      throw new ConfigException(L.l("`{0}' is an invalidate alternate-session-url-prefix.  The url-prefix must not have any embedded '/'.", prefix));
    
    _sessionPrefix = prefix;
    _sessionSuffix = null;
  }

  /**
   * Gets the session url prefix.
   */
  public String getAlternateSessionURLPrefix()
  {
    return _sessionPrefix;
  }

  /**
   * Splits out the query string and unescape the value.
   */
  public void splitQueryAndUnescape(InvocationServlet invocation,
                                    byte []rawURIBytes,
                                    int uriLength)
    throws IOException
  {
    super.splitQueryAndUnescape(invocation, rawURIBytes, uriLength);
    
    invocation.setContextURI(invocation.getURI());
  }
  
  @Override
  protected String decodeURI(String rawURI, 
                             String decodedURI, 
                             InvocationServlet invocation)
  {
    if (_sessionSuffix != null) {
      int p = decodedURI.indexOf(_sessionSuffix);

      if (p >= 0) {
        int suffixLength = _sessionSuffix.length();
        int tail = decodedURI.indexOf(';', p + suffixLength);
        String sessionId;

        if (tail > 0)
          sessionId = decodedURI.substring(p + suffixLength, tail);
        else
          sessionId = decodedURI.substring(p + suffixLength);

        decodedURI = decodedURI.substring(0, p);

        invocation.setSessionId(sessionId);

        p = rawURI.indexOf(_sessionSuffix);
        if (p > 0) {
          rawURI= rawURI.substring(0, p);
          invocation.setRawURI(rawURI);
        }
      }
    }
    else if (_sessionPrefix != null) {
      if (decodedURI.startsWith(_sessionPrefix)) {
        int prefixLength = _sessionPrefix.length();

        int tail = decodedURI.indexOf('/', prefixLength);
        String sessionId;

        if (tail > 0) {
          sessionId = decodedURI.substring(prefixLength, tail);
          decodedURI = decodedURI.substring(tail);
          invocation.setRawURI(rawURI.substring(tail));
        }
        else {
          sessionId = decodedURI.substring(prefixLength);
          decodedURI = "/";
          invocation.setRawURI("/");
        }

        invocation.setSessionId(sessionId);
      }
    }
    
    return decodedURI;
  }

  /**
   * Splits out the query string, and normalizes the URI, assuming nothing
   * needs unescaping.
   */
  public void splitQuery(InvocationServlet invocation, String rawURI)
    throws IOException
  {
    super.splitQuery(invocation, rawURI);
    
    invocation.setContextURI(invocation.getURI());
  }

  /**
   * Just normalize the URI.
   */
  public void normalizeURI(InvocationServlet invocation, String rawURI)
    throws IOException
  {
    super.normalizeURI(invocation, rawURI);

    invocation.setContextURI(invocation.getURI());
  }
}
