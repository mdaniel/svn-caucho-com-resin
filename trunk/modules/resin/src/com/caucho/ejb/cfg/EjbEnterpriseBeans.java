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
 * Configuration for an ejb bean.
 */
public class EjbEnterpriseBeans {
  private static final L10N L = new L10N(EjbEnterpriseBeans.class);

  private final EjbConfig _config;
  private final String _ejbModuleName;

  public EjbEnterpriseBeans(EjbConfig config, String ejbModuleName)
  {
    _config = config;
    _ejbModuleName = ejbModuleName;
  }

  public EjbSessionConfigProxy createSession()
  {
    return new EjbSessionConfigProxy(_config, _ejbModuleName);
  }
  
  public void addSession(EjbSessionConfigProxy sessionProxy)
    throws ConfigException
  {
    EjbSessionBean session = sessionProxy.getSession();

    _config.setBeanConfig(session.getEJBName(), session);
  }

  public EjbBeanConfigProxy createEjbBean()
  {
    return new EjbBeanConfigProxy(_config, _ejbModuleName);
  }
  
  public void addEjbBean(EjbBeanConfigProxy beanProxy)
    throws ConfigException
  {
    EjbBean bean = beanProxy.getBean();

    if (bean != null)
      _config.setBeanConfig(bean.getEJBName(), bean);
  }

  public EjbMessageConfigProxy createMessageDriven()
  {
    return new EjbMessageConfigProxy(_config, _ejbModuleName);
  }

  public void addMessageDriven(EjbMessageConfigProxy messageProxy)
    throws ConfigException
  {
    EjbMessageBean message = messageProxy.getMessage();
    
    _config.setBeanConfig(message.getEJBName(), message);
  }
}
