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

package com.caucho.server.cluster;

import com.caucho.log.Log;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The ObjectBacking encapsulates the file persistent store
 * for a distributed object.
 */
public class ObjectBacking {
  static protected final Logger log = Log.open(ObjectBacking.class);

  // The backing path
  private Path _objectPath;

  // The owning persistent store
  private Store _store;
  
  // The object id
  private String _id;
  
  // The corresponding object (when live)
  private DistributedObject _object;

  /**
   * Initialize the object backing.
   *
   * @param id the object identifier
   * @param objectPath the backing object path.
   * @param store the owning distributed store
   */
  public ObjectBacking(String id, Path objectPath, Store store)
  {
    _id = id;
    _store = store;
    _objectPath = objectPath;
  }

  /**
   * Returns the mangled id.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Returns the backing path.
   */
  public Path getPath()
  {
    return _objectPath;
  }

  /**
   * Returns any associated object.
   */
  public DistributedObject getObject()
  {
    return _object;
  }

  /**
   * Sets an associated object.
   */
  public void setObject(DistributedObject object)
  {
    _object = object;
  }

  /**
   * Opens the backing for reading.
   */
  public ReadStream openRead()
    throws IOException
  {
    return _objectPath.openRead();
  }

  /**
   * Opens the backing for writing.
   */
  public WriteStream openWrite()
    throws IOException
  {
    return _objectPath.openWrite();
  }

  /**
   * Saves a distributed object in the backing file.
   */
  synchronized void save(DistributedObject object)
  {
    WriteStream os = null;
    try {
      os = _objectPath.openWrite();

      // _store.store(object, os);

      _object = object;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
        if (os != null)
          os.close();
      } catch (IOException e1) {
      }
    }
  }

  /**
   * Removes the object from the backing.
   */
  synchronized public void remove()
  {
    try {
      _objectPath.remove();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  /**
   * Returns true if this backing allows logging.
   */
  public boolean canLog()
  {
    return log.isLoggable(Level.FINE);
  }

  /**
   * Log data to the backing's log file.
   */
  public void log(String value)
  {
    log.fine(value);
  }

  public String toString()
  {
    return "DistBacking[" + _objectPath + "]";
  }
}
