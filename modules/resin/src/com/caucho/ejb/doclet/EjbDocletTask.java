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

package com.caucho.ejb.doclet;

import java.util.*;
import java.util.logging.*;

import com.caucho.util.L10N;

import com.caucho.log.Log;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import com.caucho.make.Make;

import com.caucho.doclet.DocletCompiler;
import com.caucho.doclet.RootDocImpl;
import com.caucho.doclet.ClassDocImpl;
import com.caucho.doclet.TagImpl;

/**
 * Class loader which checks for changes in class files and automatically
 * picks up new jars.
 */
public class EjbDocletTask implements Make {
  private static final L10N L = new L10N(EjbDocletTask.class);
  private static final Logger log = Log.open(EjbDocletTask.class);

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

      ArrayList<ClassDocImpl> entityClasses = new ArrayList<ClassDocImpl>();

      Collection<ClassDocImpl> classes = rootDoc.getClasses();
	
      Iterator<ClassDocImpl> iter = classes.iterator();
      while (iter.hasNext()) {
        ClassDocImpl classDoc = iter.next();

        if (! classDoc.isAssignableTo("javax.ejb.EntityBean"))
	  continue;

	if (classDoc.getTagList("@ejb.bean") == null) {
	  log.fine(L.l("`{0}' is not an XDoclet bean",
		       classDoc.getName()));
	  continue;
	}

	String generate = classDoc.getAttribute("@ejb.bean", "generate");

	if ("false".equals(generate))
	  continue;

	entityClasses.add(classDoc);
      }

      if (entityClasses.size() == 0)
	return;
      
      new EjbJarGenerator().generate(Vfs.lookup("WEB-INF/xdoclet.ejb"),
				     rootDoc);

      for (int i = 0; i < entityClasses.size(); i++) {
	generateEntity(srcDir, dstDir, entityClasses.get(i));
      }
    }
  }

  private void generateEntity(Path srcDir, Path destDir,
			      ClassDocImpl classDoc)
    throws Exception
  {
    String className = classDoc.getName();
    Path beanPath = srcDir.lookup(className.replace('.', '/') + ".java");
    
    ArrayList<TagImpl> homeList = classDoc.getTagList("@ejb.home");

    for (int i = 0; homeList != null && i < homeList.size(); i++) {
      TagImpl homeTag = homeList.get(i);

      String homeClass = homeTag.getAttribute("local-class");
      
      if (homeClass != null) {
        Path path = destDir.lookup(homeClass.replace('.', '/') + ".java");

	if (path.getLastModified() < beanPath.getLastModified()) {
	  log.finer("generating " + homeClass);
	  
	  EjbHomeGenerator gen = new EjbHomeGenerator(classDoc);
	  gen.setLocal(true);
	  gen.setClassName(homeClass);

	  gen.generate(path);
	}
      }
    }
    
    ArrayList<TagImpl> objectList = classDoc.getTagList("@ejb.interface");

    for (int i = 0; objectList != null && i < objectList.size(); i++) {
      TagImpl objectTag = objectList.get(i);

      String objectClass = objectTag.getAttribute("local-class");
      
      if (objectClass != null) {
        Path path = destDir.lookup(objectClass.replace('.', '/') + ".java");
	
	if (path.getLastModified() < beanPath.getLastModified()) {
	  log.finer("generating " + objectClass);
	  
	  EjbObjectGenerator gen = new EjbObjectGenerator(classDoc);
	  gen.setLocal(true);
	  gen.setClassName(objectClass);

	  gen.generate(path);
	}
      }
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
