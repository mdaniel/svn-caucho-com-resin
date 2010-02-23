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
 * @author Emil Ong
 */

package com.caucho.remote.client;

import com.caucho.config.*;
import com.caucho.config.inject.AbstractBean;
import com.caucho.config.inject.BeanFactory;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.config.cfg.*;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

/**
 * Configuration class for a remote client
 */
public class RemoteClient extends BeanConfig
{
  private static final Logger log
    = Logger.getLogger(RemoteClient.class.getName());
  private static final L10N L = new L10N(RemoteClient.class);

  private Class _interface;

  /**
   * Creates a new protocol configuration object.
   */
  public RemoteClient()
  {
    setBeanConfigClass(ProtocolProxyFactory.class);
  }

  /**
   * Sets the proxy interface class.
   */
  public void setInterface(Class type)
  {
    _interface = type;

    if (! type.isInterface())
      throw new ConfigException(L.l("remote-client interface '{0}' must be an interface",
                                    type.getName()));
  }

  protected void deploy()
  {
    ProtocolProxyFactory proxyFactory = (ProtocolProxyFactory) getObject();

    Object proxy = proxyFactory.createProxy(_interface);

    InjectManager beanManager = InjectManager.create();

    BeanFactory factory = beanManager.createBeanFactory(_interface);

    if (getName() != null) {
      factory = factory.name(getName());

      addOptionalStringProperty("name", getName());
    }

    for (Annotation binding : getBindingList()) {
      factory = factory.binding(binding);
    }

    for (Annotation stereotype : getStereotypeList()) {
      factory = factory.stereotype(stereotype.annotationType());
    }

    factory.stereotype(Configured.class);

    _bean = (AbstractBean) factory.singleton(proxy);

    beanManager.addBean(_bean);
  }
}

