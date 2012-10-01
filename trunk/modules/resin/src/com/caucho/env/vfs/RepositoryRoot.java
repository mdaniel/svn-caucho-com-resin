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

package com.caucho.env.vfs;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.env.repository.RepositorySpi;
import com.caucho.vfs.Path;

/**
 * Virtual path based on an expansion repository
 */
public class RepositoryRoot
{
  private static final Logger log
    = Logger.getLogger(RepositoryRoot.class.getName());
  
  private String _id;
  private RepositorySpi _repository;
  private Path _rootDirectory;
  
  private String _rootHash;

  public RepositoryRoot(String id, 
                        RepositorySpi repository, 
                        Path rootDirectory)
  {
    _id = id;
    _repository = repository;
    _rootDirectory = rootDirectory;
    
    if (_repository == null)
      throw new NullPointerException();
  }
  
  private String getId()
  {
    return _id;
  }
  
  private Path getRootDirectory()
  {
    return _rootDirectory;
  }
  
  public void update()
  {
    if (! _repository.isActive()) {
    }
    else if (! exists()) {
      deleteLocalCopy();
    }
    else if (isModified()) {
      try {
        extractFromRepository();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private boolean exists()
  {
    String tag = getId();

    String treeHash = _repository.getTagContentHash(tag);

    if (treeHash != null)
      return true;
    else
      return false;
  }

  private boolean isModified()
  {
    String tag = getId();
    String treeHash = _repository.getTagContentHash(tag);

    if (treeHash == null)
      return false;
    else
      return ! treeHash.equals(_rootHash);
  }

  private synchronized boolean deleteLocalCopy()
  {
    try {
      Path pwd = getRootDirectory();

      pwd.removeAll();

      return true;

    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      return false;
    }
  }
  /**
   * Extract the contents from the repository into the root directory.
   */
  private synchronized boolean extractFromRepository()
    throws IOException
  {
    try {
      String tag = getId();
      String treeHash = _repository.getTagContentHash(tag);
    
      if (treeHash == null)
        return false;
      
      if (treeHash.equals(_rootHash))
        return false;
      
      Path pwd = getRootDirectory();

      pwd.mkdirs();

      if (log.isLoggable(Level.FINE)) {
        log.fine(this + " extract from repository tag=" + tag
                 + "\n  root=" + getRootDirectory()
                 + "\n  contentHash=" + treeHash);
      }

      _repository.expandToPath(treeHash, pwd);
      
      _rootHash = treeHash;

      return true;
    } catch (ConfigException e) {
      throw e;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}