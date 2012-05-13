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

import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;
import com.caucho.vfs.Path;

import java.io.IOException;

public class DirectoryIterator
  extends SplFileInfo
  implements Iterator, Traversable, SeekableIterator
{
  private String[] _list;
  private int _index;

  private DirectoryIterator _current;

  public DirectoryIterator(Env env, StringValue fileName)
  {
    super(env, fileName);

    try {
      _list = _path.list();
    }
    catch (IOException e) {
      // XXX: throw the right exception class
      throw new QuercusRuntimeException(e);
    }
  }

  protected DirectoryIterator(Path path)
  {
    super(path);

    try {
      _list = path.list();
    }
    catch (IOException e) {
      // XXX: throw new the right exception class
      throw new QuercusRuntimeException(e);
    }
  }

  @Override
  public Value current(Env env)
  {
    DirectoryIterator current = getCurrent(env);

    return current != null ? env.wrapJava(current) : UnsetValue.UNSET;
  }

  protected DirectoryIterator createCurrentIterator(Env env, Path path)
  {
    return new DirectoryIterator(path);
  }

  protected DirectoryIterator getCurrent(Env env)
  {
    if (_current == null && _index < _list.length) {
      String name = _list[_index];

      Path child = _path.lookup(name);

      _current = createCurrentIterator(env, child);
    }

    return _current;
  }

  protected DirectoryIterator getCurrentRaw()
  {
    return _current;
  }

  protected int getKey()
  {
    return _index;
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
}
