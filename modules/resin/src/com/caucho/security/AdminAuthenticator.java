/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.security;

import javax.enterprise.inject.Default;
import javax.inject.Named;

import com.caucho.config.ResinService;
import com.caucho.v5.config.Admin;
import com.caucho.v5.config.CauchoDeployment;

/**
 * The admin authenticator provides authentication for Resin admin/management
 * purposes.  It's typically defined at the &lt;resin> level.
 *
 * <code><pre>
 * &lt;resin:AdminAuthenticator path="WEB-INF/admin-users.xml"/>
 * </pre></code>
 *
 * <p>The format of the static file is as follows:
 *
 * <code><pre>
 * &lt;users>
 *   &lt;user name="h.potter" password="quidditch" roles="user,captain"/>
 *   ...
 * &lt;/users>
 * </pre></code>
 */
@ResinService
@Admin
@Named("resin-admin-authenticator")
@Default
@CauchoDeployment  
@SuppressWarnings("serial")
public class AdminAuthenticator extends XmlAuthenticator
{
  @Override
  public String getDefaultGroup()
  {
    return "resin-admin";
  }
  
  public boolean isComplete()
  {
    return false;
  }
}
