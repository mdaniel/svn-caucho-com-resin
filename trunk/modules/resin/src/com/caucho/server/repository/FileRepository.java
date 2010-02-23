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

import com.caucho.git.GitCommit;
import com.caucho.git.GitObjectStream;
import com.caucho.git.GitRepository;
import com.caucho.git.GitTree;
import com.caucho.git.GitType;
import com.caucho.jmx.Jmx;
import com.caucho.management.server.DeployControllerMXBean;
import com.caucho.management.server.EAppMXBean;
import com.caucho.management.server.EarDeployMXBean;
import com.caucho.management.server.WebAppMXBean;
import com.caucho.management.server.WebAppDeployMXBean;
import com.caucho.server.cluster.Server;
import com.caucho.server.resin.Resin;
import com.caucho.server.host.HostController;
import com.caucho.server.webapp.WebAppController;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.util.QDate;
import com.caucho.vfs.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.ObjectName;

public class FileRepository extends Repository
{
  private static final Logger log
    = Logger.getLogger(FileRepository.class.getName());

  private static final L10N L = new L10N(FileRepository.class);

  private GitRepository _git;
  
  private String _repositoryHash;
  
  private RepositoryTagMap _tagMap;

  public FileRepository(Server server)
  {
    super(server);
    
    _git = server.getGit();
  }

  /**
   * Updates the repository
   */
  public void update()
  {
    update(getTag(getRepositoryTag()));
  }

  //
  // tag metadata management
  //

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
			String root,
			String user,
			String server,
			String message,
			String version)
  {
    RepositoryTagMap tagMap;

    do {
      tagMap = addTagData(tag, root, user,
			  server, message, version);

    } while (! setTagMap(tagMap));

    return true;
  }

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
  public boolean removeTag(String tag,
			   String user,
			   String server,
			   String message)
  {
    RepositoryTagMap tagMap;

    do {
      tagMap = removeTagData(tag, user, server, message);
    } while (! setTagMap(tagMap));

    return true;
  }

  /**
   * Removes a tag
   *
   * @param tag the symbolic tag for the repository
   * @param sha1 the root for the tag's content
   * @param user the user adding a tag.
   * @param server the server adding a tag.
   * @param message user's message for the commit
   */
  public boolean removeTag(String tag,
			   String sha1,
			   String user,
			   String server,
			   String message)
  {
    return false;
  }

  //
  // git management
  //
  
  /**
   * Returns the hash stored in the git tag
   */
  protected String getTag(String tag)
  {
    return _git.getTag(tag);
  }
  
  /**
   * Sets the hash stored in the git tag
   */
  protected void setTag(String tag, String sha1)
  {
    _git.writeTag(tag, sha1);
  }

  /**
   * Returns true if the file exists.
   */
  public boolean exists(String sha1)
  {
    return _git.contains(sha1);
  }

  /**
   * Returns true if the file is a blob.
   */
  public GitType getType(String sha1)
  {
    try {
      return _git.contains(sha1) ? _git.objectType(sha1) : null;
    } catch (IOException e) {
      throw new RepositoryException(e);
    }
  }

  /**
   * Adds a path to the repository.  If the path is a directory or a
   * jar scheme, adds the contents recursively.
   */
  public String addPath(Path path)
  {
    try {
      return _git.writeFile(path);
    } catch (IOException e) {
      throw new RepositoryException(e);
    }
  }

  /**
   * Adds a path to the repository.  If the path is a directory or a
   * jar scheme, adds the contents recursively.
   */
  public String addInputStream(InputStream is)
  {
    try {
      return _git.writeInputStream(is);
    } catch (IOException e) {
      throw new RepositoryException(e);
    }
  }

  /**
   * Adds a path to the repository.  If the path is a directory or a
   * jar scheme, adds the contents recursively.
   */
  public String addInputStream(InputStream is, long length)
  {
    try {
      return _git.writeInputStream(is, length);
    } catch (IOException e) {
      throw new RepositoryException(e);
    }
  }

  /**
   * Opens a stream to a blob
   */
  public InputStream openBlob(String sha1)
    throws IOException
  {
    return _git.openBlob(sha1);
  }

  /**
   * Opens a stream to the raw git file.
   */
  public InputStream openRawGitFile(String sha1)
    throws IOException
  {
    return _git.openRawGitFile(sha1);
  }

  /**
   * Reads a git tree from the repository
   */
  public GitTree readTree(String sha1)
    throws IOException
  {
    return _git.parseTree(sha1);
  }

  /**
   * Adds a git tree to the repository
   */
  public String addTree(GitTree tree)
    throws IOException
  {
    return _git.writeTree(tree);
  }

  /**
   * Reads a git commit from the repository
   */
  public GitCommit readCommit(String sha1)
    throws IOException
  {
    return _git.parseCommit(sha1);
  }

  /**
   * Adds a git commit to the repository
   */
  public String addCommit(GitCommit commit)
    throws IOException
  {
    return _git.writeCommit(commit);
  }

  /**
   * Writes the git file from the StreamInput
   *
   * @param sha1 the file hash
   * @param is the raw contents for the new file
   */
  public void writeRawGitFile(String sha1, InputStream is)
    throws IOException
  {
    _git.writeRawGitFile(sha1, is);
  }

  /**
   * Writes the contents to a stream.
   */
  public void writeToStream(OutputStream os, String sha1)
  {
    GitObjectStream is = null;
    
    try {
      is = _git.open(sha1);
    
      if (is.getType() != GitType.BLOB)
	throw new RepositoryException(L.l("'{0}' is an unexpected type, expected 'blob'",
						is.getType()));

      WriteStream out = null;

      if (os instanceof WriteStream)
	out = (WriteStream) os;
      else
	out = Vfs.openWrite(os);

      try {
	out.writeStream(is.getInputStream());
      } finally {
	if (out != null && out != os)
	  out.close();
      }
    } catch (IOException e) {
      throw new RepositoryException(e);
    } finally {
      is.close();
    }
  }

  /**
   * Expands the repository to the filesystem.
   */
  public void expandToPath(Path path, String rootSha1)
  {
    try {
      _git.expandToPath(path, rootSha1);
    } catch (IOException e) {
      throw new RepositoryException(e);
    }
  }
}