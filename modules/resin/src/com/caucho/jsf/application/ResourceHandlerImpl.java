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

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamImpl;
import com.caucho.vfs.TempBuffer;
import com.caucho.util.LruCache;
import com.caucho.util.L10N;
import com.caucho.util.QDate;

import javax.faces.application.ResourceHandler;
import javax.faces.application.Resource;
import javax.faces.application.Application;
import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Map;
import java.net.URL;

public class ResourceHandlerImpl
  extends ResourceHandler
{
  private static final L10N L = new L10N(ResourceHandlerImpl.class);

  private static final Logger log
    = Logger.getLogger(ResourceHandlerImpl.class.getName());

  private Pattern _versionPattern = Pattern.compile("[.|_|\\-]");

  private QDate _calendar = new QDate();
  private LruCache<String, ResourceImpl> _resourceCache;

  public ResourceHandlerImpl()
  {
    _resourceCache = new LruCache<String, ResourceImpl>(1024);
  }

  public Resource createResource(String resourceName)
  {
    if (resourceName == null)
      throw new NullPointerException();

    return createResource(resourceName, null, null);


  }

  public Resource createResource(String resourceName, String libraryName)
  {
    if (resourceName == null)
      throw new NullPointerException();

    return createResource(resourceName, libraryName, null);
  }


  public Resource createResource(final String resourceName,
                                 final String libraryName,
                                 final String contentType)
  {
    if (resourceName == null)
      throw new NullPointerException();

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

    return createResource(context,
                          resourceName,
                          libraryName,
                          contentType,
                          locale);
  }

  public Resource createResource(FacesContext context,
                                 final String resourceName,
                                 final String libraryName,
                                 final String contentType,
                                 final String locale)
  {
    if (resourceName == null)
      throw new NullPointerException();

    ResourceImpl resource = null;
    try {
      String cacheKey = (locale == null ? "" : locale) +
                        ':' +
                        resourceName +
                        ':' +
                        libraryName;

      resource = _resourceCache.get(cacheKey);

      if (resource == null || resource.isStale()) {

        Path path = locateResource(resourceName, libraryName, locale);

        if (path == null) {
          log.finer(L.l("Unable to load resource '{0}' from library '{1}'",
                        resourceName,
                        libraryName));

          _resourceCache.remove(cacheKey);

          resource = null;
        }
        else {
          final String mimeType;

          if (contentType == null)
            mimeType = context.getExternalContext().getMimeType(resourceName);
          else
            mimeType = contentType;

          if (resource != null)
            resource.update(path);
          else {
            resource = new ResourceImpl(path,
                                        _calendar,
                                        resourceName,
                                        libraryName,
                                        mimeType);

            Application app = context.getApplication();

            if (app.getProjectStage() != ProjectStage.Development)
              _resourceCache.put(cacheKey, resource);
          }
        }
      }
    }
    catch (IOException e) {
      log.log(Level.FINER,
              L.l("Unable to load resource '{0}' from library '{1}'",
                  resourceName,
                  libraryName),
              e);
    }


    return resource;
  }

  private Path locateResource(String resourceName,
                              String libraryName,
                              String locale)
    throws IOException
  {
    Path path = locateResource(Vfs.lookup("resources"),
                               resourceName,
                               libraryName,
                               locale);

    if (path == null) {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      URL url = loader.getResource("META-INF/resources");

      if (url != null)
        path = locateResource(Vfs.lookup(url.toString()),
                              resourceName,
                              libraryName,
                              locale);
    }

    return path;
  }

  private Path locateResource(final Path root,
                              String resourceName,
                              String libraryName,
                              String locale)
    throws IOException
  {

    Path result = null;

    if (libraryName != null) {
      Path libPath = root.lookup((locale == null ? "./" : "./" + locale + "/") +
                                 libraryName);

      if (libPath.exists()) {
        String []paths = libPath.list();

        String version = null;

        Path base = null;
        for (String s : paths) {
          Path test = libPath.lookup("./" + s + "/" + resourceName);

          if (test.exists() && (test.isFile() || test.list().length > 0)) {

            if (version == null || compareVersions(s, version) > 0) {
              base = test;
              version = s;
            }
          }
        }

        if (base == null) {
          Path temp = libPath.lookup("./" + resourceName);

          if (temp.exists() && temp.isFile())
            result = temp;
          else
            base = temp;
        }
        else if (base.isFile()) {
          result = base;
        }


        if (result == null && base != null && base.isDirectory()) {
          paths = base.list();

          version = null;

          for (String s : paths) {
            Path test = base.lookup("./" + s);

            if (test.isFile() &&
                (version == null || compareVersions(s, version) > 0)) {
              result = test;
              version = s;
            }
          }
        }
      }
    }
    else {

      Path base = root.lookup((locale == null ? "./" : "./" + locale + "/") +
                              resourceName);

      if (base.isDirectory()) {
        String []paths = base.list();

        String version = null;

        for (String s : paths) {
          Path test = base.lookup("./" + s);

          if (test.isFile() &&
              (version == null || compareVersions(s, version) > 0)) {
            result = test;
            version = s;
          }
        }
      }
      else if (base.isFile()) {
        result = base;
      }
    }

    return result;
  }

  public int compareVersions(String ver1, String ver2)
  {
    String []ver1Parts = _versionPattern.split(ver1);

    String []ver2Parts = _versionPattern.split(ver2);

    int len;
    if (ver1Parts.length > ver2Parts.length)
      len = ver2Parts.length;
    else
      len = ver1Parts.length;


    for (int i = 0; i < len; i++) {
      char []ver1Part = ver1Parts[i].toCharArray();

      char []ver2Part = ver2Parts[i].toCharArray();

      if (ver1Part.length == ver2Part.length) {
        for (int j = 0; j < ver2Part.length; j++) {
          char c1 = ver1Part[j];
          char c2 = ver2Part[j];

          if (c1 != c2)
            return c1 - c2;
        }
      }
      else {
        return ver1Part.length - ver2Part.length;
      }
    }

    return ver1Parts.length - ver2Parts.length;
  }

  public boolean isResourceRequest(FacesContext context)
  {
    HttpServletRequest request
      = (HttpServletRequest) context.getExternalContext().getRequest();

    String pathInfo = request.getPathInfo();

    if (pathInfo != null)
      return pathInfo.startsWith(RESOURCE_IDENTIFIER);
    else
      return request.getServletPath().startsWith(RESOURCE_IDENTIFIER);
  }

  public void handleResourceRequest(FacesContext context)
    throws IOException
  {
    HttpServletRequest request
      = (HttpServletRequest) context.getExternalContext().getRequest();

    HttpServletResponse response
      = (HttpServletResponse) context.getExternalContext().getResponse();

    String method = request.getMethod();
    if (!method.equalsIgnoreCase("GET") &&
        !method.equalsIgnoreCase("POST") &&
        !method.equalsIgnoreCase("HEAD")) {
      response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED,
                         "Method not implemented");

      return;
    }


    String resourceName;

    String pathInfo = request.getPathInfo();

    if (pathInfo == null) {
      String servletPath = request.getServletPath();

      int extIdx = servletPath.lastIndexOf('.');

      resourceName = servletPath.substring(servletPath.indexOf(
        RESOURCE_IDENTIFIER) + RESOURCE_IDENTIFIER.length(), extIdx);
    }
    else {
      resourceName = pathInfo.substring(pathInfo.indexOf(RESOURCE_IDENTIFIER) +
                                        RESOURCE_IDENTIFIER.length());
    }

    String temp = request.getParameter("ln");

    String libraryName;
    String locale;

    if (temp != null && !"".equals(temp))
      libraryName = temp;
    else
      libraryName = null;

    temp = request.getParameter("loc");

    if (temp != null && !"".equals(temp))
      locale = temp;
    else
      locale = null;

    final Resource resource;

    if (locale != null)
      resource = createResource(context,
                                resourceName,
                                libraryName,
                                null,
                                locale);
    else
      resource = createResource(resourceName, libraryName);

    if (resource != null) {
      Map<String, String> headers = resource.getResponseHeaders();

      for (Map.Entry<String, String> entry : headers.entrySet()) {
        response.setHeader(entry.getKey(), entry.getValue());
      }

      response.setContentType(resource.getContentType());

      if (resource instanceof ResourceImpl) {
        ResourceImpl resourceImpl = (ResourceImpl) resource;
        response.setContentLength((int) resourceImpl.getLength());

        if (resourceImpl.userAgentNeedsUpdate(context)) {
          if (!method.equalsIgnoreCase("HEAD"))
            resourceImpl.writeToStream(response.getOutputStream());
        }
        else {
          response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        }
      }
      else {
        InputStream is = resource.getInputStream();
        OutputStream os = response.getOutputStream();

        TempBuffer tempBuffer = TempBuffer.allocate();

        try {
          byte []buffer = tempBuffer.getBuffer();
          int length = buffer.length;
          int len;

          while ((len = is.read(buffer, 0, length)) > 0)
            os.write(buffer, 0, len);
        }
        finally {
          TempBuffer.free(tempBuffer);
          tempBuffer = null;

          is.close();
        }

      }
    }
    else {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }


  public String toString()
  {
    return "ResourceHandlerImpl[]";
  }
}
