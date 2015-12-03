/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.util.Arrays;

public class DirectoryIterator
  extends SplFileInfo
  implements Iterator, Traversable, SeekableIterator
{
  private String[] _list;
  private int _index;

  private SplFileInfo _current;

  public DirectoryIterator(Env env, StringValue fileName)
  {
    super(env, fileName);

    try {
      _list = getPathList(_path);
    }
    catch (IOException e) {
      // XXX: throw the right exception class
      throw new QuercusRuntimeException(e);
    }
  }

  protected DirectoryIterator(Path parent, Path path, String fileName)
  {
    super(parent, path, fileName);

    try {
      _list = getPathList(path);
    }
    catch (IOException e) {
      // XXX: throw new the right exception class
      throw new QuercusRuntimeException(e);
    }
  }

  private static String[] getPathList(Path path)
    throws IOException
  {
    String[] list = path.list();

    String[] newList = new String[list.length + 2];

    newList[0] = ".";
    newList[1] = "..";

    System.arraycopy(list, 0, newList, 2, list.length);

    return newList;
  }

  @Override
  public Value current(Env env)
  {
    SplFileInfo current = getCurrent(env);

    return current != null ? env.wrapJava(current) : UnsetValue.UNSET;
  }

  protected SplFileInfo getCurrent(Env env)
  {
    if (_current == null && _index < _list.length) {
      String name = _list[_index];

      Path child = _path.lookup(name);

      _current = createCurrent(env, _path, child, name);
    }

    return _current;
  }

  protected SplFileInfo createCurrent(Env env,
                                      Path parent,
                                      Path path,
                                      String fileName)
  {
    return new SplFileInfo(parent, path, fileName);
  }

  protected int getKey()
  {
    return _index;
  }

  public boolean isDot(Env env)
  {
    SplFileInfo current = getCurrent(env);

    String fileName = current.getFilename(env);

    return ".".equals(fileName) || "..".equals(fileName);
  }

  @Override
  public Value key(Env env)
  {
    return LongValue.create(_index);
  }

  @Override
  public void next(Env env)
  {
    _index++;

    _current = null;
  }

  @Override
  public void rewind(Env env)
  {
    _index = 0;
  }

  @Override
  public boolean valid(Env env)
  {
    return _index < _list.length;
  }

  @Override
  public void seek(Env env, int index)
  {
    _index = index;
  }

  //
  // SplFileInfo
  //
  /*
  @Override
  public long getATime(Env env)
  {
    return getCurrent(env).getATime(env);
  }

  @Override
  public String getBasename(Env env, @Optional String suffix)
  {
    return getCurrent(env).getBasename(env, suffix);
  }

  @Override
  public long getCTime(Env env)
  {
    return getCurrent(env).getCTime(env);
  }

  @Override
  public String getExtension(Env env)
  {
    return getCurrent(env).getExtension(env);
  }

  @Override
  public SplFileInfo getFileInfo(Env env, @Optional String className)
  {
    return getCurrent(env).getFileInfo(env, className);
  }

  @Override
  public String getFilename(Env env)
  {
    return getCurrent(env).getFilename(env);
  }

  @Override
  public int getGroup(Env env)
  {
    return getCurrent(env).getGroup(env);
  }

  @Override
  public long getInode(Env env)
  {
    return getCurrent(env).getInode(env);
  }

  @Override
  public String getLinkTarget(Env env)
  {
    return getCurrent(env).getLinkTarget(env);
  }

  @Override
  public long getMTime(Env env)
  {
    return getCurrent(env).getMTime(env);
  }

  @Override
  public int getOwner(Env env)
  {
    return getCurrent(env).getOwner(env);
  }

  @Override
  public String getPath(Env env)
  {
    return getCurrent(env).getPath(env);
  }

  @Override
  public SplFileInfo getPathInfo(Env env, @Optional String className)
  {
    return getCurrent(env).getPathInfo(env, className);
  }

  @Override
  public String getPathname(Env env)
  {
    return getCurrent(env).getPathname(env);
  }

  @Override
  public int getPerms(Env env)
  {
    return getCurrent(env).getPerms(env);
  }

  @Override
  public String getRealPath(Env env)
  {
    return getCurrent(env).getRealPath(env);
  }

  @Override
  public long getSize(Env env)
  {
    return getCurrent(env).getSize(env);
  }

  @Override
  public String getType(Env env)
  {
    return getCurrent(env).getType(env);
  }

  @Override
  public boolean isDir(Env env)
  {
    return getCurrent(env).isDir(env);
  }

  @Override
  public boolean isExecutable(Env env)
  {
    return getCurrent(env).isExecutable(env);
  }

  @Override
  public boolean isFile(Env env)
  {
    return getCurrent(env).isFile(env);
  }

  @Override
  public boolean isLink(Env env)
  {
    return getCurrent(env).isLink(env);
  }

  @Override
  public boolean isReadable(Env env)
  {
    return getCurrent(env).isReadable(env);
  }

  @Override
  public boolean isWritable(Env env)
  {
    return getCurrent(env).isWritable(env);
  }

  @Override
  public SplFileObject openFile(Env env,
                                @Optional("r") String mode,
                                @Optional boolean isUseIncludePath,
                                @Optional Value context)
  {
    return getCurrent(env).openFile(env, mode, isUseIncludePath, context);
  }

  @Override
  public String __toString(Env env)
  {
    return getCurrent(env).__toString(env);
  }
  */
}
