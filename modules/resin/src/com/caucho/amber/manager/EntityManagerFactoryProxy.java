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

import com.caucho.amber.*;
import com.caucho.config.inject.HandleAware;
import com.caucho.jca.*;
import com.caucho.util.L10N;

import javax.persistence.*;
import javax.transaction.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * The @PersistenceUnit, container managed entity manager proxy, used
 * for third-party providers.
 */
public class EntityManagerFactoryProxy
  implements EntityManagerFactory
{
  private static final L10N L = new L10N(EntityManagerFactoryProxy.class);
  private static final Logger log
    = Logger.getLogger(EntityManagerFactoryProxy.class.getName());

  private final AmberContainer _amber;
  private final String _unitName;
  private EntityManagerFactory _emf;

  public EntityManagerFactoryProxy(AmberContainer amber,
                                   String unitName)
  {
    _amber = amber;
    _unitName = unitName;
  }

  /**
   * Create a new EntityManager with TRANSACTION type.
   */
  public EntityManager createEntityManager()
  {
    return getFactory().createEntityManager();
  }

  /**
   * Create a new EntityManager with the given properties.
   */
  public EntityManager createEntityManager(Map map)
  {
    return getFactory().createEntityManager(map);
  }

  /**
   * Close the factory an any resources.
   */
  public void close()
  {
  }

  /**
   * Returns true if the factory is open.
   */
  public boolean isOpen()
  {
    return getFactory().isOpen();
  }

  /**
   * Returns the properties and values for the factory
   *
   * @since JPA 2.0
   */
  public Map getProperties()
  {
    return getFactory().getProperties();
  }

  /**
   * Returns the supported properties
   *
   * @since JPA 2.0
   */
  public Set<String> getSupportedProperties()
  {
    return getFactory().getSupportedProperties();
  }

  /**
   * Returns the entity manager cache
   *
   * @since JPA 2.0
   */
  public Cache getCache()
  {
    return getFactory().getCache();
  }

  private EntityManagerFactory getFactory()
  {
    if (_emf == null)
      _emf = _amber.getEntityManagerFactory(_unitName);

    return _emf;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _unitName + "," + _emf + "]";
  }
}
