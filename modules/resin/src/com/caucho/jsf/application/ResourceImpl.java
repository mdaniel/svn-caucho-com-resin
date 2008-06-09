/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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
 * @author Alex Rojkov
 */

package com.caucho.jsf.application;

import com.caucho.vfs.Path;
import com.caucho.util.QDate;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.util.Base64;
import com.caucho.util.L10N;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.context.ExternalContext;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;
import java.net.MalformedURLException;

public class ResourceImpl
  extends Resource
{

  private static final L10N L = new L10N(ResourceHandlerImpl.class);

  private static final Logger log
    = Logger.getLogger(ResourceHandlerImpl.class.getName());

  private final static long UPDATE_INTERVAL = 300000L;

  private Path _path;
  private QDate _calendar;
  private long _lastCheck;
  private long _lastModified;
  private long _length;
  private String _lastModifiedString;
  private String _etag;
  private URL _url;

  public ResourceImpl(Path path,
                      QDate calendar,
                      String resourceName,
                      String libraryName,
                      String contentType)
  {
    _calendar = calendar;

    setResourceName(resourceName);
    setLibraryName(libraryName);
    setContentType(contentType);

    update(path);
  }

  public InputStream getInputStream()
    throws IOException
  {
    return _path.openRead();
  }

  public void writeToStream(OutputStream outputStream)
    throws IOException
  {
    _path.writeToStream(outputStream);
  }

  public String getRequestPath()
  {
    ExternalContext extContext = FacesContext.getCurrentInstance()
      .getExternalContext();

    String pathInfo = extContext.getRequestPathInfo();
    String servletPath = extContext.getRequestServletPath();

    FacesContext context = FacesContext.getCurrentInstance();

    Application app = context.getApplication();

    String locale = null;

    String appBundle = app.getMessageBundle();

    if (appBundle != null) {
      Locale l = app.getViewHandler().calculateLocale(context);

      try {
        ResourceBundle bundle
          = ResourceBundle.getBundle(appBundle,
                                     l,
                                     Thread.currentThread().getContextClassLoader());

        if (bundle != null) {
          locale = bundle.getString(ResourceHandler.LOCALE_PREFIX);
        }
      }
      catch (MissingResourceException e) {
        log.log(Level.FINER,
                L.l("Can't find bundle for base name '{0}', locale {1}",
                    appBundle,
                    l),
                e);
      }
    }

    String requestPath;

    StringBuilder params = new StringBuilder();
    if (locale != null)
      params.append("?loc=" + locale);

    if (getLibraryName() != null)
      params.append(params.length() == 0 ? "?" : "&")
        .append("ln=")
        .append(getLibraryName());


    if (pathInfo == null) {
      String suffix = servletPath.substring(servletPath.lastIndexOf('.'));
      requestPath = extContext.getRequestContextPath() +
                    ResourceHandler.RESOURCE_IDENTIFIER +
                    '/' + getResourceName() +
                    suffix + params.toString();
    }
    else {
      requestPath = extContext.getRequestContextPath() +
                    servletPath +
                    ResourceHandler.RESOURCE_IDENTIFIER +
                    '/' + getResourceName() +
                    params.toString();

    }

    return requestPath;
  }

  public Map<String, String> getResponseHeaders()
  {
    Map<String, String> result = new HashMap<String, String>();

    result.put("ETag", _etag);
    result.put("Last-Modified", _lastModifiedString);

    return result;
  }

  public URL getURL()
  {
    if (_url == null)
      try {
        _url = new URL(_path.getURL());
      }
      catch (MalformedURLException e) {
        log.log(Level.FINER,
                L.l("Can't create an instance of URL {0}", _path.getURL()),
                e);
      }
    return _url;
  }

  public boolean userAgentNeedsUpdate(FacesContext context)
  {
    String ifModifiedSince = context.getExternalContext()
      .getRequestHeaderMap()
      .get("If-Modified-Since");

    if (ifModifiedSince == null || "".equals(ifModifiedSince)) return true;

    long lastModified = 0;
    try {
      lastModified = _calendar.parseDate(ifModifiedSince);
    }
    catch (Exception e) {
      log.log(Level.FINER, L.l("Can't parse date '{0}'", ifModifiedSince), e);
      return true;
    }

    return lastModified != _lastModified;
  }

  public long getLength()
  {
    return _length;
  }

  boolean isStale()
  {
    return (_lastCheck + UPDATE_INTERVAL < Alarm.getCurrentTime());
  }

  void update(Path path)
  {
    long lastModified = path.getLastModified();
    long length = path.getLength();

    if (lastModified != _lastModified || length != _length) {
      _lastModified = lastModified;
      _length = length;

      CharBuffer cb = new CharBuffer();
      cb.append('"');
      Base64.encode(cb, path.getCrc64());
      cb.append('"');
      _etag = cb.close();

      synchronized (_calendar) {
        _calendar.setGMTTime(lastModified);
        _lastModifiedString = _calendar.printDate();
      }
    }

    if (getContentType() == null || "".equals(getContentType()))
      setContentType(path.getContentType());

    _lastCheck = Alarm.getCurrentTime();

    _path = path;

  }

  public String toString()
  {
    return "ResourceImpl[]";
  }
}
