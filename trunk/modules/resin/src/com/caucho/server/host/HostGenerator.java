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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.host;

import com.caucho.util.L10N;
import com.caucho.vfs.Dependency;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Returns a list of the current host entries
 */
abstract public class HostGenerator implements Dependency {
  static final L10N L = new L10N(HostGenerator.class);
  static final Logger log
    = Logger.getLogger(HostGenerator.class.getName());

  private HostContainer _container;

  /**
   * Returns the owning container.
   */
  public HostContainer getContainer()
  {
    return _container;
  }

  /**
   * Sets the owning container.
   */
  public void setContainer(HostContainer container)
  {
    _container = container;
  }
  
  /**
   * Returns the current array of host entries.
   */
  abstract public ArrayList<HostController> getHosts()
    throws Exception;
  
  /**
   * Returns true if the list of applications has changed.
   */
  public boolean isModified()
  {
    return false;
  }
  
  /**
   * Closes the generator
   */
  public void destroy()
  {
  }
}
