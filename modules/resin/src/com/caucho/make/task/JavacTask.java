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

package com.caucho.make.task;

import java.util.logging.*;

import com.caucho.log.Log;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import com.caucho.java.JavaCompiler;

import com.caucho.make.Make;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class JavacTask implements Make {
  private static final Logger log = Log.open(JavacTask.class);

  private Path _srcdir;
  private Path _dstdir;

  public void setSrcdir(Path path)
  {
    _srcdir = path;
  }

  public void setDstdir(Path path)
  {
    _dstdir = path;
  }

  public void make()
    throws Exception
  {
    Path dstDir = _dstdir;

    if (dstDir == null)
      dstDir = Vfs.lookup("WEB-INF/classes");
        
    Path srcDir = _srcdir;

    if (srcDir == null)
      srcDir = dstDir;

    JavaCompiler compiler = JavaCompiler.create();
    compiler.setClassDir(dstDir);

    makeAll(compiler, srcDir, dstDir);
  }

  public void makeAll(JavaCompiler compiler, Path srcDir, Path dstDir)
    throws Exception
  {
    String []list = srcDir.list();

    for (int i = 0; i < list.length; i++) {
      Path subSrc = srcDir.lookup(list[i]);

      if (subSrc.isDirectory())
        makeAll(compiler, subSrc, dstDir.lookup(list[i]));
      else if (list[i].endsWith(".java"))
        makeJava(compiler, subSrc, dstDir);
    }
  }

  public void makeJava(JavaCompiler compiler, Path javaSrc, Path dstDir)
    throws Exception
  {
    String tail = javaSrc.getTail();
    Path classFile = dstDir.lookup(tail.substring(0, tail.length() - 5) + ".class");

    if (javaSrc.getLastModified() <= classFile.getLastModified())
      return;

    compiler.compileIfModified(javaSrc.getPath(), null);
  }
}
