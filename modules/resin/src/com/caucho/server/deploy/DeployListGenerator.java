/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.server.deploy;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.vfs.Path;

import com.caucho.make.Dependency;

import com.caucho.lifecycle.Lifecycle;

/**
 * A list of deploy objects.
 */
public class DeployListGenerator<E extends DeployController> extends DeployGenerator<E> implements Dependency {
  private static final Logger log = Log.open(DeployListGenerator.class);
  private static final L10N L = new L10N(DeployListGenerator.class);

  private ArrayList<DeployGenerator<E>> _deployList = new ArrayList<DeployGenerator<E>>();

  /**
   * Creates the deploy.
   */
  public DeployListGenerator(DeployContainer container)
  {
    super(container);
  }
  
  /**
   * Adds a deploy.
   */
  public void add(DeployGenerator<E> deploy)
  {
    if (! _deployList.contains(deploy))
      _deployList.add(deploy);
  }
  
  /**
   * Removes a deploy.
   */
  public void remove(DeployGenerator<E> deploy)
  {
    _deployList.remove(deploy);
  }

  /**
   * Returns true if the deployment has modified.
   */
  public boolean isModified()
  {
    for (int i = _deployList.size() - 1; i >= 0; i--) {
      if (_deployList.get(i).isModified())
	return true;
    }

    return false;
  }

  /**
   * Redeploy if the deployment is modified.
   */
  public void redeployIfModified()
  {
    for (int i = _deployList.size() - 1; i >= 0; i--) {
      _deployList.get(i).redeployIfModified();
    }
  }

  /**
   * Force an update
   */
  public void update()
  {
    for (int i = 0; i < _deployList.size(); i++)
      _deployList.get(i).update();
  }

  /**
   * Returns the deployed keys.
   */
  public void fillDeployedKeys(Set<String> keys)
  {
    for (int i = 0; i < _deployList.size(); i++) {
      _deployList.get(i).fillDeployedKeys(keys);
    }
  }

  /**
   * Generates the entry matching the key string.
   */
  protected E generateController(String key)
  {
    for (int i = 0; i < _deployList.size(); i++) {
      E entry = _deployList.get(i).generateController(key);

      if (entry != null) {
	// merge with the rest of the entries
	for (i++; i < _deployList.size(); i++) {
	  DeployGenerator<E> deploy = _deployList.get(i);

	  entry = deploy.mergeEntry(entry, key);
	}
	
	return entry;
      }
    }

    return null;
  }

  /**
   * Merges with other matching entries.
   */
  protected E mergeEntry(E entry, String key)
  {
    for (int i = 0; i < _deployList.size(); i++) {
      entry = _deployList.get(i).mergeEntry(entry, key);
    }

    return entry;
  }
  
  /**
   * Starts the deploys.
   */
  public void start()
  {
    for (int i = 0; i < _deployList.size(); i++) {
      _deployList.get(i).start();
    }
  }
  
  /**
   * Stops the deploys.
   */
  public void stop()
  {
    for (int i = 0; i < _deployList.size(); i++) {
      _deployList.get(i).stop();
    }
  }
  
  /**
   * Closes the deploys.
   */
  public void destroy()
  {
    ArrayList<DeployGenerator<E>> deployList = new ArrayList<DeployGenerator<E>>(_deployList);
    _deployList.clear();

    for (int i = 0; i < deployList.size(); i++) {
      try {
	deployList.get(i).destroy();
      } catch (Throwable e) {
	log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  public String toString()
  {
    return "DeployListGenerator" + _deployList;
  }
}
