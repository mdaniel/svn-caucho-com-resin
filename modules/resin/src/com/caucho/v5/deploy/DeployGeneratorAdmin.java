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
 * @author Sam
 */

package com.caucho.v5.deploy;

import com.caucho.v5.jmx.server.ManagedObjectBase;
import com.caucho.v5.management.server.DeployMXBean;

abstract public class DeployGeneratorAdmin<C extends DeployGenerator>
  extends ManagedObjectBase
  implements DeployMXBean
{
  private final C _deployGenerator;

  public DeployGeneratorAdmin(C deployGenerator)
  {
    _deployGenerator = deployGenerator;
  }

  protected C getDeployGenerator()
  {
    return _deployGenerator;
  }

  abstract public String getName();

  public String getRedeployMode()
  {
    return _deployGenerator.getRedeployMode().toString();
  }

  public String getStartupMode()
  {
    return _deployGenerator.getStartupMode().toString();
  }

  public boolean isModified()
  {
    return _deployGenerator.isModified();
  }

  public String getState()
  {
    return _deployGenerator.getState();
  }

  public void start()
  {
    _deployGenerator.start();
  }

  public void stop()
  {
    _deployGenerator.stop();
  }

  public void update()
  {
    _deployGenerator.update();
  }

  public Throwable getConfigException()
  {
    return _deployGenerator.getConfigException();
  }
}
