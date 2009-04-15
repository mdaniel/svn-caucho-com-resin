/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package javax.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Configuration for cookie handling
 *
 * @since servlet 3.0
 */
public class SessionCookieConfig {
  private String _domain;
  private String _comment;
  private String _path;
  private boolean _isHttpOnly;
  private boolean _isSecure;

  /**
   * Create the new configuration.
   */
  public SessionCookieConfig(String domain,
			     String path,
			     String comment,
			     boolean isHttpOnly,
			     boolean isSecure)
  {
    _domain = domain;
    _path = path;
    _comment = comment;
    _isHttpOnly = isHttpOnly;
    _isSecure = isSecure;
  }

  /**
   * Returns the default cookie comment
   */
  public String getComment()
  {
    return _comment;
  }

  /**
   * Returns the default cookie domain
   */
  public String getDomain()
  {
    return _domain;
  }

  /**
   * Returns the default cookie path
   */
  public String getPath()
  {
    return _path;
  }

  /**
   * Returns the default cookie HttpOnly value
   */
  public boolean isHttpOnly()
  {
    return _isHttpOnly;
  }

  /**
   * Returns the default cookie Secure value
   */
  public boolean isSecure()
  {
    return _isSecure;
  }
}
