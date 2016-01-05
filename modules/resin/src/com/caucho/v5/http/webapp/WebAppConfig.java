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

package com.caucho.v5.http.webapp;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.RawString;
import com.caucho.v5.deploy.ConfigDeploy;
import com.caucho.v5.deploy.DeployMode;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.L10N;

import io.baratine.config.Configurable;

/**
 * The configuration for a web-app in the resin.conf
 */
public class WebAppConfig extends ConfigDeploy
{
  private static final L10N L = new L10N(WebAppConfig.class);
  private static final Logger log
    = Logger.getLogger(WebAppConfig.class.getName());

  // Any regexp
  private Pattern _urlRegexp;
  
  private ArrayList<Pattern> _aliasUrlRegexpList = new ArrayList<Pattern>();

  // The context path
  private String _contextPath;

  public WebAppConfig()
  {
  }

  /**
   * Gets the context path
   */
  public String getContextPath()
  {
    String cp = _contextPath;

    if (cp == null) {
      cp = getId();
    }

    if (cp == null) {
      return null;
    }

    if (cp.endsWith("/")) {
      return cp.substring(0, cp.length() - 1);
    }
    else {
      return cp;
    }
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

    if (CauchoUtil.isCaseInsensitive())
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
   * Sets the url-regexp
   */
  public void addAliasUrlRegexp(String pattern)
  {
    if (! pattern.endsWith("$"))
      pattern = pattern + "$";
    if (! pattern.startsWith("^"))
      pattern = "^" + pattern;

    Pattern urlRegexp;
    
    if (CauchoUtil.isCaseInsensitive())
      urlRegexp = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    else
      urlRegexp = Pattern.compile(pattern);
    
    _aliasUrlRegexpList.add(urlRegexp);
    
  }
  
  boolean isUrlMatch(String url)
  {
    for (int i = _aliasUrlRegexpList.size() - 1; i >= 0; i--) {
      Pattern regexp = _aliasUrlRegexpList.get(i);
      
      if (regexp.matcher(url).matches()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Sets the app-dir.
   */
  public void setAppDir(RawString appDir)
  {
    setRootDirectory(appDir);
  }

  /**
   * Sets the app-dir.
   */
  public void setDocumentDirectory(RawString dir)
  {
    setRootDirectory(dir);
  }

  /**
   * Sets the startup-mode
   */
  public void setLazyInit(boolean isLazy)
    throws ConfigException
  {
    log.config(L.l("lazy-init is deprecated.  Use <startup-mode>lazy</startup-mode> instead."));

    if (isLazy)
      setStartupMode(DeployMode.LAZY);
    else
      setStartupMode(DeployMode.AUTOMATIC);
  }

  /**
   * Sets the prologue.
   */
  /*
  public void setPrologue(WebAppConfig prologue)
  {
    _prologue = prologue;
  }
  */

  /**
   * Gets the prologue.
   */
  /*
  @Override
  public ConfigDeploy getPrologue()
  {
    return _prologue;
  }
  */

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _contextPath + "]";
  }
}
