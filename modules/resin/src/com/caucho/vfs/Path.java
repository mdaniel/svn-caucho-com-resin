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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import java.io.*;
import java.util.*;

import com.caucho.util.*;

/**
 * A virtual filesystem path, essentially represented by a URL.
 * Its API resembles a combination of  the JDK File object and the URL object.
 *
 * <p>Paths are, in general, given with the canonical file separator of
 * forward slash, '/'.  The filesystems will take care of any necessary
 * translation.
 *
 * <p>Currently available filesystems:
 * <dl>
 * <dt>file:/path/to/file<dd>Java file
 * <dt>http://host:port/path/name?query<dd>HTTP request
 * <dt>tcp://host:port<dd>Raw TCP connection
 * <dt>mailto:user@host?subject=foo&cc=user2<dd>Mail to a user.
 * <dt>log:/group/subgroup/item<dd>Logging based on the configuration file.
 * <dt>stdout:<dd>System.out
 * <dt>stderr:<dd>System.err
 * <dt>null:<dd>The equivalent of /dev/null
 * </dl>
 */
public abstract class Path {
  protected final static L10N L = new L10N(Path.class);

  private static final Integer LOCK = new Integer(0);

  private static final LruCache<PathKey,Path> _pathLookupCache
    = new LruCache<PathKey,Path>(1024);

  private static final PathKey _key = new PathKey();

  protected SchemeMap _schemeMap;

  /**
   * Creates a new Path object.
   *
   * @param root the new Path root.
   */
  protected Path(Path root)
  {
    if (root != null) {
      _schemeMap = root._schemeMap;
      return;
    }
  }

  /**
   * Looks up a new path based on the old path.
   *
   * @param name relative url to the new path
   * @return The new path.
   */
  public final Path lookup(String name)
  {
    return lookup(name, null);
  }

  /**
    * Returns a new path relative to the current one.
    *
    * <p>Path only handles scheme:xxx.  Subclasses of Path will specialize
    * the xxx.
    *
    * @param userPath relative or absolute path, essentially any url.
    * @param newAttributes attributes for the new path.
    *
    * @return the new path or null if the scheme doesn't exist
    */
   public Path lookup(String userPath, Map<String,Object> newAttributes)
   {
     if (newAttributes != null)
       return lookupImpl(userPath, newAttributes);

     /*
     synchronized (_key) {
       _key.init(this, userPath);

       // server/2d33
       Path path = null; //  _pathLookupCache.get(_key);

       if (path != null)
         return path;
     }
     */

     Path path = lookupImpl(userPath, newAttributes);

     // _pathLookupCache.putIfNew(new PathKey(this, userPath), path);

     return path;
   }

  /**
   * Returns a new path relative to the current one.
   *
   * <p>Path only handles scheme:xxx.  Subclasses of Path will specialize
   * the xxx.
   *
   * @param userPath relative or absolute path, essentially any url.
   * @param newAttributes attributes for the new path.
   *
   * @return the new path or null if the scheme doesn't exist
   */
  public Path lookupImpl(String userPath, Map<String,Object> newAttributes)
  {
    if (userPath == null)
      return lookupImpl(getPath(), newAttributes);

    String scheme = scanScheme(userPath);

    if (scheme == null)
      return schemeWalk(userPath, newAttributes, userPath, 0);

    Path path;

    SchemeMap schemeMap = _schemeMap;
    if (schemeMap == null)
      schemeMap = SchemeMap.getLocalSchemeMap();

    // Special case to handle the windows special schemes
    // c:xxx -> file:/c:xxx
    if (CauchoSystem.isWindows()) {
      int length = scheme.length();
      int ch;

      if (length == 1 &&
          ((ch = scheme.charAt(0)) >= 'a' && ch <= 'z' ||
            ch >= 'A' && ch <= 'Z')) {
        path = schemeMap.getScheme("file");

        if (path != null)
          return path.schemeWalk(userPath, newAttributes, "/" + userPath, 0);
      }
    }

    path = schemeMap.getScheme(scheme);

    // assume the foo:bar is a subfile
    if (path == null)
      return schemeWalk(userPath, newAttributes, userPath, 0);

    return path.schemeWalk(userPath, newAttributes,
                           userPath, scheme.length() + 1);
  }

  /**
   * Looks up a path using the local filesystem conventions. e.g. on
   * Windows, a name of 'd:\foo\bar\baz.html' will look up the baz.html
   * on drive d.
   *
   * @param name relative url using local filesystem separators.
   */
  public final Path lookupNative(String name)
  {
    return lookupNative(name, null);
  }
  /**
   * Looks up a native path, adding attributes.
   */
  public Path lookupNative(String name, Map<String,Object> attributes)
  {
    return lookup(name, attributes);
  }

  /**
   * Looks up all the resources matching a name.  (Generally only useful
   * with MergePath.
   */
  public ArrayList<Path> getResources(String name)
  {
    ArrayList<Path> list = new ArrayList<Path>();
    Path path = lookup(name);
    if (path.exists())
      list.add(path);

    return list;
  }

  /**
   * Looks up all the existing resources.  (Generally only useful
   * with MergePath.
   */
  public ArrayList<Path> getResources()
  {
    ArrayList<Path> list = new ArrayList<Path>();

    //if (exists())
      list.add(this);

    return list;
  }

  /**
   * Returns the parent path.
   */
  public Path getParent()
  {
    return this;
  }

  /**
   * Returns the scheme portion of a uri.  Since schemes are case-insensitive,
   * normalize them to lower case.
   */
  protected String scanScheme(String uri)
  {
    int i = 0;
    if (uri == null)
      return null;

    int length = uri.length();
    if (length == 0)
      return null;

    int ch = uri.charAt(0);
    if (ch >= 'a' && ch <= 'z' ||
	ch >= 'A' && ch <= 'Z') {
      for (i = 1; i < length; i++) {
	ch = uri.charAt(i);

	if (ch == ':')
	  return uri.substring(0, i).toLowerCase();

	if (! (ch >= 'a' && ch <= 'z' ||
	       ch >= 'A' && ch <= 'Z' ||
	       ch >= '0' && ch <= '0' ||
	       ch == '+' || ch == '-' || ch == '.'))
	  break;
      }
    }

    return null;
  }

  /**
   * Path-specific lookup.  Path implementations will override this.
   *
   * @param userPath the user's lookup() path.
   * @param newAttributes the attributes for the new path.
   * @param newPath the lookup() path
   * @param offset offset into newPath to start lookup.
   *
   * @return the found path
   */
  abstract protected Path schemeWalk(String userPath,
				     Map<String,Object> newAttributes,
                                     String newPath, int offset);

  /**
   * Returns the full url for the given path.
   */
  public String getURL()
  {
    return escapeURL(getScheme() + ":" + getFullPath());
  }
  
  /**
   * Returns the url scheme
   */
  public abstract String getScheme();
  /**
   * Returns the hostname
   */
  public String getHost()
  {
    throw new UnsupportedOperationException();
  }
  /**
   * Returns the port.
   */
  public int getPort()
  {
    throw new UnsupportedOperationException();
  }
  /**
   * Returns the path.  e.g. for HTTP, returns the part after the
   * host and port.
   */
  public abstract String getPath();

  /**
   * Returns the last segment of the path.
   *
   * <p>e.g. for http://www.caucho.com/products/index.html, getTail()
   * returns 'index.html'
   */
  public String getTail()
  {
    return "";
  }
  /**
   * Returns the query string of the path.
   */
  public String getQuery()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the native representation of the path.
   *
   * On Windows, getNativePath() returns 'd:\\foo\bar.html',
   * getPath() returns '/d:/foo/bar.html'
   */
  public String getNativePath()
  {
    return getFullPath();
  }
  /**
   * Returns the last string used as a lookup, if available.  This allows
   * parsers to give intelligent error messages, with the user's path
   * instead of the whole path.
   *
   * The following will print '../test.html':
   * <code><pre>
   * Path path = Pwd.lookup("/some/dir").lookup("../test.html");
   * System.out.println(path.getUserPath());
   * </pre></code>
   *
   */
  public String getUserPath()
  {
    return getPath();
  }

  /**
   * Sets the user path.  Useful for temporary files caching another
   * URL.
   */
  public void setUserPath(String userPath)
  {
  }
  /**
   * Returns the full path, including the restricted root.
   *
   * <p>For the following, path.getPath() returns '/file.html', while
   * path.getFullPath() returns '/chroot/file.html'.
   * <code><pre>
   * Path chroot = Pwd.lookup("/chroot").createRoot();
   * Path path = chroot.lookup("/file.html");
   * </pre></code>
   */
  public String getFullPath()
  {
    return getPath();
  }

  /**
   * For union paths like MergePath, return the relative path into
   * that path.
   */
  public String getRelativePath()
  {
    return getPath();
  }

  /**
   * Tests if the file exists.
   */
  public boolean exists()
  {
    return false;
  }

  /**
   * Returns the mime-type of the file.
   * <p>Mime-type ignorant filesystems return 'application/octet-stream'
   */
  public String getContentType()
  {
    return "application/octet-stream";
  }

  /**
   * Tests if the path refers to a directory.
   */
  public boolean isDirectory()
  {
    return false;
  }

  /**
   * Tests if the path refers to a file.
   */
  public boolean isFile()
  {
    return false;
  }

  /**
   * Tests if the path is marked as executable
   */
  public boolean isExecutable()
  {
    return false;
  }

  /**
   * Change the executable status of the of the oath.
   *
   * @throws UnsupportedOperationException
   */
  public boolean setExecutable(boolean isExecutable)
  {
    return false;
  }

  /**
   * Tests if the path refers to a symbolic link.
   */
  public boolean isSymbolicLink()
  {
    return false;
  }

  /**
   * Tests if the path refers to a hard link.
   */
  public boolean isHardLink()
  {
    return false;
  }

  /**
   * Tests if the path refers to an object.
   */
  public boolean isObject()
  {
    return false;
  }

  /**
   * Returns the length of the file in bytes.
   * @return 0 for non-files
   */
  public long getLength()
  {
    return 0;
  }

  /**
   * Returns the last modified time of the file.  According to the jdk,
   * this may not correspond to the system time.
   * @return 0 for non-files.
   */
  public long getLastModified()
  {
    return 0;
  }

  public void setLastModified(long time)
  {
  }

  /**
   * Returns the last access time of the file.
   *
   * @return 0 for non-files.
   */
  public long getLastAccessTime()
  {
    return getLastModified();
  }

  /**
   * Returns the create time of the file.
   *
   * @return 0 for non-files.
   */
  public long getCreateTime()
  {
    return getLastModified();
  }

  /**
   * Tests if the file can be read.
   */
  public boolean canRead()
  {
    return false;
  }

  /**
   * Tests if the file can be written.
   */
  public boolean canWrite()
  {
    return false;
  }

  public int getInode()
  {
    return 0;
  }

  public int getGroup()
  {
    return 0;
  }

  /**
   * Tests if the file can be read.
   */
  public boolean canExecute()
  {
    return canRead();
  }

  /**
   * Changes the group
   */
  public boolean changeGroup(int gid)
    throws IOException
  {
    return false;
  }

  /**
   * Changes the group
   */
  public boolean changeGroup(String groupName)
    throws IOException
  {
    return false;
  }

  /**
   * Changes the permissions
   *
   * @return true if successful
   */
  public boolean chmod(int value)
  {
    return false;
  }

  public int getOwner()
  {
    return 0;
  }

  /**
   * Changes the owner
   *
   * @return true if successful
   */
  public boolean changeOwner(int uid)
    throws IOException
  {
    return false;
  }

  /**
   * Changes the owner
   *
   * @return true if successful
   */
  public boolean changeOwner(String ownerName)
    throws IOException
  {
    return false;
  }

  public long getDiskSpaceFree()
  {
    return 0;
  }

  public long getDiskSpaceTotal()
  {
    return 0;
  }

  /**
   * @return The contents of this directory or null if the path does not
   * refer to a directory.
   */
  public String []list() throws IOException
  {
    return new String[0];
  }

  /**
   * Returns a jdk1.2 Iterator for the contents of this directory.
   */
  public Iterator<String> iterator() throws IOException
  {
    return new ArrayIterator(list());
  }

  /**
   * Creates the directory named by this path.
   * @return true if successful.
   */
  public boolean mkdir() throws IOException
  {
    return false;
  }

  /**
   * Creates the directory named by this path and any parent directories.
   * @return true if successful.
   */
  public boolean mkdirs() throws IOException
  {
    return false;
  }

  /**
   * Removes the file or directory named by this path.
   *
   * @return true if successful
   */
  public boolean remove() throws IOException
  {
    return false;
  }

  /**
   * Removes the all files and directories below this path.
   *
   * @return true if successful.
   */
  public boolean removeAll() throws IOException
  {
    if (isDirectory()) {
      String []list = list();

      for (int i = 0; i < list.length; i++) {
        Path subpath = lookup(list[i]);
        subpath.removeAll();
      }
    }

    return remove();
  }

  /**
   * Renames the file or directory to the name given by the path.
   * @return true if successful
   */
  public boolean renameTo(Path path) throws IOException
  {
    return false;
  }

  /**
   * Renames the file or directory to the name given by the path.
   * @return true if successful
   */
  public final boolean renameTo(String path) throws IOException
  {
    return renameTo(lookup(path));
  }

  /**
   * Creates a restricted root, like the Unix chroot call.
   * Restricted roots cannot access schemes, so file:/etc/passwd cannot
   * be used.
   *
   * <p>createRoot is useful for restricting JavaScript scripts without
   * resorting to the dreadfully slow security manager.
   */
  public Path createRoot()
  {
    return createRoot(SchemeMap.getNullSchemeMap());
  }

  public Path createRoot(SchemeMap schemeMap)
  {
    throw new UnsupportedOperationException("createRoot");
  }

  /**
   * Binds the context to the current path.  Later lookups will return
   * the new context instead of the current path.  Essentially, this is a
   * software symbolic link.
   */
  public void bind(Path context)
  {
    throw new UnsupportedOperationException("bind");
  }

  /**
   * unbinds a link.
   */
  public void unbind()
  {
    throw new UnsupportedOperationException("unbind");
  }

  /**
   * Gets the object at the path.  Normal filesystems will generally
   * typically return null.
   *
   * <p>A bean filesystem or a mime-type aware filesystem could deserialize
   * the contents of the file.
   */
  public Object getValue() throws Exception
  {
    throw new UnsupportedOperationException("getValue");
  }

  /**
   * Sets the object at the path.
   *
   * <p>Normal filesystems will generally do nothing. However, a bean
   * filesystem or a mime-type aware filesystem could serialize the object
   * and store it.
   */
  public void setValue(Object obj) throws Exception
  {
    throw new UnsupportedOperationException("setValue");
  }

  /**
   * Gets an attribute of the object.
   */
  public Object getAttribute(String name) throws IOException
  {
    return null;
  }

  /**
   * Returns a iterator of all attribute names set for this object.
   * @return null if path has no attributes.
   */
  public Iterator getAttributeNames() throws IOException
  {
    return null;
  }

  /**
   * Opens a resin ReadStream for reading.
   */
  public final ReadStream openRead() throws IOException
  {
    StreamImpl impl = openReadImpl();
    impl.setPath(this);

    return new ReadStream(impl);
  }

  /**
   * Opens a resin WriteStream for writing.
   */
  public final WriteStream openWrite() throws IOException
  {
    StreamImpl impl = openWriteImpl();
    impl.setPath(this);
    return new WriteStream(impl);
  }

  /**
   * Opens a resin ReadWritePair for reading and writing.
   *
   * <p>A chat channel, for example, would open its socket using this
   * interface.
   */
  public ReadWritePair openReadWrite() throws IOException
  {
    StreamImpl impl = openReadWriteImpl();
    impl.setPath(this);
    WriteStream writeStream = new WriteStream(impl);
    ReadStream readStream = new ReadStream(impl, writeStream);
    return new ReadWritePair(readStream, writeStream);
  }

  /**
   * Opens a resin ReadWritePair for reading and writing.
   *
   * <p>A chat channel, for example, would open its socket using this
   * interface.
   *
   * @param is pre-allocated ReadStream to be initialized
   * @param os pre-allocated WriteStream to be initialized
   */
  public void openReadWrite(ReadStream is, WriteStream os) throws IOException
  {
    StreamImpl impl = openReadWriteImpl();
    impl.setPath(this);

    os.init(impl);
    is.init(impl, os);
  }

  /**
   * Opens a resin stream for appending.
   */
  public WriteStream openAppend() throws IOException
  {
    StreamImpl impl = openAppendImpl();
    return new WriteStream(impl);
  }

  /**
   * Opens a random-access stream.
   */
  public RandomAccessStream openRandomAccess() throws IOException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Creates the file named by this Path and returns true if the
   * file is new.
   */
  public boolean createNewFile() throws IOException
  {
    synchronized (LOCK) {
      if (! exists()) {
	WriteStream s = openWrite();
	s.close();
	return true;
      }
    }

    return false;
  }

  /**
   * Creates a unique temporary file as a child of this directory.
   *
   * @param prefix filename prefix
   * @param suffix filename suffix, defaults to .tmp
   * @return Path to the new file.
   */
  public Path createTempFile(String prefix, String suffix) throws IOException
  {
    if (prefix == null || prefix.length () < 3)
      throw new IllegalArgumentException("prefix too short: " + prefix);

    if (suffix == null)
      suffix = ".tmp";

    synchronized (LOCK) {
      for (int i = 0; i < 32768; i++) {
        int r = Math.abs((int) RandomUtil.getRandomLong());
        Path file = lookup(prefix + r + suffix);

        if (file.createNewFile())
          return file;
      }
    }

    throw new IOException("cannot create temp file");
  }

  /**
   * Utility to write the contents of this path to the destination stream.
   *
   * @param os destination stream.
   */
  public void writeToStream(OutputStream os)
    throws IOException
  {
    StreamImpl is = openReadImpl();
    TempBuffer tempBuffer = TempBuffer.allocate();
    try {
      byte []buffer = tempBuffer.getBuffer();
      int length = buffer.length;
      int len;

      while ((len = is.read(buffer, 0, length)) > 0)
	os.write(buffer, 0, len);
    } finally {
      TempBuffer.free(tempBuffer);

      is.close();
    }
  }

  /**
   * Utility to write the contents of this path to the destination stream.
   *
   * @param os destination stream.
   */
  public void writeToStream(OutputStreamWithBuffer os)
    throws IOException
  {
    StreamImpl is = openReadImpl();

    try {
      byte []buffer = os.getBuffer();
      int offset = os.getBufferOffset();
      int length = buffer.length;

      while (true) {
	int sublen = length - offset;

	if (sublen <= 0) {
	  buffer = os.nextBuffer(offset);
	  offset = 0;
	  sublen = length;
	}

	sublen = is.read(buffer, offset, sublen);

	if (sublen <= 0) {
	  os.setBufferOffset(offset);
	  return;
	}

	offset += sublen;
      }
    } finally {
      is.close();
    }
  }

  /**
   * Returns the crc64 code.
   */
  public long getCrc64()
  {
    try {
      if (isDirectory()) {
	String []list = list();

	long digest = 0;

	for (int i = 0; i < list.length; i++) {
	  digest = Crc64.generate(digest, list[i]);
	}

	return digest;
      }
      else if (canRead()) {
	ReadStream is = openRead();

	try {
	  long digest = 0;

	  int ch;

	  while ((ch = is.read()) >= 0) {
	    digest = Crc64.next(digest, ch);
	  }

	  return digest;
	} finally {
	  is.close();
	}
      }
      else {
	return -1; // Depend requires -1
      }
    } catch (IOException e) {
      // XXX: log
      e.printStackTrace();

      return -1;
    }
  }

  /**
   * Returns the object at this path.  Normally, only paths like JNDI
   * will support this.
   */
  public Object getObject()
    throws IOException
  {
    throw new UnsupportedOperationException(getScheme() + ": doesn't support getObject");
  }

  /**
   * Sets the object at this path.  Normally, only paths like JNDI
   * will support this.
   */
  public void setObject(Object obj)
    throws IOException
  {
    throw new UnsupportedOperationException(getScheme() + ": doesn't support setObject");
  }

  public int hashCode()
  {
    return toString().hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof Path))
      return false;
    else
      return getPath().equals(((Path) o).getPath());
  }

  public String toString()
  {
    return getPath();
  }

  protected StreamImpl openReadImpl() throws IOException
  {
    throw new UnsupportedOperationException("openRead:" + getClass().getName());
  }

  protected StreamImpl openWriteImpl() throws IOException
  {
    throw new UnsupportedOperationException("openWrite:" + getClass().getName());
  }

  protected StreamImpl openReadWriteImpl() throws IOException
  {
    throw new UnsupportedOperationException("openReadWrite:" + getClass().getName());
  }

  protected StreamImpl openAppendImpl() throws IOException
  {
    throw new UnsupportedOperationException("openAppend:" + getClass().getName());
  }

  protected static String escapeURL(String rawURL)
  {
    CharBuffer cb = null;
    int length = rawURL.length();

    for (int i = 0; i < length; i++) {
      char ch = rawURL.charAt(i);

      switch (ch) {
      case ' ':
	if (cb == null) {
	  cb = new CharBuffer();
	  cb.append(rawURL, 0, i);
	}
	cb.append("%20");
	break;
	
      case '#':
	if (cb == null) {
	  cb = new CharBuffer();
	  cb.append(rawURL, 0, i);
	}
	cb.append("%23");
	break;

      case '%':
	if (cb == null) {
	  cb = new CharBuffer();
	  cb.append(rawURL, 0, i);
	}
	cb.append("%25");
	break;

      default:
	if (cb != null)
	  cb.append(ch);
	break;
      }
    }

    if (cb != null)
      return cb.toString();
    else
      return rawURL;
  }

  private class ArrayIterator implements Iterator<String> {
    String []list;
    int index;

    public boolean hasNext() { return index < list.length; }
    public String next() { return index < list.length ? list[index++] : null; }
    public void remove() { throw new UnsupportedOperationException(); }

    ArrayIterator(String []list)
    {
      this.list = list;
      index = 0;
    }
  }

  static class PathKey {
    private Path _parent;
    private String _lookup;

    PathKey()
    {
    }

    PathKey(Path parent, String lookup)
    {
      _parent = parent;
      _lookup = lookup;
    }

    void init(Path parent, String lookup)
    {
      _parent = parent;
      _lookup = lookup;
    }

    public int hashCode()
    {
      return System.identityHashCode(_parent) * 65521 + _lookup.hashCode();
    }

    public boolean equals(Object test)
    {
      if (! (test instanceof PathKey))
        return false;

      PathKey key = (PathKey) test;

      return (_parent == key._parent && _lookup.equals(key._lookup));
    }
  }
}
