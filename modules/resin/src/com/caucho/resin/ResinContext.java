/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.resin;

import com.caucho.loader.CompilingLoader;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.lang.annotation.Annotation;

/**
 * Embeddable Resin context for unit testing of
 * application modules in the correct environment but
 * without the overhead of the Resin server.
 *
 * <code><pre>
 * static void main(String []args)
 * {
 *   ResinContext cxt = new ResinContext();
 *
 *   cxt.addModule("test.jar");
 *   cxt.start();
 *
 *   ClassLoader loader = cxt.beginRequest();
 *   try {
 *     MyMain main = cxt.getInstance(MyMain.class);
 *
 *     main.main(args);
 *   } finally {
 *     cxt.endRequest(loader);
 *   }
 * }
 * </pre></code>
 */
public class ResinContext
{
  private static final L10N L = new L10N(ResinContext.class);

  private EnvironmentClassLoader _classLoader;

  public ResinContext()
  {
    _classLoader = EnvironmentClassLoader.create("resin-context");
  }

  /**
   * Adds a new module (jar or classes directory)
   */
  public void addModule(String path)
  {
    Path modulePath = Vfs.lookup(path);

    if (path.endsWith(".jar")) {
      //
    }
    else {
      CompilingLoader loader = new CompilingLoader();
      loader.setPath(modulePath);
      loader.init();

      _classLoader.addLoader(loader);
    }
  }

  /**
   * Initializes the context.
   */
  public void start()
  {
  }

  /**
   * Returns a new instance of the given type with optional bindings.
   */
  public <T> T getInstance(Class<T> type, Annotation ...bindings)
  {
    return null;
  }

  /**
   * Enters the Resin context and begins a new request on the thread.
   */
  public ClassLoader beginRequest()
  {
    Thread thread = Thread.currentThread();
    
    ClassLoader oldContext = thread.getContextClassLoader();
    
    return oldContext;
  }

  /**
   * Completes the thread's request and exits the Resin context.
   */
  public void completeRequest(ClassLoader oldContext)
  {
    Thread thread = Thread.currentThread();

    thread.setContextClassLoader(oldContext);
  }

  /**
   * Shuts the context down.
   */
  public void close()
  {
  }

  public String toString()
  {
    return getClass().getName() + "[]";
  }
}
