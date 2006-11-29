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

package com.caucho.amber.entity;

/**
 * An entity listener instance.
 */
public interface Listener {
  public static final int PRE_PERSIST  = 0x01;
  public static final int POST_PERSIST = 0x02;
  public static final int PRE_REMOVE   = 0x04;
  public static final int POST_REMOVE  = 0x08;
  public static final int PRE_UPDATE   = 0x10;
  public static final int POST_UPDATE  = 0x20;
  public static final int POST_LOAD    = 0x40;

  /**
   * Called before EntityManager.persist().
   */
  public void __caucho_prePersist(Object entity);

  /**
   * Called after the entity has been made persistent.
   */
  public void __caucho_postPersist(Object entity);

  /**
   * Called before EntityManager.remove().
   */
  public void __caucho_preRemove(Object entity);

  /**
   * Called after the entity has been made removed.
   */
  public void __caucho_postRemove(Object entity);

  /**
   * Called before database update operations.
   */
  public void __caucho_preUpdate(Object entity);

  /**
   * Called after database update operations.
   */
  public void __caucho_postUpdate(Object entity);

  /**
   * Called after an entity has been loaded into
   * the current persistence context.
   */
  public void __caucho_postLoad(Object entity);
}
