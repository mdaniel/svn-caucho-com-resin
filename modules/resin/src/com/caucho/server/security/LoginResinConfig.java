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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.security;

import com.caucho.config.cfg.BeanConfig;
import com.caucho.config.*;
import com.caucho.util.L10N;

/**
 * The login configures a basic: or form:.
 */
public class LoginResinConfig extends BeanConfig {
  private static final L10N L = new L10N(LoginResinConfig.class);

  public LoginResinConfig()
  {
    setScope("singleton");
    setName("login");
  }

  @Override
  public Class getBeanConfigClass()
  {
    return LoginFilter.class;
  }

  /**
   * Override the old meaning of type for backward compat.
   */
  @Override
  public void setType(Class cl)
  {
    setClass(cl);
  }

  /**
   * Check for correct type.
   */
  @Override
  public void setClass(Class cl)
  {
    super.setClass(cl);

    if (! LoginFilter.class.isAssignableFrom(cl))
      throw new ConfigException(L.l("<login> class '{0}' must implement com.caucho.server.security.LoginFilter"));
  }

  public AbstractLogin getLoginObject()
  {
    return (AbstractLogin) getComponentFactory().get();
  }

  public String toString()
  {
    return "Login[]";
  }
}

