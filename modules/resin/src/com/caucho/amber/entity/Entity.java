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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.amber.entity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.caucho.amber.type.EntityType;
import com.caucho.amber.connection.AmberConnectionImpl;

/**
 * An entity instance
 */
public interface Entity {
  public static final int TRANSIENT = 0;
  public static final int P_NEW = 1;
  public static final int P_NON_TRANSACTIONAL = 2;
  public static final int P_TRANSACTIONAL = 3;
  public static final int P_DELETING = 4;
  public static final int P_DELETED = 5;

  /**
   * Makes the entity persistent.
   */
  public boolean __caucho_makePersistent(AmberConnectionImpl aConn,
					 EntityType entityType)
    throws SQLException;

  /**
   * Makes the entity persistent.
   */
  public void __caucho_makePersistent(AmberConnectionImpl aConn,
				      EntityItem item)
    throws SQLException;

  /**
   * Creates the entity in the database, making it persistent-new.
   */
  public boolean __caucho_create(AmberConnectionImpl aConn,
				 EntityType entityType)
    throws SQLException;
  
  /**
   * Set the primary key.
   */
  public void __caucho_setPrimaryKey(Object key);
  
  /**
   * Get the primary key.
   */
  public Object __caucho_getPrimaryKey();
  
  /**
   * Get the entity type.
   */
  public EntityType __caucho_getEntityType();
  
  /**
   * Sets the connection.
   */
  public void __caucho_setConnection(AmberConnectionImpl aConn);

  /**
   * Returns true if the entity matches.
   */
  public boolean __caucho_match(String className, Object key);

  /**
   * Loads the entity from the database.
   */
  public EntityItem __caucho_home_find(AmberConnectionImpl aConn,
				       AmberEntityHome home,
				       ResultSet rs, int index)
    throws SQLException;

  /**
   * Returns a new entity.
   */
  public Entity __caucho_home_new(AmberConnectionImpl aConn,
				  AmberEntityHome home,
				  Object key)
    throws SQLException;

  /**
   * Creates a new instance based the current entity
   */
  public Entity __caucho_copy(AmberConnectionImpl aConn, EntityItem cacheItem);

  /**
   * Retrieves data from the data store.
   */
  public void __caucho_retrieve(AmberConnectionImpl aConn)
    throws SQLException;

  /**
   * Loads the entity from the database.
   */
  public void __caucho_load(AmberConnectionImpl aConn, ResultSet rs, int index)
    throws SQLException;

  /**
   * Loads the entity from the database.
   */
  public void __caucho_setKey(PreparedStatement pstmt, int index)
    throws SQLException;

  /**
   * Expires data
   */
  public void __caucho_expire();

  /**
   * Deletes the entity from the database.
   */
  public void __caucho_delete();

  /**
   * Called when a foreign object is created/deleted.
   */
  public void __caucho_invalidate_foreign(String table, Object key);

  /**
   * Flushes changes to the backing store.
   */
  public boolean __caucho_flush()
    throws SQLException;

  /**
   * After a commit.
   */
  public void __caucho_afterCommit();

  /**
   * After a rollback.
   */
  public void __caucho_afterRollback();

  /**
   * Loads the values from the object.
   */
  // public void __caucho_loadFromObject(Object src);
}
