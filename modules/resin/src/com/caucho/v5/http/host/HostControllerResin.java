/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.host;

import java.util.Map;

import com.caucho.v5.http.host.HostBuilder;
import com.caucho.v5.http.host.HostConfig;
import com.caucho.v5.http.host.HostController;
import com.caucho.v5.vfs.PathImpl;

/**
 * A configuration entry for a host
 */
public class HostControllerResin
  extends HostController
{
  HostControllerResin(String id,
                 PathImpl rootDirectory,
                 String hostName,
                 HostConfig config,
                 HostContainerResin container,
                 Map<String,Object> varMap)
  {
    super(id, rootDirectory, hostName, config, container, varMap);
  }

  public HostControllerResin(String id,
                             PathImpl rootDirectory,
                             String hostName,
                             HostContainerResin container)
  {
    this(id, rootDirectory, hostName, null, container, null);
  }
  
  @Override
  public HostContainerResin getContainer()
  {
    return (HostContainerResin) super.getContainer();
  }

  /**
   * Creates a new instance of the host object.
   */
  @Override
  protected HostBuilder createInstanceBuilder()
  {
    return new HostBuilderResin(getContainer(), this, getHostName());
  }

  /**
   * Returns a printable view.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "]";
  }
}
