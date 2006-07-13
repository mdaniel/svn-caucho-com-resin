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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Emil Ong
 */

package com.caucho.quercus.lib.file;

import java.io.IOException;

import com.caucho.quercus.env.ResourceValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.QuercusClass;

/**
 * Represents a PHP directory listing
 */
public class WrappedDirectoryValue extends DirectoryValue {
  private Env _env;
  private Value _wrapper;

  public WrappedDirectoryValue(Env env, QuercusClass qClass)
  {
    _env = env;
    _wrapper = qClass.callNew(_env, new Value[0]);
  }

  public boolean opendir(StringValue path, LongValue flags)
  {
    return _wrapper.callMethod(_env, "dir_opendir", path, flags).toBoolean();
  }

  /**
   * Returns the next value.
   */
  public Value readdir()
  {
    return _wrapper.callMethod(_env, "dir_readdir");
  }

  /**
   * Rewinds the directory
   */
  public void rewinddir()
  {
    _wrapper.callMethod(_env, "dir_rewinddir");
  }

  /**
   * Closes the directory
   */
  public void close()
  {
    _wrapper.callMethod(_env, "dir_closedir");
  }

  /**
   * Converts to a string.
   * @param env
   */
  public String toString()
  {
    return "Directory[]";
  }
}

