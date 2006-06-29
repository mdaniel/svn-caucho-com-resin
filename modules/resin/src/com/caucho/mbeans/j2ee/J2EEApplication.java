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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.mbeans.j2ee;

import com.caucho.server.e_app.EarDeployController;
import com.caucho.loader.EnvironmentLocal;

/**
 * Management interface for a J2EE Application (EAR).
 */
public class J2EEApplication extends J2EEDeployedObject {
  private static EnvironmentLocal<J2EEApplication> _j2eeApplicationLocal
    = new EnvironmentLocal<J2EEApplication>("caucho.jmx.j2ee.J2EEApplication");

  private final EarDeployController _earDeployController;

  static J2EEApplication getLocal()
  {
    return _j2eeApplicationLocal.get();
  }

  public J2EEApplication(EarDeployController earDeployController)
  {
    _earDeployController = earDeployController;
    _j2eeApplicationLocal.set(this);
  }

  protected String getName()
  {
    String name = _earDeployController.getId();

    return name == null ? "default" : name;
  }

  protected boolean isJ2EEApplication()
  {
    return false;
  }

  public String getDeploymentDescriptor()
  {
    // XXX:
    return "";
  }

  /**
   * Returns the ObjectNames of the {@link J2EEModule}
   * management beans that are contained within this application.
   */
  public String []getModules()
  {
    return queryObjectNames(new String[][] {
      { "j2eeType", "AppClientModule" },
      { "j2eeType", "EJBModule" },
      { "j2eeType", "WebModule" },
      { "j2eeType", "ResourceAdapterModule"}
    });
  }

}
