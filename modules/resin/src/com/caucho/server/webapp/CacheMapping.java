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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.webapp;

import javax.servlet.*;

import com.caucho.util.L10N;

import com.caucho.config.types.Period;

/**
 * Configuration for a cache-mapping.
 */
public class CacheMapping {
  static L10N L = new L10N(CacheMapping.class);

  // The path-mapping pattern
  private String _urlPattern;
  
  // The period
  private long _expiresPeriod = Long.MIN_VALUE;

  /**
   * Creates the path mapping.
   */
  public CacheMapping()
  {
  }

  /**
   * Sets the urlPattern
   */
  public void setUrlPattern(String urlPattern)
  {
    _urlPattern = urlPattern;
  }

  /**
   * Gets the urlPattern.
   */
  public String getUrlPattern()
  {
    return _urlPattern;
  }

  /**
   * Sets the period
   */
  public void setExpires(Period period)
  {
    _expiresPeriod = period.getPeriod();
    System.out.println("PERIOD: " + _expiresPeriod);
  }

  /**
   * Gets the expires period.
   */
  public long getExpires()
  {
    return _expiresPeriod;
  }

  /**
   * Init
   */
  public void init()
    throws ServletException
  {
    if (_urlPattern == null)
      throw new ServletException(L.l("cache-mapping needs 'url-pattern' attribute."));
    if (_expiresPeriod == Long.MIN_VALUE)
      throw new ServletException(L.l("cache-mapping needs 'expires' attribute."));
  }
}
