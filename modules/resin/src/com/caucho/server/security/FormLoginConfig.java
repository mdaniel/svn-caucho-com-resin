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

package com.caucho.server.security;

import java.util.logging.*;

import javax.servlet.*;
import javax.naming.*;

import com.caucho.log.Log;

import com.caucho.util.L10N;

import com.caucho.vfs.WriteStream;
import com.caucho.vfs.LogStream;

/**
 * Configuration for the login-config.
 */
public class FormLoginConfig {
  static final Logger log = Log.open(FormLoginConfig.class);
  static final L10N L = new L10N(FormLoginConfig.class);

  private String _formLoginPage;
  private String _formErrorPage;
  private boolean _isInternalForward;

  /**
   * Creates the form-login-config.
   */
  public FormLoginConfig()
  {
  }

  /**
   * Sets the form-login-page.
   */
  public void setFormLoginPage(String page)
  {
    _formLoginPage = page;
  }

  /**
   * Gets the form-login-page.
   */
  public String getFormLoginPage()
  {
    return _formLoginPage;
  }

  /**
   * Sets the form-error-page.
   */
  public void setFormErrorPage(String page)
  {
    _formErrorPage = page;
  }

  /**
   * Gets the form-error-page.
   */
  public String getFormErrorPage()
  {
    return _formErrorPage;
  }

  /**
   * Gets the internal-forward
   */
  public boolean isInternalForward()
  {
    return _isInternalForward;
  }

  /**
   * Sets the internal-forward
   */
  public void setInternalForward(boolean isInternalForward)
  {
    _isInternalForward = isInternalForward;
  }
}
