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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.curl;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.EnvCleanup;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.util.L10N;

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
public class CurlHttpRequest
  implements EnvCleanup
{
  private static final Logger log
    = Logger.getLogger(CurlHttpRequest.class.getName());
  private static final L10N L = new L10N(CurlHttpRequest.class);

  private CurlResource _curl;
  private CurlHttpConnection _conn;

  public CurlHttpRequest(CurlResource curlResource)
  {
    _curl = curlResource;
  }

  /**
   * Returns a HttpRequest specific to the Http request method.
   */
  public static final CurlHttpRequest getRequest(CurlResource curl)
  {
    String requestMethod = curl.getRequestMethod();

    if (requestMethod.equals("GET"))
      return new HttpGetRequest(curl);
    else if (requestMethod.equals("POST"))
      return new HttpPostRequest(curl);
    else if (requestMethod.equals("PUT"))
      return new HttpPutRequest(curl);
    else
      return new CurlHttpRequest(curl);
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

      _conn = CurlHttpConnection.createConnection(url,
                                              _curl.getUsername(),
                                              _curl.getPassword(),
                                              _curl,
                                              proxyURL,
                                              _curl.getProxyUsername(),
                                              _curl.getProxyPassword(),
                                              _curl.getProxyType());
    }
    else {
      _conn = CurlHttpConnection.createConnection(url,
                                              _curl.getUsername(),
                                              _curl.getPassword(),
                                              _curl);
    }
  }

  /**
   * Initializes the connection.
   */
  protected boolean init(Env env)
    throws ProtocolException
  {
    if (_conn == null || _curl == null)
      return false;

    if (! "GET".equals(_curl.getRequestMethod())) {
      _conn.setRequestMethod(_curl.getRequestMethod());
    }

    HashMap<String,String> _properties = _curl.getRequestPropertiesMap();

    for (Map.Entry<String, String> entry : _properties.entrySet()) {
      _conn.setRequestProperty(entry.getKey(), entry.getValue());
    }

    _conn.setInstanceFollowRedirects(_curl.getIsFollowingRedirects());

    int timeout = _curl.getConnectTimeout();
    if (timeout >= 0)
      _conn.setConnectTimeout(timeout);

    timeout = _curl.getReadTimeout();
    if (timeout >= 0)
      _conn.setReadTimeout(timeout);

    return true;
  }

  /**
   * Attempt to connect to the server.
   */
  protected void connect(Env env)
    throws ConnectException, SocketTimeoutException,
           UnknownHostException, IOException
  {
    if (_conn != null)
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
  protected boolean finish(Env env)
    throws IOException
  {
    if (_curl == null || _conn == null)
      return false;

    _curl.setResponseCode(_conn.getResponseCode());

    Value header = getHeader(env, env.createBinaryBuilder());

    if (header == BooleanValue.FALSE)
      return false;

    _curl.setHeader(header.toStringValue());

    Value body = getBody(env, env.createBinaryBuilder());

    if (body == BooleanValue.FALSE)
      return false;

    _curl.setBody(body.toStringValue());

    _curl.setContentLength(_conn.getContentLength());

    _curl.setCookie(_conn.getHeaderField("Set-Cookie"));

    _conn.close();

    return true;
  }

  /**
   * Perform this request.
   */
  public final boolean execute(Env env)
  {
    try {
      create(env);

      if (! init(env))
        return false;

      connect(env);

      transfer(env);

      return finish(env);
    }
    catch (MalformedURLException e) {
      error(env, CurlModule.CURLE_URL_MALFORMAT, e.getMessage(), e);

      return false;
    }
    catch (SocketTimeoutException e) {
      error(env, CurlModule.CURLE_OPERATION_TIMEOUTED, "connection timed out", e);

      return false;
    }
    catch (ConnectException e) {
      error(env, CurlModule.CURLE_COULDNT_CONNECT, e.getMessage(), e);

      return false;
    }
    catch (ProtocolException e) {
      throw new QuercusModuleException(e.getMessage());
      //error(0, e.getMessage(), e);
    }
    catch (UnknownHostException e) {
      error(env, CurlModule.CURLE_COULDNT_RESOLVE_HOST,
            "unknown host: " + e.getMessage(), e);

      return false;
    }
    catch (IOException e) {
      error(env, CurlModule.CURLE_RECV_ERROR, e.getMessage(), e);

      return false;
    }
  }

  protected final CurlResource getCurlResource()
  {
    return _curl;
  }

  protected final CurlHttpConnection getHttpConnection()
  {
    return _conn;
  }

  protected final void error(Env env, int code, String error)
  {
    log.log(Level.FINE, error);

    if (_curl.getIsVerbose())
      env.warning(error);

    _curl.setError(error);
    _curl.setErrorCode(code);
  }

  protected final void error(Env env, int code, String error, Throwable e)
  {
    log.log(Level.FINE, error, e);

    if (_curl.getIsVerbose())
      env.warning(error);

    _curl.setError(error);
    _curl.setErrorCode(code);
  }

  /**
   * Returns a valid URL or null on error.
   */
  protected final URL getURL(Env env, String urlString, int port)
    throws MalformedURLException
  {
    if (urlString == null)
      return null;

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
  private final Value getHeader(Env env, StringValue bb)
  {
    String httpStatus = _conn.getHeaderField(0);

    int i = 0;

    if (_conn.getHeaderFieldKey(0) == null && httpStatus != null) {
      i++;
    }
    else {
      // according to the JDK docs, implementations may or may not return the
      // status as the first header (Google's URLFetch choose not to do so)

      try {
        int responseCode = _conn.getResponseCode();

        httpStatus = "HTTP/1.1 " + responseCode + " " + _conn.getResponseMessage();
      }
      catch (IOException e) {
        throw new QuercusException(e);
      }
    }

    bb.append(httpStatus);
    bb.append("\r\n");

    if (_curl.getHeaderCallback() != null) {
      StringValue sb = env.createUnicodeBuilder();

      sb.append(httpStatus);
      sb.append("\r\n");

      Value len = _curl.getHeaderCallback().call(env, env.wrapJava(_curl), sb);

      if (len.toInt() != sb.length()) {
        _curl.setErrorCode(CurlModule.CURLE_WRITE_ERROR);
        return BooleanValue.FALSE;
      }
    }

    String key;

    while ((key = _conn.getHeaderFieldKey(i)) != null) {
      bb.append(key);
      bb.append(": ");
      bb.append(_conn.getHeaderField(i));
      bb.append("\r\n");

      if (_curl.getHeaderCallback() != null) {
        StringValue sb = env.createUnicodeBuilder();

        sb.append(key);
        sb.append(": ");
        sb.append(_conn.getHeaderField(i));
        sb.append("\r\n");

        Value len = _curl.getHeaderCallback().call(env,
                                                   env.wrapJava(_curl),
                                                   sb);

        if (len.toInt() != sb.length()) {
          _curl.setErrorCode(CurlModule.CURLE_WRITE_ERROR);
          return BooleanValue.FALSE;
        }
      }

      i++;
    }

    bb.append("\r\n");

    if (_curl.getHeaderCallback() != null) {
      StringValue sb = env.createUnicodeBuilder();

      sb.append("\r\n");

      Value len = _curl.getHeaderCallback().call(env,
                                                 env.wrapJava(_curl),
                                                 sb);

      if (len.toInt() != sb.length()) {
        _curl.setErrorCode(CurlModule.CURLE_WRITE_ERROR);
        return BooleanValue.FALSE;
      }
    }

    return bb;
  }

  /**
   * Returns the server response body.
   */
  private final Value getBody(Env env, StringValue bb)
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
      throw new QuercusModuleException(e);
    }

    if (_curl.getReadCallback() != null) {
      Value len = _curl.getReadCallback().call(env, env.wrapJava(_curl), bb);

      if (len.toInt() != bb.length()) {
        _curl.setErrorCode(CurlModule.CURLE_WRITE_ERROR);
        return BooleanValue.FALSE;
      }
    }

    return bb;
  }

  /**
   * Cleanup resources associated with this connection.
   */
  public void cleanup()
  {
    if (_conn != null)
      _conn.close();
  }
}
