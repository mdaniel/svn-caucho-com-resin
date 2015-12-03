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
import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.make.CachedDependency;
import com.caucho.util.CacheListener;
import com.caucho.util.CurrentTime;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

/**
 * Jar is a cache around a jar file to avoid scanning through the whole
 * file on each request.
 *
 * <p>When the Jar is created, it scans the file and builds a directory
 * of the Jar entries.
 */
public class Jar implements CacheListener {
  private static final Logger log = Logger.getLogger(Jar.class.getName());
  private static final L10N L = new L10N(Jar.class);
  
  private static LruCache<Path,Jar> _jarCache;

  private static EnvironmentLocal<Integer> _jarSize
    = new EnvironmentLocal<Integer>("caucho.vfs.jar-size");
  
  private static ZipEntry NULL_ZIP = new ZipEntry("null");
  
  private LruCache<String,ZipEntry> _zipEntryCache
    = new LruCache<String,ZipEntry>(64);
  
  private Path _backing;
  private boolean _backingIsFile;

  private AtomicInteger _changeSequence = new AtomicInteger();
  
  private JarDepend _depend;
  
  // saved last modified time
  private long _lastModified;
  // saved length
  private long _length;
  // last time the file was checked
  private long _lastTime;

  // cached zip file to read jar entries
  private final AtomicReference<ZipFile> _zipFileRef
    = new AtomicReference<ZipFile>();

  private Boolean _isSigned;
  private Certificate[] _certificates;

  /**
   * Creates a new Jar.
   *
   * @param backing canonical path
   */
  private Jar(Path backing)
  {
    if (backing instanceof JarPath)
      throw new IllegalStateException();
    
    _backing = backing;
    
    /*
    if (backing.getNativePath().indexOf("cssparser.jar") >= 0)
      System.out.println("JAR: " + backing);
      */
    
    _backingIsFile = (_backing.getScheme().equals("file")
                      && _backing.canRead());
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
    
    Jar jar = _jarCache.get(backing);
    if (jar == null) {
      jar = new Jar(backing);
      jar = _jarCache.putIfNew(backing, jar);
    }
    
    return jar;
  }

  /**
   * Return a Jar for the path.  If the backing already exists, return
   * the old jar.
   */
  static Jar getJar(Path backing)
  {
    if (_jarCache != null) {
      Jar jar = _jarCache.get(backing);

      return jar;
    }

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
  
  public int getChangeSequence()
  {
    return _changeSequence.get();
  }

  private boolean isSigned()
  {
    Boolean isSigned = _isSigned;

    if (isSigned != null)
      return isSigned;

    try {
      Manifest manifest = getManifest();

      if (manifest == null) {
        _isSigned = Boolean.FALSE;
        return false;
      }

      Map<String,Attributes> entries = manifest.getEntries();

      if (entries == null) {
        _isSigned = Boolean.FALSE;
        return false;
      }
      
      for (Attributes attr : entries.values()) {
        for (Object key : attr.keySet()) {
          String keyString = String.valueOf(key);

          if (keyString.contains("Digest")) {
            _isSigned = Boolean.TRUE;

            return true;
          }
        }
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    _isSigned = Boolean.FALSE;
    
    return false;
  }

  /**
   * Returns Manifest
   *
   */
  public Manifest getManifest()
    throws IOException
  {
    Manifest manifest;

    JarFile jarFile = getJarFile();

    try {
      if (jarFile == null)
        manifest = null;
      else
        manifest = jarFile.getManifest();
    } finally {
      closeJarFile(jarFile);
    }

    return manifest;
  }

  /**
   * Returns any certificates.
   */
  public Certificate []getCertificates(String path)
  {
    if (! isSigned()) {
      return null;
    }
    
    if (_certificates != null) {
      return _certificates;
    }
    
    if (path.length() > 0 && path.charAt(0) == '/') {
      path = path.substring(1);
    }

    try {
      if (! _backing.canRead())
        return null;
      
      JarFile jarFile = getJarFile();
      JarEntry entry;
      InputStream is = null;

      try {
        entry = jarFile.getJarEntry(path);

        if (entry != null) {
          is = jarFile.getInputStream(entry);

          while (is.skip(65536) > 0) {
          }

          is.close();

          _certificates = entry.getCertificates();
        }
      } finally {
        closeJarFile(jarFile);
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return null;
    }

    return _certificates;
  }

  /**
   * Returns true if the entry exists in the jar.
   *
   * @param path the path name inside the jar.
   */
  public boolean exists(String path)
  {
    // server/249f, server/249g
    // XXX: facelets vs issue of meta-inf (i.e. lower case)

    try {
      ZipEntry entry = getZipEntry(path);

      return entry != null;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return false;
  }

  /**
   * Returns true if the entry is a directory in the jar.
   *
   * @param path the path name inside the jar.
   */
  public boolean isDirectory(String path)
  {
    try {
      ZipEntry entry = getZipEntry(path);

      return entry != null && entry.isDirectory();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return false;
  }

  /**
   * Returns true if the entry is a file in the jar.
   *
   * @param path the path name inside the jar.
   */
  public boolean isFile(String path)
  {
    try {
      ZipEntry entry = getZipEntry(path);

      return entry != null && ! entry.isDirectory();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return false;
  }

  /**
   * Returns the last-modified time of the entry in the jar file.
   *
   * @param path full path to the jar entry
   * @return the length of the entry
   */
  public long getLastModified(String path)
  {
    try {
      // this entry time can cause problems ...
      ZipEntry entry = getZipEntry(path);

      return entry != null ? entry.getTime() : -1;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return  -1;
  }

  /**
   * Returns the length of the entry in the jar file.
   *
   * @param path full path to the jar entry
   * @return the length of the entry
   */
  public long getLength(String path)
  {
    try {
      ZipEntry entry = getZipEntry(path);

      long length = entry != null ? entry.getSize() : -1;
      
      return length;
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
      
      return -1;
    }
  }

  /**
   * Readable if the jar is readable and the path refers to a file.
   */
  public boolean canRead(String path)
  {
    try {
      ZipEntry entry = getZipEntry(path);

      return entry != null && ! entry.isDirectory();
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
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
  public String []list(String path) throws IOException
  {
    // XXX:
    
    return new String[0];
  }

  /**
   * Opens a stream to an entry in the jar.
   *
   * @param path relative path into the jar.
   */
  public ZipStreamImpl openReadImpl(Path path) 
    throws IOException
  {
    String pathName = path.getPath();
    
    return openReadImpl(pathName);
  }
  
  public ZipStreamImpl openReadImpl(String pathName)
    throws IOException
  {
    if (pathName.length() > 0 && pathName.charAt(0) == '/')
      pathName = pathName.substring(1);

    ZipEntry entry;
    ZipStreamImpl zipIs = null;
    
    ZipFile zipFile = getZipFile();

    try {
      entry = zipFile.getEntry(pathName);
      
      if (entry == null) {
        pathName = "/" + pathName;
        entry = zipFile.getEntry(pathName);
      }

      if (entry != null) {
        InputStream is = zipFile.getInputStream(entry);

        zipIs = new ZipStreamImpl(zipFile, entry, is, pathName);
        
        return zipIs;
      }
      else {
        throw new FileNotFoundException(pathName);
      }
    } finally {
      if (zipIs == null) {
        zipFile.close();
      }
    }
  }

  /**
   * Clears any cached JarFile.
   */
  public void clearCache()
  {
    ZipFile zipFile = _zipFileRef.getAndSet(null);

    if (zipFile != null)
      try {
        zipFile.close();
      } catch (Exception e) {
      }
  }

  public ZipEntry getZipEntry(String path)
    throws IOException
  {
    ZipEntry entry = _zipEntryCache.get(path);
    
    if (entry != null && isCacheValid()) {
      if (entry == NULL_ZIP)
        return null;
      else
        return entry;
    }
    
    entry = getZipEntryImpl(path);
    
    if (entry != null) {
      _zipEntryCache.put(path, entry);
    }
    else {
      _zipEntryCache.put(path, NULL_ZIP);
    }
    
    return entry;
  }

  private ZipEntry getZipEntryImpl(String path)
    throws IOException
  {
    if (path.startsWith("/"))
      path = path.substring(1);

    boolean isValid = false;

    ZipFile zipFile = getZipFile();
    
    try {
      if (zipFile != null) {
        ZipEntry entry = zipFile.getEntry(path);
        
        isValid = true;
        
        return entry;
      }
      else
        return null;
    } finally {
      if (isValid)
        closeZipFile(zipFile);
      else if (zipFile != null)
        zipFile.close();
    }
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

    isCacheValid();

    if (! _backingIsFile)
      throw new FileNotFoundException(_backing.getNativePath());
    
    try {
      jarFile = new JarFile(_backing.getNativePath());

        /*
        if (_backing.getNativePath().indexOf("cssparser.jar") > 0)
          System.out.println("JAR: " + _backing + " " + jarFile);
          */
    }
    catch (IOException ex) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("Error opening jar file '{0}'", _backing.getNativePath()));

      throw ex;
    }

    return jarFile;
  }
  
  private void closeJarFile(JarFile jarFile)
  {
    try {
      if (jarFile != null)
        jarFile.close();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  /**
   * Returns the Java ZipFile for this Jar.  Accessing the entries with
   * the ZipFile is faster than scanning through them.
   *
   * getJarFile is not thread safe.
   */
  public ZipFile getZipFile()
    throws IOException
  {
    isCacheValid();

    ZipFile zipFile = _zipFileRef.getAndSet(null);

    if (zipFile != null)
      return zipFile;

    if (_backingIsFile) {
      try {
        zipFile = new ZipFile(_backing.getNativePath());

        /*
        if (_backing.getNativePath().indexOf("cssparser") >= 0) {
          System.out.println("ZIP: " + _backing.getNativePath() + " " + zipFile);
          Thread.dumpStack();
        }
        */
      }
      catch (IOException ex) {
        if (log.isLoggable(Level.FINE))
          log.log(Level.FINE, L.l("Error opening jar file '{0}'", _backing.getNativePath()));

        throw ex;
      }

      getLastModifiedImpl();
    }

    return zipFile;
  }

  public void closeZipFile(ZipFile zipFile)
  {
    if (zipFile == null)
      return;

    if (_zipFileRef.compareAndSet(null, zipFile))
      return;

    try {
      /*
      if (_backing.getNativePath().indexOf("cssparser") >= 0)
        System.out.println("CLOSE-ZIP: " + _backing.getNativePath() + " " + zipFile);
        */
      
      zipFile.close();
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Returns the last modified time for the path.
   *
   *
   * @return the last modified time of the jar in milliseconds.
   */
  private long getLastModifiedImpl()
  {
    isCacheValid();
    
    return _lastModified;
  }
  
  /**
   * Returns the last modified time for the path.
   *
   *
   * @return the last modified time of the jar in milliseconds.
   */
  private boolean isCacheValid()
  {
    long now = CurrentTime.getCurrentTime();

    if ((now - _lastTime < 100) && ! CurrentTime.isTest())
      return true;

    long oldLastModified = _lastModified;
    long oldLength = _length;
    
    long newLastModified = _backing.getLastModified();
    long newLength = _backing.getLength();
    
    _lastTime = now;

    if (newLastModified == oldLastModified && newLength == oldLength) {
      _lastTime = now;
      return true;
    }
    else {
      _changeSequence.incrementAndGet();
      
      // If the file has changed, close the old file
      clearCache();
      
      _depend = null;
      _isSigned = null;
      _zipEntryCache.clear();
      
      _lastModified = newLastModified;
      _length = newLength;
      
      _lastTime = now;

      return false;
    }
  }

  public void close()
  {
    removeEvent();
  }
  
  @Override
  public void removeEvent()
  {
    clearCache();
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

  @Override
  public String toString()
  {
    return _backing.toString();
  }

  /**
   * StreamImpl to read from a ZIP file.
   */
  public class ZipStreamImpl extends StreamImpl {
    private ZipFile _zipFile;
    private ZipEntry _zipEntry;
    private InputStream _zis;
    
    private String _pathName;

    /**
     * Create the new stream  impl.
     *
     * @param zipFile
     * @param zipEntry
     * @param zis the underlying zip stream.
     * @param pathName
     */
    ZipStreamImpl(ZipFile zipFile,
                  ZipEntry zipEntry,
                  InputStream zis, 
                  String pathName)
    {
      _zipFile = zipFile;
      _zipEntry = zipEntry;
      _zis = zis;
      
      // System.out.println("OPEN: " + pathName + " " + zis);
      // setPath(path);
    }
    
    public ZipEntry getZipEntry()
    {
      return _zipEntry;
    }

    /**
     * Returns true since this is a read stream.
     */
    @Override
    public boolean canRead() { return true; }
 
    @Override
    public int getAvailable() throws IOException
    {
      InputStream zis = _zis;
      
      if (zis == null)
        return -1;
      
      return _zis.available();
    }
 
    @Override
    public int read(byte []buf, int off, int len) throws IOException
    {
      InputStream zis = _zis;
      
      if (zis == null)
        return -1;
      
      return zis.read(buf, off, len);
    }
 
    @Override
    public void close() throws IOException
    {
      ZipFile zipFile = _zipFile;
      _zipFile = null;
      
      InputStream zis = _zis;
      _zis = null;
      
      try {
        IoUtil.close(zis);
//      //  System.out.println("CLOSE: " + zis + " " + _pathName);
      } catch (Throwable e) {
      }

      try {
        closeZipFile(zipFile);
        /*
        if (zipFile != null)
          zipFile.close();
          */
      } catch (Throwable e) {
      }
    }

    /*
    @Override
    protected void finalize()
      throws IOException
    {
      close();
    }
    */
  }

  class JarDepend extends CachedDependency
    implements PersistentDependency {
    private Depend _depend;
    private boolean _isDigestModified;
    
    /**
     * Create a new dependency.
     *
     * @param depend the source file
     */
    JarDepend(Depend depend)
    {
      _depend = depend;
    }
    
    /**
     * Create a new dependency.
     *
     * @param depend the source file
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
    @Override
    public boolean isModifiedImpl()
    {
      if (_isDigestModified || _depend.isModified()) {
        _changeSequence.incrementAndGet();
        return true;
      }
      else
        return false;
    }

    /**
     * Returns true if the dependency is modified.
     */
    @Override
    public boolean logModified(Logger log)
    {
      return _depend.logModified(log);
    }

    /**
     * Returns the string to recreate the Dependency.
     */
    @Override
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
     * @param jarDepend the source file
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
     * Returns true if the dependency is modified.
     */
    public boolean logModified(Logger log)
    {
      return _depend.logModified(log) || _jarDepend.logModified(log);
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
