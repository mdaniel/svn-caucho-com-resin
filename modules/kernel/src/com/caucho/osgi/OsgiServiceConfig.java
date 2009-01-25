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

package com.caucho.osgi;

import java.util.*;
import java.util.logging.*;

import javax.annotation.*;
import com.caucho.config.*;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.types.*;

import com.caucho.util.*;
import org.osgi.framework.*;

/**
 * osgi-service configuration
 */
public class OsgiServiceConfig extends BeanConfig
{
  private static final L10N L = new L10N(OsgiServiceConfig.class);
  private static final Logger log
    = Logger.getLogger(OsgiServiceConfig.class.getName());

  public OsgiServiceConfig()
  {
  }

  @PostConstruct
  public void init()
  {
    super.init();

    Object service = getObject();

    InjectManager webBeans = InjectManager.create();
    BundleContext bundle = webBeans.getInstanceByType(BundleContext.class);

    if (bundle == null)
      throw new ConfigException(L.l("The current environment does not have a BundleContext"));

    Class api = getClassType();
    String interfaces[] = new String[] { api.getName() };
    Dictionary properties = null;

    bundle.registerService(interfaces, service, properties);
  }
}
