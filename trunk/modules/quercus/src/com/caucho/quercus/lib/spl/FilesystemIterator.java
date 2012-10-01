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

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnsetValue;
import com.caucho.quercus.env.Value;
import com.caucho.vfs.Path;

public class FilesystemIterator extends DirectoryIterator
{
  public static final int CURRENT_AS_PATHNAME = 32;
  public static final int CURRENT_AS_FILEINFO = 0;
  public static final int CURRENT_AS_SELF = 16;
  public static final int CURRENT_MODE_MASK = 240;

  public static final int KEY_AS_PATHNAME = 0;
  public static final int KEY_AS_FILENAME = 256;
  public static final int FOLLOW_SYMLINKS = 512;
  public static final int KEY_MODE_MASK = 3840;
  public static final int NEW_CURRENT_AND_KEY = 256;
  public static final int SKIP_DOTS = 4096;
  public static final int UNIX_PATHS = 8192;

  private int _flags;

  public FilesystemIterator(Env env,
                            StringValue fileName,
                            @Optional("-1") int flags)
  {
    super(env, fileName);

    if (flags < 0) {
      flags = KEY_AS_PATHNAME | CURRENT_AS_FILEINFO | SKIP_DOTS;
    }

    _flags = flags;
  }

  protected FilesystemIterator(Path path, int flags)
  {
    super(path);

    _flags = flags;
  }

  public int getFlags()
  {
    return _flags;
  }

  public void setFlags(int flags)
  {
    _flags = flags;
  }

  @Override
  public Value key(Env env)
  {
    int flags = _flags;

    if ((flags & KEY_AS_PATHNAME) == KEY_AS_PATHNAME) {
      DirectoryIterator current = getCurrent(env);

      if (current == null) {
        return UnsetValue.UNSET;
      }

      String path = current.getPathname();

      return env.createString(path);
    }
    else if ((flags & KEY_AS_FILENAME) == KEY_AS_FILENAME) {
      DirectoryIterator current = getCurrent(env);

      if (current == null) {
        return UnsetValue.UNSET;
      }

      String path = current.getFilename();

      return env.createString(path);
    }
    else {
      return super.key(env);
    }
  }
}
