/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.server.repository;

import java.util.Map;

import com.caucho.server.cluster.Server;

/**
 * Public API for the deployment
 */
public class LocalRepositoryManager
{
  private Server _server;
  private Repository _repository;

  public LocalRepositoryManager()
  {
    _server = Server.getCurrent();

    _repository = _server.getLocalRepository();
  }

  /**
   * Returns the current tag map.
   */
  public Map<String,RepositoryTagEntry> getTagMap()
  {
    return _repository.getTagMap();
  }

  //
  // git management
  //

  /**
   * Returns true if the file exists.
   */
  public boolean exists(String sha1)
  {
    return _repository.exists(sha1);
  }

  /**
   * Returns true if the file is a blob.
   */
  public boolean isBlob(String sha1)
  {
    return _repository.isBlob(sha1);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _repository + "]";
  }
}
