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
 * @author Rodrigo Westrupp
 */

package com.caucho.quercus.lib.postgres;

import java.util.logging.Logger;
import com.caucho.util.Log;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.mysql.Mysqli;

/**
 * Postgres Thread.
 */
public class PostgresThread extends Thread {

  private static final Logger log = Log.open(PostgresThread.class);

  private Env tenv = null;
  private Mysqli tconn = null;
  private String tquery = null;
  private Value tvalue = null;
  private PostgresModule tmodule = null;
  private boolean tstarted = false;
  private boolean tfinished = false;
  private boolean tcancelled = false;

  public void setModule(PostgresModule module) {
    tmodule = module;
  }

  public void setQuery(Env env, Mysqli conn, String query) {
    tenv = env;
    tconn = conn;
    tquery = query;
  }

  public Value getValue() {
    return tvalue;
  }

  public synchronized void runQuery()
  {
    if (!tcancelled) {
      tstarted = true;
      tvalue = tmodule.pg_query(tenv, tconn, tquery);
      tfinished = true;
	}
  }

  public synchronized boolean cancelQuery()
  {
    if (!tstarted) {
      tcancelled = true;
	}

    return tcancelled;
  }

  public void run() {
    runQuery();
  }
}
