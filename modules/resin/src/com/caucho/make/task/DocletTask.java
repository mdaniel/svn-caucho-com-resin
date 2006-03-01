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

import java.util.*;
import java.util.logging.*;

import com.caucho.log.Log;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import com.caucho.make.Make;

import com.caucho.doclet.DocletCompiler;
import com.caucho.doclet.RootDocImpl;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class DocletTask implements Make {
  private static final Logger log = Log.open(DocletTask.class);

  private Path _srcdir;
  private Path _dstdir;
  private boolean _isComplete;

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

    ArrayList<String> paths = new ArrayList<String>();

    gatherPaths(paths, srcDir);

    DocletCompiler doclet = new DocletCompiler();
    if (! _isComplete) {
      _isComplete = true;
      RootDocImpl rootDoc = doclet.run(srcDir, paths);
    }
  }

  public void gatherPaths(ArrayList<String> paths, Path srcDir)
    throws Exception
  {
    String []list = srcDir.list();

    for (int i = 0; i < list.length; i++) {
      Path subSrc = srcDir.lookup(list[i]);

      if (subSrc.isDirectory())
        gatherPaths(paths, subSrc);
      else if (list[i].endsWith("Bean.java"))
        paths.add(subSrc.getNativePath());
    }
  }
}
