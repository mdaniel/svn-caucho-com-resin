/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Sam
 */


package com.caucho.quercus.lib;

import com.caucho.Version;
import com.caucho.jmx.Jmx;
import com.caucho.naming.Jndi;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.ReadOnly;
import com.caucho.server.webapp.Application;
import com.caucho.util.L10N;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class ResinModule
  extends AbstractQuercusModule
{
  private static final L10N L = new L10N(ResinModule.class);

  /**
   * Perform a jndi lookup to retrieve an object.
   *
   * @param name a fully qualified name "java:comp/env/foo", or a short-form "foo".
   *
   * @return the object, or null if it is not found.
   */
  public static Object jndi_lookup(String name)
  {
    return Jndi.lookup(name);
  }


  /**
   * Returns the version of the Resin server software.
   */
  public String resin_version()
  {
    return Version.FULL_VERSION;
  }

}
