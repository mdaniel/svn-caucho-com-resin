/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Enumeration;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.util.jar.JarFile;
import java.util.jar.Manifest;

import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.lang.ref.SoftReference;

import com.caucho.util.LruCache;
import com.caucho.util.Alarm;
import com.caucho.util.CacheListener;

import com.caucho.log.Log;

import com.caucho.loader.EnvironmentLocal;

import com.caucho.make.PersistentDependency;
import com.caucho.make.CachedDependency;

/**
 * Jar is a cache around a jar file to avoid scanning through the whole
 * file on each request.
 *
 * <p>When the Jar is created, it scans the file and builds a directory
 * of the Jar entries.
 */
public class Jar implements CacheListener {
  private static final Logger log = Log.open(Jar.class);
  
  private static LruCache<Path,Jar> _jarCache;

  private static EnvironmentLocal<Integer> _jarSize =
    new EnvironmentLocal<Integer>("caucho.vfs.jar-size");
  
  private Path _backing;

  private JarDepend _depend;
  
  // saved last modified time
  private long _lastModified;
  // saved length
  private long _length;
  // last time the file was checked
  private long _lastTime;

  // cached zip file to read jar entries
  private SoftReference<JarFile> _jarFileRef;
  // last time the zip file was modified
  private long _jarLastModified;

  // file to be closed
  private SoftReference<JarFile> _closeJarFileRef;

  /**
   * Creates a new Jar.
   *
   * @param path canonical path
   */
  private Jar(Path backing)
  {
    _backing = backing;
  }

  /**
   * Return a Jar for the path.  If the backing already exists, return
   * the old jar.
   */
  static Jar create(Path backing)
  {
    if (_jarCache == null) {
      int size = 256;

      Integer iSize = _jarSize.get();

      if (iSize != null)
        size = iSize.intValue();
      
      _jarCache = new LruCache<Path,Jar>(size);
    }
    
    synchronized (_jarCache) {
      Jar jar = _jarCache.get(backing);
      if (jar == null) {
        jar = new Jar(backing);
        _jarCache.put(backing, jar);
      }
    
      return jar;
    }
  }

  /**
   * Return a Jar for the path.  If the backing already exists, return
   * the old jar.
   */
  static Jar getJar(Path backing)
  {
    if (_jarCache != null)
      return _jarCache.get(backing);
    else
      return null;
  }

  /**
   * Return a Jar for the path.  If the backing already exists, return
   * the old jar.
   */
  public static PersistentDependency createDepend(Path backing)
  {
    Jar jar = create(backing);

    return jar.getDepend();
  }

  /**
   * Return a Jar for the path.  If the backing already exists, return
   * the old jar.
   */
  public static PersistentDependency createDepend(Path backing, long digest)
  {
    Jar jar = create(backing);

    return new JarDigestDepend(jar.getJarDepend(), digest);
  }

  /**
   * Returns the backing path.
   */
  Path getBacking()
  {
    return _backing;
  }

  /**
   * Returns the dependency.
   */
  public PersistentDependency getDepend()
  {
    return getJarDepend();
  }

  /**
   * Returns the dependency.
   */
  private JarDepend getJarDepend()
  {
    if (_depend == null || _depend.isModified())
      _depend = new JarDepend(new Depend(getBacking()));

    return _depend;
  }

  /**
   * Returns true if the entry is a file in the jar.
   *
   * @param path the path name inside the jar.
   */
  public Manifest getManifest()
    throws IOException
  {
    Manifest manifest;

    synchronized (this) {
      JarFile jarFile = getJarFile();

      if (jarFile == null)
	manifest = null;
      else
	manifest = jarFile.getManifest();
    }

    closeJarFile();

    return manifest;
  }

  /**
   * Returns true if the entry exists in the jar.
   *
   * @param path the path name inside the jar.
   */
  public synchronized boolean exists(String path)
  {
    boolean exists;

    synchronized (this) {
      exists = getSafeJarEntry(path) != null;
    }

    closeJarFile();

    return exists;
  }

  /**
   * Returns true if the entry is a directory in the jar.
   *
   * @param path the path name inside the jar.
   */
  public boolean isDirectory(String path)
  {
    boolean isDirectory;

    synchronized (this) {
      if (! path.endsWith("/"))
	path = path + '/';
    
      ZipEntry entry = getSafeJarEntry(path);

      if (entry == null && (path.equals("/") || path.equals("")))
	isDirectory = true;
      else
	isDirectory = entry != null && entry.isDirectory();
    }

    closeJarFile();

    return isDirectory;
  }

  /**
   * Returns true if the entry is a file in the jar.
   *
   * @param path the path name inside the jar.
   */
  public boolean isFile(String path)
  {
    boolean isFile;

    synchronized (this) {
      ZipEntry entry = getSafeJarEntry(path);

      isFile = entry != null && ! entry.isDirectory();
    }

    closeJarFile();

    return isFile;
  }

  /**
   * Returns the length of the entry in the jar file.
   *
   * @param path full path to the jar entry
   * @return the length of the entry
   */
  public long getLastModified(String path)
  {
    long lastModified;

    synchronized (this) {
      ZipEntry entry = getSafeJarEntry(path);

      if (entry == null)
	lastModified = 0;
      else
	lastModified = _lastModified;
    }

    closeJarFile();

    return lastModified;
  }

  /**
   * Returns the length of the entry in the jar file.
   *
   * @param path full path to the jar entry
   * @return the length of the entry
   */
  public long getLength(Path path)
  {
    long length;

    synchronized (this) {
      ZipEntry entry = getSafeJarEntry(path.getPath());

      if (entry == null)
	length = -1;
      else
	length = entry.getSize();
    }

    closeJarFile();

    return length;
  }

  /**
   * Readable if the jar is readable and the path refers to a file.
   */
  public boolean canRead(String path)
  {
    boolean canRead;

    synchronized (this) {
      canRead = _backing.canRead() && isFile(path);
    }

    closeJarFile();

    return canRead;
  }

  /**
   * Can't write to jars.
   */
  public boolean canWrite(String path)
  {
    return false;
  }

  /**
   * Lists all the files in this directory.
   */
  public String []list(String pathName) throws IOException
  {
    if (pathName.length() > 0 && ! pathName.endsWith("/"))
      pathName = pathName + "/";
    if (pathName.startsWith("/"))
      pathName = pathName.substring(1);

    ArrayList<String> list = new ArrayList<String>();

    String []result = null;

    synchronized (this) {
      JarFile jarFile = getJarFile();

      if (jarFile != null) {
	Enumeration e = jarFile.entries();
	while (e.hasMoreElements()) {
	  ZipEntry entry = (ZipEntry) e.nextElement();
	  String name = entry.getName();

	  if (name.startsWith(pathName) && ! name.equals(pathName)) {
	    String subName = name.substring(pathName.length());

	    int p = subName.indexOf('/');
          
	    if (p < 0)
	      list.add(subName);
	    else if (p == subName.length() - 1)
	      list.add(subName.substring(0, p));
	  }
	}

	result = (String []) list.toArray(new String[list.size()]);
      }
    }

    closeJarFile();

    if (result != null) {
      return result;
    }
    
    ReadStream backingIs = _backing.openRead();
    ZipInputStream is = new ZipInputStream(backingIs);

    try {
      ZipEntry entry;
    
      while ((entry = is.getNextEntry()) != null) {
	String name = entry.getName();

	if (name.startsWith(pathName) && ! name.equals(pathName)) {
	  String subName = name.substring(pathName.length());

	  int p = subName.indexOf('/');
            
	  if (p < 0)
	    list.add(subName);
	  else if (p == subName.length() - 1)
	    list.add(subName.substring(0, p));
	}
      }
    } finally {
      is.close();
      backingIs.close();
    }

    return (String []) list.toArray(new String[list.size()]);
  }

  /**
   * Opens a stream to an entry in the jar.
   *
   * @param path relative path into the jar.
   */
  public StreamImpl openReadImpl(Path path) throws IOException
  {
    String pathName = path.getPath();

    if (pathName.length() > 0 && pathName.charAt(0) == '/')
      pathName = pathName.substring(1);

    ZipFile zipFile = new ZipFile(_backing.getNativePath());
    ZipEntry entry;
    InputStream is = null;

    try {
      entry = zipFile.getEntry(pathName);
      if (entry != null) {
	is = zipFile.getInputStream(entry);

	return new ZipStreamImpl(zipFile, is, null, path);
      }
      else {
	throw new FileNotFoundException(path.toString());
      }
    } finally {
      if (is == null) {
	zipFile.close();
      }
    }
  }

  public String toString()
  {
    return _backing.toString();
  }

  /**
   * Clears all the cached files in the jar.  Needed to avoid some
   * windows NT issues.
   */
  public static void clearJarCache()
  {
    LruCache<Path,Jar> jarCache = _jarCache;
    
    if (jarCache == null)
      return;

    ArrayList<Jar> jars = new ArrayList<Jar>();
    
    synchronized (jarCache) {
      Iterator<Jar> iter = jarCache.values();
      
      while (iter.hasNext())
	jars.add(iter.next());
    }

    for (int i = 0; i < jars.size(); i++) {
      Jar jar = jars.get(i);
        
      if (jar != null)
	jar.clearCache();
    }
  }

  /**
   * Clears any cached JarFile.
   */
  public void clearCache()
  {
    JarFile jarFile = null;

    synchronized (this) {
      SoftReference<JarFile> jarFileRef = _jarFileRef;
      _jarFileRef = null;
      
      if (jarFileRef != null)
	jarFile = jarFileRef.get();
    }
      
    try {
      if (jarFile != null)
	jarFile.close();
    } catch (Exception e) {
    }
  }

  /**
   * Returns the named jar entry.
   *
   * @param path name in the jar file of the path.
   *
   * @return the jar entry or null.
   */
  private ZipEntry getSafeJarEntry(String path)
  {
    try {
      return getJarEntry(path);
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
      
      return null;
    }
  }

  /**
   * Returns the jar entry.  Since the entries are cached, this needs to
   * reload the cache on a jar file change.
   *
   * @param path name in the jar file of the path.
   */
  private ZipEntry getJarEntry(String path)
    throws IOException
  {
    if (path.startsWith("/"))
      path = path.substring(1);
    
    JarFile jarFile = getJarFile();
    
    if (jarFile != null)
      return jarFile.getEntry(path);
    else
      return null;
  }

  /**
   * Returns the Java ZipFile for this Jar.  Accessing the entries with
   * the ZipFile is faster than scanning through them.
   *
   * getJarFile is not thread safe.
   */
  private JarFile getJarFile()
    throws IOException
  {
    JarFile jarFile = null;
    
    if (getLastModifiedImpl() == _jarLastModified) {
      SoftReference<JarFile> jarFileRef = _jarFileRef;

      if (jarFileRef != null) {
	jarFile = jarFileRef.get();
	
	if (jarFile != null)
	  return jarFile;
      }
    }

    SoftReference<JarFile> oldJarRef = _jarFileRef;
    _jarFileRef = null;

    JarFile oldFile = null;
    if (oldJarRef == null) {
    }
    else if (_closeJarFileRef == null)
      _closeJarFileRef = oldJarRef;
    else
      oldFile = oldJarRef.get();

    if (oldFile != null) {
      try {
	oldFile.close();
      } catch (Throwable e) {
	e.printStackTrace();
      }
    }

    if (_backing.getScheme().equals("file") && _backing.canRead()) {
      jarFile = new JarFile(_backing.getNativePath());

      _jarFileRef = new SoftReference<JarFile>(jarFile);
      _jarLastModified = getLastModifiedImpl();
    }

    return jarFile;
  }

  /**
   * Returns the last modified time for the path.
   *
   * @param path path into the jar.
   *
   * @return the last modified time of the jar in milliseconds.
   */
  private long getLastModifiedImpl()
  {
    long now = Alarm.getCurrentTime();

    if (now == _lastTime)
      return _lastModified;

    long oldLastModified = _lastModified;
    long oldLength = _length;
    
    _lastModified = _backing.getLastModified();
    _length = _backing.getLength();
    _lastTime = now;

    // If the file has changed, close the old file
    if (_lastModified != oldLastModified || _length != oldLength) {
      SoftReference<JarFile> oldFileRef = _jarFileRef;
	
      _jarFileRef = null;
      _jarLastModified = 0;

      JarFile oldCloseFile = null;
      if (_closeJarFileRef != null)
	oldCloseFile = _closeJarFileRef.get();

      _closeJarFileRef = oldFileRef;

      if (oldCloseFile != null) {
	try {
	  oldCloseFile.close();
	} catch (Throwable e) {
	}
      }
    }

    return _lastModified;
  }

  /**
   * Closes any old jar waiting for close.
   */
  private void closeJarFile()
  {
    JarFile jarFile = null;
    
    synchronized (this) {
      if (_closeJarFileRef != null)
	jarFile = _closeJarFileRef.get();
      _closeJarFileRef = null;
    }
    
    if (jarFile != null) {
      try {
	jarFile.close();
      } catch (IOException e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  public void close()
  {
    removeEvent();
  }
  
  public void removeEvent()
  {
    JarFile jarFile = null;
    
    synchronized (this) {
      if (_jarFileRef != null)
	jarFile = _jarFileRef.get();

      _jarFileRef = null;
    }

    try {
      if (jarFile != null)
	jarFile.close();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }
    
    closeJarFile();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (o == null || ! getClass().equals(o.getClass()))
      return false;

    Jar jar = (Jar) o;

    return _backing.equals(jar._backing);
  }

  /**
   * StreamImpl to read from a ZIP file.
   */
  static class ZipStreamImpl extends StreamImpl {
    private ZipFile _zipFile;
    private InputStream _zis;
    private InputStream _is;

    /**
     * Create the new stream  impl.
     *
     * @param zis the underlying zip stream.
     * @param is the backing stream.
     * @param path the path to the jar entry.
     */
    ZipStreamImpl(ZipFile file, InputStream zis, InputStream is, Path path)
    {
      _zipFile = file;
      _zis = zis;
      _is = is;
      
      setPath(path);
    }

    /**
     * Returns true since this is a read stream.
     */
    public boolean canRead() { return true; }
 
    public int getAvailable() throws IOException
    {
      if (_zis == null)
        return -1;
      else
        return _zis.available();
    }
 
    public int read(byte []buf, int off, int len) throws IOException
    {
      int readLen = _zis.read(buf, off, len);
 
      return readLen;
    }
 
    public void close() throws IOException
    {
      ZipFile zipFile = _zipFile;
      _zipFile = null;
      
      InputStream zis = _zis;
      _zis = null;
      
      InputStream is = _is;
      _is = null;
      
      try {
	if (zis != null)
	  zis.close();
      } catch (Throwable e) {
      }

      try {
	if (zipFile != null)
	  zipFile.close();
      } catch (Throwable e) {
      }

      if (is != null)
        is.close();
    }

    protected void finalize()
      throws IOException
    {
      close();
    }
  }

  static class JarDepend extends CachedDependency
    implements PersistentDependency {
    private Depend _depend;
    private boolean _isDigestModified;
    
    /**
     * Create a new dependency.
     *
     * @param source the source file
     */
    JarDepend(Depend depend)
    {
      _depend = depend;
    }
    
    /**
     * Create a new dependency.
     *
     * @param source the source file
     */
    JarDepend(Depend depend, long digest)
    {
      _depend = depend;

      _isDigestModified = _depend.getDigest() != digest;
    }

    /**
     * Returns the underlying depend.
     */
    Depend getDepend()
    {
      return _depend;
    }

    /**
     * Returns true if the dependency is modified.
     */
    public boolean isModifiedImpl()
    {
      return _isDigestModified || _depend.isModified();
    }

    /**
     * Returns the string to recreate the Dependency.
     */
    public String getJavaCreateString()
    {
      String sourcePath = _depend.getPath().getPath();
      long digest = _depend.getDigest();
      
      return ("new com.caucho.vfs.Jar.createDepend(" +
	      "com.caucho.vfs.Vfs.lookup(\"" + sourcePath + "\"), " +
	      digest + "L)");
    }

    public String toString()
    {
      return "Jar$JarDepend[" + _depend.getPath() + "]";
    }
  }

  static class JarDigestDepend implements PersistentDependency {
    private JarDepend _jarDepend;
    private Depend _depend;
    private boolean _isDigestModified;
    
    /**
     * Create a new dependency.
     *
     * @param source the source file
     */
    JarDigestDepend(JarDepend jarDepend, long digest)
    {
      _jarDepend = jarDepend;
      _depend = jarDepend.getDepend();

      _isDigestModified = _depend.getDigest() != digest;
    }

    /**
     * Returns true if the dependency is modified.
     */
    public boolean isModified()
    {
      return _isDigestModified || _jarDepend.isModified();
    }

    /**
     * Returns the string to recreate the Dependency.
     */
    public String getJavaCreateString()
    {
      String sourcePath = _depend.getPath().getPath();
      long digest = _depend.getDigest();
      
      return ("new com.caucho.vfs.Jar.createDepend(" +
	      "com.caucho.vfs.Vfs.lookup(\"" + sourcePath + "\"), " +
	      digest + "L)");
    }
  }
}
