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
 * @author Alex Rojkov
 */
package com.caucho.server.resin;

import com.caucho.server.webapp.WebApp;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class ResinURLStreamHandlerFactory implements URLStreamHandlerFactory
{
  private static ResinURLStreamHandlerFactory _factory
    = new ResinURLStreamHandlerFactory();
  
  private static URLStreamHandler _urlStreamHandler
    = new ResinURLStreamHandler();

  private ResinURLStreamHandlerFactory()
  {
  }
  
  public static URLStreamHandlerFactory create()
  {
    return _factory;
  }

  @Override
  public URLStreamHandler createURLStreamHandler(String protocol)
  {
    if ("jndi".equals(protocol))
      return _urlStreamHandler;

    return null;
  }

  static class ResinURLStreamHandler extends URLStreamHandler
  {
    @Override
    protected URLConnection openConnection(URL url)
      throws IOException
    {
      WebApp webApp = WebApp.getCurrent();

      if (webApp != null)
        return webApp.getResource(url);
      else
        return null;
    }
  }
}
