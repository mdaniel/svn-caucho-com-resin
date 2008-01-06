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

package com.caucho.ejb.cfg;

import com.caucho.config.BuilderProgram;
import com.caucho.config.BuilderProgramContainer;
import com.caucho.config.ConfigException;
import com.caucho.config.LineConfigException;
import com.caucho.config.j2ee.EjbInjectProgram;
import com.caucho.config.j2ee.InjectIntrospector;
import com.caucho.config.j2ee.JndiBindProgram;
import com.caucho.config.types.EjbLocalRef;
import com.caucho.config.types.EjbRef;
import com.caucho.config.types.EnvEntry;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.gen.BeanAssembler;
import com.caucho.ejb.gen.SessionAssembler;
import com.caucho.ejb.gen.StatelessAssembler;
import com.caucho.ejb.session.SessionServer;
import com.caucho.ejb.session.StatelessServer;
import com.caucho.java.gen.JavaClassGenerator;
import com.caucho.management.j2ee.J2EEManagedObject;
import com.caucho.management.j2ee.StatefulSessionBean;
import com.caucho.management.j2ee.StatelessSessionBean;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.ejb.*;
import java.util.ArrayList;
import java.lang.reflect.*;

/**
 * Configuration for an ejb stateless session bean.
 */
public class EjbStatelessBean extends EjbSessionBean {
  private static final L10N L = new L10N(EjbStatelessBean.class);

  /**
   * Creates a new session bean configuration.
   */
  public EjbStatelessBean(EjbConfig ejbConfig, String ejbModuleName)
  {
    super(ejbConfig, ejbModuleName);

    setSessionType("Stateless");
  }

  /**
   * Returns the kind of bean.
   */
  public String getEJBKind()
  {
    return "stateless";
  }
}
