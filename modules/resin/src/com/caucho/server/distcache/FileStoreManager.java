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

package com.caucho.server.distcache;

import com.caucho.config.*;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.Server;
import com.caucho.server.resin.Resin;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import java.io.InputStream;
import javax.annotation.PostConstruct;
import java.util.logging.*;

/**
 * Class storing distributed objects based on the filesystem.
 */
public class FileStoreManager extends StoreManager {
  private static final L10N L = new L10N(FileStoreManager.class);
  private final static Logger log
    = Logger.getLogger(FileStoreManager.class.getName());
  
  private final FileBacking _backing = new FileBacking();

  /**
   * Create a new file-based persistent objectStore.
   */
  public FileStoreManager()
  {
  }

  /**
   * Sets the file objectStore's path.
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
  {
    if (! super.init())
      return false;

    Cluster cluster = Server.getCurrent().getCluster();

    if (cluster.getTriadList()[0].getServerList().length != 1)
      throw new ConfigException(L.l("file-store can only be used in single-server configurations."));

    try {
      String serverId = Server.getCurrent().getServerId();
    
      String tableName = _backing.serverNameToTableName(serverId);
    
      _backing.setTableName(tableName);

      _backing.init(1);

      return true;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
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
   * Loads the session from the filesystem.
   *
   * @param clusterObj the object to fill
   */
  public boolean load(ClusterObject clusterObj, Object obj)
    throws Exception
  {
    return _backing.load(clusterObj, obj);
  }

  /**
   * Saves the session to the filesystem.
   *
   * @param obj the object to save
   * @param tempStream stream to the serialized object
   * @param dataHash sha-1 hash of the datam
   * @param updateCount how many times the object has been updated
   */
  public void store(ClusterObject obj,
		    TempOutputStream tempStream,
		    byte []dataHash, byte []oldDataHash)
    throws Exception
  {
    if (dataHash == null)
      return;

    int length = tempStream.getLength();
    InputStream is = tempStream.openInputStreamNoFree();
    try {
      _backing.store(obj.getObjectId(), obj.getStoreId(),
		     is, length, dataHash, oldDataHash,
		     obj.getExpireInterval(), 0, 0, 0);

      if (log.isLoggable(Level.FINE))
        log.fine("file store: " + obj.getObjectId() + " length=" +
                 length);
    } finally {
      is.close();
    }
  }
  
  /**
   * Updates the object's objectAccess time in the persistent objectStore.
   *
   * @param uniqueId the identifier of the object.
   */
  @Override
  public void accessImpl(HashKey objectId)
    throws Exception
  {
    _backing.updateAccess(objectId);
  }
  
  /**
   * Sets the timef for the expires interval.
   *
   * @param uniqueId the identifier of the object.
   * @param long the time in ms for the expire
   */
  @Override
  public void setExpireInterval(HashKey objectId, long expires)
    throws Exception
  {
    _backing.setExpireInterval(objectId, expires);
  }

  /**
   * When the session is no longer valid, objectRemove it from the backing objectStore.
   */
  @Override
  public void remove(ClusterObject obj)
    throws Exception
  {
    removeClusterObject(obj.getObjectId());

    _backing.remove(obj.getObjectId());
  }
}
