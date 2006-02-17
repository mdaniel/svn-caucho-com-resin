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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.jca;

import java.util.ArrayList;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.transaction.UserTransaction;
import javax.transaction.*;
import javax.transaction.xa.*;

import javax.security.auth.Subject;

import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ConnectionRequestInfo;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.transaction.TransactionImpl;

/**
 * Saved state for a suspend.
 */
public class UserTransactionSuspendState {
  private static final Logger log = Log.open(UserTransactionSuspendState.class);
  private static final L10N L = new L10N(UserTransactionSuspendState.class);

  private ArrayList<PoolItem> _poolItems = new ArrayList<PoolItem>();
  
  /**
   * Creates the suspend state.
   */
  public UserTransactionSuspendState(ArrayList<PoolItem> poolItems)
  {
    _poolItems.addAll(poolItems);
  }

  /**
   * Returns the pooled items.
   */
  public ArrayList<PoolItem> getPoolItems()
  {
    return _poolItems;
  }
}

