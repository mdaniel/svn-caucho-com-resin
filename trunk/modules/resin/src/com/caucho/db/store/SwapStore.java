/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.db.store;

import com.caucho.java.WorkDir;
import com.caucho.server.util.CauchoSystem;
import com.caucho.vfs.Path;

/**
 * The swap store is a singleton Store to manage buffered data that might
 * need to swap to a backing store.
 */
public final class SwapStore {
  private static SwapStore _swap;

  private final Store _store;

  private SwapStore()
  {
    ClassLoader loader = ClassLoader.getSystemClassLoader();
    
    Path workDir = WorkDir.getLocalWorkDir(loader);

    String serverId = CauchoSystem.getServerId();

    Path path = workDir.lookup("tmp-" + serverId + ".swap");

    try {
      _store = Store.create(path);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Returns the swap store.
   */
  public static SwapStore create()
  {
    synchronized (SwapStore.class) {
      if (_swap == null)
	_swap = new SwapStore();

      return _swap;
    }
  }
}
