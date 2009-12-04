/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.loader.enhancer;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.*;
import java.util.zip.*;

import com.caucho.bytecode.ByteCodeClassMatcher;
import com.caucho.bytecode.ByteCodeClassScanner;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.*;

/**
 * Interface for a scan manager
 */
public class ScanManager {
  private static final Logger log
    = Logger.getLogger(ScanManager.class.getName());
  
  private final ScanListener []_listeners;

  public ScanManager(ArrayList<ScanListener> listeners)
  {
    _listeners = new ScanListener[listeners.size()];
    
    listeners.toArray(_listeners);
  }

  public void scan(EnvironmentClassLoader loader, URL url)
  {
    // #3576
    scan(loader, Vfs.lookup(url));
  }
  
  public void scan(EnvironmentClassLoader loader, Path root)
  {
    ScanListener []listeners = new ScanListener[_listeners.length];

    boolean hasListener = false;
    for (int i = 0; i < _listeners.length; i++) {
      if (_listeners[i].isRootScannable(root)) {
	listeners[i] = _listeners[i];
	hasListener = true;
      }
    }

    if (! hasListener) {
      return;
    }

    if (root instanceof JarPath) {
      JarByteCodeMatcher matcher
	= new JarByteCodeMatcher(loader, root, listeners);
    
      scanForJarClasses(((JarPath) root).getContainer(), matcher);
    }
    else {
      PathByteCodeMatcher matcher
	= new PathByteCodeMatcher(loader, root, listeners);
      
      scanForClasses(root, root, matcher);
    }
  }

  private void scanForClasses(Path root,
			      Path path,
			      PathByteCodeMatcher matcher)
  {
    try {
      if (path.isDirectory()) {
	for (String name : path.list())
	  scanForClasses(root, path.lookup(name), matcher);

	return;
      }

      if (! path.getPath().endsWith(".class"))
	return;

      matcher.init(root, path);

      ReadStream is = path.openRead();
      
      try {
	ByteCodeClassScanner classScanner
	  = new ByteCodeClassScanner(path.getPath(), is, matcher);

	classScanner.scan();
      } finally {
	is.close();
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  private void scanForJarClasses(Path path, JarByteCodeMatcher matcher)
  {
    ZipFile zipFile = null;

    try {
      zipFile = new ZipFile(path.getNativePath());

      Enumeration<? extends ZipEntry> e = zipFile.entries();

      while (e.hasMoreElements()) {
	ZipEntry entry = e.nextElement();

	String entryName = entry.getName();
	if (! entryName.endsWith(".class"))
	  continue;

	matcher.init(entryName);
      
	ReadStream is = Vfs.openRead(zipFile.getInputStream(entry));
	try {
	  ByteCodeClassScanner classScanner
	    = new ByteCodeClassScanner(path.getPath(), is, matcher);

	  classScanner.scan();
	} finally {
	  is.close();
	}
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
	if (zipFile != null)
	  zipFile.close();
      } catch (Exception e) {
      }
    }
  }

  static class JarByteCodeMatcher extends ScanByteCodeMatcher {
    private String _entryName;

    JarByteCodeMatcher(EnvironmentClassLoader loader,
		       Path root,
		       ScanListener []listeners)
    {
      super(loader, root, listeners);
    }

    void init(String entryName)
    {
      super.init();
      
      _entryName = entryName;
    }

    String getClassName()
    {
      int p = _entryName.lastIndexOf('.');

      String className = _entryName.substring(0, p);

      return className.replace('/', '.');
    }
  }

  static class PathByteCodeMatcher extends ScanByteCodeMatcher {
    private Path _root;
    private Path _path;

    PathByteCodeMatcher(EnvironmentClassLoader loader,
			Path root,
			ScanListener []listeners)
    {
      super(loader, root, listeners);
    }

    void init(Path root, Path path)
    {
      super.init();

      _root = root;
      _path = path;
    }

    String getClassName()
    {
      String rootName = _root.getFullPath();
      String name = _path.getFullPath();
      
      int p = name.lastIndexOf('.');

      String className = name.substring(rootName.length(), p);

      return className.replace('/', '.');
    }
  }
    
  abstract static class ScanByteCodeMatcher implements ByteCodeClassMatcher {
    private final EnvironmentClassLoader _loader;
    private final Path _root;
    private final ScanListener []_listeners;
    private final ScanListener []_currentListeners;

    ScanByteCodeMatcher(EnvironmentClassLoader loader,
			Path root,
			ScanListener []listeners)
    {
      _loader = loader;
      _root = root;
      
      _listeners = listeners;
      _currentListeners = new ScanListener[listeners.length];
    }

    void init()
    {
      for (int i = 0; i < _listeners.length; i++)
	_currentListeners[i] = _listeners[i];
    }
    
    /**
     * Returns true if the annotation class is a match.
     */
    public boolean isClassMatch(String className, int modifiers)
    {
      int activeCount = 0;

      for (int i = _listeners.length - 1; i >= 0; i--) {
	ScanListener listener = _currentListeners[i];

	if (listener == null)
	  continue;

	ScanMatch scanMatch
	  = listener.isScanMatchClass(className, modifiers);
	
	if (scanMatch == ScanMatch.MATCH) {
	  listener.classMatchEvent(_loader, _root, getClassName());
	  _currentListeners[i] = null;
	}
	else if (scanMatch == ScanMatch.DENY) {
	  _currentListeners[i] = null;
	}
	else
	  activeCount++;
      }

      return activeCount == 0;
    }
    
    /**
     * Returns true if the annotation class is a match.
     */
    public boolean isAnnotationMatch(CharBuffer annotationClassName)
    {
      int activeCount = 0;

      for (int i = _listeners.length - 1; i >= 0; i--) {
	ScanListener listener = _currentListeners[i];

	if (listener == null)
	  continue;

	if (listener.isScanMatchAnnotation(annotationClassName)) {
	  listener.classMatchEvent(_loader, _root, getClassName());
	  _currentListeners[i] = null;
	}
	else
	  activeCount++;
      }

      return activeCount == 0;
    }

    abstract String getClassName();
  }
}
