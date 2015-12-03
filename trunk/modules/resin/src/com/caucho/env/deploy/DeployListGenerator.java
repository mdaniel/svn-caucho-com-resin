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

package com.caucho.env.deploy;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.vfs.Dependency;

/**
 * A list of deploy objects.
 */
public class DeployListGenerator<E extends DeployControllerApi<?>>
  extends DeployGenerator<E> implements Dependency {
  private static final Logger log
    = Logger.getLogger(DeployListGenerator.class.getName());
  
  private ArrayList<DeployGenerator<E>> _generatorList
    = new ArrayList<DeployGenerator<E>>();

  /**
   * Creates the deploy.
   */
  public DeployListGenerator(DeployContainer<E> container)
  {
    super(container);
  }
  
  /**
   * Adds a deploy.
   */
  public void add(DeployGenerator<E> deploy)
  {
    if (! _generatorList.contains(deploy))
      _generatorList.add(deploy);
  }
  
  /**
   * Removes a deploy.
   */
  public void remove(DeployGenerator<E> deploy)
  {
    _generatorList.remove(deploy);
  }

  /**
   * Returns true if the deployment has modified.
   */
  @Override
  public boolean isModified()
  {
    for (int i = _generatorList.size() - 1; i >= 0; i--) {
      if (_generatorList.get(i).isModified()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Logs the modified location.
   */
  @Override
  public boolean logModified(Logger log)
  {
    for (int i = _generatorList.size() - 1; i >= 0; i--) {
      if (_generatorList.get(i).logModified(log))
        return true;
    }

    return false;
  }

  /**
   * Redeploy if the deployment is modified.
   *
   * XXX:
   */
  @Override
  public void updateIfModified()
  {
    for (int i = _generatorList.size() - 1; i >= 0; i--) {
      _generatorList.get(i).updateIfModified();
    }
  }

  /**
   * Force an update
   */
  @Override
  public void update()
  {
    for (int i = 0; i < _generatorList.size(); i++)
      _generatorList.get(i).update();
  }

  /**
   * Returns the deployed keys.
   */
  @Override
  public void fillDeployedNames(Set<String> keys)
  {
    for (int i = 0; i < _generatorList.size(); i++) {
      _generatorList.get(i).fillDeployedNames(keys);
    }
  }

  /**
   * Generates the controller matching the key string.
   */
  @Override
  protected void generateController(String key, ArrayList<E> controllers)
  {
    for (int i = 0; i < _generatorList.size(); i++) {
      _generatorList.get(i).generateController(key, controllers);
    }
    
    /*
      if (controller != null)
        continue;
      
      // merge with the rest of the entries
      for (int j = 0; j < _generatorList.size(); j++) {
        DeployGenerator<E> generator = _generatorList.get(j);

        // XXX: issue with server/10tl
        controller = generator.mergeController(controller, key);
      }

      return controller;
    }

    return null;
    */
  }

  /**
   * Merges with other matching entries.
   */
  @Override
  protected void mergeController(E controller, String key)
  {
    for (int i = 0; i < _generatorList.size(); i++) {
      _generatorList.get(i).mergeController(controller, key);
    }
  }
  
  /**
   * Starts the deploys.
   */
  @Override
  protected void startImpl()
  {
    super.startImpl();

    for (int i = 0; i < _generatorList.size(); i++) {
      _generatorList.get(i).start();
    }
  }
  
  /**
   * Stops the deploys.
   */
  @Override
  protected void stopImpl()
  {
    for (int i = 0; i < _generatorList.size(); i++) {
      _generatorList.get(i).stop();
    }

    super.stopImpl();
  }
  
  /**
   * Closes the deploys.
   */
  @Override
  protected void destroyImpl()
  {
    ArrayList<DeployGenerator<E>> generatorList
      = new ArrayList<DeployGenerator<E>>(_generatorList);
    
    _generatorList.clear();

    for (int i = 0; i < generatorList.size(); i++) {
      try {
        generatorList.get(i).destroy();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    super.destroyImpl();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _generatorList + "]";
  }
}
