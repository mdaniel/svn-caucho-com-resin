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

package com.caucho.server.port;

import java.util.logging.*;
import javax.annotation.*;

import com.caucho.config.*;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.program.*;
import com.caucho.server.cluster.*;
import com.caucho.server.port.*;
import com.caucho.util.*;

/**
 * Represents a protocol connection.
 */
public class ProtocolPort extends Port
{
  private static final L10N L = new L10N(ProtocolPort.class);

  private static final Logger log
    = Logger.getLogger(ProtocolPort.class.getName());
  
  // The protocol
  private Class _protocolClass;
  private ContainerProgram _init;

  public ProtocolPort()
  {
  }

  public ProtocolPort(ClusterServer server)
  {
    super(server);
  }

  /**
   * Sets protocol class.
   */
  public void setType(Class cl)
  {
    setClass(cl);
  }

  /**
   * Sets protocol class.
   */
  public void setClass(Class cl)
  {
    Config.validate(cl, Protocol.class);

    _protocolClass = cl;
  }

  public void setInit(ContainerProgram init)
  {
    if (_protocolClass == null)
      throw new ConfigException(L.l("<init> requires a protocol class"));

    _init = init;
  }

  /**
   * Initializes the port.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_protocolClass != null) {
      InjectManager webBeans = InjectManager.create();

      Protocol protocol
	= (Protocol) webBeans.createTransientObjectNoInit(_protocolClass);
      
      if (_init != null)
	_init.configure(protocol);

      Config.init(protocol);

      setProtocol(protocol);
    }
    else
      throw new ConfigException(L.l("protocol requires either a class"));
  }
}
