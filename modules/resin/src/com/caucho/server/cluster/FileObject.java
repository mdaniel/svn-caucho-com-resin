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

package com.caucho.server.cluster;

import java.io.*;
import java.net.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.log.Log;

/**
 * Data for the file object.
 */
public class FileObject extends ClusterObject {
  private static final Logger log = Log.open(FileObject.class);

  private final Path _path;

  FileObject(StoreManager storeManager, Store store, String id, Path root)
  {
    super(storeManager, store, id);

    _path = calculatePath(root,
			  store.getId(),
			  id);
    
    // clustering can't use this
    // setPrimary(! storeManager.isAlwaysLoad());
  }

  FileObject(StoreManager storeManager, String storeId, String id, Path root)
  {
    super(storeManager, storeId, id);

    _path = calculatePath(root,
			  storeId,
			  id);

    // clustering can't use this
    // setPrimary(! storeManager.isAlwaysLoad());
  }

  /**
   * Returns the path to the object in the filesystem.
   */
  Path getPath()
  {
    return _path;
  }

  public ReadStream openRead()
    throws IOException
  {
    return _path.openRead();
  }

  WriteStream openWrite()
    throws IOException
  {
    return _path.openWrite();
  }

  public void removeImpl()
  {
    try {
      _path.remove();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  Path calculatePath(Path top, String managerId, String key)
  {
    return calculatePath(top, getServerId(), managerId, key);
  }

  static Path calculatePath(Path top, String serverId,
			    String managerId, String key)
  {
    long crc = Crc64.generate(key);
    CharBuffer cb = CharBuffer.allocate();

    addDigit(cb, crc);
    addDigit(cb, crc >> 60);
    addDigit(cb, crc >> 56);

    String hash = cb.close();

    managerId = serverId + '-' + managerId;

    Path parent = top.lookup(managerId).lookup(hash);

    try {
      parent.mkdirs();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return parent.lookup(key);
  }

  static private void addDigit(CharBuffer cb, long digit)
  {
    digit = digit & 0xf;

    if (digit <= 9)
      cb.append((char) ('0' + digit));
    else
      cb.append((char) ('a' + digit - 10));
  }

  protected String getServerId()
  {
    return Cluster.getServerId();
  }

  public String toString()
  {
    return "FileObject[" + _path + "]";
  }
}
