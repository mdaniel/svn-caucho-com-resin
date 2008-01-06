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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.curl;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.util.L10N;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Represents a generic Http request.
 */
public class HttpRequest
  implements Closeable
{
  private static final Logger log
    = Logger.getLogger(HttpRequest.class.getName());
  private static final L10N L = new L10N(HttpRequest.class);

  private CurlResource _curl;
  private HttpConnection _conn;

  public HttpRequest(CurlResource curlResource)
  {
    _curl = curlResource;
  }

  /**
   * Returns a HttpRequest specific to the Http request method.
   */
  public static final HttpRequest getRequest(CurlResource curl)
  {
    String requestMethod = curl.getRequestMethod();

    if (requestMethod.equals("GET"))
      return new HttpGetRequest(curl);
    else if (requestMethod.equals("POST"))
      return new HttpPostRequest(curl);
    else if (requestMethod.equals("PUT"))
      return new HttpPutRequest(curl);
    else
      return new HttpRequest(curl);
  }

  /**
   * Opens the connection.
   */
  protected final void create(Env env)
    throws MalformedURLException, IOException
  {
    URL url = getURL(env, _curl.getURL(), _curl.getPort());

    if (url == null)
      return;

    if (_curl.getIsProxying()) {
      URL proxyURL = getURL(env, _curl.getProxyURL(), _curl.getProxyPort());

      _conn = HttpConnection.createConnection(url,
                                              _curl.getUsername(),
                                              _curl.getPassword(),
                                              _curl,
                                              proxyURL,
                                              _curl.getProxyUsername(),
                                              _curl.getProxyPassword(),
                                              _curl.getProxyType());
    }
    else {
      _conn = HttpConnection.createConnection(url,
                                              _curl.getUsername(),
                                              _curl.getPassword(),
                                              _curl);
    }
  }

  /**
   * Initializes the connection.
   */
  protected void init(Env env)
    throws ProtocolException
  {
    _conn.setRequestMethod(_curl.getRequestMethod());

    HashMap<String,String> _properties = _curl.getRequestPropertiesMap();

    for (Map.Entry<String, String> entry: _properties.entrySet()) {
      _conn.setRequestProperty(entry.getKey(), entry.getValue());
    }

    _conn.setInstanceFollowRedirects(_curl.getIsFollowingRedirects());

    int timeout = _curl.getConnectTimeout();
    if (timeout >= 0)
      _conn.setConnectTimeout(timeout);

    timeout = _curl.getReadTimeout();
    if (timeout >= 0)
      _conn.setReadTimeout(timeout);
  }

  /**
   * Attempt to connect to the server.
   */
  protected void connect(Env env)
    throws ConnectException, SocketTimeoutException,
           UnknownHostException, IOException
  {
    _conn.connect(_curl);
  }

  /**
   * Transfer data to the server.
   */
  protected void transfer(Env env)
    throws IOException
  {

  }

  /**
   * Closes the connection and sends data and connection info to curl.
   */
  protected void finish(Env env)
    throws IOException
  {
    _curl.setResponseCode(_conn.getResponseCode());

    _curl.setHeader(getHeader(env.createBinaryBuilder()));
    _curl.setBody(getBody(env.createBinaryBuilder()));

    _curl.setContentLength(_conn.getContentLength());

    _curl.setCookie(_conn.getHeaderField("Set-Cookie"));

    _conn.close();
  }

  /**
   * Perform this request.
   */
  public final void execute(Env env)
  {
    try {
      create(env);

      init(env);

      connect(env);

      transfer(env);

      finish(env);
    }
    catch (MalformedURLException e) {
      error(env, CurlModule.CURLE_URL_MALFORMAT, e.getMessage(), e);
    }
    catch (SocketTimeoutException e) {
      error(env, CurlModule.CURLE_OPERATION_TIMEOUTED, "connection timed out", e);
    }
    catch (ConnectException e) {
      error(env, CurlModule.CURLE_COULDNT_CONNECT, e.getMessage(), e);
    }
    catch (ProtocolException e) {
      throw new QuercusModuleException(e.getMessage());
      //error(0, e.getMessage(), e);
    }
    catch (UnknownHostException e) {
      error(env, CurlModule.CURLE_COULDNT_RESOLVE_HOST, "unknown host: " + e.getMessage(), e);
    }
    catch (IOException e) {
      error(env, CurlModule.CURLE_RECV_ERROR, e.getMessage(), e);
    }
  }

  protected final CurlResource getCurlResource()
  {
    return _curl;
  }

  protected final HttpConnection getHttpConnection()
  {
    return _conn;
  }

  protected final void error(Env env, int code, String error)
  {
    log.log(Level.FINE, error);

    if (_curl.getIsVerbose())
      env.warning(L.l(error));

    _curl.setError(error);
    _curl.setErrorCode(code);
  }

  protected final void error(Env env, int code, String error, Throwable e)
  {
    log.log(Level.FINE, error, e);

    if (_curl.getIsVerbose())
      env.warning(L.l(error));

    _curl.setError(error);
    _curl.setErrorCode(code);
  }

  /**
   * Returns a valid URL or null on error.
   */
  protected final URL getURL(Env env, String urlString, int port)
    throws MalformedURLException
  {
    URL url;

    if (urlString.indexOf("://") < 0)
      url = new URL("http://" + urlString);
    else
      url = new URL(urlString);

    if (port >= 0)
      url = new URL(url.getProtocol(), url.getHost(), port, url.getFile());

    return url;
  }

  /**
   * Returns the server response header.
   */
  private final StringValue getHeader(StringValue bb)
  {
    // Append server response to the very top
    bb.append(_conn.getHeaderField(0));
    bb.append("\r\n");

    String key;
    int i = 1;

    while ((key = _conn.getHeaderFieldKey(i)) != null) {
      bb.append(key);
      bb.append(": ");
      bb.append(_conn.getHeaderField(i));
      bb.append("\r\n");
      i++;
    }

    bb.append("\r\n");

    return bb;
  }

  /**
   * Returns the server response body.
   */
  private final StringValue getBody(StringValue bb)
    throws SocketTimeoutException, IOException
  {
    InputStream in;

    if ((_conn.getResponseCode() < 400))
      in = _conn.getInputStream();
    else
      in = _conn.getErrorStream();

    if (in == null)
      return StringValue.EMPTY;

    String encoding = _conn.getHeaderField("Content-Encoding");

    if (encoding != null) {
      if (encoding.equals("gzip"))
        in = new GZIPInputStream(in);
      else if (encoding.equals("deflate"))
        in = new InflaterInputStream(in);
      else if (encoding.equals("identity")) {
      }
      else {
        _curl.setError(encoding);
        _curl.setErrorCode(CurlModule.CURLE_BAD_CONTENT_ENCODING);
        return StringValue.EMPTY;
      }
    }

    int ch;
    
    try {
      while ((ch = in.read()) >= 0) {
        bb.appendByte(ch);
      }
    }
    catch (IOException e) {
    }

    return bb;
  }

  /**
   * Disconnects the connection.
   */
  public void close()
  {
    if (_conn != null)
      _conn.close();
  }
}
