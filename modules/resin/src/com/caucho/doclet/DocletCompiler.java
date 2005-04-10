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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.doclet;

import java.util.*;
import java.io.*;
import java.util.logging.*;

import com.sun.tools.javadoc.Main;

import com.caucho.util.L10N;
import com.caucho.util.CharBuffer;
import com.caucho.util.CauchoSystem;

import com.caucho.java.JavaCompileException;

import com.caucho.vfs.Vfs;
import com.caucho.vfs.Path;
import com.caucho.vfs.Encoding;
import com.caucho.vfs.MemoryStream;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.IOExceptionWrapper;

import com.caucho.loader.DynamicClassLoader;

import com.caucho.java.WorkDir;

import com.caucho.config.Config;

import com.caucho.log.Log;

/**
 * Runs the doclet.
 */
public class DocletCompiler {
  private static final L10N L = new L10N(DocletCompiler.class);
  private static final Logger log = Log.open(DocletCompiler.class);

  private String _userPrefix;
  private boolean _isDead;
  private Process _process;

  private Path _srcDir;
  private long _maxCompileTime = 30000;
  private String _charEncoding;

  private ArrayList<String> _args;

  public RootDocImpl run(Path srcDir, ArrayList<String> paths)
    throws Exception
  {
    _srcDir = srcDir;

    return compileInt(paths);
  }

  private Path getSourceDir()
  {
    return _srcDir;
  }

  private String getSourceDirName()
  {
    return _srcDir.getPath();
  }

  private String getCompiler()
  {
    return "javadoc";
  }

  private String getClassPath()
  {
    DynamicClassLoader loader =
      (DynamicClassLoader) Thread.currentThread().getContextClassLoader();

    return loader.getClassPath();
  }

  /**
   * Compile the configured file.
   *
   * @param path the path to the java source.
   * @param lineMap mapping from the generated source to the original files.
   */
  protected RootDocImpl compileInt(ArrayList<String> paths)
    throws IOException
  {
    MemoryStream tempStream = new MemoryStream();
    WriteStream error = new WriteStream(tempStream);
    MemoryStream outStream = new MemoryStream();
    WriteStream out = new WriteStream(outStream);
    InputStream inputStream = null;
    InputStream errorStream = null;
    boolean chdir = CauchoSystem.isUnix();

    ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();

    _process = null;

    try {
      String doclet = getCompiler();

      String path = paths.get(0);

      String parent = Vfs.lookupNative(path).getParent().getNativePath();

      ArrayList<String> argList = new ArrayList<String>();
      argList.add(doclet);

      for (int i = 0; _args != null && i < _args.size(); i++) {
        argList.add(_args.get(i));
      }

      if (_charEncoding != null) {
        String encoding = Encoding.getJavaName(_charEncoding);
        argList.add("-encoding");
        argList.add(encoding);
      }

      Path workDir = WorkDir.getLocalWorkDir();

      Path docletPath = workDir.lookup("_doclet/doclet.xml");
      docletPath.getParent().mkdirs();

      String classPath = normalizeClassPath(getClassPath(), ! chdir);

      argList.add("-classpath");
      argList.add(classPath);
      
      argList.add("-doclet");
      argList.add("com.caucho.doclet.XmlDoclet");
      
      argList.add("-path");
      argList.add(docletPath.getNativePath());

      String []env = new String [] {
        "CLASSPATH=" + classPath,
      };

      for (int i = 0; i < paths.size(); i++) {
        argList.add(_srcDir.lookup(paths.get(i)).getNativePath());
      }

      String []args = argList.toArray(new String[argList.size()]);

      TempStream ts = new TempStream(null);
      WriteStream ws = new WriteStream(ts);
      PrintWriter pw = ws.getPrintWriter();

      Main.execute("resin-javadoc", pw, pw, pw,
		   "com.caucho.doclet.XmlDoclet", args);

      pw.close();
      ws.close();

      if (log.isLoggable(Level.FINER)) {
	ReadStream rs = ts.openRead();
	StringBuilder cb = new StringBuilder();
	int ch;
	while ((ch = rs.read()) >= 0)
	  cb.append((char) ch);

	log.finer(cb.toString());
      }

      RootDocImpl rootDoc = new RootDocImpl();

      new Config().configure(rootDoc, docletPath);

      return rootDoc;
    } catch (Exception e) {
      throw new IOExceptionWrapper(e);
    } finally {
      Thread.currentThread().setContextClassLoader(oldLoader);
      
      if (inputStream != null)
	inputStream.close();
      if (errorStream != null)
	errorStream.close();
      tempStream.destroy();
    }
  }

  /**
   * Converts any relative classpath references to the full path.
   */
  String normalizeClassPath(String classPath, boolean generateRelative)
  {
    char sep = CauchoSystem.getPathSeparatorChar();
    int head = 0;
    int tail = 0;

    CharBuffer cb = CharBuffer.allocate();

    while (head < classPath.length()) {
      tail = classPath.indexOf(sep, head);
      if (tail < 0)
        tail = classPath.length();

      if (tail > head) {
        String segment = classPath.substring(head, tail);

        if (cb.length() != 0)
          cb.append(sep);
      
        cb.append(normalizePath(segment, generateRelative));
      }

      head = tail + 1;
    }

    return cb.close();
  }
  /**
   * Normalizes a path.
   */
  String normalizePath(String segment, boolean generateRelative)
  {
    if (_userPrefix == null) {
      Path userPath = Vfs.lookup(CauchoSystem.getUserDir());
      char sep = CauchoSystem.getFileSeparatorChar();
      _userPrefix = userPath.getNativePath();
      
      if (_userPrefix.length() == 0 ||
          _userPrefix.charAt(_userPrefix.length() - 1) != sep) {
        _userPrefix = _userPrefix + sep;
      }
    }
    
    Path path = Vfs.lookup(segment);
    String nativePath = path.getNativePath();

    if (! generateRelative)
      return nativePath;

    if (nativePath.startsWith(_userPrefix))
      nativePath = nativePath.substring(_userPrefix.length());

    if (nativePath.equals(""))
      return ".";
    else
      return nativePath;
  }
}
