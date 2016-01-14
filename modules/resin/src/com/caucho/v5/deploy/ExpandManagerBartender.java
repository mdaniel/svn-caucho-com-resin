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

import java.io.IOException;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.vfs.PathImpl;

/**
 * The generator for the deploy
 */
public class ExpandManagerBartender
{
  private final String _id;
  private final PathImpl _bartenderDirectory;

  private TreeSet<String> _keySet = new TreeSet<String>();
  private long _digest;

  /**
   * Creates the deploy.
   */
  public ExpandManagerBartender(String id,
                                PathImpl bartenderDirectory)
  {
    _id = id;
    _bartenderDirectory = bartenderDirectory;
  }

  /**
   * Gets the war expand directory.
   */
  public PathImpl getBartenderDirectory()
  {
    return _bartenderDirectory;
  }
  
  /**
   * Returns the set of deployed keys based on scanning the directory.
   */
  final TreeSet<String> getDeployedKeys()
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
      throw ConfigException.wrap(e);
    }
  }

  /**
   * Returns true if the deployment has modified.
   */
  boolean isModified()
  {
    long digest = getDigest();

    boolean isModified = _digest != digest;

    /*
    Logger log = Logger.getLogger(this.getClass().getName());

    log.warning(String.format("is modified %s %d %d",
                              isModified,
                              _digest,
                              digest));
                              */

    return isModified;
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
  long getDigest()
  {
    return getBartenderDirectory().getCrc64();
  }
  
  /**
   * Return the entry names for all deployed objects.
   */
  protected TreeSet<String> scanKeys()
    throws IOException
  {
    TreeSet<String> keys = new TreeSet<String>();

    for (String fileName : getBartenderDirectory().list()) {
      int p = fileName.lastIndexOf('.');
      
      if (p > 0) {
        String key = fileName.substring(0, p);
        keys.add(key);
      }
    }
    
    return keys;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "," + getBartenderDirectory() + "]";
  }
}
