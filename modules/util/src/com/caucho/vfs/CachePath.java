/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import com.caucho.util.CacheListener;
import com.caucho.util.LruCache;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * A cache wrapper over a path.  All files are automatically cached.
 * Once the cache is created, you use the file just like any other Path:
 *
 * <code><pre>
 * Path root = new CachePath(Vfs.lookup("test/cacheable"));
 *
 * ReadStream is = root.lookup("test.file").openRead();
 * ...
 * is.close();
 * </pre></code>
 *
 * <p>The cache size is governed by two limits: memory size and number of
 * entries.  The size of the disk is considered infinite, and it limited
 * only by the number of entries.
 *
 * <p>The cache entries are stored in an LRU with a capacity of the number
 * of entries.  If loading a new file pushes the memory size over the limit,
 * the old files are dropped from the memory cache.
 */
public class CachePath extends FilesystemPath {
  private final static int BLOCK_SIZE = 1024;
  
  private Path _backingRoot;
  private LruCache<String,Cache> _cache;
  private long _maxSize;
  private long _maxEntrySize;
  private long _size;
  
  private Path _file;
  private Cache _item;
  private boolean _removeOnRelease;

  // statistics
  private long _readTotalCount;
  private long _readHitCount;

  /**
   * Creates a new cache.
   *
   * @param root underlying backing store.
   * @param entries the number of entries in the cache.
   * @param capacity total size in bytes allowed in the memory cache.
   */
  public CachePath(Path root, int entries, long capacity)
  {
    super(null, "/", "/");
    _root = this;

    _backingRoot = root.createRoot();
    _cache = new LruCache<String,Cache>(entries);
    _maxSize = capacity;
    _maxEntrySize = _maxSize / 64;
    _maxEntrySize += BLOCK_SIZE - 1;
    _maxEntrySize -= _maxEntrySize % BLOCK_SIZE;
    
    if (_maxEntrySize > 1024 * 1024)
      _maxEntrySize = 1024 * 1024;

    clear();
  }

  /**
   * Internal creation of a path for the vfs.
   *
   * @param root the root path (i.e. the cache root)
   * @param userPath the path the application used in the lookup
   * @param path canonical path
   */
  protected CachePath(FilesystemPath root, String userPath, String path)
  {
    super(root, userPath, path);
  }

  /**
   * If set, files bumped from the cache are removed from the underlying
   * filesystem.
   *
   * @param remove if true, remove the underlying file on a release.
   */
  public void setRemoveOnRelease(boolean remove)
  {
    ((CachePath) _root)._removeOnRelease = remove;
  }

  /**
   * Looks up the cache entry for a given path.
   *
   * @param userPath the string given by the user in lookup()
   * @param attributes the user's initialization attributes
   * @param path the canonical path name
   *
   * @return a cache path entry
   */
  public Path fsWalk(String userPath,
			Map<String,Object> attributes,
			String path)
  {
    return new CachePath(_root, userPath, path);
  }

  /**
   * The scheme for the path is cache:
   */
  public String getScheme()
  {
    return "cache";
  }

  /**
   * Clear the memory cache.
   */
  public void clear()
  {
    CachePath cRoot = (CachePath) _root;
    
    synchronized (cRoot) {
      cRoot._cache.clear();
      cRoot._size = 0;
    }
  }

  /**
   * Returns true if the underlying file exists.
   */
  public boolean exists()
  {
    return getFile().exists();
  }

  public boolean isDirectory()
  {
    return getFile().isDirectory();
  }

  public boolean isFile()
  {
    return getFile().isFile();
  }

  public long getLength()
  {
    return getFile().getLength();
  }

  public long getLastModified()
  {
    return getFile().getLastModified();
  }

  public void setLastModified(long time)
  {
    getFile().setLastModified(time);
  }

  public boolean canRead()
  {
    return getFile().canRead();
  }

  public boolean canWrite()
  {
    return getFile().canWrite();
  }
  
  public String []list() throws IOException
  {
    return getFile().list();
  }
  
  public boolean mkdir()
    throws IOException
  {
    return getFile().mkdir();
  }
  
  public boolean mkdirs()
    throws IOException
  {
    return getFile().mkdirs();
  }
  
  public boolean remove()
    throws IOException
  {
    if (getFile().remove()) {
      CachePath root = (CachePath) _root;
      root._cache.remove(getPath());

      return true;
    }
    else
      return false;
  }
  
  public boolean renameTo(Path path)
    throws IOException
  {
    if (getFile().renameTo(path)) {
      CachePath root = (CachePath) _root;
      root._cache.remove(getPath());

      return true;
    }
    else
      return false;
  }

  /**
   * Write the contents of the cache item to the output stream.
   *
   * @param os destination output stream.
   */
  public void writeToStream(OutputStream os) throws IOException
  {
    CachePath root = (CachePath) _root;
    LruCache<String,Cache> cache = root._cache;

    String path = getPath();
    
    if (_item == null)
      _item = cache.get(path);

    if (_item != null) {
      TempBuffer head = _item.getHead();

      if (head != null) {
        for (; head != null; head = head.getNext())
          os.write(head.getBuffer(), 0, head.getLength());
        return;
      }
    }

    Path file = getFile();
    long length = file.getLength();

    if (length <= root._maxEntrySize && length > 0) {
      ReadStream is = file.openRead();
      TempBuffer head;

      try {
        head = copyFromStream(is, os, length);
      } finally {
        is.close();
      }

      _item = new Cache(root, file, head);

      synchronized (root) {
        long size = _item.getSize();
        root._size += _item.getSize();
        cache.put(path, _item);
      }

      int i = 16;
      while (root._maxSize < root._size && i-- > 0) {
        root._cache.removeTail();
      }
    }
    else 
      getFile().writeToStream(os);
  }

  /**
   * Opens a read stream to the path.  When the file is in the memory
   * cache, the read stream returns the memory.  Otherwise it reads from
   * the backing file.
   */
  public StreamImpl openReadImpl() throws IOException
  {
    CachePath root = (CachePath) _root;
    LruCache<String,Cache> cache = root._cache;

    String path = getPath();
    Cache item = cache.get(path);
    
    root._readTotalCount++;
    if (item != null) {
      root._readHitCount++;
      TempReadStream rs = new TempReadStream(item.getHead());
      rs.setFreeWhenDone(false);
      return rs;
    }
    
    Path file = getFile();
    long length = file.getLength();

    if (length <= root._maxEntrySize && length > 0) {
      try {
        ReadStream is = file.openRead();
	TempBuffer head = null;
	try {
	  head = copyFromStream(is, null, length);
	} finally {
	  is.close();
	}

        item = new Cache((CachePath) root, file, head);
      
        synchronized (root) {
          root._size += item.getSize();
          root._cache.put(path, item);
        }
      
        while (root._size > root._maxSize) {
          root._cache.removeTail();
        }

        TempReadStream rs = new TempReadStream(head);
        rs.setFreeWhenDone(false);
        return rs;
      } catch (IOException e) {
      }
    }
    
    return file.openReadImpl();
  }

  private TempBuffer copyFromStream(ReadStream is, OutputStream os,
                                    long length)
    throws IOException
  {
    TempBuffer head = new TempBuffer(BLOCK_SIZE);
    TempBuffer tail = head;
    int len;

    while ((len = is.readAll(tail.getBuffer(), 0, tail.getCapacity())) > 0) {
      length -= len;

      tail.setLength(len);
      if (os != null)
        os.write(tail.getBuffer(), 0, len);

      if (length > 0) {
        tail.setNext(new TempBuffer(BLOCK_SIZE));
        tail = tail.getNext();
      }
      else
        break;
    }

    return head;
  }

  public StreamImpl openWriteImpl() throws IOException
  {
    String path = getPath();
    LruCache<String,Cache> cache = ((CachePath) _root)._cache;
    cache.remove(path);
    return getFile().openWriteImpl();
  }

  public StreamImpl openAppendImpl() throws IOException
  {
    return getFile().openAppendImpl();
  }

  public StreamImpl openReadWriteImpl() throws IOException
  {
    return getFile().openReadWriteImpl();
  }

  /**
   * Returns the hash code, which is based on the underlying file.
   */
  public int hashCode()
  {
    return getFile().hashCode();
  }

  public boolean equals(Object b)
  {
    if (b instanceof CachePath) {
      CachePath test = (CachePath) b;

      return getFile().equals(test.getFile());
    }
    else
      return getFile().equals(b);
  }

  /**
   * Returns the underlying file.
   */
  private Path getFile()
  {
    if (_file == null)
      _file = ((CachePath) _root)._backingRoot.lookup(getPath());
    
    return _file;
  }

  static class Cache implements CacheListener {
    private CachePath _root;
    private Path _path;
    private TempBuffer _head;
    private long _size;

    Cache(CachePath root, Path path, TempBuffer head)
    {
      _root = root;
      _path = path;
      _head = head;
      
      for (TempBuffer ptr = head; ptr != null; ptr = ptr.getNext())
	_size += ptr.getCapacity();
    }

    TempBuffer getHead()
    {
      return _head;
    }

    long getSize()
    {
      return _size;
    }

    public void removeEvent()
    {
      synchronized (_root) {
	_root._size -= _size;
        _size = 0;
      }

      // Don't free because the entry might be in use
      _head = null;

      if (_root._removeOnRelease) {
        try {
          _path.remove();
        } catch (IOException e) {
        }
      }
    }
  }
}
