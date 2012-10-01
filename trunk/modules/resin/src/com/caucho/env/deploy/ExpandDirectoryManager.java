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
import java.util.TreeSet;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.vfs.Path;

/**
 * Manages the expand directory for the generator, like the webapps directory.
 * The expand manager is responsible for scanning for matching directories.
 */
class ExpandDirectoryManager
{
  private static final Logger log = Logger.getLogger(ExpandDirectoryManager.class.getName());

  private final String _id;
  
  private final Path _expandDirectory;
  
  private final String _expandPrefix;
  private final String _expandSuffix;

  private final ArrayList<String> _requireFiles = new ArrayList<String>();
  
  private long _digest;
  private TreeSet<String> _keySet = new TreeSet<String>();

  /**
   * Creates the deploy.
   */
  public ExpandDirectoryManager(String id,
                                Path expandDirectory,
                                String expandPrefix,
                                String expandSuffix,
                                ArrayList<String> requireFiles)
  {
    _id = id;
    _expandDirectory = expandDirectory;
    _expandPrefix = expandPrefix;
    _expandSuffix = expandSuffix;
    _requireFiles.addAll(requireFiles);
  }
  
  void reset()
  {
    _digest = -1;
    _keySet = new TreeSet<String>();
  }
  
  public String getId()
  {
    return _id;
  }

  /**
   * Gets the war expand directory.
   */
  public Path getExpandDirectory()
  {
    return _expandDirectory;
  }

  /**
   * Gets the expand prefix.
   */
  public String getExpandPrefix()
  {
    return _expandPrefix;
  }

  /**
   * Gets the expand suffix.
   */
  public String getExpandSuffix()
  {
    return _expandSuffix;
  }

  /**
   * Returns the location of an expanded archive, or null if no archive with
   * the passed name is deployed.
   *
   * @param name a name, without an extension
   */
  Path getExpandPath(String key)
  {
    String pathName = getExpandPrefix() + key + getExpandSuffix();
    
    Path path = getExpandDirectory().lookup(pathName);
    
    return path;
  }
  
  /**
   * Returns the set of deployed keys based on scanning the directory.
   */
  TreeSet<String> getDeployedKeys()
  {
    long oldDigest = _digest;
    TreeSet<String> oldKeys = _keySet;
    
    long newDigest = getDigest();
      
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
  
  boolean isModified()
  {
    return _digest != getDigest();
  }
  
  boolean logModified(Logger log)
  {
    if (isModified()) {
      log.info(this + " is modified");
      return true;
    }
    
    return false;
  }

  /**
   * Returns the digest of the expand and archive directories.
   */
  long getDigest()
  {
    return getExpandDirectory().getCrc64();
  }
  
  /**
   * Return the entry names for all deployed objects.
   */
  private TreeSet<String> scanKeys()
    throws IOException
  {
    TreeSet<String> keys = new TreeSet<String>();

    // collect all the new war expand directories
    for (String pathName : getExpandDirectory().list()) {
      if (! pathName.startsWith(_expandPrefix))
        continue;
      
      if (! pathName.startsWith(_expandSuffix))
        continue;
      
      Path rootDirectory = getExpandDirectory().lookup(pathName);
      
      if (! isValidDirectory(rootDirectory, pathName))
        continue;
      
      int begin = _expandPrefix.length();
      int end = pathName.length() - _expandSuffix.length();
      String key = pathName.substring(begin, end);

      keys.add(key);
    }
    
    return keys;
  }

  private boolean isValidDirectory(Path rootDirectory, String pathName)
  {
    if (! rootDirectory.isDirectory()) {
      return false;
    }

    if (pathName.equalsIgnoreCase("web-inf")
        || pathName.equalsIgnoreCase("meta-inf")
        || pathName.startsWith(".")
        || pathName.endsWith(".")) {
      return false;
    }
    
    if (_expandPrefix.equals("")
        && (pathName.startsWith("_"))) {
      return false;
    }

    for (int j = 0; j < _requireFiles.size(); j++) {
      String file = _requireFiles.get(j);

      if (! rootDirectory.lookup(file).canRead())
        return false;
    }
    
    Path expandHash = rootDirectory.lookup(ExpandDeployController.APPLICATION_HASH_PATH);
    
    if (expandHash.canRead()) {
      log.finer(this + " " + rootDirectory + " contains an application hash");
      return false;
    }
    
    return true;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getExpandDirectory() + "]";
  }
}
