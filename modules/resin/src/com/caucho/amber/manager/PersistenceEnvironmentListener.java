/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

import java.net.URL;

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;

import com.caucho.vfs.*;
import com.caucho.server.vfs.*;

/**
 * Listener for environment start to detect and load persistence.xml
 */
public class PersistenceEnvironmentListener implements EnvironmentListener {
  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
    throws Throwable
  {
    Path pwd = Vfs.getPwd(loader);
    
    URL []urls = loader.getURLs();

    for (int i = 0; i < urls.length; i++) {
      Path path = pwd.lookup(urls[i].toString());

      Path persistenceXml = path.lookup("META-INF/persistence.xml");

      if (persistenceXml.canRead()) {
	AmberContainer container = AmberContainer.getLocalContainer();
	
	container.addPersistenceRoot(path);
      }
    }
  }
  
  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
  }
}


