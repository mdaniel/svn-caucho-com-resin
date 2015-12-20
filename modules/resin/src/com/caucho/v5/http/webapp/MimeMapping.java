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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.webapp;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.util.L10N;

/**
 * Configuration for a mime-mapping.
 */
public class MimeMapping {
  static L10N L = new L10N(MimeMapping.class);

  // The mime-mapping extension
  private String _extension;
  
  // The mimeType
  private String _mimeType;

  /**
   * Creates the mime mapping.
   */
  public MimeMapping()
  {
  }

  /**
   * Sets the extension
   */
  @ConfigArg(0)
  public void setExtension(String extension)
    throws ServletException
  {
    if (! extension.startsWith("."))
      extension = "." + extension;
    
    _extension = extension;
  }

  /**
   * Gets the extension.
   */
  public String getExtension()
  {
    return _extension;
  }

  /**
   * Sets the mime type
   */
  @ConfigArg(1)
  public void setMimeType(String mimeType)
  {
    _mimeType = mimeType;
  }

  /**
   * Gets the mime type
   */
  public String getMimeType()
  {
    return _mimeType;
  }

  /**
   * Init
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    if (_extension == null)
      throw new ServletException(L.l("mime-mapping needs 'extension' attribute."));
    if (_mimeType == null)
      throw new ServletException(L.l("mime-mapping needs 'mime-type' attribute."));
  }
}
