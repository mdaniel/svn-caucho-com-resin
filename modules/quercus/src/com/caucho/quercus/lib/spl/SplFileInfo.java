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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.spl;

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.vfs.Path;

public class SplFileInfo
{
  protected Path _path;
  private String _openFileClassName;
  private String _infoFileClassName;

  public SplFileInfo(Env env, StringValue fileName)
  {
    _path = init(env, fileName, false);
  }

  protected SplFileInfo(Path path)
  {
    _path = path;
  }

  protected SplFileInfo(Env env, StringValue fileName, boolean isUseIncludePath)
  {
    _path = init(env, fileName, true);
  }

  protected Path init(Env env, StringValue fileName, boolean isUseIncludePath)
  {
    if (isUseIncludePath) {
      return env.lookupInclude(fileName);
    }
    else {
      return env.lookupPwd(fileName);
    }
  }

  public long getATime()
  {
    return _path.getLastAccessTime() / 1000;
  }

  public String getBasename(@Optional String suffix)
  {
    String name = _path.getTail();

    if (suffix != null && name.endsWith(suffix)) {
      name = name.substring(0, name.length() - suffix.length());
    }

    return name;
  }

  public long getCTime()
  {
    return _path.getCreateTime() / 1000;
  }

  public String getExtension()
  {
    String name = _path.getTail();

    int pos = name.lastIndexOf('.');

    if (0 <= pos && pos + 1 < name.length()) {
      return name.substring(pos + 1);
    }
    else {
      return "";
    }
  }

  public SplFileInfo getFileInfo(@Optional String className)
  {
    throw new UnimplementedException("SplFileInfo::getFileInfo()");
  }

  public String getFilename()
  {
    return _path.getTail();
  }

  public int getGroup()
  {
    return _path.getGroup();
  }

  public long getInode()
  {
    return _path.getInode();
  }

  public String getLinkTarget()
  {
    return _path.readLink();
  }

  public long getMTime()
  {
    return _path.getLastModified() / 1000;
  }

  public int getOwner()
  {
    return _path.getOwner();
  }

  public String getPath()
  {
    Path parent = _path.getParent();

    return parent.getNativePath();
  }

  public SplFileInfo getPathInfo(@Optional String className)
  {
    throw new UnimplementedException("SplFileInfo::getPathInfo()");
  }

  public String getPathname()
  {
    return _path.getNativePath();
  }

  public int getPerms()
  {
    return _path.getMode();
  }

  public String getRealPath()
  {
    return _path.realPath();
  }

  public long getSize()
  {
    return _path.getLength();
  }

  public String getType()
  {
    if (_path.isLink()) {
      return "link";
    }
    else if (_path.isDirectory()) {
      return "dir";
    }
    else if (_path.isFile()) {
      return "file";
    }
    else {
      /// XXX: throw RuntimeException
      return null;
    }
  }

  public boolean isDir(Env env)
  {
    return _path.isDirectory();
  }

  public boolean isExecutable()
  {
    return _path.isExecutable();
  }

  public boolean isFile()
  {
    return _path.isFile();
  }

  public boolean isLink()
  {
    return _path.isLink();
  }

  public boolean isReadable()
  {
    return _path.canRead();
  }

  public boolean isWritable()
  {
    return _path.canWrite();
  }

  public SplFileObject openFile(@Optional("r") String mode,
                                @Optional boolean isUseIncludePath,
                                @Optional Value context)
  {
    throw new UnimplementedException("SplFileInfo::openFile()");
  }

  public void setFileClass(@Optional String className)
  {
    _openFileClassName = className;
  }

  public void setInfoClass(@Optional String className)
  {
    _infoFileClassName = className;
  }

  public String __toString()
  {
    return _path.getNativePath();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _path + "]";
  }
}
