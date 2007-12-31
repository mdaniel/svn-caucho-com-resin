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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.dispatch;

import com.caucho.config.BuilderProgram;
import com.caucho.config.BuilderProgramContainer;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
//import com.caucho.soa.rest.RestProtocolServlet;
//import com.caucho.soa.rest.JAXBRestProtocolServlet;
//import com.caucho.soa.rest.HessianRestProtocolServlet;
//import com.caucho.soa.servlet.HessianProtocolServlet;
//import com.caucho.soa.servlet.ProtocolServlet;
//import com.caucho.soa.servlet.SoapProtocolServlet;
import com.caucho.remote.server.ProtocolServletFactory;
import com.caucho.util.*;
import com.caucho.webbeans.component.*;
import com.caucho.webbeans.manager.*;

import java.util.*;
import javax.annotation.PostConstruct;

/**
 * Configuration for a servlet web-service protocol.
 */
public class ServletProtocolConfig {
  private static L10N L = new L10N(ServletProtocolConfig.class);

  private String _type;

  private Class _factoryClass;

  private BuilderProgramContainer _program
    = new BuilderProgramContainer();

  /**
   * Creates a new protocol configuration object.
   */
  public ServletProtocolConfig()
  {
  }

  public void setType(String type)
  {
    try {
      _type = type;

      String name = ProtocolServletFactory.class.getName() + "/" + type;
    
      ArrayList<String> drivers = Services.getServices(name);

      if (drivers.size() == 0)
	throw new ConfigException(L.l("'{0}' is an unknown servlet protocol.",
				      type));

      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      _factoryClass = Class.forName(drivers.get(0), false, loader);

      Config.validate(_factoryClass, ProtocolServletFactory.class);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public String getType()
  {
    return _type;
  }

  public void addBuilderProgram(BuilderProgram program)
  {
    _program.addProgram(program);
  }

  public BuilderProgram getProgram()
  {
    return _program;
  }

  @PostConstruct
  public void init()
  {
    if (_type == null)
      throw new ConfigException(L.l("'type' is a required attribute of <protocol>")); 
  }
  
  public ProtocolServletFactory createFactory()
  {
    WebBeansContainer webBeans = WebBeansContainer.create();
    
    ComponentImpl comp
      = (ComponentImpl) webBeans.createTransient(_factoryClass);

    Object factory = comp.createNoInit();

    _program.configure(factory);

    return (ProtocolServletFactory) factory;
  }
}
