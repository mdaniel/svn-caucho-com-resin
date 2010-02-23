/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.boot;

import com.caucho.config.*;
import com.caucho.config.inject.*;
import com.caucho.config.program.*;
import com.caucho.lifecycle.*;
import com.caucho.server.admin.*;
import com.caucho.server.resin.*;
import com.caucho.security.*;
import com.caucho.security.PasswordDigest;
import com.caucho.security.PasswordUser;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.annotation.*;
import java.util.*;
import java.util.logging.*;

/**
 * Configuration for management.
 */
public class BootManagementConfig
{
  private static L10N L = new L10N(BootManagementConfig.class);
  private static Logger log
    = Logger.getLogger(BootManagementConfig.class.getName());

  private AdminAuthenticator _auth;
  
  /**
   * Adds a user
   */
  public XmlAuthenticator.User createUser()
  {
    if (_auth == null)
      _auth = new AdminAuthenticator();

    return _auth.createUser();
  }
  
  /**
   * Adds a user
   */
  public void addUser(XmlAuthenticator.User user)
  {
    _auth.addUser(user);
  }

  public AdminAuthenticator getAdminAuthenticator()
  {
    return _auth;
  }

  public void setAdminAuthenticator(AdminAuthenticator auth)
  {
    _auth = auth;
  }

  public String getAdminCookie()
  {
    if (_auth != null)
      return _auth.getHash();
    else
      return null;
  }

  public void addPath(ConfigProgram program)
  {
  }

  public void addLogService(ConfigProgram program)
  {
  }

  public void addDeployService(ConfigProgram program)
  {
  }

  public void addStatService(ConfigProgram program)
  {
  }

  public void addJmxService(ConfigProgram program)
  {
  }

  public void addXaLogService(ConfigProgram program)
  {
  }

  @PostConstruct
  public void init()
  {
    try {
      if (_auth != null) {
	_auth.init();
      
	InjectManager manager = InjectManager.create();
	BeanFactory factory = manager.createBeanFactory(Authenticator.class);
	factory.type(Authenticator.class);
	factory.type(AdminAuthenticator.class);

	manager.addBean(factory.singleton(_auth));
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw ConfigException.create(e);
    }
  }
}
