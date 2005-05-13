/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.metadata;

import java.lang.reflect.Method;

import java.util.ArrayList;

import java.util.logging.Logger;

import javax.sql.DataSource;

import javax.transaction.UserTransaction;

import javax.ejb.Stateless;
import javax.ejb.Stateful;
import javax.ejb.Entity;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.MethodPermissions;
import javax.ejb.TransactionAttribute;
import javax.ejb.Inject;

import com.caucho.amber.gen.AmberEnhancer;

import com.caucho.config.ConfigException;

import com.caucho.config.types.InitProgram;

import com.caucho.ejb.EjbServerManager;

import com.caucho.ejb.cfg.EjbConfig;
import com.caucho.ejb.cfg.EjbBean;
import com.caucho.ejb.cfg.EjbSessionBean;
import com.caucho.ejb.cfg.MethodSignature;
import com.caucho.ejb.cfg.EjbMethod;
import com.caucho.ejb.cfg.EjbMethodPattern;

import com.caucho.ejb.entity2.EntityIntrospector;

import com.caucho.util.L10N;
import com.caucho.util.Log;

/**
 * Configuration for a new bean based on metadata.
 */
public class EJBEnhancer extends AmberEnhancer {
  private static final L10N L = new L10N(EJBEnhancer.class);
  private static final Logger log = Log.open(EJBEnhancer.class);

  private EjbServerManager _ejbManager;
  private EntityIntrospector _introspector;

  public EJBEnhancer(EjbServerManager ejbManager,
		     EntityIntrospector introspector)
  {
    super(ejbManager.getAmberManager().getEnvManager());
    
    _ejbManager = ejbManager;
    _introspector = introspector;
  }
}

