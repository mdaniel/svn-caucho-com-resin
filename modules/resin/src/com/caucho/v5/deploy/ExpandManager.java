/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.deploy;

import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.caucho.v5.util.Crc64;

/**
 * Manages the expansion sub-managers
 */
class ExpandManager
{
  private final String _id;
  
  private final ExpandManagerDirectory _directoryManager;
  private final ExpandManagerArchive _archiveManager;
  private final ExpandManagerBartender _bartenderManager;

  private TreeSet<String> _keySet;
  
  // private final ExpandManagerVersion _versionManager;

  /**
   * Creates the deploy.
   */
  public ExpandManager(String id,
                       ExpandManagerDirectory directoryManager,
                       ExpandManagerArchive archiveManager,
                       ExpandManagerBartender bartenderManager)
  {
    _id = id;
    
    _directoryManager = directoryManager;
    _archiveManager = archiveManager;
    _bartenderManager = bartenderManager;
    //_repositoryManager = repositoryManager;
    
    directoryManager.reset();
    
    TreeSet<String> keySet = new TreeSet<String>();
    
    /*
    if (_repositoryManager != null) {
      keySet.addAll(_repositoryManager.getDeployedKeys());
    }
    */
    
    keySet.addAll(_archiveManager.getDeployedKeys());
    keySet.addAll(_bartenderManager.getDeployedKeys());
    keySet.addAll(_directoryManager.getDeployedKeys());
    
    // XXX: needs to filter through repository manager to remove
    // undeployed items
    
    _keySet = keySet;
    
    // _versionManager = new ExpandManagerVersion(id, keySet, isVersioning);
  }
  
  boolean isModified()
  {
    if (_directoryManager.isModified()) {
      return true;
    }
    else if (_archiveManager.isModified()) {
      return true;
    }
    else if (_bartenderManager.isModified()) {
      return true;
    }
    /*
    else if (_repositoryManager != null && _repositoryManager.isModified()) {
      return true;
    }
    */
    else {
      return false;
    }
  }
  
  long getDigest()
  {
    long digest = 0;
    
    digest = Crc64.generate(digest, _directoryManager.getDigest());
    // digest = Crc64.generate(digest, _archiveManager.getDigest());
    digest = Crc64.generate(digest, _bartenderManager.getDigest());
    
    /*
    if (_repositoryManager != null) {
      digest = Crc64.generate(digest, _repositoryManager.calculateDigest());
    }
    */

    return digest;
  }
  
  boolean logModified(Logger log)
  {
    if (_bartenderManager.logModified(log))
      return true;
    else if (_directoryManager.logModified(log))
      return true;
    else if (_archiveManager.logModified(log))
      return true;
    /*
    else if (_repositoryManager != null && _repositoryManager.logModified(log))
      return true;
      */
    else
      return false;
  }

  Set<String> getBaseKeySet()
  {
    //return _versionManager.getBaseKeySet();
    return getKeySet();
  }
  
  Set<String> getKeySet()
  {
    // return _versionManager.getKeySet();
    return _keySet;
  }
  
  /*
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
  */
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
