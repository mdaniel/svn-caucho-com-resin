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
 * FilePath implements the native filesystem.
 */
public class FilePath extends FilesystemPath {
  // The underlying Java File object.
  private static byte []NEWLINE = CauchoSystem.getNewlineString().getBytes();
  
  private File _file;

  /**
   * @param path canonical path
   */
  protected FilePath(FilesystemPath root, String userPath, String path)
  {
    super(root, userPath, path);
    
    _separatorChar = CauchoSystem.getFileSeparatorChar();
  }

  FilePath(String path)
  {
    this(Vfs.PWD != null ? Vfs.PWD._root : null,
	 path, normalizePath("/", initialPath(path),
                             0, CauchoSystem.getFileSeparatorChar()));

    if (_root == null) {
      _root = new FilePath(null, "/", "/");
      _root._root = _root;
    }

    _separatorChar = _root._separatorChar;
  }

  protected static String initialPath(String path)
  {
    if (path == null)
      return getPwd();
    else if (path.length() > 0 && path.charAt(0) == '/')
      return path;
    else {
      String dir = getPwd();

      if (dir.length() > 0 && dir.charAt(dir.length() - 1) == '/') 
	return dir + path;
      else
	return dir + "/" + path;
    }
  }

  /**
   * Gets the system's user dir (pwd) and convert it to the Resin format.
   */
  private static String getPwd()
  {
    String path = CauchoSystem.getUserDir();

    path = path.replace(CauchoSystem.getFileSeparatorChar(), '/');

    if (CauchoSystem.isWindows())
      path = convertFromWindowsPath(path);

    return path;
  }

  /**
   * a:xxx -> /a:xxx
   * ///a:xxx -> /a:xxx
   * //xxx -> /:/xxx
   * 
   */
  private static String convertFromWindowsPath(String path)
  {
    int colon = path.indexOf(':');
    int length = path.length();
    char ch;
    
    if (colon == 1 && (ch = path.charAt(0)) != '/' && ch != '\\')
      return "/" + path.charAt(0) + ":/" + path.substring(2);
    else if (length > 1 &&
	     ((ch = path.charAt(0)) == '/' || ch == '\\') &&
	     ((ch = path.charAt(1)) == '/' || ch == '\\')) {
      if (colon < 0)
	return "/:" + path;

      for (int i = colon - 2; i > 1; i--) {
	if ((ch = path.charAt(i)) != '/' && ch != '\\')
	  return "/:" + path;
      }

      ch = path.charAt(colon - 1);

      if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z')
	return path.substring(colon - 2);
      else
	return "/:" + path;
    }
    else
      return path;
  }

  /**
   * Lookup the path, handling windows weirdness
   */
  protected Path schemeWalk(String userPath,
                            Map<String,Object> attributes,
			    String filePath,
                            int offset)
  {
    if (! CauchoSystem.isWindows())
      return super.schemeWalk(userPath, attributes, filePath, offset);
    
    String canonicalPath;
    
    if (filePath.length() < offset + 2)
      return super.schemeWalk(userPath, attributes, filePath, offset);

    char ch1 = filePath.charAt(offset + 1);
    char ch2 = filePath.charAt(offset);

    if ((ch2 == '/' || ch2 == _separatorChar) &&
        (ch1 == '/' || ch1 == _separatorChar))
      return super.schemeWalk(userPath, attributes,
                              convertFromWindowsPath(filePath.substring(offset)), 0);
    else
      return super.schemeWalk(userPath, attributes, filePath, offset);
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
  protected Path fsWalk(String userPath,
			Map<String,Object> attributes,
			String path)
  {
    return new FilePath(_root, userPath, path);
  }

  public String getScheme()
  {
    return "file";
  }

  /**
   * Returns the full url for the given path.
   */
  public String getURL()
  {
    return escapeURL("file:" + getFullPath());
  }

  /**
   * Returns the native path.
   */
  public String getNativePath()
  {
    if (_separatorChar == '/' && ! CauchoSystem.isWindows())
      return getFullPath();
    
    String path = getFullPath();
    int length = path.length();
    CharBuffer cb = CharBuffer.allocate();
    char ch;
    int offset = 0;
    // For windows, convert /c: to c:
    if (CauchoSystem.isWindows()) {
      if (length >= 3 &&
          path.charAt(0) == '/' &&
          path.charAt(2) == ':' &&
          ((ch = path.charAt(1)) >= 'a' && ch <= 'z' ||
           ch >= 'A' && ch <= 'Z')) {
        offset = 1;
      }
      else if (length >= 3 &&
               path.charAt(0) == '/' &&
               path.charAt(1) == ':' &&
               path.charAt(2) == '/') {
        cb.append('\\');
        cb.append('\\');
        offset = 3;
      }
    }

    for (; offset < length; offset++) {
      ch = path.charAt(offset);
      if (ch == '/')
	cb.append(_separatorChar);
      else
	cb.append(ch);
    }

    return cb.close();
  }

  public boolean exists()
  {
    if (_separatorChar == '\\' && isAux())
      return false;
    else
      return getFile().exists();
  }

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
      perms += 0222;

    return perms;
  }

  public boolean isDirectory()
  {
    return getFile().isDirectory();
  }

  public boolean isFile()
  {
    if (_separatorChar == '\\' && isAux())
      return false;
    else
      return getFile().isFile();
  }

  public long getLength()
  {
    return getFile().length();
  }

  public long getLastModified()
  {
    return getFile().lastModified();
  }

  // This exists in JDK 1.2
  public void setLastModified(long time)
  {
    getFile().setLastModified(time);
  }

  public boolean canRead()
  {
    File file = getFile();
    
    if (_separatorChar == '\\' && isAux())
      return false;
    else
      return file.canRead();
  }

  public boolean canWrite()
  {
    File file = getFile();
    
    if (_separatorChar == '\\' && isAux())
      return false;
    else
      return file.canWrite();
  }

  /**
   * Returns a list of files in the directory.
   */
  public String []list() throws IOException
  {
    String []list = getFile().list();

    if (list != null)
      return list;

    return new String[0];
  }
  
  public boolean mkdir()
    throws IOException
  {
    boolean value = getFile().mkdir();
    if (! value && ! getFile().isDirectory())
      throw new IOException("cannot create directory");

    return value;
  }
  
  public boolean mkdirs()
    throws IOException
  {
    File file = getFile();
    
    boolean value;

    synchronized (file) {
      value = file.mkdirs();
    }
    
    if (! value && ! file.isDirectory())
      throw new IOException("Cannot create directory: " + getFile());

    return value;
  }
  
  public boolean remove()
  {
    if (getFile().delete())
      return true;
    
    if (getPath().endsWith(".jar")) {
      Jar.create(this).clearCache();
      return getFile().delete();
    }

    return false;
  }
  
  public boolean truncate(long length)
    throws IOException
  {
    File file = getFile();

    FileOutputStream fos = new FileOutputStream(file);

    try {
      fos.getChannel().truncate(length);

      return true;
    } finally {
      fos.close();
    }
  }
  
  public boolean renameTo(Path path)
  {
    if (! (path instanceof FilePath))
      return false;

    FilePath file = (FilePath) path;

    return this.getFile().renameTo(file.getFile());
  }

  /**
   * Returns the stream implementation for a read stream.
   */
  public StreamImpl openReadImpl() throws IOException
  {
    if (_separatorChar == '\\' && isAux())
      throw new FileNotFoundException(_file.toString());

    /* XXX: only for Solaris (?)
    if (isDirectory())
      throw new IOException("is directory");
    */

    return new FileReadStream(new FileInputStream(getFile()), this);
  }

  public StreamImpl openWriteImpl() throws IOException
  {
    VfsStream os;

    os = new VfsStream(null, new FileOutputStream(getFile()), this);

    os.setNewline(NEWLINE);

    return os;
  }

  public StreamImpl openAppendImpl() throws IOException
  {
    FileOutputStream fos = null;

    try {
      fos = new FileOutputStream(getFile().toString(), true);
    } catch (IOException e) {
      // MacOS hack
      fos = new FileOutputStream(getFile().toString());
    }

    VfsStream os = new VfsStream(null, fos);

    os.setNewline(NEWLINE);
    
    return os;
  }

  public StreamImpl openReadWriteImpl() throws IOException
  {
    VfsStream os;

    os = new VfsStream(new FileInputStream(getFile()),
                       new FileOutputStream(getFile()),
                       this);

    os.setNewline(NEWLINE);
    
    return os;
  }

  /**
   * Returns the stream implementation for a random-access stream.
   */
  public RandomAccessStream openRandomAccess() throws IOException
  {
    if (_separatorChar == '\\' && isAux())
      throw new FileNotFoundException(_file.toString());

    return new FileRandomAccessStream(new RandomAccessFile(getFile(), "rw"));
  }

  @Override
  protected Path copy()
  {
    return new FilePath(getRoot(), getUserPath(), getPath());
  }

  public int hashCode()
  {
    return getFullPath().hashCode();
  }

  public boolean equals(Object b)
  {
    if (this == b)
      return true;
    
    if (! (b instanceof FilePath))
      return false;

    FilePath file = (FilePath) b;

    return getFullPath().equals(file.getFullPath());
  }

  /**
   * Lazily returns the native File object.
   */
  public File getFile()
  {
    if (_file != null)
      return _file;

    if (CauchoSystem.isTesting())
      _file = new File(getFullPath());
    else
      _file = new File(getNativePath());

    return _file;
  }

  /**
   * Special case for the evil windows special
   */
  protected boolean isAux()
  {
    File file = getFile();

    String path = getFullPath().toLowerCase();

    int len = path.length();
    int p = path.indexOf("/aux");
    int ch;
    if (p >= 0 && (p + 4 >= len || path.charAt(p + 4) == '.'))
      return true;
    
    p = path.indexOf("/con");
    if (p >= 0 && (p + 4 >= len || path.charAt(p + 4) == '.'))
      return true;
    
    p = path.indexOf("/lpt");
    if (p >= 0 && (p + 5 >= len || path.charAt(p + 5) == '.'))
      return true;
    
    p = path.indexOf("/nul");
    if (p >= 0 && (p + 4 >= len || path.charAt(p + 4) == '.'))
      return true;

    return false;
  }
}
