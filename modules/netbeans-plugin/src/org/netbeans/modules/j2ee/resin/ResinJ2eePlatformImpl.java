/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package org.netbeans.modules.j2ee.resin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eeModule;
import org.netbeans.modules.j2ee.deployment.plugins.api.J2eePlatformImpl;
import org.netbeans.spi.project.libraries.LibraryImplementation;

import org.openide.util.NbBundle;
import org.openide.util.Utilities;

public class ResinJ2eePlatformImpl extends J2eePlatformImpl
{
  private static final Logger log = Logger.getLogger(ResinJ2eePlatformImpl.class.getName());

  
  public boolean isToolSupported(String toolName)
  {
    return false;
  }
  
  public File[] getToolClasspathEntries(String toolName)
  {
    return new File[0];
  }
  
  public Set getSupportedSpecVersions()
  {
    Set result = new HashSet();
    result.add(J2eeModule.J2EE_14);
    //result.add(J2eeModule.JAVA_EE_5);
    return result;
  }
  
  public java.util.Set getSupportedModuleTypes()
  {
    Set result = new HashSet();
    result.add(J2eeModule.EAR);
    result.add(J2eeModule.WAR);
    result.add(J2eeModule.EJB);
    return result;
  }
  
  public java.io.File[] getPlatformRoots()
  {
    return new File[0];
  }
  
  public LibraryImplementation[] getLibraries()
  {
    return new LibraryImplementation[0];
  }
  
  public java.awt.Image getIcon()
  {
    return Utilities.loadImage("org/netbeans/modules/j2ee/resin/resources/server.gif"); // NOI18N
    
  }
  
  public String getDisplayName()
  {
    return NbBundle.getMessage(ResinJ2eePlatformImpl.class, "MSG_ResinServerPlatform");
  }
  
  public Set getSupportedJavaPlatformVersions()
  {
    Set versions = new HashSet();
    versions.add("1.4"); // NOI18N
    versions.add("1.5"); // NOI18N
    return versions;
  }
  
  public JavaPlatform getJavaPlatform()
  {
    return JavaPlatformManager.getDefault().getDefaultPlatform();
  }
  
}