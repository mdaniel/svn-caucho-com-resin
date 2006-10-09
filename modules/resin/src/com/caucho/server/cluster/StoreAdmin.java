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
 * @author Sam
 */


package com.caucho.server.cluster;

import javax.management.ObjectName;

import com.caucho.management.server.*;

public class StoreAdmin extends AbstractManagedObject
  implements PersistentStoreMXBean
{
  private final StoreManager _manager;

  protected StoreAdmin(StoreManager manager)
  {
    _manager = manager;
  }
  
  public String getName()
  {
    return null;
  }

  public String getType()
  {
    return "PersistentStore";
  }

  //
  // configuration
  //
  
  public String getStoreType()
  {
    return "none";
  }

  //
  // statistics
  //

  /**
   * Returns the object count.
   */
  public long getObjectCount()
  {
    return _manager.getObjectCount();
  }
  
  /**
   * Returns the total objects loaded.
   */
  public long getLoadCountTotal()
  {
    return _manager.getLoadCount();
  }

  /**
   * Returns the objects which failed to load.
   */
  public long getLoadFailCountTotal()
  {
    return _manager.getLoadFailCount();
  }

  /**
   * Returns the total objects saved.
   */
  public long getSaveCountTotal()
  {
    return _manager.getSaveCount();
  }

  /**
   * Returns the objects which failed to save.
   */
  public long getSaveFailCountTotal()
  {
    return _manager.getSaveFailCount();
  }

  public String toString()
  {
    return "StoreAdmin[" + getObjectName() + "]";
  }
}
