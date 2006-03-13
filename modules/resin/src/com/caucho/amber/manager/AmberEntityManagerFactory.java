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

package com.caucho.amber.manager;

import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;

/**
 * Amber's EntityManagerFactory container.
 */
public class AmberEntityManagerFactory implements EntityManagerFactory {
  private static final Logger log
    = Logger.getLogger(AmberEntityManagerFactory.class.getName());

  private AmberPersistenceUnit _unit;
  private boolean _isOpen = true;

  AmberEntityManagerFactory(AmberPersistenceUnit unit)
  {
    _unit = unit;
  }
  
  /**
   * Create a new EntityManager with TRANSACTION type.
   */
  public EntityManager createEntityManager()
  {
    return createEntityManager(PersistenceContextType.TRANSACTION);
  }
  
  /**
   * Create a new EntityManager with the given persistence type.
   */
  public EntityManager createEntityManager(PersistenceContextType type)
  {
    return new AmberEntityManager(_unit);
  }

  /**
   * Returns an entity manager related to JTA.
   */
  public EntityManager getEntityManager()
  {
    return new EntityManagerProxy(_unit);
  }

  /**
   * Close the factory an any resources.
   */
  public void close()
  {
    _isOpen = false;
  }

  /**
   * Returns true if the factory is open.
   */
  public boolean isOpen()
  {
    return _isOpen;
  }

  public String toString()
  {
    return "AmberEntityManagerFactory[" + _unit.getName() + "]";
  }
}


