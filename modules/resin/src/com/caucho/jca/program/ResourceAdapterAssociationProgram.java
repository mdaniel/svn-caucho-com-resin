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
 * @author Scott Ferguson;
 */

package com.caucho.jca.program;

import com.caucho.config.*;
import com.caucho.config.manager.InjectManager;
import com.caucho.config.program.*;
import com.caucho.jca.*;
import com.caucho.util.*;
import com.caucho.webbeans.*;
import com.caucho.webbeans.manager.*;

import javax.webbeans.*;
import javax.resource.spi.*;

/**
 * Program to associate a resource adapter
 */
public class ResourceAdapterAssociationProgram extends ConfigProgram {
  private static final L10N L
    = new L10N(ResourceAdapterAssociationProgram.class);

  private final ResourceAdapterController _raController;
  
  /**
   * Creates a resource adapter program based on the class.
   */
  public ResourceAdapterAssociationProgram(Class cl)
  {
    ResourceArchive ra
      = ResourceArchiveManager.findResourceArchive(cl.getName());

    if (ra == null) {
      throw new ConfigException(L.l("'{0}' does not have a defined resource-adapter.  Check the rar or META-INF/resin-ra.xml files",
				    cl.getName()));
    }

    InjectManager webBeans = InjectManager.create();
    
    _raController
      = webBeans.getInstanceByType(ResourceAdapterController.class,
				   Names.create(ra.getResourceAdapterClass().getName()));
    
    if (_raController == null) {
      throw new ConfigException(L.l("'{0}' does not have a configured resource-adapter for '{1}'.",
				    ra.getResourceAdapterClass().getName(),
				    cl.getName()));
    }

  }
  
  /**
   * Configures the bean using the current program.
   * 
   * @param bean the bean to configure
   * @param env the Config environment
   */
  public void inject(Object bean, ConfigContext env)
  {
    try {
      ResourceAdapterAssociation association
	= (ResourceAdapterAssociation) bean;

      association.setResourceAdapter(_raController.getResourceAdapter());
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
