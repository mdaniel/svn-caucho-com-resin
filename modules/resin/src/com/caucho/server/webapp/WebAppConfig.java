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

package com.caucho.server.webapp;

import java.util.regex.Pattern;

import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.util.CauchoSystem;

import com.caucho.log.Log;

import com.caucho.vfs.Path;

import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.BuilderProgramContainer;
import com.caucho.config.ConfigException;

import com.caucho.config.types.RawString;

import com.caucho.server.deploy.DeployController;

/**
 * The configuration for a web.app in the resin.conf
 */
public class WebAppConfig {
  static final L10N L = new L10N(WebAppConfig.class);
  static final Logger log = Log.open(WebAppConfig.class);

  // The id
  private String _id;

  // Any regexp
  private Pattern _urlRegexp;

  // The context path
  private String _contextPath;

  // The app dir
  private String _appDir;

  // The archive path;
  private String _archivePath;

  // startup mode
  private String _startupMode;

  // redeploy mode
  private String _redeployMode;
  
  // The configuration program
  private BuilderProgramContainer _program = new BuilderProgramContainer();

  /**
   * Sets the id.
   */
  public void setId(String id)
  {
    _id = id;
  }

  /**
   * Gets the id.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Gets the context path
   */
  public String getContextPath()
  {
    String cp = _contextPath;

    if (cp == null)
      cp = getId();

    if (cp == null)
      return null;

    if (cp.endsWith("/"))
      return cp.substring(0, cp.length() - 1);
    else
      return cp;
  }

  /**
   * Sets the context path
   */
  public void setContextPath(String path)
    throws ConfigException
  {
    if (! path.startsWith("/"))
      throw new ConfigException(L.l("context-path '{0}' must start with '/'.",
				    path));
    
    _contextPath = path;
  }

  /**
   * Sets the url-regexp
   */
  public void setURLRegexp(String pattern)
  {
    if (! pattern.endsWith("$"))
      pattern = pattern + "$";
    if (! pattern.startsWith("^"))
      pattern = "^" + pattern;

    if (CauchoSystem.isCaseInsensitive())
      _urlRegexp = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    else
      _urlRegexp = Pattern.compile(pattern);
  }

  /**
   * Gets the regexp.
   */
  public Pattern getURLRegexp()
  {
    return _urlRegexp;
  }

  /**
   * Sets the app-dir.
   */
  public void setAppDir(RawString appDir)
  {
    /*
    if (appDir.endsWith(".jar") || appDir.endsWith(".war")) {
      throw new ConfigException(L.l("app-dir `{0}' may not be a .jar or .war.  app-dir must specify a directory.",
                                    appDir));
    }
    */
    
    _appDir = appDir.getValue();
  }

  /**
   * Gets the app-dir.
   */
  public String getAppDir()
  {
    return _appDir;
  }

  /**
   * Sets the app-dir.
   */
  public String getDocumentDirectory()
  {
    return getAppDir();
  }

  /**
   * Sets the app-dir.
   */
  public void setDocumentDirectory(RawString dir)
  {
    setAppDir(dir);
  }

  /**
   * Sets the archive-path
   */
  public void setArchivePath(RawString path)
  {
    _archivePath = path.getValue();
  }

  /**
   * Gets the archive-path
   */
  public String getArchivePath()
  {
    return _archivePath;
  }

  /**
   * Sets the startup-mode
   */
  public void setStartupMode(String mode)
    throws ConfigException
  {
    _startupMode = DeployController.toStartupCode(mode);
  }

  /**
   * Gets the startup mode.
   */
  public String getStartupMode()
  {
    return _startupMode;
  }

  /**
   * Sets the startup-mode
   */
  public void setLazyInit(boolean isLazy)
    throws ConfigException
  {
    log.config(L.l("lazy-init is deprecated.  Use <startup-mode>lazy</startup-mode> instead."));

    if (isLazy)
      setStartupMode(DeployController.STARTUP_LAZY);
    else
      setStartupMode(DeployController.STARTUP_AUTOMATIC);
  }

  /**
   * Sets the redeploy-mode
   */
  public void setRedeployMode(String mode)
    throws ConfigException
  {
    _redeployMode = DeployController.toRedeployCode(mode);
  }

  /**
   * Gets the redeploy mode.
   */
  public String getRedeployMode()
  {
    return _redeployMode;
  }

  /**
   * Adds to the builder program.
   */
  public void addBuilderProgram(BuilderProgram program)
  {
    _program.addProgram(program);
  }

  /**
   * Returns the program.
   */
  public BuilderProgram getBuilderProgram()
  {
    return _program;
  }

  /**
   * Initialization checks.
   */
  public void init()
    throws ConfigException
  {
    if (_urlRegexp != null && (_appDir == null || _appDir.indexOf("$") < 0)) {
      log.config(L.l("<web-app> with url-regexp expects a <document-directory>  with replacement variables (e.g. $1)."));
    }
  }

  /*
  public boolean equals(Object o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    WebAppConfig config = (WebAppConfig) o;

    return _id.equals(config._id);
  }
  */
}
