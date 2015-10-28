/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.jmx.server;

import com.caucho.v5.jmx.Description;
import com.caucho.v5.jmx.server.ManagedObjectMXBean;

/**
 * MBean API for the TransactionManager connection pool.
 *
 * <pre>
 * resin:type=TransactionManager,...
 * </pre>
 */
@Description("The TransactionManager")
public interface TransactionManagerMXBean extends ManagedObjectMXBean {
  
  //
  // Statistics
  //
  
  /**
   * Returns the total number of in-progress transactions.
   */
  @Description("The count of in-progress transactions")
  public int getTransactionCount();
  
  /**
   * Returns the total number of committed transaction.
   */
  @Description("The total number of committed transactions")
  public long getCommitCountTotal();
  
  /**
   * Returns the number of failed commits in the final resource commit
   */
  @Description("The total number of failed committed transactions")
  public long getCommitResourceFailCountTotal();
  
  /**
   * Returns the total number of rollbacks transaction.
   */
  @Description("The total number of rolledback transactions")
  public long getRollbackCountTotal();
}
