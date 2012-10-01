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

package com.google.appengine.api.files;

/**
 * Factory for creating file services.
 */
public class AppEngineFile
{
  private final FileSystem _fileSystem;
  private final String _namePart;

  public AppEngineFile(FileSystem fileSystem, String name)
  {
    _fileSystem = fileSystem;
    _namePart = name;
  }

  public AppEngineFile(String fullPath)
  {
    int p = fullPath.indexOf('/', 1);

    String fsName = fullPath.substring(1, p);

    _fileSystem = FileSystem.fromName(fsName);
    _namePart = fullPath.substring(p + 1);
  }


  public FileSystem getFileSystem()
  {
    return _fileSystem;
  }

  public String getNamePart()
  {
    return _namePart;
  }

  public String getFullPath()
  {
    return "/" + getFileSystem().getName() + "/" + getNamePart();
  }

  public boolean isReadable()
  {
    throw new UnsupportedOperationException();
  }

  public boolean isWritable()
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getFullPath() + "]";
  }

  public static enum FileSystem {
    BLOBSTORE {
      public String getName()
      {
        return "blobstore";
      }
    },
    GS {
      public String getName()
      {
        return "gs";
      }
    };

    abstract public String getName();

    public static FileSystem fromName(String name)
    {

      if (name.equals("blobstore"))
        return FileSystem.BLOBSTORE;
      else if (name.equals("gs"))
        return FileSystem.GS;
      else
        throw new IllegalArgumentException();

    }
  }
}
