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

package com.caucho.env.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.caucho.env.git.GitCommit;
import com.caucho.env.git.GitTree;
import com.caucho.env.git.GitType;
import com.caucho.vfs.Path;

public interface Repository
{
  /**
   * Updates the repository
   */
  public void update();
  /**
   * Returns the tag map.
   */
  public Map<String,RepositoryTagEntry> getTagMap();

  /**
   * Returns the tag root.
   */
  public String getTagRoot(String tag);

  /**
   * Adds a tag
   *
   * @param tag the symbolic tag for the repository
   * @param sha1 the root for the tag's content
   * @param user the user adding a tag.
   * @param server the server adding a tag.
   * @param message user's message for the commit
   * @param version symbolic version name for the commit
   */
  public boolean setTag(String tag,
                        String sha1,
                        String user,
                        String server,
                        String message,
                        String version);
  
  /**
   * Returns the hash stored in the git tag
   */
  public String getTag(String tag);
  
  /**
   * Sets the hash stored in the git tag
   */
  public void setTag(String tag, String sha1);

  /**
   * Removes a tag
   *
   * @param tag the symbolic tag for the repository
   * @param user the user adding a tag.
   * @param server the server adding a tag.
   * @param message user's message for the commit
   */
  public boolean removeTag(String tag,
                           String user,
                           String server,
                           String message);

  //
  // git file management
  //

  /**
   * Returns true if the file exists.
   */
  public boolean exists(String sha1);

  /**
   * Returns true if the file is a blob.
   */
  public GitType getType(String sha1);

  /**
   * Returns true if the file is a blob.
   */
  public boolean isBlob(String sha1);

  /**
   * Returns true if the file is a tree
   */
  public boolean isTree(String sha1);

  /**
   * Returns true if the file is a commit
   */
  public boolean isCommit(String sha1);

  /**
   * Validates a file, checking that it and its dependencies exist.
   */
  public boolean validateFile(String sha1)
    throws IOException;

  /**
   * Adds a path to the repository.  If the path is a directory or a
   * jar scheme, adds the contents recursively.
   */
  public String addPath(Path path);

  /**
   * Adds a stream to the repository.
   */
  public String addInputStream(InputStream is)
    throws IOException;

  /**
   * Adds a stream to the repository.
   */
  public String addInputStream(InputStream is, long length)
    throws IOException;

  /**
   * Opens a stream to a git blob
   */
  public InputStream openBlob(String sha1)
    throws IOException;

  /**
   * Reads a git tree from the repository
   */
  public GitTree readTree(String sha1)
    throws IOException;

  /**
   * Adds a git tree to the repository
   */
  public String addTree(GitTree tree)
    throws IOException;

  /**
   * Reads a git commit from the repository
   */
  public GitCommit readCommit(String sha1)
    throws IOException;

  /**
   * Adds a git commit to the repository
   */
  public String addCommit(GitCommit commit)
    throws IOException;

  /**
   * Opens a stream to the raw git file.
   */
  public InputStream openRawGitFile(String sha1)
    throws IOException;

  /**
   * Writes a raw git file
   */
  public void writeRawGitFile(String sha1, InputStream is)
    throws IOException;

  /**
   * Writes the contents to a stream.
   */
  public void writeToStream(OutputStream os, String sha1);

  /**
   * Expands the repository to the filesystem.
   */
  public void expandToPath(Path path, String root);
}
