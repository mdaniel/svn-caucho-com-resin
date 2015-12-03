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

package com.caucho.security;

import com.caucho.config.Admin;
import com.caucho.config.CauchoDeployment;
import com.caucho.config.Service;
import com.caucho.config.types.Period;
import com.caucho.distcache.AbstractCache;
import com.caucho.distcache.ClusterCache;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.server.distcache.CacheImpl;
import com.caucho.util.Alarm;
import com.caucho.util.Base64;
import com.caucho.util.Crc64;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

import javax.enterprise.inject.Default;
import javax.inject.Named;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

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
@Service
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
