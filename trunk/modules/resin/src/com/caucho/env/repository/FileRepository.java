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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.caucho.env.git.GitCommit;
import com.caucho.env.git.GitObjectStream;
import com.caucho.env.git.GitSystem;
import com.caucho.env.git.GitTree;
import com.caucho.env.git.GitType;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

public class FileRepository extends AbstractRepository
{
  private static final L10N L = new L10N(FileRepository.class);

  private GitSystem _git;
  
  private AtomicReference<String> _rootHash = new AtomicReference<String>();
  
  private Lifecycle _lifecycle = new Lifecycle();
  
  public FileRepository()
  {
    this(GitSystem.getCurrent());
  }

  public FileRepository(GitSystem git)
  {
    _git = git;
    
    if (_git == null) {
      throw new IllegalStateException(L.l("{0} is required for {1}",
                                          GitSystem.class.getSimpleName(),
                                          getClass().getSimpleName()));
    }
  }

  /**
   * Updates the repository
   */
  @Override
  public void checkForUpdate(boolean isExact)
  {
    update(getRepositoryRootHash(), false);
  }

  //
  // tag metadata management
  //

  /**
   * Adds a tag
   *
   * @param tagName the symbolic tag for the repository
   * @param contentHash the sha1 hash of the tag's content
   * @param commitMessage user's message for the commit
   * @param commitMetaData additional commit metadata
   */
  @Override
  public boolean putTag(String tagName,
                        String contentHash,
                        Map<String,String> commitMetaData)
  {
    RepositoryTagMap tagMap;

    do {
      tagMap = addTagData(tagName, contentHash, commitMetaData);
    } while (! setTagMap(tagMap));
    
    return true;
  }

  /**
   * Removes a tag
   *
   * @param tagName the symbolic tag for the repository
   * @param commitMessage user's message for the commit
   * @param commitMetaData additional metadata
   */
  @Override
  public boolean removeTag(String tagName,
                           Map<String,String> commitMetaData)
  {
    RepositoryTagMap tagMap;

    do {
      tagMap = removeTagData(tagName, commitMetaData);
    } while (! setTagMap(tagMap));

    return true;
  }

  /**
   * Returns the hash stored in the git tag
   */
  @Override
  public String getRepositoryRootHash()
  {
    String rootHash = _rootHash.get();
    
    if (rootHash != null && rootHash.length() > 6)
      return rootHash;
    
    String value = _git.getTag(getRepositoryTag());
    
    _rootHash.compareAndSet(null, value);
    
    value = _rootHash.get();
    
    if (value != null && value.length() > 6)
      return value;
    else
      return null;
  }
  
  /**
   * Sets the hash stored in the git tag
   */
  @Override
  public void setRepositoryRootHash(String sha1)
  {
    if (sha1 != null) {
      _rootHash.set(sha1);

      _git.writeTag(getRepositoryTag(), sha1);
    }
  }

  /**
   * Returns true if the file exists.
   */
  @Override
  public boolean exists(String hash)
  {
    if (hash == null)
      throw new NullPointerException();
    
    return _git.contains(hash);
  }

  /**
   * Returns true if the file is a blob.
   */
  @Override
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
  @Override
  public String addBlob(InputStream is)
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
  @Override
  public String addBlob(InputStream is, long length)
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
  @Override
  public InputStream openBlob(String sha1)
    throws IOException
  {
    return _git.openBlob(sha1);
  }

  /**
   * Opens a stream to the raw git file.
   */
  @Override
  public InputStream openRawGitFile(String sha1)
    throws IOException
  {
    return _git.openRawGitFile(sha1);
  }

  /**
   * Reads a git tree from the repository
   */
  @Override
  public GitTree readTree(String sha1)
    throws IOException
  {
    return _git.parseTree(sha1);
  }

  /**
   * Adds a git tree to the repository
   */
  @Override
  public String addTree(GitTree tree)
    throws IOException
  {
    return _git.writeTree(tree);
  }

  /**
   * Reads a git commit from the repository
   */
  @Override
  public GitCommit readCommit(String sha1)
    throws IOException
  {
    return _git.parseCommit(sha1);
  }

  /**
   * Adds a git commit to the repository
   */
  @Override
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
  @Override
  public void writeRawGitFile(String sha1, InputStream is)
    throws IOException
  {
    synchronized (_git) {
      _git.writeRawGitFile(sha1, is);
    }
  }

  /**
   * Writes the git file from the StreamInput
   *
   * @param sha1 the file hash
   * @param is the raw contents for the new file
   */
  @Override
  public void validateRawGitFile(String sha1)
  {
    _git.validateRawGitFile(sha1);
  }

  /**
   * Writes the contents to a stream.
   */
  @Override
  public void writeBlobToStream(String blobHash, OutputStream os)
  {
    GitObjectStream is = null;
    
    try {
      is = _git.open(blobHash);
    
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

  @Override
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  @Override
  public void start()
  {
    if (! _lifecycle.toActive())
      return;
    
    super.start();
  }

  /**
   * Expands the repository to the filesystem.
   */
  @Override
  public void expandToPath(String contentHash, Path path)
  {
    try {
      _git.expandToPath(path, contentHash);
    } catch (IOException e) {
      throw new RepositoryException(e);
    }
  }
}