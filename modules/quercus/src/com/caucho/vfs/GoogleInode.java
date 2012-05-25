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

package com.caucho.vfs;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Inode representing the meta-data of the GS file.
 */
@SuppressWarnings("serial")
public final class GoogleInode implements Serializable {
  private final String _name;
  private final FileType _type;
  private final long _length;
  private long _lastModified;
  private transient HashMap<String,GoogleInode> _dirMap;

  @SuppressWarnings("unused")
  private GoogleInode()
  {
    _name = null;
    _type = FileType.NONE;
    _length = -1;
    _lastModified = -1;
  }

  public GoogleInode(String name,
                     FileType type,
                     long length,
                     long lastModified)
  {
    _name = name;
    _type = type;
    _length = length;
    _lastModified = lastModified;
  }

  public GoogleInode(GoogleInode inode)
  {
    _name = inode._name;
    _type = inode._type;
    _length = inode._length;
    _lastModified = inode._lastModified;
  }

  public final String getName()
  {
    return _name;
  }

  public final boolean exists()
  {
    return _type == FileType.FILE || _type == FileType.DIRECTORY;
  }

  public final boolean isFile()
  {
    return _type == FileType.FILE;
  }

  public final boolean isDirectory()
  {
    return _type == FileType.DIRECTORY;
  }

  public final long getLength()
  {
    return _length;
  }

  public final long getLastModified()
  {
    return _lastModified;
  }

  public void setLastModified(long time)
  {
    _lastModified = time;
  }

  public HashMap<String,GoogleInode> getDirMap()
  {
    return _dirMap;
  }

  public void setDirMap(HashMap<String,GoogleInode> map)
  {
    _dirMap = map;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _name + "," + _type + "," + _length + "]";
  }

  public static enum FileType {
    NONE,
    FILE,
    DIRECTORY;
  }
}
