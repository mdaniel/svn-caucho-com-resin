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

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

/**
 * Configuration for an ejb bean.
 */
public class EjbEnterpriseBeans {
  private static final L10N L = new L10N(EjbEnterpriseBeans.class);

  private EjbConfig _config;

  public EjbEnterpriseBeans(EjbConfig config)
  {
    _config = config;
  }

  public EjbSessionConfigProxy createSession()
  {
    return new EjbSessionConfigProxy(_config);
  }
  
  public void addSession(EjbSessionConfigProxy sessionProxy)
    throws ConfigException
  {
    EjbSessionBean session = sessionProxy.getSession();

    _config.setBeanConfig(session.getEJBName(), session);
  }

  public EjbEntityConfigProxy createEntity()
  {
    return new EjbEntityConfigProxy(_config);
  }
  
  public void addEntity(EjbEntityConfigProxy entityProxy)
    throws ConfigException
  {
    EjbEntityBean entity = entityProxy.getEntity();

    _config.setBeanConfig(entity.getEJBName(), entity);
  }

  public EjbBeanConfigProxy createEjbBean()
  {
    return new EjbBeanConfigProxy(_config);
  }
  
  public void addEjbBean(EjbBeanConfigProxy beanProxy)
    throws ConfigException
  {
    EjbBean bean = beanProxy.getBean();

    _config.setBeanConfig(bean.getEJBName(), bean);
  }

  public EjbMessageConfigProxy createMessageDriven()
  {
    return new EjbMessageConfigProxy(_config);
  }

  public void addMessageDriven(EjbMessageConfigProxy messageProxy)
    throws ConfigException
  {
    EjbMessageBean message = messageProxy.getMessage();
    
    _config.setBeanConfig(message.getEJBName(), message);
  }
}
