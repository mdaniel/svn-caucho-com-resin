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

package com.caucho.boot;

import com.caucho.config.*;
import com.caucho.config.program.*;
import com.caucho.lifecycle.*;
import com.caucho.server.admin.*;
import com.caucho.server.resin.*;
import com.caucho.security.*;
import com.caucho.security.PasswordDigest;
import com.caucho.security.PasswordUser;
import com.caucho.server.security.*;
import com.caucho.webbeans.manager.*;
import com.caucho.util.L10N;

import javax.annotation.*;
import javax.webbeans.*;
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

  private ManagementAuthenticator _auth;

  /**
   * Adds a user
   */
  public void addUser(User user)
  {
    if (_auth == null)
      _auth = new ManagementAuthenticator();

    _auth.addUser(user.getName(), user.getPasswordUser());
  }

  public String getAdminCookie()
  {
    if (_auth != null)
      return _auth.getHash();
    else
      return null;
  }

  public void addBuilderProgram(ConfigProgram program)
  {
  }

  @PostConstruct
  public void init()
  {
    try {
      if (_auth != null)
	_auth.init();
    } catch (Exception e) {
      e.printStackTrace();
      
      throw ConfigException.create(e);
    }
  }

  public static class User {
    private String _name;
    private String _password;
    private boolean _isDisabled;

    public void setName(String name)
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public void setPassword(String password)
    {
      _password = password;
    }

    public String getPassword()
    {
      return _password;
    }

    public void setDisable(boolean isDisabled)
    {
      _isDisabled = isDisabled;
    }

    public boolean isDisable()
    {
      return _isDisabled;
    }

    PasswordUser getPasswordUser()
    {
      if (_name == null)
	throw new ConfigException(L.l("management <user> requires a 'name' attribute"));
      
      boolean isAnonymous = false;
      
      return new PasswordUser(new BasicPrincipal(_name),
			      _password.toCharArray(),
			      _isDisabled, isAnonymous,
			      new String[] { "resin-admin" });
    }
  }
}
