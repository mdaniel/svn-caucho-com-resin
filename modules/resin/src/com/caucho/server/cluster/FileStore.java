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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.cluster;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.util.Alarm;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.TempStream;

/**
 * Class storing distributed objects based on the filesystem.
 */
public class FileStore extends StoreManager {
  protected Path _path;

  /**
   * Create a new file-based persistent session store.
   *
   * @param manager the session manager for the application
   * @param path path to the session directory
   */
  public FileStore()
  {
  }

  /**
   * Sets the file store's path.
   */
  public void setPath(Path path)
  {
    _path = path;
  }

  public void addText(String value)
  {
    _path = Vfs.lookup(value.trim());
  }

  public Path getPath()
  {
    return _path;
  }

  /**
   * Clears the files which are too old.
   */
  public void clearOldObjects()
  {
    clearOldObjects(getPath(), Alarm.getCurrentTime());
  }

  /**
   * Clears the files which are too old.
   */
  public void clearOldObjects(Path dir, long now)
  {
    try {
      String []list = dir.list();
      
      for (int i = 0; list != null && i < list.length; i++) {
        Path path = dir.lookup(list[i]);

        if (path.isFile() && path.canRead()) {
          long lastModified = path.getLastModified();

          if (lastModified + _maxIdleTime < now) {
	    log.finer("timeout file: " + path);
            
            path.remove();
          }
        }
	else if (path.isDirectory())
	  clearOldObjects(path, now);
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Creates the cluster object.
   */
  ClusterObject create(Store store, String id)
  {
    return new SharedFileObject(this, store, id, getPath());
  }

  /**
   * Loads the session from the filesystem.
   *
   * @param session the session to fill
   */
  public boolean load(ClusterObject clusterObj, Object obj)
    throws Exception
  {
    FileObject fileObj = (FileObject) clusterObj;
    
    ReadStream is = null;
    try {
      is = fileObj.openRead();
      
      if (log.isLoggable(Level.FINE))
	log.fine("load file: " + fileObj.getPath());
      
      return clusterObj.load(is, obj);
    } catch (IOException e) {
      log.fine("no saved object: " + e);
    } finally {
      if (is != null)
        is.close();
    }

    return false;
  }

  /**
   * Saves the session to the filesystem.
   *
   * @param session the session to save
   */
  public void store(ClusterObject obj, TempStream is, long crc, int updateCount)
    throws Exception
  {
    FileObject fileObj = (FileObject) obj;
    
    WriteStream os = null;
    
    try {
      os = fileObj.openWrite();

      ReadStream rs = is.openRead();
      os.writeStream(rs);

      if (log.isLoggable(Level.FINE))
        log.fine("store file: " + fileObj.getPath());
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      if (os != null) {
        try {
          os.close();
        } catch (IOException e2) {}
          
        os = null;
      }
        
      // fileObj.remove();
    } finally {
      if (os != null)
        os.close();
    }
  }
  
  /**
   * Updates the object's access time in the persistent store.
   *
   * @param uniqueId the identifier of the object.
   */
  public void accessImpl(String uniqueId)
    throws Exception
  {
  }

  /**
   * When the session is no longer valid, remove it from the backing store.
   */
  public void remove(ClusterObject obj)
    throws Exception
  {
    removeClusterObject(obj.getStoreId(), obj.getObjectId());

    obj.removeImpl();

    if (log.isLoggable(Level.FINE))
      log.fine("remove file-store session: " + obj);
  }
}
