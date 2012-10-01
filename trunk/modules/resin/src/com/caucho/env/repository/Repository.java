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

package com.caucho.env.repository;

import java.io.InputStream;
import java.util.Map;

import com.caucho.vfs.Path;

/**
 * The Repository is a collection of archives organized by a tag map. Each
 * archive is equivalent to a .jar file or a directory, consisting of
 * the binary data Blobs, the directory name Tree, and a .git Commit item
 * to track versions.
 * 
 * The tag map is a map of strings to tag entries, where the entry is
 * the sha1 of the .git Commit root of the archive, and metadata.
 */
public interface Repository
{
  /**
   * Returns the current read-only tag map.
   */
  public Map<String,RepositoryTagEntry> getTagMap();

  /**
   * Convenience method returning the tag's contentHash.
   */
  public String getTagContentHash(String tag);
  
  /**
   * Adds and commits a jar/zip archive
   */
  public String commitArchive(CommitBuilder commitTag,
                              Path contentArchive);
  
  /**
   * Adds and commits a jar/zip archive.
   */
  public String commitArchive(CommitBuilder commitTag,
                              InputStream is);
  
  /**
   * Adds and commits a full path, recursively
   */
  public String commitPath(CommitBuilder commitTag,
                           Path contentDirectory);

  /**
   * Removes a tag
   *
   * @param tagName the symbolic tag for the repository
   * @param user the user adding a tag.
   * @param server the server adding a tag.
   * @param message user's message for the commit
   */
  public boolean removeTag(CommitBuilder commitTag);
  
  //
  // tag listeners
  //
  
  /**
   * Adds a tag change listener
   */
  public void addListener(String tagName, RepositoryTagListener listener);
  
  /**
   * Adds a tag change listener
   */
  public void removeListener(String tagName, RepositoryTagListener listener);
}
