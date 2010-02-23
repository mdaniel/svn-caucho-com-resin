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

package com.caucho.db.debug;

import com.caucho.db.Database;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.util.logging.Logger;

/**
 * Manager for a basic Java-based database.
 */
public class DebugQuery {
  private static final Logger log = Log.open(DebugStore.class);
  private static final L10N L = new L10N(DebugStore.class);

  Database _db;
  
  public DebugQuery(Path path)
    throws Exception
  {
    _db = new Database();
    _db.setPath(path);
    _db.init();
  }

  public static void main(String []args)
    throws Exception
  {
    if (args.length != 2) {
      System.out.println("usage: DebugQuery db-directory query");
      return;
    }

    Path path = Vfs.lookup(args[0]);

    WriteStream out = Vfs.openWrite(System.out);

    DebugQuery query = new DebugQuery(path);

    out.close();
  }
}
