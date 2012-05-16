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

package com.caucho.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.util.IoUtil;
import com.caucho.vfs.GoogleStoreInode.FileType;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileReadChannel;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.files.GSFileOptions.GSFileOptionsBuilder;

/**
 * FilePath implements the native filesystem.
 */
public class GoogleStorePath extends FilesystemPath {
  private static Logger log = Logger.getLogger(GoogleStorePath.class.getName());

  private static final String QUERCUS_ROOT_PATH = "caucho-quercus-root";

  private final FileService _fileService;
  private final String _bucket;

  private GoogleStorePath _parent;
  private GoogleStoreInode _inode;

  /**
   * @param path canonical path
   */
  protected GoogleStorePath(FilesystemPath root, String userPath, String path)
  {
    super(root, userPath, path);

    _separatorChar = getFileSeparatorChar();

    GoogleStorePath gsRoot = (GoogleStorePath) root;

    _fileService = gsRoot._fileService;
    _bucket = gsRoot._bucket;
  }

  public GoogleStorePath()
  {
    this(null);
  }

  public GoogleStorePath(String bucket)
  {
    super(null, "/", "/");

    try {
      _root = this;

      _fileService = FileServiceFactory.getFileService();

      if (bucket == null) {
        bucket = _fileService.getDefaultGsBucketName();
      }

      _bucket = bucket;

      _inode = new GoogleStoreInode("", FileType.DIRECTORY, 0, 0);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    if (readDirMap() == null) {
      _inode.setDirMap(new HashMap<String,GoogleStoreInode>());

      writeDir(_inode.getDirMap());
    }
  }

  /**
   * Lookup the actual path relative to the filesystem root.
   *
   * @param userPath the user's path to lookup()
   * @param attributes the user's attributes to lookup()
   * @param path the normalized path
   *
   * @return the selected path
   */
  @Override
  public Path fsWalk(String userPath,
                     Map<String,Object> attributes,
                     String path)
  {
    if ("".equals(path) || "/".equals(path)) {
      return _root;
    }
    else {
      return new GoogleStorePath(_root, userPath, path);
    }
  }

  @Override
  public GoogleStorePath getParent()
  {
    if (_parent == null) {
      _parent = (GoogleStorePath) super.getParent();
    }

    return _parent;
  }
  
  private boolean isRoot()
  {
    return getTail().equals("");
  }

  /**
   * Returns true if the path itself is cacheable
   */
  @Override
  protected boolean isPathCacheable()
  {
    return true;
  }

  @Override
  public String getScheme()
  {
    return "file";
  }

  /**
   * Returns the full url for the given path.
   */
  @Override
  public String getURL()
  {
    return escapeURL(getScheme() + ":" + getFullPath());
  }

  @Override
  public boolean exists()
  {
    GoogleStoreInode inode = getGsInode();

    return inode != null && inode.exists();
  }

  @Override
  public int getMode()
  {
    int perms = 0;

    if (isDirectory()) {
      perms += 01000;
      perms += 0111;
    }

    if (canRead())
      perms += 0444;

    if (canWrite())
      perms += 0220;

    return perms;
  }

  @Override
  public boolean isDirectory()
  {
    GoogleStoreInode inode = getGsInode();

    return inode != null && inode.isDirectory();
  }

  @Override
  public boolean isFile()
  {
    GoogleStoreInode inode = getGsInode();

    return inode != null && inode.isFile();
  }

  @Override
  public long getLength()
  {
    GoogleStoreInode inode = getGsInode();

    return inode != null ? inode.getLength() : -1;
  }

  @Override
  public long getLastModified()
  {
    long value = getLastModified2();

    return value;
  }

  public long getLastModified2()
  {
    GoogleStoreInode inode = getGsInode();

    if (inode == null) {
      return -1;
    }

    return inode.getLastModified();
  }

  @Override
  public void setLastModified(long time)
  {
    GoogleStoreInode inode = getGsInode();

    inode.setLastModified(time);

    writeGsInode(inode);
  }

  @Override
  public boolean canRead()
  {
    return exists();
  }

  @Override
  public boolean canWrite()
  {
    return exists();
  }

  /**
   * Returns a list of files in the directory.
   */
  @Override
  public String []list() throws IOException
  {
    HashMap<String,GoogleStoreInode> dir = getDir();

    if (dir == null) {
      return null;
    }
    
    ArrayList<String> names = new ArrayList<String>();
    
    for (GoogleStoreInode inode : dir.values()) {
      if (inode.exists()) {
        names.add(inode.getName());
      }
    }

    String []list = new String[names.size()];
    names.toArray(list);

    Arrays.sort(list);

    return list;
  }

  @Override
  public boolean mkdir()
    throws IOException
  {
    if (exists()) {
      return false;
    }

    Path parent = getParent();

    if (parent.isFile()) {
      return false;
    }

    GoogleStoreInode inode
      = new GoogleStoreInode(getTail(), FileType.DIRECTORY, 0, 0);

    writeGsInode(inode);

    return true;
  }

  @Override
  public boolean mkdirs()
    throws IOException
  {
    if (exists()) {
      return false;
    }

    Path parent = getParent();

    parent.mkdirs();

    if (! parent.isDirectory()) {
      return false;
    }

    return mkdir();
  }

  @Override
  public boolean remove()
  {
    GoogleStorePath parent = (GoogleStorePath) getParent();
    
    if (! parent.isDirectory()) {
      return false;
    }
    
    GoogleStoreInode inode = parent.getGsInode(getTail());
    
    if (inode == null || ! inode.exists()) {
      return false;
    }
    
    inode = new GoogleStoreInode(getTail(), FileType.NONE, -1, -1);
    
    parent.updateDir(inode);

    return true;
  }

  /*
  @Override
  public boolean truncate(long length)
    throws IOException
  {
    File file = getFile();

    clearStatusCache();

    FileOutputStream fos = new FileOutputStream(file);

    try {
      fos.getChannel().truncate(length);

      return true;
    } finally {
      fos.close();
    }
  }
  */

  /*
  @Override
  public boolean renameTo(Path path)
  {
    if (! (path instanceof GoogleStorePath))
      return false;

    GoogleStorePath file = (GoogleStorePath) path;

    clearStatusCache();
    file.clearStatusCache();

    return this.getFile().renameTo(file.getFile());
  }
  */

  /**
   * Returns the stream implementation for a read stream.
   */
  @Override
  public StreamImpl openReadImpl() throws IOException
  {
    try {
      if (! isRoot()) {
        GoogleStoreInode inode = getParent().getGsInode(getTail());
        
        if (inode == null || ! inode.exists()) {
          // php/8134
          throw new FileNotFoundException(getFullPath());
        }
      }
      
      AppEngineFile file = getGsFile();
      // AppEngineFile file = getBlobFile();

      boolean isLock = false;
      FileReadChannel is = _fileService.openReadChannel(file, isLock);

      return new GoogleStoreReadStream(this, is);
    } catch (FileNotFoundException e) {
      throw e;
    } catch (IOException e) {
      FileNotFoundException e1 = new FileNotFoundException(getURL() + ": " + e);
      e1.initCause(e);

      throw e1;
    }
  }

  @Override
  public StreamImpl openWriteImpl() throws IOException
  {
    // System.out.println("XX-WRITE: " + getFullPath());

    GSFileOptionsBuilder builder = new GSFileOptionsBuilder();
    builder.setMimeType("application/octet-stream");
    builder.setBucket(_bucket);

    String key = getFullPath();

    key = key.substring(1);

    if (key.equals("")) {
      key = QUERCUS_ROOT_PATH;
    }

    builder.setKey(key);

    AppEngineFile file = _fileService.createNewGSFile(builder.build());
    // AppEngineFile file = getGsFile();
    // AppEngineFile file = getBlobFile();

    boolean isLock = true;
    FileWriteChannel os = _fileService.openWriteChannel(file, isLock);
    return new GoogleStoreWriteStream(this, os, _inode);
  }

  @Override
  public RandomAccessStream openFileRandomAccess() throws IOException
  {
    // System.out.println("XX-RANDOM_ACCESS: " + getFullPath());
    return new GoogleStoreRandomAccessStream(this, openWriteImpl());
  }

  @Override
  protected Path copy()
  {
    return new GoogleStorePath(getRoot(), getUserPath(), getPath());
  }

  @Override
  public String getNativePath()
  {
    String fullPath = getFullPath();

    if ("".equals(fullPath) || "/".equals(fullPath)) {
      fullPath = "/" + QUERCUS_ROOT_PATH;
    }

    return "/gs/" + _bucket + fullPath;
    
    //return "/blobstore" + fullPath;
  }

  private GoogleStoreInode getGsInode(String name)
  {
    HashMap<String,GoogleStoreInode> dirMap = getDir();

    GoogleStoreInode gsInode = null;
    if (dirMap != null) {
      gsInode = dirMap.get(name);
    }

    if (gsInode == null) {
      gsInode = new GoogleStoreInode(name, FileType.NONE, -1, -1);
    }

    return gsInode;
  }

  private HashMap<String,GoogleStoreInode> getDir()
  {
    if (! isDirectory()) {
      return null;
    }

    if (_inode.getDirMap() == null) {
      _inode.setDirMap(readDirMap());
    }

    return _inode.getDirMap();
  }

  private HashMap<String,GoogleStoreInode> readDirMap()
  {
    ReadStream is = null;

    try {
      is = openRead();

      Hessian2Input hIn = new Hessian2Input(is);

      HashMap map = (HashMap) hIn.readObject();

      hIn.close();

      return map;
    } catch (FileNotFoundException e) {
      log.log(Level.ALL, e.toString(), e);

      return null;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);

      return null;
    } finally {
      IoUtil.close(is);
    }
  }

  void updateDir(GoogleStoreInode inode)
  {
    HashMap<String,GoogleStoreInode> map = readDirMap();

    if (map == null) {
      map = new HashMap<String,GoogleStoreInode>();
    }

    map.put(inode.getName(), inode);

    GoogleStoreInode dirInode = _inode;

    if (dirInode == null || ! dirInode.exists() || ! dirInode.isDirectory()) {
      dirInode = new GoogleStoreInode(getTail(), FileType.DIRECTORY, 0, 0);

      if (! equals(_root)) {
        writeGsInode(dirInode);
      }
      else {
        setGsInode(dirInode);
      }
    }

    dirInode.setDirMap(map);

    writeDir(map);
  }

  private void writeDir(HashMap<String,GoogleStoreInode> map)
  {
    WriteStream out = null;
    try {
      out = openWrite();

      Hessian2Output hOut = new Hessian2Output(out);

      hOut.writeObject(map);

      hOut.close();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      IoUtil.close(out);
    }
  }

  void setGsInode(GoogleStoreInode inode)
  {
    _inode = inode;
  }

  void writeGsInode(GoogleStoreInode inode)
  {
    clearStatusCache();

    GoogleStoreInode oldInode = _inode;

    setGsInode(inode);

    if (this == _root) {
    }
    else if (oldInode == null
             || ! oldInode.isDirectory()
             || ! getParent().isDirectory()) {
      GoogleStorePath parent = (GoogleStorePath) getParent();

      parent.updateDir(inode);
    }
  }

  private GoogleStoreInode getGsInode()
  {
    if (_inode == null) {
      _inode = getParent().getGsInode(getTail());
    }

    return _inode;
  }

  public AppEngineFile getGsFile()
  {
    String path = getNativePath();

    AppEngineFile file = new AppEngineFile(path);

    return file;
  }

  public AppEngineFile getBlobFile()
  {
    String path = getNativePath();

    AppEngineFile file = new AppEngineFile(path);

    return file;
  }

  @Override
  public int hashCode()
  {
    return getFullPath().hashCode();
  }

  @Override
  public boolean equals(Object b)
  {
    if (this == b)
      return true;

    if (! (b instanceof GoogleStorePath))
      return false;

    GoogleStorePath file = (GoogleStorePath) b;

    return getFullPath().equals(file.getFullPath());
  }

  @Override
  public String toString()
  {
    return getURL();
  }
}
