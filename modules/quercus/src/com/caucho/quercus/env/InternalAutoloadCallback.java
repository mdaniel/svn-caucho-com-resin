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
 * @author Scott Ferguson
 */

package com.caucho.quercus.env;

import com.caucho.quercus.QuercusException;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.VfsStream;

import java.io.InputStream;
import java.net.URL;

/**
 * Internal call to autoload an internally defined PHP, e.g. for the SPL
 * library.
 */
public class InternalAutoloadCallback
{
  private final String _prefix;

  public InternalAutoloadCallback(String prefix)
  {
    if (! prefix.endsWith("/"))
      prefix = prefix + "/";

    _prefix = prefix;
  }

  /**
   * Evaluates the callback with 1 arguments.
   *
   * @param env the calling environment
   */
  public QuercusClass loadClass(Env env, String name)
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    URL url = loader.getResource(_prefix + name + ".php");

    if (url == null) {
      return null;
    }

    String urlStr = url.toString();

    // for JBoss, #5606
    // XXX: use this for resin and no-resin?
    if (! env.getQuercus().isResin() && urlStr.startsWith("vfs:")) {
      try {
        InputStream is = url.openStream();

        try {
          ReadStream rs = new ReadStream(new VfsStream(is, null));

          QuercusPage page = env.getQuercus().parse(rs);

          page.execute(env);
        }
        finally {
          try {
            is.close();
          }
          catch (Exception e) {
          }
        }
      }
      catch (Exception e) {
        throw new QuercusException(e);
      }
    }
    else {
      Path path = env.getPwd().lookup(urlStr);

      env.executePage(path);
    }

    return env.findClass(name, -1, false, false, false);
  }
}

