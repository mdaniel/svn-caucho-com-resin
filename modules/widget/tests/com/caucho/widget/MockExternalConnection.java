/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.widget;

import com.caucho.vfs.XmlWriter;

import javax.servlet.http.Cookie;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.*;

public class MockExternalConnection
  implements ExternalConnection
{
  private static final String SECURE_SCHEME = "https";
  private static final String INSECURE_SCHEME = "http";

  private static final String TARGET_PARAMETER_PREFIX = "~";

  private ByteArrayOutputStream _out
    = new ByteArrayOutputStream();

  private XmlWriter _widgetWriter;
  private String _charset = "utf-8"; // XXX: should be like servlet?

  private Map<String, String[]> _parameterMap;

  private Map<String, Object> _applicationAttributeMap = new HashMap<String, Object>();

  private String _scheme = INSECURE_SCHEME;
  private String _serverName = "mock";
  private int _serverPort = 80;
  private boolean _isSecure;
  private String _pathInfo;
  private static final String SUBMIT_TARGET_POSTFIX = "~";

  public MockExternalConnection()
  {
    _parameterMap = new LinkedHashMap<String, String[]>();
  }

  public MockExternalConnection(String urlString)
    throws MalformedURLException
  {
    URL url = new URL(urlString);

    _scheme = url.getProtocol();

    if (_scheme.equals(INSECURE_SCHEME))
      _isSecure = false;
    else if (_scheme.equals(SECURE_SCHEME))
      _isSecure = true;
    else
      throw new MalformedURLException("unknown scheme: " + _scheme);

    _serverName = url.getHost();
    _serverPort = url.getPort();
    _pathInfo = url.getPath();

    if (_pathInfo.length() == 0)
      _pathInfo = null;

    String parameterString = url.getQuery();

    if (parameterString != null)
      _parameterMap = parseParameterString(parameterString);
    else
      _parameterMap = new LinkedHashMap<String, String[]>();
  }

  private LinkedHashMap<String,String[]> parseParameterString(String parameterString)
    throws MalformedURLException
  {
    LinkedHashMap<String, String[]> parameterMap = new LinkedHashMap<String, String[]>();

    String[] parameterLines = parameterString.split("&");

    for (String parameterLine : parameterLines) {
      int i = parameterLine.indexOf('=');

      if (i < 0)
        throw new MalformedURLException(parameterLine);

      String name = parameterLine.substring(0, i);
      String value = parameterLine.substring(i+1, parameterLine.length());

      String[] parameterArray = parameterMap.get(name);
      String[] newParameterArray;

      if (parameterArray == null)
        newParameterArray = new String[] { value };
      else {
        int last = parameterArray.length;

        newParameterArray = new String[last + 1];

        System.arraycopy(parameterArray, 0, newParameterArray, 0, last);

        newParameterArray[last] = value;
      }

      parameterMap.put(name, newParameterArray);
    }

    String[] submitPrefixValues = parameterMap.get(SUBMIT_TARGET_POSTFIX);

    String submitPrefix = null;

    if (submitPrefixValues != null && submitPrefixValues.length != 0)
      submitPrefix = submitPrefixValues[0];

    if (submitPrefix != null) {
      LinkedHashMap<String, String[]> newMap = new LinkedHashMap<String, String[]>();

      for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
        String key = entry.getKey();
        String[] values = entry.getValue();

        if (key.equals(SUBMIT_TARGET_POSTFIX)) {
        }
        else if (key.endsWith(SUBMIT_TARGET_POSTFIX)) {
          key = key.substring(0, key.length() - 1);
        }
        else {
          key = submitPrefix + key;
        }

        newMap.put(key, values);
      }

      parameterMap = newMap;
    }

    return parameterMap;
  }

  public void setApplicationAttribute(String name, Object value)
  {
    _applicationAttributeMap.put(name, value);
  }

  public <T> T getApplicationAttribute(String name)
  {
    return (T) _applicationAttributeMap.get(name);
  }

  public Enumeration<String> getApplicationAttributeNames()
  {
    return Collections.enumeration(_applicationAttributeMap.keySet());
  }

  public void removeApplicationAttribute(String name)
  {
    _applicationAttributeMap.remove(name);
  }

  public void setParameterMap(Map<String, String[]> parameterMap)
  {
    _parameterMap = parameterMap;
  }

  public Map<String, String[]> getParameterMap()
  {
    return _parameterMap;
  }

  public String getPathInfo()
  {
    return _pathInfo;
  }

  public Cookie[] getCookies()
  {
    throw new UnsupportedOperationException();
  }

  public String getRequestedSessionId()
  {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdValid()
  {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdFromCookie()
  {
    throw new UnsupportedOperationException();
  }

  public boolean isRequestedSessionIdFromURL()
  {
    throw new UnsupportedOperationException();
  }

  public String getAuthType()
  {
    throw new UnsupportedOperationException();
  }

  public String getRemoteUser()
  {
    throw new UnsupportedOperationException();
  }

  public boolean isUserInRole(String role)
  {
    throw new UnsupportedOperationException();
  }

  public Principal getUserPrincipal()
  {
    throw new UnsupportedOperationException();
  }

  public String getProtocol()
  {
    throw new UnsupportedOperationException();
  }

  public String getScheme()
  {
    return _scheme;
  }

  public String getServerName()
  {
    return _serverName;
  }

  public int getServerPort()
  {
    return _serverPort;
  }

  public String getRemoteAddr()
  {
    throw new UnsupportedOperationException();
  }

  public String getRemoteHost()
  {
    throw new UnsupportedOperationException();
  }

  public int getRemotePort()
  {
    throw new UnsupportedOperationException();
  }

  public String getLocalAddr()
  {
    throw new UnsupportedOperationException();
  }

  public String getLocalName()
  {
    throw new UnsupportedOperationException();
  }

  public int getLocalPort()
  {
    throw new UnsupportedOperationException();
  }

  public void setRequestCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    throw new UnsupportedOperationException();
  }

  public String getRequestCharacterEncoding()
  {
    throw new UnsupportedOperationException();
  }

  public InputStream getInputStream()
    throws IOException
  {
    throw new UnsupportedOperationException();
  }

  public int getRequestContentLength()
  {
    throw new UnsupportedOperationException();
  }

  public String getRequestContentType()
  {
    throw new UnsupportedOperationException();
  }

  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    return new BufferedReader(new StringReader("unimplemented"));
  }

  public Locale getRequestLocale()
  {
    throw new UnsupportedOperationException();
  }

  public Enumeration<Locale> getRequestLocales()
  {
    throw new UnsupportedOperationException();
  }

  public void setSecure(boolean isSecure)
  {
    _isSecure = isSecure;
  }

  public boolean isSecure()
  {
    return _isSecure;
  }

  public <T> T getRequestAttribute(String name)
  {
    throw new UnsupportedOperationException();
  }

  public void setRequestAttribute(String name, Object value)
  {
    throw new UnsupportedOperationException();
  }

  public Enumeration<String> getRequestAttributeNames()
  {
    throw new UnsupportedOperationException();
  }

  public void removeRequestAttribute(String name)
  {
    throw new UnsupportedOperationException();
  }

  public <T> T getSessionAttribute(String name)
  {
    throw new UnsupportedOperationException();
  }

  public void setSessionAttribute(String name, Object value)
  {
    throw new UnsupportedOperationException();
  }

  public Enumeration<String> getSessionAttributeNames()
  {
    throw new UnsupportedOperationException();
  }

  public void removeSessionAttribute(String name)
  {
    throw new UnsupportedOperationException();
  }

  public void addCookie(Cookie cookie)
  {
    throw new UnsupportedOperationException();
  }

  public XmlWriter getWriter()
    throws IOException
  {
    if (_widgetWriter == null) {
      Writer writer = new OutputStreamWriter(_out, _charset);
      _widgetWriter = new XmlWriter(writer);
    }

    return _widgetWriter;
  }

  public OutputStream getOutputStream()
    throws IOException
  {
    return _out;
  }

  public void setResponseContentType(String type)
  {
    throw new UnsupportedOperationException();
  }

  public String getResponseContentType()
  {
    throw new UnsupportedOperationException();
  }

  public String getResponseCharacterEncoding()
  {
    throw new UnsupportedOperationException();
  }

  public void setResponseCharacterEncoding(String charset)
  {
    throw new UnsupportedOperationException();
  }

  public void setResponseLocale(Locale locale)
  {
    throw new UnsupportedOperationException();
  }

  public Locale getResponseLocale()
  {
    throw new UnsupportedOperationException();
  }


  public String createURL(String pathInfo, Map<String, String[]> parameterMap)
  {
    return createURL(null, _scheme, pathInfo, parameterMap);
  }


  public String createURL(String pathInfo,
                          Map<String, String[]> parameterMap,
                          boolean isSecure)
  {
    if (isSecure)
      return createURL(null, SECURE_SCHEME, pathInfo, parameterMap);
    else
      return createURL(null, INSECURE_SCHEME, pathInfo, parameterMap);
  }


  public String createSubmitURL(String submitPrefix,
                                String pathInfo,
                                Map<String, String[]> parameterMap)
  {
    return createURL(submitPrefix, _scheme, pathInfo, parameterMap);
  }


  public String createSubmitURL(String submitPrefix,
                                String pathInfo,
                                Map<String, String[]> parameterMap,
                                boolean isSecure)
  {
    if (isSecure)
      return createURL(submitPrefix, SECURE_SCHEME, pathInfo, parameterMap);
    else
      return createURL(submitPrefix, INSECURE_SCHEME, pathInfo, parameterMap);
  }

  private String createURL(String qualifiedId, String scheme, String pathInfo, Map<String, String[]> parameterMap)
  {
    boolean isSubmitTarget = qualifiedId != null;

    StringBuilder urlBuilder = new StringBuilder();

    urlBuilder.append(scheme);
    urlBuilder.append("://");
    urlBuilder.append(_serverName);
    urlBuilder.append(":");
    urlBuilder.append(_serverPort);

    if (pathInfo != null && pathInfo.length() > 0) {
      if (pathInfo.charAt(0) != '/')
        urlBuilder.append('/');

      urlBuilder.append(pathInfo);
    }
    else
      urlBuilder.append('/');

    if (isSubmitTarget) {
      urlBuilder.append('?');
      urlBuilder.append(SUBMIT_TARGET_POSTFIX);
      urlBuilder.append('=');
      urlBuilder.append(qualifiedId);
    }

    boolean isFirst = !isSubmitTarget;

    for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
      String key = entry.getKey();
      String[] values = entry.getValue();

      if (values == null)
        continue;

      if (key != null && values != null && values.length > 0) {

        for (String value : values) {
          if (value == null)
            continue;

          if (isFirst) {
            urlBuilder.append('?');
            isFirst = false;
          }
          else
            urlBuilder.append('&');

          urlBuilder.append(key);

          if (isSubmitTarget)
            urlBuilder.append(SUBMIT_TARGET_POSTFIX);

          urlBuilder.append('=');
          urlBuilder.append(value);
        }
      }
    }

    return urlBuilder.toString();
  }

  public String createResourceURL(String path)
  {
    throw new UnsupportedOperationException();
  }

  public String getOutputAsString()
  {
    try {
      return new String(_out.toByteArray(), _charset);
    }
    catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    }
  }
}
