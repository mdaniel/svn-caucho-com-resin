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

package com.caucho.server.deploy;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.util.jar.Manifest;
import java.util.jar.Attributes;

import java.util.regex.Pattern;

import com.caucho.config.types.FileSetType;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;

import com.caucho.util.Log;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Jar;
import com.caucho.vfs.Path;
import com.caucho.vfs.Depend;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;

/**
 * A deployment entry that expands from an archive (Jar/Zip) file.
 */
abstract public class ExpandDeployController<I extends DeployInstance>
  extends DeployController<I> {
  private static final Logger log = Log.open(ExpandDeployController.class);

  private Object _archiveExpandLock = new Object();

  private Path _rootDirectory;
  private Path _archivePath;

  private FileSetType _expandCleanupFileSet;

  // classloader for the manifest entries
  private DynamicClassLoader _manifestLoader;
  private Manifest _manifest;

  protected ExpandDeployController()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  protected ExpandDeployController(ClassLoader loader)
  {
    super(loader);

    _rootDirectory = Vfs.getPwd(loader);
  }
  
  /**
   * Gets the root directory
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * Sets the root directory
   */
  public void setRootDirectory(Path rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  /**
   * Gets the archive path.
   */
  public Path getArchivePath()
  {
    return _archivePath;
  }

  /**
   * Sets the archive path.
   */
  public void setArchivePath(Path path)
  {
    _archivePath = path;
  }

  /**
   * Returns the manifest.
   */
  public Manifest getManifest()
  {
    return _manifest;
  }

  /**
   * Sets the manifest class loader.
   */
  public void setManifestClassLoader(DynamicClassLoader loader)
  {
    _manifestLoader = loader;
  }

  /**
   * Sets the archive auto-remove file set.
   */
  public void setExpandCleanupFileSet(FileSetType fileSet)
  {
    _expandCleanupFileSet = fileSet;
  }

  /**
   * Expand an archive file.  The _archiveExpandLock must be obtained
   * before the expansion.
   */
  protected void expandArchive()
    throws IOException
  {
    synchronized (_archiveExpandLock) {
      if (! expandArchiveImpl()) {
	try {
	  Thread.sleep(2000);
	} catch (InterruptedException e) {
	}
	
	expandArchiveImpl();
      }

      Path path = getRootDirectory().lookup("META-INF/MANIFEST.MF");
      if (path.canRead()) {
	ReadStream is = path.openRead();
	try {
	  _manifest = new Manifest(is);
	} finally {
	  is.close();
	}
      }
    }
  }

  /**
   * Adds any class path from the manifest.
   */
  protected void addManifestClassPath()
    throws IOException
  {
    DynamicClassLoader loader = Environment.getDynamicClassLoader();
    if (loader == null)
      return;
    
    Manifest manifest = getManifest();

    if (manifest == null)
      return;

    Attributes main = manifest.getMainAttributes();

    if (main == null)
      return;
    
    String classPath = main.getValue("Class-Path");

    Path pwd = null;

    if (getArchivePath() != null)
      pwd = getArchivePath().getParent();
    else
      pwd = getRootDirectory();

    if (classPath == null) {
    }
    else if (_manifestLoader != null)
      _manifestLoader.addManifestClassPath(classPath, pwd);
    else
      loader.addManifestClassPath(classPath, pwd);
  }
  
  /**
   * Expand an archive.  The _archiveExpandLock must be obtained before the
   * expansion.
   */
  private boolean expandArchiveImpl()
    throws IOException
  {
    Path archivePath = getArchivePath();

    if (archivePath == null)
      return true;
    
    if (! archivePath.canRead())
      return true;

    Path expandDir = getRootDirectory();
    Path parent = expandDir.getParent();
    
    try {
      parent.mkdirs();
    } catch (Throwable e) {
    }
    
    Path dependPath = expandDir.lookup("META-INF/resin-war.digest");
    Depend depend = null;

      // XXX: change to a hash
    if (dependPath.canRead()) {
      ReadStream is = null;
      try {
        is = dependPath.openRead();

	String line = is.readLine();

        long digest;

	if (line != null) {
	  digest = Long.parseLong(line.trim());

	  depend = new Depend(archivePath, digest);

	  if (! depend.isModified())
	    return true;
	}
      } catch (Throwable e) {
	log.log(Level.FINE, e.toString(), e);
      } finally {
        try {
          if (is != null)
            is.close();
        } catch (IOException e) {
        }
      }
    }

    if (depend == null)
      depend = new Depend(archivePath);

    try {
      if (log.isLoggable(Level.INFO))
        getLog().info("expanding " + archivePath + " to " + expandDir);

      removeExpandDirectory(expandDir);

      expandDir.mkdirs();

      ReadStream rs = archivePath.openRead(); 
      ZipInputStream zis = new ZipInputStream(rs);

      try {
        ZipEntry entry;

        byte []buffer = new byte[1024];
      
        while ((entry = zis.getNextEntry()) != null) {
          String name = entry.getName();
          Path path = expandDir.lookup(name);

          if (entry.isDirectory())
            path.mkdirs();
          else {
            long length = entry.getSize();
            long lastModified = entry.getTime();
            path.getParent().mkdirs();

            WriteStream os = path.openWrite();
            try {
              int len;
              while ((len = zis.read(buffer, 0, buffer.length)) > 0)
                os.write(buffer, 0, len);
            } catch (IOException e) {
              log.log(Level.FINE, e.toString(), e);
            } finally {
              os.close();
            }

            if (lastModified > 0)
              path.setLastModified(lastModified);
          }
        }
      } finally {
        try {
          zis.close();
        } catch (IOException e) {
        }
        try {
          rs.close();
        } catch (IOException e) {
        }
      }
    } catch (IOException e) {
      log.log(Level.WARNING, e.toString(), e);
      // If the jar is incomplete, it should throw an exception here.
      return false;
    }
    
    try {
      dependPath.getParent().mkdirs();
      WriteStream os = dependPath.openWrite();

      os.println(depend.getDigest());
      
      os.close();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    return true;
  }

  /**
   * Recursively remove all files in a directory.  Used for wars when
   * they change.
   *
   * @param dir root directory to start removal
   */
  protected void removeExpandDirectory(Path path)
  {
    String prefix = path.getPath();

    if (! prefix.endsWith("/"))
      prefix = prefix + "/";

    removeExpandDirectory(path, prefix);
  }

  /**
   * Recursively remove all files in a directory.  Used for wars when
   * they change.
   *
   * @param dir root directory to start removal
   */
  protected void removeExpandDirectory(Path path, String prefix)
  {
    try {
      if (path.isDirectory()) {
        String []list = path.list();
        for (int i = 0; list != null && i < list.length; i++) {
          removeExpandDirectory(path.lookup(list[i]), prefix);
        }
      }

      removeExpandFile(path, prefix);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Removes an expanded file.
   */
  protected void removeExpandFile(Path path, String prefix)
    throws IOException
  {
    if (_expandCleanupFileSet == null ||
	_expandCleanupFileSet.isMatch(path, prefix))
      path.remove();
  }

  /**
   * Returns the hash code.
   */
  public int hashCode()
  {
    return getName().hashCode();
  }

  /**
   * Returns equality.
   */
  public boolean equals(Object o)
  {
    // server/125g
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    DeployController entry = (DeployController) o;

    return getName().equals(entry.getName());
  }
}
