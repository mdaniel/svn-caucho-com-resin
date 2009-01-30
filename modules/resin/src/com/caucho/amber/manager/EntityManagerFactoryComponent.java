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

package com.caucho.amber.manager;

import javax.persistence.*;
import javax.persistence.spi.*;
import javax.context.ApplicationScoped;
import javax.context.CreationalContext;

import com.caucho.amber.cfg.*;
import com.caucho.config.inject.*;

/**
 * The Entity manager webbeans component
 */
public class EntityManagerFactoryComponent extends FactoryComponent {
  private final AmberContainer _amber;
  private final PersistenceProvider _provider;
  private final PersistenceUnitConfig _unit;
  private final String _unitName;
  private EntityManagerFactory _factory;

  public EntityManagerFactoryComponent(AmberContainer amber,
                                       PersistenceProvider provider,
				       PersistenceUnitConfig unit)
  {
    super(EntityManagerFactory.class, unit.getName());
    setScopeType(ApplicationScoped.class);

    _amber = amber;
    _provider = provider;
    _unit = unit;
    _unitName = unit.getName();
  }

  @Override
  public Object create()
  {
    // jpa/2110
    if (_factory == null)
      _factory = _amber.getEntityManagerFactory(_unit.getName());
    
    return _factory;
  }

  @Override
  protected Object createNew(CreationalContext context)
  {
    if (_factory == null)
      _factory = _amber.getEntityManagerFactory(_unit.getName());

    return _factory;
  }
}
