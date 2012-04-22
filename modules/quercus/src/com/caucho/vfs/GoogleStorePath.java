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

import java.io.IOException;
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

  private final FileService _fileService;
  private final String _bucket;

  private GoogleStorePath _parent;
  private GoogleStoreInode _inode;
  private HashMap<String,GoogleStoreInode> _dirMap;

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

      if (bucket == null)
        bucket = _fileService.getDefaultGsBucketName();

      _bucket = bucket;
    } catch (IOException e) {
      throw new IllegalStateException(e);
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
    return new GoogleStorePath(_root, userPath, path);
  }
  
  @Override
  public GoogleStorePath getParent()
  {
    if (_parent == null) {
      _parent = (GoogleStorePath) super.getParent();
    }
    
    return _parent;
  }

  /**
   * Returns true if the path itself is cacheable
   */
  @Override
  protected boolean isPathCacheable()
  {
    return true;
  }

  public String getScheme()
  {
    return "gsfile";
  }

  /**
   * Returns the full url for the given path.
   */
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
    
    return inode != null && inode.isDirectory();
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
    GoogleStoreInode inode = getGsInode();
    
    return inode != null ? inode.getLastModified() : -1;
  }

  /*
  @Override
  public void setLastModified(long time)
  {
    getFile().setLastModified(time);
  }
  */

  @Override
  public boolean canRead()
  {
    return isFile();
  }

  @Override
  public boolean canWrite()
  {
    return isFile();
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

    String []list = new String[dir.size()];
    dir.keySet().toArray(list);
      
    Arrays.sort(list);

    return list;
  }

  /*
  public boolean mkdir()
    throws IOException
  {
    boolean value = getFile().mkdir();

    if (! value && ! getFile().isDirectory())
      throw new IOException("cannot create directory");

    return value;
  }
  */

  /*
  @Override
  public boolean mkdirs()
    throws IOException
  {
    File file = getFile();

    boolean value;

    synchronized (file) {
      value = file.mkdirs();
    }

    clearStatusCache();

    if (! value && ! file.isDirectory())
      throw new IOException("Cannot create directory: " + getFile());

    return value;
  }
  */

  /*
  @Override
  public boolean remove()
  {
    if (getFile().delete()) {
      clearStatusCache();

      return true;
    }

    return false;
  }
  */

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
    AppEngineFile file = getGsFile();
    
    boolean isLock = true;
    FileReadChannel is = _fileService.openReadChannel(file, isLock);

    return new GoogleStoreReadStream(this, is);
  }

  @Override
  public StreamImpl openWriteImpl() throws IOException
  {
    GSFileOptionsBuilder builder = new GSFileOptionsBuilder();
    builder.setMimeType("binary/octet-stream");
    builder.setBucket(_bucket);
    builder.setKey(getFullPath().substring(1));
    
    AppEngineFile file = _fileService.createNewGSFile(builder.build());
    
    boolean isLock = true;
    FileWriteChannel os = _fileService.openWriteChannel(file, isLock);

    return new GoogleStoreWriteStream(this, os);
  }
  
  @Override
  protected Path copy()
  {
    return new GoogleStorePath(getRoot(), getUserPath(), getPath());
  }
  
  @Override
  public String getNativePath()
  {
    return "/gs/" + _bucket + getFullPath();
  }
  
  private GoogleStoreInode getGsInode(String name)
  {
    HashMap<String,GoogleStoreInode> dirMap = getDir();
    
    if (dirMap != null)
      return dirMap.get(name);
    else
      return null;
  }
  
  private HashMap<String,GoogleStoreInode> getDir()
  {
    if (_dirMap == null) {
      _dirMap = readDirMap();
    }
    
    return _dirMap;
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
    
    if (dirInode == null || ! dirInode.exists()) {
      dirInode = new GoogleStoreInode(getTail(), FileType.DIRECTORY, 0, 0);
      
      if (! equals(_root)) {
        writeGsInode(dirInode);
      }
      else {
        setGsInode(dirInode);
      }
    }
    
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
    GoogleStoreInode oldInode = _inode;
    
    setGsInode(inode);
    
    if (oldInode == null || ! oldInode.isDirectory()) {
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
    return new AppEngineFile(getNativePath());
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
