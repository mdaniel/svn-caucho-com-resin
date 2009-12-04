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
 * @author Scott Ferguson
 */

package com.caucho.amber.manager;

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.AddLoaderListener;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.net.URL;

/**
 * Listener for environment start to detect and load persistence.xml
 */
public class PersistenceEnvironmentListener implements AddLoaderListener
{
  public PersistenceEnvironmentListener()
  {
  }

  public boolean isEnhancer()
  {
    return true;
  }
  
  /**
   * Handles the case where the environment is starting (after init).
   */
  public void addLoader(EnvironmentClassLoader loader)
  {
    AmberContainer amber = AmberContainer.create(loader);

    // called to configure the enhancer when the classloader updates before
    // any loading of the class
    amber.configurePersistenceRoots();
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
  }

  public int hashCode()
  {
    return getClass().hashCode();
  }

  public boolean equals(Object o)
  {
    if (o == null)
      return false;

    return getClass().equals(o.getClass());
  }
}


