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

package com.caucho.git;

import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/**
 * Tree structure from a jar
 */
public class GitCommitJar {
  private GitCommitTree _commit = new GitCommitTree();

  private Path _jar;

  public GitCommitJar(Path jar)
    throws IOException
  {
    _jar = jar;

    ReadStream is = jar.openRead();
    try {
      ZipInputStream zin = new ZipInputStream(is);

      ZipEntry entry;
      
      while ((entry = zin.getNextEntry()) != null) {
	String path = entry.getName();
	long length = entry.getSize();

	if (entry.isDirectory())
	  continue;
	
	if (length < 0)
	  throw new RuntimeException("can't handle dynamic length");

	_commit.addFile(path, 0664, zin, length);
      }
    } finally {
      is.close();
    }

    _commit.commit();
  }

  public ArrayList<String> getCommitList()
  {
    return _commit.getCommitList();
  }

  public String getDigest()
  {
    return _commit.getDigest();
  }

  public String findPath(String sha1)
  {
    return _commit.findPath(sha1);
  }

  public InputStream openFile(String sha1)
    throws IOException
  {
    String path = _commit.findPath(sha1);

    if (path.endsWith("/")) {
      GitWorkingTree tree = _commit.findTree(path);

      return tree.openFile();
    }
    else {
      ZipFile file = new ZipFile(_jar.getNativePath());

      try {
	ZipEntry entry = file.getEntry(path);
	InputStream is = file.getInputStream(entry);

	try {
	  return _commit.writeBlob(is, entry.getSize());
	} finally {
	  is.close();
	}
      } finally {
	file.close();
      }
    }
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName() + "[]");
  }
}
