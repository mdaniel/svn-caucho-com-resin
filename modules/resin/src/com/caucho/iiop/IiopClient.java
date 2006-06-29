/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.iiop;

import com.caucho.config.ConfigException;
import com.caucho.ejb.EJBClientInterface;
import com.caucho.ejb.cfg.EjbBean;
import com.caucho.ejb.cfg.EjbConfig;
import com.caucho.vfs.JarPath;
import com.caucho.vfs.Path;

public class IiopClient implements EJBClientInterface {
  private EjbConfig _ejbConfig = new EjbConfig(null);
  private IiopStubLoader _loader;
  
  /**
   * Adds a jar
   */
  public void addEJBJar(Path jar)
    throws ConfigException
  {
    Path path = JarPath.create(jar).lookup("META-INF/ejb-jar.xml");

    if (path.canRead())
      _ejbConfig.addEJBPath(jar, path);
  }

  /**
   * Returns the home interface.
   */
  public Class getEJBHome(String ejbName)
    throws ConfigException
  {
    EjbBean bean = _ejbConfig.getBeanConfig(ejbName);
    System.out.println("B: " + bean);

    if (bean == null)
      return null;
    else
      return bean.getRemoteHomeClass();
  }
  
  /**
   * Initialize the client.
   */
  public void initEJBs()
    throws Exception
  {
    /*
    Iterator<EjbBean> iter = _ejbConfig.getBeanConfigIter();

    while (iter.hasNext()) {
      EjbBean bean = iter.next();

      Class remoteHome = bean.getRemoteHome();
      Class remoteView = bean.getRemote();

      if (remoteHome == null || remoteView == null)
	continue;

      if (_loader == null) {
	_loader = new IiopStubLoader();

	_loader.setPath(WorkDir.getLocalWorkDir());
      }

      if (remoteHome != null)
	_loader.addStubClass(remoteHome.getName());

      if (remoteView != null)
	_loader.addStubClass(remoteView.getName());
    }

    if (_loader != null) {
      Thread thread = Thread.currentThread();
      DynamicClassLoader classLoader =
	(DynamicClassLoader) thread.getContextClassLoader();

      classLoader.addLoader(_loader);
    }
    */
  }
}
