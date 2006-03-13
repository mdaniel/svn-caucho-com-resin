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
import java.util.logging.Level;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.LockModeType;
import javax.persistence.FlushModeType;

import com.caucho.amber.AmberQuery;
import com.caucho.amber.AmberException;
import com.caucho.amber.AmberRuntimeException;

import com.caucho.amber.entity.Entity;
import com.caucho.amber.entity.AmberEntityHome;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.entity.EntityFactory;

import com.caucho.amber.query.UserQuery;
import com.caucho.amber.query.AbstractQuery;
import com.caucho.amber.query.QueryParser;
import com.caucho.amber.query.QueryCacheKey;
import com.caucho.amber.query.ResultSetCacheChunk;

import com.caucho.amber.entity.AmberCompletion;
import com.caucho.amber.entity.TableInvalidateCompletion;
import com.caucho.amber.entity.RowInvalidateCompletion;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.table.Table;

import com.caucho.amber.collection.AmberCollection;

import com.caucho.amber.query.AbstractQuery;

import com.caucho.ejb.EJBExceptionWrapper;

import com.caucho.jca.UserTransactionProxy;
import com.caucho.jca.CloseResource;

import com.caucho.util.L10N;

/**
 * The entity manager from a entity manager proxy.
 */
public class AmberEntityManager extends AmberConnection
  implements EntityManager {
  private static final L10N L = new L10N(AmberEntityManager.class);
  private static final Logger log
    = Logger.getLogger(AmberEntityManager.class.getName());

  /**
   * Creates a manager instance.
   */
  AmberEntityManager(AmberPersistenceUnit persistenceUnit)
  {
    super(persistenceUnit);

    register(); // initThreadConnection(); // ejb/0q00
  }

  public String toString()
  {
    AmberPersistenceUnit persistenceUnit = getPersistenceUnit();
    
    if (persistenceUnit != null)
      return "AmberEntityManager[" + persistenceUnit.getName() + "]";
    else
      return "AmberEntityManager[closed]";
  }
}
