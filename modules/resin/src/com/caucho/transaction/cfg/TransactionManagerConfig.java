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

package com.caucho.transaction.cfg;

import com.caucho.config.ConfigException;
import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.transaction.xalog.AbstractXALogManager;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configures the transaction manager.
 */
public class TransactionManagerConfig {
  private static L10N L = new L10N(TransactionManagerConfig.class);
  private static Logger log = Log.open(TransactionManagerConfig.class);

  private AbstractXALogManager _xaLog;

  /**
   * Configures the xa log.
   */
  public AbstractXALogManager createTransactionLog()
    throws ConfigException
  {
    try {
      Class cl = Class.forName("com.caucho.transaction.xalog.XALogManager");
      
      _xaLog = (AbstractXALogManager) cl.newInstance();
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (_xaLog == null)
      throw new ConfigException(L.l("<transaction-log> requires Resin Professional.  See http://www.caucho.com for information and licensing."));

    return _xaLog;
  }

  /**
   * Initializes the XA manager.
   */
  public void start()
    throws ConfigException
  {
    try {
      TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

      tm.setXALogManager(_xaLog);

      _xaLog.start();
    } catch (IOException e) {
      throw new ConfigException(e);
    }
  }
}
