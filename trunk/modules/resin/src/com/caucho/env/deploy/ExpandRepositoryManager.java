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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.env.repository.Repository;
import com.caucho.env.repository.RepositorySystem;
import com.caucho.env.repository.RepositoryTagEntry;
import com.caucho.util.Crc64;

/**
 * The generator for the deploy
 */
class ExpandRepositoryManager
{
  private final String _tagPrefix;
  private final Repository _repository;

  private TreeSet<String> _keySet = new TreeSet<String>();
  
  private long _digest;
  private Map<String,RepositoryTagEntry> _oldRepositoryMap;

  /**
   * Creates the deploy.
   */
  public ExpandRepositoryManager(String id)
  {
    _tagPrefix = id + "/";
    
    _repository = RepositorySystem.getCurrentRepository();
  }

  /**
   * Returns true if the deployment has modified.
   */
  boolean isModified()
  {
    if (_oldRepositoryMap == _repository.getTagMap())
      return false;
    else {
      long newDigest = calculateDigest();
      
      return _digest != newDigest;
    }
  }

  /**
   * Log the reason for modification
   */
  boolean logModified(Logger log)
  {
    long digest = calculateDigest();

    if (_digest != digest) {
      log.info(this + " is modified");

      return true;
    }

    return false;
  }
  
  /**
   * Returns the set of deployed keys based on scanning the directory.
   */
  TreeSet<String> getDeployedKeys()
  {
    long oldDigest = _digest;
    TreeSet<String> oldKeys = _keySet;
    
    long newDigest = calculateDigest();
      
    if (oldDigest == newDigest) {
      return oldKeys;
    }
    
    try {
      TreeSet<String> newKeys = scanKeys();
    
      _digest = newDigest;
      _keySet = newKeys;
    
      return newKeys;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public long calculateDigest()
  {
    long digest = 0;
    
    String prefix = _tagPrefix;
    
    ArrayList<String> tags = new ArrayList<String>();
    Map<String,RepositoryTagEntry> tagMap = _repository.getTagMap();

    for (String key : tagMap.keySet()) {
      if (key.startsWith(prefix)) {
        tags.add(key);
      }
    }

    Collections.sort(tags);
    
    for (String tag : tags) {
      digest = Crc64.generate(digest, tag);

      RepositoryTagEntry entry = tagMap.get(tag);

      if (entry.getRoot() != null) {
        digest = Crc64.generate(digest, entry.getRoot());
      }
    }
    
    return digest;
  }
  
  /**
   * Return the entry names for all repository objects.
   */
  private TreeSet<String> scanKeys()
    throws IOException
  {
    TreeSet<String> keySet = new TreeSet<String>();
    
    String prefix = _tagPrefix;
    
    Map<String,RepositoryTagEntry> tagMap = _repository.getTagMap();

    for (String tag : tagMap.keySet()) {
      if (tag.startsWith(prefix)) {
        String key = tag.substring(prefix.length());
        
        int p = key.indexOf('/');
        if (p >= 0) {
          // server/2509
          key = key.substring(0, p);
          
          // continue;
        }
        
        keySet.add(key);
      }
    }
    
    return keySet;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _tagPrefix + "]";
  }
}
