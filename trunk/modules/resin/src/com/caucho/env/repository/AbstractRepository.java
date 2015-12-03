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
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.git.GitCommit;
import com.caucho.env.git.GitCommitJar;
import com.caucho.env.git.GitSystem;
import com.caucho.env.git.GitTree;
import com.caucho.env.git.GitType;
import com.caucho.server.admin.GitJarStreamSource;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.TempOutputStream;

abstract public class AbstractRepository implements Repository, RepositorySpi
{
  private static final Logger log
    = Logger.getLogger(AbstractRepository.class.getName());

  private static final L10N L = new L10N(AbstractRepository.class);

  private String _repositoryTag;

  private RepositoryTagMap _tagMap = new RepositoryTagMap();
  
  private ConcurrentHashMap<String,CopyOnWriteArrayList<RepositoryTagListener>>
  _tagListenerMap = new ConcurrentHashMap<String,CopyOnWriteArrayList<RepositoryTagListener>>();

  protected AbstractRepository()
  {
    _repositoryTag = "resin/repository/root";
  }

  /**
   * Initialize the repository
   */
  public void init()
  {
  }

  /**
   * Start the repository
   */
  public void start()
  {
    checkForUpdate();
  }
  
  public void stop()
  {
    
  }

  /**
   * Returns the .git repository tag
   */
  protected String getRepositoryTag()
  {
    return _repositoryTag;
  }

  /**
   * Updates the repository
   */
  public final void checkForUpdate()
  {
    checkForUpdate(false);
  }

  /**
   * Updates the repository
   */
  @Override
  public void checkForUpdate(boolean isExact)
  {
    loadLocalRoot();
  }
  
  protected void loadLocalRoot()
  {
    String sha1 = getRepositoryRootHash();
    
    if (sha1 != null)
      updateTagMap(sha1, false);
  }

  /**
   * Updates based on a sha1 commit entry
   */
  protected boolean update(String sha1, boolean isNew)
  {
    String oldSha1 = _tagMap.getCommitHash();

    if (sha1 == null || sha1.equals(oldSha1)) {
      return true;
    }

    updateLoad(sha1, isNew);

    return false;
  }
  
  protected String getTagHash()
  {
    return _tagMap.getCommitHash();
  }
  
  protected long getTagSequence()
  {
    return _tagMap.getSequence();
  }

  protected void updateLoad(String sha1, boolean isNew)
  {
    updateTagMap(sha1, isNew);
  }

  protected void updateTagMap(String sha1, boolean isNew)
  {
    try {
      RepositoryTagMap tagMap
        = new RepositoryTagMap(this, sha1, isNew);

      setTagMap(tagMap);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  //
  // deploy tag management
  //

  /**
   * Returns the tag commit hash
   */
  protected String getCommitHash()
  {
    return _tagMap.getCommitHash();
  }

  /**
   * Returns the tag map.
   */
  @Override
  public Map<String,RepositoryTagEntry> getTagMap()
  {
    return _tagMap.getTagMap();
  }

  /**
   * Returns the tag root.
   */
  @Override
  public String getTagContentHash(String tag)
  {
    RepositoryTagEntry entry = getTagMap().get(tag);

    if (entry != null)
      return entry.getRoot();
    else
      return null;
  }

  /**
   * Adds a tag
   *
   * @param tagName the symbolic tag for the repository
   * @param contentHash the hash of the tag's content
   * @param commitMessage the commit message for the tag update
   * @param commitMetaData additional commit attributes
   */
  @Override
  abstract public boolean putTag(String tag,
                                 String contentHash,
                                 Map<String,String> commitMetaData);

  /**
   * Removes a tag
   *
   * @param tagName the symbolic tag for the repository
   * @param commitMessage user's message for the commit
   * @param commitMetaData additional commit meta-data
   */
  @Override
  abstract public boolean removeTag(String tag,
                                    Map<String,String> commitMetaData);

  /**
   * Convenience method for adding the content of a jar.
   */
  @Override
  public String commitArchive(CommitBuilder commit,
                              Path archivePath)
  {
    commit.validate();
    
    String contentHash = addArchive(archivePath);
    
    RepositoryTagEntry oldEntry = getTagMap().get(commit.getId());
    
    if (oldEntry != null && oldEntry.getRoot().equals(contentHash))
      return contentHash;
    
    if (putTag(commit.getId(), 
               contentHash, 
               commit.getAttributes())) {
      return contentHash;
    }
    else
      return null;
  }

  /**
   * Convenience method for adding the content of a jar.
   */
  @Override
  public String commitArchive(CommitBuilder commit,
                              InputStream is)
  {
    commit.validate();
    
    String contentHash = addArchive(is);
    
    if (putTag(commit.getId(), 
               contentHash, 
               commit.getAttributes())) {
      return contentHash;
    }
    else
      return null;
  }

  /**
   * Convenience method for adding a path/directory.
   */
  @Override
  public String commitPath(CommitBuilder commit,
                           Path directoryPath)
  {
    commit.validate();
    
    String contentHash = addPath(directoryPath);
    
    if (putTag(commit.getId(), 
               contentHash, 
               commit.getAttributes())) {
      return contentHash;
    }
    else
      return null;
  }

  /**
   * Convenience method for adding the content of a jar.
   */
  @Override
  public boolean removeTag(CommitBuilder commit)
  {
    commit.validate();
    
    return removeTag(commit.getId(), commit.getAttributes());
  }
  
  /**
   * Creates a tag entry
   *
   * @param tagName the symbolic tag for the repository
   * @param contentHash the hash of the tag's content
   * @param commitMessage user's message for the commit
   * @param commitMetaData additional attributes for the commit
   */
  protected RepositoryTagMap addTagData(String tagName,
                                        String contentHash,
                                        Map<String,String> commitMetaData)
  {
    try {
      checkForUpdate();

      RepositoryTagMap repositoryTagMap = _tagMap;

      Map<String,RepositoryTagEntry> tagMap = repositoryTagMap.getTagMap();

      ValidateHashResult validResult = validateHash(tagName, contentHash);
      if (! validResult.isValid()) {
        throw new RepositoryException(L.l("'{0}' with sha1='{1}' has invalid or missing repository content",
                                          validResult.getName(), contentHash));
      }
      
      if (log.isLoggable(Level.FINE)) {
        log.fine(this + " committing " + tagName + "\n  '"
                 + (commitMetaData != null ? commitMetaData.get("message") : null)
                 + "'\n  " + contentHash);
      }

      String parent = null;

      RepositoryTagEntry entry
        = new RepositoryTagEntry(this, tagName, contentHash, parent,
                                 commitMetaData);

      Map<String,RepositoryTagEntry> newTagMap
        = new TreeMap<String,RepositoryTagEntry>(tagMap);

      newTagMap.put(tagName, entry);

      RepositoryTagMap newDeployTagMap
        = new RepositoryTagMap(this, repositoryTagMap, newTagMap);
      
      // #4450 - always return a value

      return newDeployTagMap;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RepositoryException(e);
    }
  }

  /**
   * Removes a tag entry
   *
   * @param tag the symbolic tag for the repository
   * @param user the user adding a tag.
   * @param server the server adding a tag.
   * @param message user's message for the commit
   * @param version symbolic version name for the commit
   */
  protected RepositoryTagMap removeTagData(String tagName,
                                           Map<String,String> commitMetaData)
  {
    try {
      if (tagName == null)
        throw new NullPointerException();
      
      checkForUpdate();

      RepositoryTagMap repositoryTagMap = _tagMap;

      Map<String,RepositoryTagEntry> tagMap = repositoryTagMap.getTagMap();

      RepositoryTagEntry oldEntry = tagMap.get(tagName);

      if (oldEntry == null)
        return repositoryTagMap;

      Map<String,RepositoryTagEntry> newTagMap
        = new TreeMap<String,RepositoryTagEntry>(tagMap);

      newTagMap.remove(tagName);

      RepositoryTagMap newDeployTagMap
        = new RepositoryTagMap(this, repositoryTagMap, newTagMap);

      if (_tagMap == repositoryTagMap) {
        return newDeployTagMap;
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RepositoryException(e);
    }

    return null;
  }

  protected boolean setTagMap(RepositoryTagMap tagMap)
  {
    if (tagMap == null)
      throw new NullPointerException();

    RepositoryTagMap oldTagMap = null;
    
    synchronized (this) {
      oldTagMap = _tagMap;
      
      if (tagMap.getCommitHash().equals(oldTagMap.getCommitHash())) {
        return true;
      }
      else if (tagMap.compareTo(oldTagMap) < 0) {
        updateRepositoryRoot(oldTagMap.getCommitHash(),
                             oldTagMap.getSequence());
        
        return false;
      }
        
      _tagMap = tagMap;

      updateRepositoryRoot(tagMap.getCommitHash(), tagMap.getSequence());
    }

    if (log.isLoggable(Level.FINER))
      log.finer(this + " updating deployment " + tagMap);
    
    notifyTagListeners(oldTagMap.getTagMap(), tagMap.getTagMap());

    return true;
  }
  
  protected void updateRepositoryRoot(String sha1, long sequence)
  {
    setRepositoryRootHash(sha1);
  }
  
  private void notifyTagListeners(Map<String,RepositoryTagEntry> oldTagMap,
                                  Map<String,RepositoryTagEntry> newTagMap)
  {
    for (Map.Entry<String,RepositoryTagEntry> entry : newTagMap.entrySet()) {
      String tag = entry.getKey();
      RepositoryTagEntry newEntry = entry.getValue();
      
      RepositoryTagEntry oldEntry = oldTagMap.get(tag);
      
      if (oldEntry == null) {
        onTagChange(tag);
      }
      else if (! newEntry.getTagEntryHash().equals(oldEntry.getTagEntryHash())) {
        onTagChange(tag);
      }
    }
    
    for (String tag : oldTagMap.keySet()) {
      if (! newTagMap.containsKey(tag)) {
        onTagChange(tag);
      }
    }
  }
  
  /**
   * Adds a tag listener 
   */
  @Override
  public void addListener(String tag, RepositoryTagListener listener)
  {
    CopyOnWriteArrayList<RepositoryTagListener> listeners;
    listeners = _tagListenerMap.get(tag);
    
    if (listeners == null) {
      listeners = new CopyOnWriteArrayList<RepositoryTagListener>();
      
      _tagListenerMap.putIfAbsent(tag, listeners);
      
      listeners = _tagListenerMap.get(tag);
    }
    
    listeners.add(listener);
  }
  
  /**
   * Adds a tag listener 
   */
  @Override
  public void removeListener(String tag, RepositoryTagListener listener)
  {
    CopyOnWriteArrayList<RepositoryTagListener> listeners;
    listeners = _tagListenerMap.get(tag);
    
    if (listeners == null) {
      return;
    }
    
    listeners.remove(listener);
  }
  
  private void onTagChange(String tag)
  {
    int p = tag.lastIndexOf('/');
    
    if (p >= 0) {
      onTagChange(tag.substring(0, p));
    }
    else if (! tag.isEmpty()) {
      onTagChange("");
    }
    
    CopyOnWriteArrayList<RepositoryTagListener> listeners;
    listeners = _tagListenerMap.get(tag);
    
    if (listeners != null) {
      for (RepositoryTagListener listener : listeners) {
        listener.onTagChange(tag);
      }
    }
  }
  

  //
  // git tag management
  //

  /**
   * Returns the sha1 stored at the gitTag
   */
  @Override
  abstract public String getRepositoryRootHash();

  /**
   * Writes the sha1 stored at the gitTag
   */
  @Override
  abstract public void setRepositoryRootHash(String repositoryCommitHash);

  //
  // git file management
  //

  /**
   * Returns true if the file exists.
   */
  @Override
  abstract public boolean exists(String sha1);

  /**
   * Returns true if the file is a blob.
   */
  @Override
  abstract public GitType getType(String sha1);

  /**
   * Returns true if the file is a blob.
   */
  @Override
  public final boolean isBlob(String sha1)
  {
    return GitType.BLOB == getType(sha1);
  }

  /**
   * Returns true if the file is a tree
   */
  @Override
  public final boolean isTree(String sha1)
  {
    return GitType.TREE == getType(sha1);
  }

  /**
   * Returns true if the file is a commit
   */
  @Override
  public final boolean isCommit(String sha1)
  {
    return GitType.COMMIT == getType(sha1);
  }

  /**
   * Validates a file, checking that it and its dependencies exist.
   */
  @Override
  public ValidateHashResult validateHash(String name, String sha1)
    throws IOException
  {
    //GitType type = getType(sha1);
    
    GitType type = validateRawHash(sha1);

    if (type == GitType.BLOB) {
      if (log.isLoggable(Level.FINEST))
        log.finest(this + " valid " + type + " " + sha1);

      return new ValidateHashResult(name, sha1, true);
    }
    else if (type == GitType.COMMIT) {
      GitCommit commit = readCommit(sha1);

      if (commit == null) {
        return new ValidateHashResult(name, sha1, false);
      }

      return validateHash(name, commit.getTree()).updateIfTrue(name);
    }
    else if (type == GitType.TREE) {
      GitTree tree = readTree(sha1);

      for (GitTree.Entry entry : tree.entries()) {
        ValidateHashResult result = validateHash(entry.getName(),
                                                 entry.getSha1());
        
        if (! result.isValid()) { 
          if (log.isLoggable(Level.FINE))
            log.fine(this + " invalid " + entry);

          return result;
        }
      }

      if (log.isLoggable(Level.FINEST))
        log.finest(this + " valid " + type + " " + sha1);

      return new ValidateHashResult(name, sha1, true);
    }
    else {
      if (log.isLoggable(Level.FINE))
        log.fine(this + " invalid " + sha1);

      return new ValidateHashResult(name, sha1, false);
    }
  }

  protected GitType validateRawHash(String hash)
  {
    try {
      InputStream is = openRawGitFile(hash);
      
      return GitSystem.validate(hash, is);
    } catch (Exception e) {
      log.warning(this + " validate " + hash + " " + e);
      log.log(Level.FINER, e.toString(), e);
      
      validateRawGitFile(hash);
      
      return null;
    }
  }
  
  /**
   * Adds a path to the repository.  If the path is a directory, 
   * adds the contents recursively.
   */
  @Override
  public String addPath(Path path)
  {
    try {
      return addPathRec(path);
    } catch (IOException e) {
      throw new RepositoryException(e);
    }
  }
  
  private String addPathRec(Path path)
    throws IOException
  {
    if (path.isFile()) {
      long length = path.getLength();
      
      InputStream is = null;
      
      try {
        is = path.openRead();
        
        String hash = addBlob(is, length);
        
        if (hash == null)
          throw new NullPointerException();
        
        return hash;
      } finally {
        IoUtil.close(is);
      }
    }
    else {
      GitTree tree = new GitTree();
      
      for (String fileName : path.list()) {
        Path subPath = path.lookup(fileName);
        
        String subHash = addPathRec(subPath);
        
        tree.addEntry(fileName, 775, subHash);
      }
      
      String hash = addTree(tree);
      
      if (hash == null)
        throw new NullPointerException();
      
      return hash;
    }
  }

  /**
   * Adds a path to the repository.  If the path is a directory, 
   * adds the contents recursively.
   */
  @Override
  public String addArchive(Path path)
  {
    GitCommitJar commit = null;

    try {
      commit = new GitCommitJar(path);
      
      return addArchive(commit);
    } catch (IOException e) {
      throw new RepositoryException(e);
    } finally {
      if (commit != null)
        commit.close();
    }
  }

  /**
   * Adds a path to the repository.  If the path is a directory, 
   * adds the contents recursively.
   */
  public String addArchive(InputStream is)
  {
    GitCommitJar commit = null;

    try {
      commit = new GitCommitJar(is);
      
      return addArchive(commit);
    } catch (IOException e) {
      throw new RepositoryException(e);
    } finally {
      if (commit != null)
        commit.close();
    }
  }

  protected String addArchive(GitCommitJar commit)
    throws IOException
  {
    for (String hash : commit.getCommitList()) {
      GitJarStreamSource gitSource = new GitJarStreamSource(hash, commit);
      
      if (! exists(hash)) {
        InputStream is = gitSource.openInputStream();
        
        try {
          writeRawGitFile(hash, is);
        } catch (IOException e) {
          throw new IOException(commit.findPath(hash) + ":" + hash + ": " + e.getMessage(), e);
        } catch (RuntimeException e) {
          throw new RuntimeException(commit.findPath(hash) + ":" + hash + ": " + e.getMessage(), e);
        } finally {
          is.close();
        }
      }
    }
    
    return commit.getDigest();
  }

  /**
   * Adds a stream to the repository.
   */
  @Override
  public String addBlob(InputStream is, long length)
    throws IOException
  {
    String type = "blob";

    TempOutputStream os = new TempOutputStream();

    String hash = GitSystem.writeData(os, type, is, length);

    writeRawGitFile(hash, os.openInputStream());
    
    return hash;
  }

  /**
   * Opens a stream to a git blob
   */
  @Override
  abstract public InputStream openBlob(String sha1)
    throws IOException;

  /**
   * Reads a git tree from the repository
   */
  @Override
  abstract public GitTree readTree(String treeHash)
    throws IOException;

  /**
   * Adds a git tree to the repository
   */
  @Override
  public String addTree(GitTree tree)
    throws IOException
  {
    TempOutputStream treeOut = new TempOutputStream();

    tree.toData(treeOut);
    
    int treeLength = treeOut.getLength();
    
    InputStream is = treeOut.openRead();
    
    try {
      TempOutputStream os = new TempOutputStream();
      String type = "tree";

      String contentHash = GitSystem.writeData(os, type, is, treeLength);

      writeRawGitFile(contentHash, os.openInputStream());
      
      return contentHash;
    } finally {
      is.close();
    }
  }

  /**
   * Reads a git commit from the repository
   */
  @Override
  abstract public GitCommit readCommit(String commitHash)
    throws IOException;

  /**
   * Adds a git commit to the repository
   */
  @Override
  abstract public String addCommit(GitCommit commit)
    throws IOException;

  /**
   * Opens a stream to the raw git file.
   */
  @Override
  abstract public InputStream openRawGitFile(String contentHash)
    throws IOException;

  /**
   * Writes a raw git file
   */
  @Override
  abstract public void writeRawGitFile(String contentHash, InputStream is)
    throws IOException;

  /**
   * Removes a raw git file
   */
  @Override
  public void validateRawGitFile(String contentHash)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Writes the contents to a stream.
   */
  @Override
  abstract public void writeBlobToStream(String blobHash, OutputStream os);

  /**
   * Expands the repository to the filesystem.
   */
  @Override
  abstract public void expandToPath(String contentHash, Path path);

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _repositoryTag + "]";
  }
}
