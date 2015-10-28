/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.admin;

import java.util.logging.Logger;

import com.caucho.server.resin.Resin;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.env.system.RootDirectorySystem;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;

/**
 * Configures the transaction manager.
 */
public class TransactionManager
{
  private static L10N L = new L10N(TransactionManager.class);
  private static Logger log
    = Logger.getLogger(TransactionManager.class.getName());

  private final Resin _resin;
  private final Path _path;

  private TransactionLog _transactionLog;

  public TransactionManager()
  {
    _resin = null;
    _path = RootDirectorySystem.getCurrentDataDirectory();
  }

  @Deprecated
  public TransactionManager(Resin resin)
  {
    _resin = resin;
    _path = RootDirectorySystem.getCurrentDataDirectory();
  }

  @Deprecated
  public TransactionManager(Path path)
  {
    _resin = null;
    _path = path;
  }

  public Path getPath()
  {
    if (_path != null)
      return _path;
    else
      return Vfs.lookup("resin-data");
  }

  /**
   * Configures the xa log.
   */
  public TransactionLog createTransactionLog()
    throws ConfigException
  {
    if (_transactionLog == null)
      _transactionLog =  new TransactionLog(this);
    
    return _transactionLog;
  }

  /**
   * Initializes the XA manager.
   */
  public void start()
    throws ConfigException
  {
    if (_transactionLog != null)
      _transactionLog.start();
  }

  public void destroy()
  {
    TransactionLog transactionLog = _transactionLog;
    _transactionLog = null;

    if (transactionLog != null)
      transactionLog.destroy();
  }
}
