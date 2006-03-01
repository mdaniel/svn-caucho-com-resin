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

import java.io.InputStream;

import java.net.URL;

import java.util.HashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.amber.cfg.PersistenceConfig;
import com.caucho.amber.cfg.PersistenceUnitConfig;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;

import com.caucho.loader.EnvironmentLocal;

import com.caucho.vfs.Path;

/**
 * Environment-based container.
 */
public class AmberContainer {
  private static final Logger log
    = Logger.getLogger(AmberContainer.class.getName());
  
  private static final EnvironmentLocal<AmberContainer> _localContainer
    = new EnvironmentLocal<AmberContainer>();

  private HashMap<String,PersistenceUnitConfig> _unitMap
    = new HashMap<String,PersistenceUnitConfig>();

  /**
   * Returns the local container.
   */
  public static AmberContainer getLocalContainer()
  {
    synchronized (_localContainer) {
      AmberContainer container = _localContainer.getLevel();

      if (container == null) {
	container = new AmberContainer();

	_localContainer.set(container);
      }

      return container;
    }
  }
  
  /**
   * Adds a persistence root.
   */
  public void addPersistenceRoot(Path root)
  {
    Path persistenceXml = root.lookup("META-INF/persistence.xml");
    InputStream is = null;

    try {
      is = persistenceXml.openRead();

      PersistenceConfig persistence = new PersistenceConfig();
      persistence.setRoot(root);

      new Config().configure(persistence, is,
			     "com/caucho/amber/cfg/persistence-30.rnc");

      for (PersistenceUnitConfig unit : persistence.getUnitList()) {
	_unitMap.put(unit.getName(), unit);
	
	System.out.println(unit);
      }
    } catch (ConfigException e) {
      e.printStackTrace();
      log.warning(e.getMessage());
    } catch (Throwable e) {
      e.printStackTrace();
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      try {
	if (is != null)
	  is.close();
      } catch (Throwable e) {
      }
    }
  }
}


