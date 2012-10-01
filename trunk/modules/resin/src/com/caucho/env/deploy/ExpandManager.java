/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.env.deploy;

import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.caucho.util.Crc64;

/**
 * Manages the expansion sub-managers
 */
class ExpandManager
{
  private final String _id;
  
  private final ExpandDirectoryManager _directoryManager;
  private final ExpandArchiveManager _archiveManager;
  private final ExpandRepositoryManager _repositoryManager;
  
  private final ExpandVersionManager _versionManager;

  /**
   * Creates the deploy.
   */
  public ExpandManager(String id,
                       ExpandDirectoryManager directoryManager,
                       ExpandArchiveManager archiveManager,
                       ExpandRepositoryManager repositoryManager,
                       boolean isVersioning)
  {
    _id = id;
    
    _directoryManager = directoryManager;
    _archiveManager = archiveManager;
    _repositoryManager = repositoryManager;
    
    directoryManager.reset();
    
    TreeSet<String> keySet = new TreeSet<String>();
    
    keySet.addAll(_repositoryManager.getDeployedKeys());
    keySet.addAll(_archiveManager.getDeployedKeys());
    keySet.addAll(_directoryManager.getDeployedKeys());
    
    // XXX: needs to filter through repository manager to remove
    // undeployed items
    
    _versionManager = new ExpandVersionManager(id, keySet, isVersioning);
  }
  
  boolean isModified()
  {
    return (_directoryManager.isModified()
            || _archiveManager.isModified()
            || _repositoryManager.isModified());
  }
  
  long getDigest()
  {
    long digest = 0;
    
    digest = Crc64.generate(digest, _directoryManager.getDigest());
    // digest = Crc64.generate(digest, _archiveManager.getDigest());
    digest = Crc64.generate(digest, _repositoryManager.calculateDigest());

    return digest;
  }
  
  boolean logModified(Logger log)
  {
    if (_directoryManager.logModified(log))
      return true;
    else if (_archiveManager.logModified(log))
      return true;
    else if (_repositoryManager.logModified(log))
      return true;
    else
      return false;
  }

  Set<String> getBaseKeySet()
  {
    return _versionManager.getBaseKeySet();
  }
  
  Set<String> getKeySet()
  {
    return _versionManager.getKeySet();
  }
  
  ExpandVersion getPrimaryVersion(String key)
  {
    return _versionManager.getPrimaryVersion(key);
  }
  
  ExpandVersion getVersion(String key)
  {
    return _versionManager.getVersion(key);
  }

  ExpandVersionGroup getBaseVersionGroup(String key)
  {
    return _versionManager.getBaseVersionGroup(key);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
