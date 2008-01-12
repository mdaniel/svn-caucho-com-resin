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
 * @author Rodrigo Westrupp;
 */

package com.caucho.config.j2ee;

import com.caucho.amber.manager.EntityManagerProxy;
import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.ConfigContext;
import com.caucho.util.L10N;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PersistenceContextInjectProgram extends BuilderProgram {
  private static final Logger log
    = Logger.getLogger(PersistenceContextInjectProgram.class.getName());
  private static final L10N L = new L10N(PersistenceContextInjectProgram.class);

  private String _jndiName;
  private String _unitName;
  private PersistenceContextType _type;
  private Map _properties;
  private AccessibleInject _inject;
  private EntityManagerFactory _factory;

  PersistenceContextInjectProgram(AccessibleInject inject,
				  String jndiName,
				  String unitName,
				  PersistenceContextType type,
				  Map properties)
  {
    _inject = inject;
    _jndiName = jndiName;
    _unitName = unitName;
    _type = type;
    _properties = properties;
  }

  PersistenceContextInjectProgram(AccessibleInject inject,
				  PersistenceContext pc)
  {
    _inject = inject;
    
    _jndiName = pc.name();
    _unitName = pc.unitName();
    _type = pc.type();
    
    _properties = new HashMap();
    
    PersistenceProperty[] propertyList = pc.properties();
    for (int i = 0; propertyList != null && i < propertyList.length; i++) {
      PersistenceProperty prop = propertyList[i];

      _properties.put(prop.name(), prop.value());
    }

    System.out.println("PROP!:");
  }

  @Override
  public void configureImpl(ConfigContext builder, Object bean)
    throws ConfigException
  {
    if (_factory == null)
      _factory = createFactory();
  }

  EntityManagerFactory createFactory()
  {
    throw new ConfigException("CAN'T MAKE, DUDE");
  }

  @Override
  public Object configureImpl(ConfigContext builder, Class type)
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _jndiName + "," + _unitName + "]";
  }
}
