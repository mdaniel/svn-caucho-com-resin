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

package com.caucho.amber.entity;

import java.util.logging.Logger;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import com.caucho.amber.type.EntityType;

import com.caucho.amber.query.ResultSetCacheChunk;

/**
 * Code to update the cache value on the completion of a transaction.
 */
public class AmberCompletion {
  private static final L10N L = new L10N(AmberCompletion.class);
  private static final Logger log = Log.open(AmberCompletion.class);

  /**
   * Code when the transaction completes.
   *
   * @return true if the entry should be deleted.
   */
  public boolean complete(EntityType rootType,
			  Object key,
			  EntityItem entityItem)
  {
    return false;
  }

  /**
   * Code to update caches when a transaction completes.
   *
   * @return true if the entry should be deleted.
   */
  public boolean complete(ResultSetCacheChunk cacheChunk)
  {
    return false;
  }
}
