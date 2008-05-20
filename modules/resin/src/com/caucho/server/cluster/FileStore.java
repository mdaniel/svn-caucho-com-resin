/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.cluster;

import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.Vfs;

import javax.annotation.PostConstruct;
import java.util.logging.*;

/**
 * Class storing distributed objects based on the filesystem.
 */
public class FileStore extends StoreManager {
  private final static Logger log
    = Logger.getLogger(FileStore.class.getName());
  
  private final FileBacking _backing = new FileBacking();

  /**
   * Create a new file-based persistent store.
   */
  public FileStore()
  {
  }

  /**
   * Sets the file store's path.
   */
  public void setPath(Path path)
  {
    _backing.setPath(path);
  }

  public void addText(String value)
  {
    _backing.setPath(Vfs.lookup(value.trim()));
  }

  public Path getPath()
  {
    return _backing.getPath();
  }

  /**
   * Initialize.
   */
  @PostConstruct
  @Override
  public boolean init()
    throws Exception
  {
    if (! super.init())
      return false;
    
    String serverId = Cluster.getServerId();
    
    String tableName = _backing.serverNameToTableName(serverId);
    
    _backing.setTableName(tableName);

    _backing.init(1);

    return true;
  }

  /**
   * Start
   */
  @Override
  public boolean start()
    throws Exception
  {
    if (! super.start())
      return false;
    
    _backing.start();

    return true;
  }

  /**
   * Clears the files which are too old.
   */
  @Override
  public void clearOldObjects()
  {
    try {
      _backing.clearOldObjects(getMaxIdleTime());
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Returns true if this server is a primary for the given object id.
   */
  @Override
  protected boolean isPrimary(String id)
  {
    return true;
  }
  
  /**
   * Creates the cluster object.
   */
  @Override
  ClusterObject create(Store store, String id)
  {
    return new ClusterObject(this, store, id);
  }

  /**
   * Loads the session from the filesystem.
   *
   * @param clusterObj the object to fill
   */
  public boolean load(ClusterObject clusterObj, Object obj)
    throws Exception
  {
    return _backing.loadSelf(clusterObj, obj);
  }

  /**
   * Saves the session to the filesystem.
   *
   * @param obj the object to save
   * @param tempStream stream to the serialized object
   * @param crc digest of the serialized stream
   * @param updateCount how many times the object has been updated
   */
  public void store(ClusterObject obj,
		    TempStream tempStream,
		    long crc)
    throws Exception
  {
    if (crc == 0)
      return;

    int length = tempStream.getLength();
    ReadStream is = tempStream.openReadAndSaveBuffer();
    try {
      _backing.storeSelf(obj.getUniqueId(), is, length,
			 obj.getExpireInterval(), 0, 0, 0);

      if (log.isLoggable(Level.FINE))
        log.fine("file store: " + obj.getUniqueId() + " length=" +
                 length);
    } finally {
      is.close();
    }
  }
  
  /**
   * Updates the object's access time in the persistent store.
   *
   * @param uniqueId the identifier of the object.
   */
  @Override
  public void accessImpl(String objectId, String uniqueId)
    throws Exception
  {
    _backing.updateAccess(uniqueId);
  }
  
  /**
   * Sets the timef for the expires interval.
   *
   * @param uniqueId the identifier of the object.
   * @param long the time in ms for the expire
   */
  @Override
  public void setExpireInterval(String uniqueId, long expires)
    throws Exception
  {
    _backing.setExpireInterval(uniqueId, expires);
  }

  /**
   * When the session is no longer valid, remove it from the backing store.
   */
  @Override
  public void remove(ClusterObject obj)
    throws Exception
  {
    removeClusterObject(obj.getStoreId(), obj.getObjectId());

    _backing.remove(obj.getUniqueId());
  }
}
