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

package com.caucho.server.webapp;

import com.caucho.config.ConfigException;
import com.caucho.config.types.RawString;
import com.caucho.env.deploy.DeployConfig;
import com.caucho.env.deploy.DeployMode;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The configuration for a web-app in the resin.conf
 */
public class WebAppConfig extends DeployConfig {
  private static final L10N L = new L10N(WebAppConfig.class);
  private static final Logger log
    = Logger.getLogger(WebAppConfig.class.getName());

  // Any regexp
  private Pattern _urlRegexp;
  
  private ArrayList<Pattern> _aliasUrlRegexpList = new ArrayList<Pattern>();

  // The context path
  private String _contextPath;
  
  private WebAppConfig _prologue;

  public WebAppConfig()
  {
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
   * Sets the url-regexp
   */
  public void addAliasUrlRegexp(String pattern)
  {
    if (! pattern.endsWith("$"))
      pattern = pattern + "$";
    if (! pattern.startsWith("^"))
      pattern = "^" + pattern;

    Pattern urlRegexp;
    
    if (CauchoSystem.isCaseInsensitive())
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
  public void setPrologue(WebAppConfig prologue)
  {
    _prologue = prologue;
  }

  /**
   * Gets the prologue.
   */
  @Override
  public DeployConfig getPrologue()
  {
    return _prologue;
  }

  /**
   * Returns the context path for a URI, including regexp processing.
   */
  public String getContextPath(String uri)
  {
    Pattern regexp = getURLRegexp();
    
    String contextPath = getContextPath(regexp, uri);
    
    if (contextPath != null) {
      return contextPath;
    }
    
    for (Pattern pattern : _aliasUrlRegexpList) {
      contextPath = getContextPath(pattern, uri);

      if (contextPath != null) {
        return contextPath;
      }
    }
    
    return null;
  }
  
  private String getContextPath(Pattern regexp, String uri)
  {
    if (regexp == null) {
      return null;
    }
    
    Matcher matcher = regexp.matcher(uri);

    int tail = 0;
    while (tail >= 0 && tail <= uri.length()) {
      String prefix = uri.substring(0, tail);

      matcher.reset(prefix);

      if (matcher.find() && matcher.start() == 0) {
        return uri.substring(0, matcher.end());
      }

      if (tail < uri.length()) {
        tail = uri.indexOf('/', tail + 1);
        if (tail < 0) {
          tail = uri.length();
        }
      }
      else
        break;
    }
    
    return null;
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _contextPath + "]";
  }
}
