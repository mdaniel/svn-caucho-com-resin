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
 * @author Sam
 */
package com.caucho.netbeans;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceCreationException;
import org.netbeans.modules.j2ee.deployment.plugins.api.InstanceProperties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import javax.enterprise.deploy.spi.exceptions.DeploymentManagerCreationException;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ResinConfiguration
  implements Cloneable
{

  private static final PluginL10N L = new PluginL10N(ResinConfiguration.class);
  private static final Logger log
    = Logger.getLogger(ResinConfiguration.class.getName());
  private static final String PROPERTY_AUTOLOAD_ENABLED = "autoload_enabled";
  private static final String PROPERTY_DEBUG_PORT = "debugger_port";
  private static final String PROPERTY_DISPLAY_NAME
    = InstanceProperties.DISPLAY_NAME_ATTR;
  private static final String PROPERTY_JAVA_PLATFORM = "java_platform";
  private static final String PROPERTY_JAVA_OPTS = "java_opts";
  private static final String PLATFORM_PROPERTY_ANT_NAME = "platform.ant.name";
  private static final String URI_TOKEN_HOME = ":home=";
  private static final String URI_TOKEN_CONF = ":conf=";
  private static final String URI_TOKEN_SERVER_ID = ":server-id=";
  private static final String URI_TOKEN_SERVER_PORT = ":server-port=";
  private static final String URI_TOKEN_SERVER_ADDRESS = ":server-address=";
  private ResinInstance _resin;
  private File _resinConf;
  private String _serverId;
  private JavaPlatform _javaPlatform;
  private int _debugPort = 0;
  private int _startTimeout = 60 * 1000;
  private int _stopTimeout = 60 * 1000;

  public ResinConfiguration(ResinInstance resin)
    throws DeploymentManagerCreationException
  {
    _resin = resin;
  }

  public ResinInstance getResinInstance()
  {
    return _resin;
  }

  String getContextPath()
  {
    return "/test";
  }

  int getPort()
  {
    return _resin.getPort();
  }

  @Override
  protected Object clone()
  {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(e);
    }
  }

  public File getResinConf()
  {
    return new File(_resin.getConf());
  }

  public void setResinConf(File resinConf)
  {
    _resinConf = resinConf;
  }

  public File getResinHome()
  {
    return new File(_resin.getHome());
  }

  public File getResinRoot() {
    return new File(_resin.getRoot());
  }

  public File getWebapps() {
    return new File(_resin.getWebapps());
  }

  public String getServerId()
  {
    return _serverId;
  }

  public void setServerId(String serverId)
  {
    _serverId = serverId;
  }

  public String getServerAddress()
  {
    return _resin.getAddress();
  }

  public int getServerPort()
  {
    return _resin.getPort();
  }

  /**
   * Returns the debug port, 0 means a free port should be determined.
   */
  public int getDebugPort()
  {
    return _debugPort;
  }

  public void setDebugPort(int debugPort)
  {
    _debugPort = debugPort;
  }

  /**
   * Returns the java platform.
   */
  public JavaPlatform getJavaPlatform()
  {
    return _javaPlatform;
  }

  public void setJavaPlatform(JavaPlatform javaPlatform)
  {
    _javaPlatform = javaPlatform;
  }

  public static String getJavaPlatformName(JavaPlatform javaPlatform)
  {
    return ((String) javaPlatform.getProperties()
      .get(PLATFORM_PROPERTY_ANT_NAME));
  }

  public void setJavaPlatformByName(String javaPlatformName)
  {
    JavaPlatformManager platformManager = JavaPlatformManager.getDefault();
    JavaPlatform javaPlatform = platformManager.getDefaultPlatform();

    JavaPlatform[] installedPlatforms = platformManager.getPlatforms(null,
                                                                     new Specification(
                                                                       "J2SE",
                                                                       null));

    for (JavaPlatform installedPlatform : installedPlatforms) {
      String platformName = getJavaPlatformName(installedPlatform);

      if (platformName != null && platformName.equals(javaPlatformName)) {
        javaPlatform = installedPlatform;
        break;
      }
    }

    _javaPlatform = javaPlatform;
  }

  public int getStartTimeout()
  {
    return _startTimeout;
  }

  public void setStartTimeout(int startTimeout)
  {
    _startTimeout = startTimeout;
  }

  public int getStopTimeout()
  {
    return _stopTimeout;
  }

  public void setStopTimeout(int stopTimeout)
  {
    _stopTimeout = stopTimeout;
  }

  /**
   * Calculates a javaHome based on the {@link #getJavaPlatform()}
   * javaHome.
   */
  public File calculateJavaHome()
  {
    JavaPlatform javaPlatform = _javaPlatform;

    if (javaPlatform == null) {
      javaPlatform = JavaPlatformManager.getDefault().getDefaultPlatform();
    }

    return FileUtil.toFile((FileObject) javaPlatform.getInstallFolders()
      .iterator()
      .next());
  }

  public List<URL> getClasses()
  {
    // XXX: s/b urls to Resin libraries
    return new ArrayList<URL>();
  }

  public List<URL> getSources()
  {
    // XXX: s/b urls to Resin sources
    return new ArrayList<URL>();
  }

  public List<URL> getJavadocs()
  {
    return new ArrayList<URL>();
  }

  public String getDisplayName()
  {
    return _resin.getDisplayName();
  }

  private void requiredFile(String name, File file)
    throws IllegalStateException
  {
    if (file == null) {
      throw new IllegalStateException(L.l("''{0}'' is required", name));
    }

    if (!file.exists()) {
      throw new IllegalStateException(L.l("''{0}'' does not exist", file));
    }
  }

  public void validate()
    throws IllegalStateException
  {
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _resin.getUrl() + "]";
  }
}
