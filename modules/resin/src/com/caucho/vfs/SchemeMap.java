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

import java.io.*;
import java.net.*;
import java.util.*;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.EnvironmentClassLoader;

/**
 * The top-level filesystem schemes are collected into a single map.
 *
 * <p>The default scheme has a number of standard filesystems, file:, mailto:,
 * jndi:, http:.
 *
 * <p>Applications can add schemes in the configuration file.  When first
 * accessed, the SchemeMap will look in the Registry to match the scheme.
 * If the new scheme exists, it will instantiate a single root instance and
 * use that for the remainder of the application.
 * <code><pre>
 * &lt;caucho.com>
 *  &lt;vfs scheme="foo" class-name="test.vfs.FooPath"/>
 * &lt;/caucho.com>
 * </pre></code>
 */
public class SchemeMap {
  // Constant null scheme map for protected filesystems.
  private static final SchemeMap NULL_SCHEME_MAP = new SchemeMap(null);
  
  // constant system-wide root scheme map.
  private static SchemeMap DEFAULT_SCHEME_MAP;

  private static EnvironmentLocal<SchemeMap> _localSchemeMap =
    new EnvironmentLocal<SchemeMap>();

  private ClassLoader _loader;
  private HashMap<String,Path> _schemeMap;

  /**
   * Create an empty SchemeMap.
   */
  private SchemeMap()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Create an empty SchemeMap.  Normally, applications will never call this.
   */
  private SchemeMap(ClassLoader loader)
  {
    for (;
	 loader != null && ! (loader instanceof EnvironmentClassLoader);
	 loader = loader.getParent()) {
    }

    _loader = loader;
    _schemeMap = new HashMap<String,Path>();
  }

  /**
   * The null scheme map is useful for protected filesystems as used
   * in createRoot().  That way, no dangerous code can get access to
   * files using, for example, the file: scheme.
   */
  static SchemeMap getNullSchemeMap()
  {
    return NULL_SCHEME_MAP;
  }
  
  /**
   * Returns the local scheme map.
   */
  public static SchemeMap getLocalSchemeMap()
  {
    return _localSchemeMap.get();
  }
  
  /**
   * Returns the named root Path of the scheme, e.g. for file:.
   *
   * @param scheme the scheme name for the selected path.
   * @return the named root or a NotFoundPath.
   */
  public static Path getScheme(String scheme)
  {
    SchemeMap schemeMap = _localSchemeMap.get();

    Path root = schemeMap.get(scheme);

    if (root != null)
      return root;

    return new NotFoundPath(scheme + ":");
  }

  /**
   * Sets the named root Path of the scheme.
   *
   * @param scheme name of the scheme.
   * @param handler root Path for the scheme.
   */
  public static void setScheme(String scheme, Path handler)
  {
    SchemeMap schemeMap;

    synchronized (_localSchemeMap) {
      schemeMap = _localSchemeMap.getLevel();

      if (schemeMap == null) {
	schemeMap = new SchemeMap();
	_localSchemeMap.set(schemeMap);
      }
    }

    schemeMap.put(scheme, handler);
  }

  /**
   * Removes the named scheme from the top-level filesystem.
   *
   * @param scheme name of the scheme to remove.
   */
  public static Path removeScheme(String scheme)
  {
    SchemeMap schemeMap = _localSchemeMap.getLevel();

    if (schemeMap != null)
      return schemeMap.remove(scheme);
    else
      return null;
  }

  /**
   * Gets the scheme from the schemeMap.
   */
  public Path get(String scheme)
  {
    Path path = _schemeMap.get(scheme);

    if (path != null)
      return path;
    else if (_loader == null)
      return new NotFoundPath(scheme + ":");

    SchemeMap parent = _localSchemeMap.get(_loader.getParent());
    
    if (parent != null)
      return parent.get(scheme);
    else
      return new NotFoundPath(scheme + ":");
  }

  /**
   * Puts a new value in the schemeMap.
   */
  private Path put(String scheme, Path path)
  {
    return _schemeMap.put(scheme, path);
  }

  /**
   * Removes value from the schemeMap.
   */
  private Path remove(String scheme)
  {
    return _schemeMap.remove(scheme);
  }

  /**
   * Initialize the JNI.
   */
  public static void initJNI()
  {
    // order matters because of static init and license checking
    FilesystemPath jniFilePath = JniFilePath.create();

    if (jniFilePath != null) {
      DEFAULT_SCHEME_MAP.put("file", jniFilePath);
      Vfs.PWD = jniFilePath;
      Vfs.setPwd(jniFilePath);
    }
  }

  static {
    DEFAULT_SCHEME_MAP = new SchemeMap(null);

    DEFAULT_SCHEME_MAP.put("file", new FilePath(null));
    
    DEFAULT_SCHEME_MAP.put("memory", new MemoryScheme());
    
    DEFAULT_SCHEME_MAP.put("jar", new JarScheme(null)); 
    DEFAULT_SCHEME_MAP.put("mailto",
				 new MailtoPath(null, null, null, null));
    DEFAULT_SCHEME_MAP.put("http", new HttpPath("127.0.0.1", 0));
    DEFAULT_SCHEME_MAP.put("https", new HttpsPath("127.0.0.1", 0));
    DEFAULT_SCHEME_MAP.put("tcp", new TcpPath(null, null, null, "127.0.0.1", 0));
    DEFAULT_SCHEME_MAP.put("tcps", new TcpsPath(null, null, null, "127.0.0.1", 0));
    DEFAULT_SCHEME_MAP.put("log", new LogPath(null, "/", null, "/"));
    DEFAULT_SCHEME_MAP.put("merge", new MergePath());

    StreamImpl stdout = StdoutStream.create();
    StreamImpl stderr = StderrStream.create();
    DEFAULT_SCHEME_MAP.put("stdout", stdout.getPath());
    DEFAULT_SCHEME_MAP.put("stderr", stderr.getPath());
    VfsStream nullStream = new VfsStream(null, null);
    DEFAULT_SCHEME_MAP.put("null", new ConstPath(null, nullStream));
    DEFAULT_SCHEME_MAP.put("jndi", new JndiPath());

    _localSchemeMap.setGlobal(DEFAULT_SCHEME_MAP);
  }
}
