/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.cfg;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

/**
 * Proxy for an ejb bean configuration.  This proxy is needed to handle
 * the merging of ejb definitions.
 */
public class EjbEntityConfigProxy extends EjbBeanConfigProxy {
  private static final L10N L = new L10N(EjbBeanConfigProxy.class);

  private EjbEntityBean _entity;

  /**
   * Creates a new entity bean configuration.
   */
  public EjbEntityConfigProxy(EjbConfig config, String ejbModuleName)
  {
    super(config, ejbModuleName);
  }

  /**
   * Initializes and configures the entity bean.
   */
  public void init()
    throws Throwable
  {
    EjbBean oldBean = getConfig().getBeanConfig(getEJBName());

    if (oldBean == null) {
      _entity = new EjbEntityBean(getConfig(), getEJBModuleName());
      _entity.setEJBName(getEJBName());
      _entity.setLocation(getLocation());
      _entity.setAllowPOJO(getConfig().isAllowPOJO());
    }
    else if (! (oldBean instanceof EjbEntityBean)) {
      throw new ConfigException(L.l("entity bean `{0}' conflicts with prior {1} bean at {2}.",
				    getEJBName(), oldBean.getEJBKind(),
				    oldBean.getLocation()));
    }
    else {
      _entity = (EjbEntityBean) oldBean;
    }
    

    _entity.addDependencyList(getDependencyList());

    getBuilderProgram().configure(_entity);
  }

  /**
   * Returns the entity config.
   */
  public EjbEntityBean getEntity()
  {
    return _entity;
  }
}
