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

package com.caucho.quercus.env;

import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.LinkedHashMap;

import javax.servlet.http.HttpServletRequest;

import com.caucho.vfs.WriteStream;

/**
 * Represents the server
 */
public class ServerArrayValue extends ArrayValueImpl {
  private static final StringValue SERVER_NAME_V
    = new StringValue("SERVER_NAME");
  private static final StringValue SERVER_PORT_V
    = new StringValue("SERVER_PORT");
  private static final StringValue REMOTE_HOST_V
    = new StringValue("REMOTE_HOST");
  private static final StringValue REMOTE_ADDR_V
    = new StringValue("REMOTE_ADDR");
  private static final StringValue REMOTE_PORT_V
    = new StringValue("REMOTE_PORT");
  
  private static final StringValue DOCUMENT_ROOT_V
    = new StringValue("DOCUMENT_ROOT");
  
  private static final StringValue SERVER_PROTOCOL_V
    = new StringValue("SERVER_PROTOCOL");
  private static final StringValue REQUEST_METHOD_V
    = new StringValue("REQUEST_METHOD");
  private static final StringValue QUERY_STRING_V
    = new StringValue("QUERY_STRING");
  
  private static final StringValue REQUEST_URI_V
    = new StringValue("REQUEST_URI");
  private static final StringValue SCRIPT_NAME_V
    = new StringValue("SCRIPT_NAME");
  private static final StringValue SCRIPT_FILENAME_V
    = new StringValue("SCRIPT_FILENAME");
  private static final StringValue PATH_INFO_V
    = new StringValue("PATH_INFO");
  private static final StringValue PATH_TRANSLATED_V
    = new StringValue("PATH_TRANSLATED");
  
  private static final StringValue PHP_SELF_V
    = new StringValue("PHP_SELF");
  
  private static final StringValue HTTPS_V
    = new StringValue("HTTPS");
  
  private final Env _env;
  
  private boolean _isFilled;

  public ServerArrayValue(Env env)
  {
    _env = env;
  }
  
  /**
   * Converts to a string.
   * @param env
   */
  public String toString(Env env)
  {
    return "Array";
  }
  
  /**
   * Converts to an object.
   */
  public Object toObject()
  {
    return null;
  }

  /**
   * Adds a new value.
   */
  public Value put(Value key, Value value)
  {
    throw new UnsupportedOperationException("_SERVER is read-only");
  }

  /**
   * Gets a new value.
   */
  public Value get(Value key)
  {
    if (! _isFilled)
      fillMap();

    return super.get(key);
  }
  
  /**
   * Returns the array ref.
   */
  public Var getRef(Value key)
  {
    if (! _isFilled)
      fillMap();
    
    return super.getRef(key);
  }
  
  /**
   * Copy for assignment.
   */
  public Value copy()
  {
    if (! _isFilled)
      fillMap();
    
    return new ArrayValueImpl(this);
  }

  /**
   * Returns an iterator of the entries.
   */
  public Set<Map.Entry<Value,Value>> entrySet()
  {
    if (! _isFilled)
      fillMap();
    
    return super.entrySet();
  }

  /**
   * Convenience for lib.
   */
  public void put(String key, String value)
  {
    throw new UnsupportedOperationException("_SERVER is read-only");
  }

  /**
   * Prints the value.
   * @param env
   */
  public void print(Env env)
    throws IOException
  {
    env.getOut().print("Array");
  }

  /**
   * Fills the map.
   */
  private void fillMap()
  {
    HttpServletRequest request = _env.getRequest();

    super.put(SERVER_NAME_V,
	      new StringValue(request.getServerName()));
    super.put(SERVER_PORT_V,
	      new LongValue(request.getServerPort()));
    super.put(REMOTE_HOST_V,
	      new StringValue(request.getRemoteHost()));
    super.put(REMOTE_ADDR_V,
	      new StringValue(request.getRemoteAddr()));
    super.put(REMOTE_PORT_V,
	      new LongValue(request.getRemotePort()));
    
    super.put(SERVER_PROTOCOL_V,
	      new StringValue(request.getProtocol()));
    super.put(REQUEST_METHOD_V,
	      new StringValue(request.getMethod()));
    
    if (request.getQueryString() != null) {
      super.put(QUERY_STRING_V,
		new StringValue(request.getQueryString()));
    }
    
    super.put(DOCUMENT_ROOT_V,
	      new StringValue(request.getRealPath("/")));
    
    super.put(SCRIPT_NAME_V,
	      new StringValue(request.getContextPath() +
			      request.getServletPath()));

    String requestURI = request.getRequestURI();
    String queryString = request.getQueryString();

    if (queryString != null)
      requestURI = requestURI + '?' + queryString;
    
    super.put(REQUEST_URI_V,
	      new StringValue(requestURI));
    super.put(SCRIPT_FILENAME_V,
	      new StringValue(request.getRealPath(request.getServletPath())));

    if (request.getPathInfo() != null) {
      super.put(PATH_INFO_V,
		new StringValue(request.getPathInfo()));
      super.put(PATH_TRANSLATED_V,
		new StringValue(request.getRealPath(request.getPathInfo())));
    }

    if (request.isSecure())
      super.put(HTTPS_V, new StringValue("on"));

    String contextPath = request.getContextPath();
    String servletPath = request.getServletPath();
    String pathInfo = request.getPathInfo();

    if (pathInfo == null)
      super.put(PHP_SELF_V, new StringValue(contextPath + servletPath));
    else
      super.put(PHP_SELF_V, new StringValue(contextPath + servletPath + pathInfo));

    Enumeration e = request.getHeaderNames();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();

      String value = request.getHeader(key);

      super.put(convertHttpKey(key), new StringValue(value));
    }
  }

  /**
   * Converts a header key to HTTP_
   */
  private StringValue convertHttpKey(String key)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("HTTP_");

    int len = key.length();
    for (int i = 0; i < len; i++) {
      char ch = key.charAt(i);

      if (Character.isLowerCase(ch))
	sb.append(Character.toUpperCase(ch));
      else if (ch == '-')
	sb.append('_');
      else
	sb.append(ch);
    }

    return new StringValue(sb.toString());
  }
}

