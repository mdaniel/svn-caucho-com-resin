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
import java.util.TreeSet;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * The generator for the deploy
 */
class ExpandArchiveManager
{
  private static final L10N L = new L10N(ExpandArchiveManager.class);

  private final String _id;
  private final Path _archiveDirectory;

  private final String _extension;

  private TreeSet<String> _keySet = new TreeSet<String>();
  private long _digest;

  /**
   * Creates the deploy.
   */
  public ExpandArchiveManager(String id,
                              Path archiveDirectory,
                              String extension)
  {
    _id = id;
    _archiveDirectory = archiveDirectory;
    _extension = extension;
    
    if (! extension.startsWith("."))
      throw new ConfigException(L.l("deployment extension '{0}' must begin with '.'",
                                    extension));

  }

  /**
   * Gets the war expand directory.
   */
  public Path getArchiveDirectory()
  {
    return _archiveDirectory;
  }

  /**
   * Returns the location for deploying an archive with the specified name.
   *
   * @param name a name, without an extension
   */
  public Path getArchivePath(String name)
  {
    return getArchiveDirectory().lookup(name + getExtension());
  }

  /**
   * Returns the extension.
   */
  public String getExtension()
  {
    return _extension;
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

  /**
   * Returns true if the deployment has modified.
   */
  boolean isModified()
  {
    return _digest != getDigest();
  }

  /**
   * Log the reason for modification
   */
  boolean logModified(Logger log)
  {
    long digest = getDigest();

    if (_digest != digest) {
      String reason = "";
      
      reason = this + " is modified";

      log.info(reason);

      return true;
    }

    return false;
  }

  /**
   * Returns the digest of the expand and archive directories.
   */
  private long getDigest()
  {
    return getArchiveDirectory().getCrc64();
  }
  
  /**
   * Return the entry names for all deployed objects.
   */
  private TreeSet<String> scanKeys()
    throws IOException
  {
    TreeSet<String> keys = new TreeSet<String>();

    for (String fileName : getArchiveDirectory().list()) {
      if (! fileName.endsWith(getExtension()))
        continue;

      Path archivePath = getArchiveDirectory().lookup(fileName);
      
      if (! archivePath.canRead())
        continue;
      if (archivePath.isDirectory())
        continue;
      
      int end = fileName.length() - _extension.length();
      
      String key = fileName.substring(0, end);
      
      keys.add(key);
    }
    
    return keys;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "," + getArchiveDirectory() + "]";
  }
}
