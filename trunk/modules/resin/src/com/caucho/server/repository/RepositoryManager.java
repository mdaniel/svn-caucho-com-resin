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

import com.caucho.git.GitTree;
import com.caucho.git.GitType;
import com.caucho.server.cluster.Server;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Public API for the repository
 */
public class RepositoryManager
{
  private Server _server;
  private Repository _repository;

  public RepositoryManager()
  {
    this(Server.getCurrent());
  }

  protected RepositoryManager(Server server)
  {
    _server = server;

    _repository = _server.getRepository();
  }

  /**
   * Returns the current tag map.
   */
  public Map<String,RepositoryTagEntry> getTagMap()
  {
    return _repository.getTagMap();
  }

  /**
   * Returns the current tag map.
   */
  public RepositoryTagEntry getTag(String name)
  {
    return _repository.getTagMap().get(name);
  }

  //
  // tag management
  //

  /**
   * Adds a tag
   *
   * @param tag the symbolic tag for the repository
   * @param sha1 the root for the tag's content
   * @param user the user adding a tag.
   * @param message user's message for the commit
   * @param version symbolic version name for the commit
   */
  public boolean setTag(String tag,
                        String sha1,
                        String user,
                        String message,
                        String version)
  {
    return _repository.setTag(tag, sha1, user, _server.getServerId(),
                              message, version);
  }

  /**
   * Removes a tag
   *
   * @param tag the symbolic tag for the repository
   * @param user the user adding a tag.
   * @param message user's message for the commit
   */
  public boolean removeTag(String tag,
                           String user,
                           String message)
  {
    return _repository.removeTag(tag, user, _server.getServerId(),
                                 message);
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
  public GitType getType(String sha1)
  {
    return _repository.getType(sha1);
  }

  /**
   * Returns true if the file is a blob.
   */
  public final boolean isBlob(String sha1)
  {
    return getType(sha1) == GitType.BLOB;
  }

  /**
   * Adds a new path to the repository
   */
  public String addPath(String url)
  {
    Path path = Vfs.lookup(url);

    return _repository.addPath(path);
  }

  /**
   * Adds a .git tree to the repository
   */
  public String addTree(GitTree tree)
    throws IOException
  {
    return _repository.addTree(tree);
  }

  /**
   * Adds the raw git file to the repository.
   *
   * @param sha1 the sha1 hash of the file's contents
   * @param is the input stream to the contents
   */
  public void writeRawGitFile(String sha1, InputStream is)
    throws IOException
  {
    _repository.writeRawGitFile(sha1, is);
  }

  /**
   * Writes a file to a stream
   */
  public void writeToStream(OutputStream os, String sha1)
  {
    _repository.writeToStream(os, sha1);
  }

  /**
   * Extracts the git tree to a path
   */
  public void expandToPath(Path path, String sha1)
  {
    _repository.expandToPath(path, sha1);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _repository + "]";
  }
}
