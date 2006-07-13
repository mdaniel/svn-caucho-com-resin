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

package com.caucho.management.j2ee;

import com.caucho.Version;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.server.resin.ServerController;

/**
 * Management interface for a J2EE server.
 * The J2EEServer corresponds to one instance of the product.
 */
public class J2EEServer extends J2EEManagedObject {
  private static EnvironmentLocal<J2EEServer> _j2eeServerLocal
    = new EnvironmentLocal<J2EEServer>("caucho.jmx.j2ee.J2EEServer");

  private final ServerController _serverController;

  static J2EEServer getLocal()
  {
    return _j2eeServerLocal.get();
  }

  public J2EEServer(ServerController serverController)
  {
    _serverController = serverController;

    _j2eeServerLocal.set(this);
  }

  protected String getName()
  {
    return _serverController.getId();
  }

  protected boolean isJ2EEServer()
  {
    return false;
  }

  protected boolean isJ2EEApplication()
  {
    return false;
  }

  /**
   * Returns the ObjectNames of the
   * {@link J2EEApplication}, {@link AppClientModule},
   * {@link ResourceAdapterModule}, {@link EJBModule}, and
   * {@link WebModule}
   * management beans for this server.
   */
  public String []getDeployedObjects()
  {
    return queryObjectNamesSet(
      new String[][] {
        { "j2eeType", "J2EEApplication"},
        { "j2eeType", "AppClientModule"},
        { "j2eeType", "ResourceAdapterModule"},
        { "j2eeType", "EJBModule"},
        { "j2eeType", "WebModule"}
      });
  }

  /**
   * Returns the ObjectNames of the
   * {@link JCAResource}, {@link JavaMailResource},
   * {@link JDBCResource}, {@link JMSResource},
   * {@link JNDIResource}, {@link JTAResource},
   * {@link RMI_IIOPResource}, {and @link URLResource},
   * management beans for this server.
   */
  public String []getResources()
  {
    return queryObjectNamesSet(
      new String[][] {
        { "j2eeType", "JCAResource" },
        { "j2eeType", "JavaMailResource" },
        { "j2eeType", "JDBCResource" },
        { "j2eeType", "JMSResource" },
        { "j2eeType", "JNDIResource" },
        { "j2eeType", "JTAResource" },
        { "j2eeType", "RMI_IIOPResource" },
        { "j2eeType", "URLResource" },
      });
  }

  /**
   * Returns the ObjectNames of the {@link JVM}
   * management beans for each virtual machine on which
   * this J2EEServer has running threads.
   */
  public String []getJavaVMs()
  {
    return queryObjectNames("j2eeType", "JVM");
  }

  /**
   * Returns the server vendor
   */
  public String getServerVendor()
  {
    return "Caucho Technology, Inc.";
  }

  /**
   * Returns the server version
   */
  public String getServerVersion()
  {
    return Version.FULL_VERSION;
  }

}
