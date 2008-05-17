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
import com.caucho.util.LruCache;
import com.caucho.util.L10N;

import javax.faces.application.ResourceHandler;
import javax.faces.application.Resource;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;

public class ResourceHandlerImpl
  extends ResourceHandler
{
  private static final L10N L = new L10N(ResourceHandlerImpl.class);

  private static final Logger log
    = Logger.getLogger(ResourceHandlerImpl.class.getName());

  private Pattern _versionPattern = Pattern.compile("[.|_|\\-]");
  private LruCache<String, Resource> _resourceCache;

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


  public Resource createResource(String resourceName,
                                 String libraryName,
                                 String contentType)
  {
    if (resourceName == null)
      throw new NullPointerException();

    ResourceImpl resource = null;
    try {

      Path path = locateResource(resourceName, libraryName);

      if (path == null) {
        log.finer(L.l("Unable to load resource '{0}' from library '{1}'",
                      resourceName,
                      libraryName));
      }
      else {
        resource = new ResourceImpl(path,
                                    resourceName,
                                    libraryName,
                                    contentType);
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

  private Path locateResource(String resourceName, String libraryName)
    throws IOException
  {
    Path path = locateResource(Vfs.lookup("resources"),
                               resourceName,
                               libraryName);

    if (path == null) {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      URL url = loader.getResource("META-INF/resources");

      if (url != null)
        path = locateResource(Vfs.lookup(url.toString()),
                              resourceName,
                              libraryName);
    }

    return path;
  }

  private Path locateResource(final Path root,
                              String resourceName,
                              String libraryName)
    throws IOException
  {
    String locale = null;

    Path path = null;

    if (libraryName != null) {
      Path libPath = root.lookup((locale == null ? "./" : "./" + locale + "/") +
                                 libraryName);

      if (libPath.exists()) {
        String[] paths = libPath.list();

        String version = null;

        for (String s : paths) {
          Path test = libPath.lookup("./" + s + "/" + resourceName);

          if (test.exists() && (test.isFile() || test.list().length > 0)) {

            if (version == null || compareVersions(s, version) > 0) {
              path = test;
              version = s;
            }
          }
        }

        if (path == null) {
          path = libPath.lookup("./" + resourceName);
        }
        else if (path != null && path.isDirectory()) {
          paths = path.list();

          version = null;

          for (String s : paths) {
            Path test = path.lookup("./" + s);

            if (test.isFile() &&
                (version == null || compareVersions(s, version) > 0)) {
              path = test;
              version = s;
            }
          }
        }

      }
    }
    else {
      path = root.lookup((locale == null ? "./" : "./" + locale + "/") +
                         resourceName);

      if (path != null && path.isDirectory()) {
        String[] paths = path.list();

        String version = null;

        for (String s : paths) {
          Path test = path.lookup("./" + s);

          if (test.isFile() &&
              (version == null || compareVersions(s, version) > 0)) {
            path = test;
            version = s;
          }
        }
      }
    }
    
    return path;
  }

  public int compareVersions(String ver1, String ver2)
  {
    String[] ver1Parts = _versionPattern.split(ver1);

    String[] ver2Parts = _versionPattern.split(ver2);

    int len;
    if (ver1Parts.length > ver2Parts.length)
      len = ver2Parts.length;
    else
      len = ver1Parts.length;


    for (int i = 0; i < len; i++) {
      char[] ver1Part = ver1Parts[i].toCharArray();

      char[] ver2Part = ver2Parts[i].toCharArray();

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

  public String toString()
  {
    return "ResourceHandlerImpl[]";
  }
}
