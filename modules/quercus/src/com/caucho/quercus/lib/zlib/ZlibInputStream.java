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

package com.caucho.quercus.lib.zlib;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.quercus.lib.file.ReadStreamInput;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.VfsStream;

import java.io.IOException;

/**
 * Input from a compressed stream.
 *
 *
 */
public class ZlibInputStream extends ReadStreamInput
{
  private Env _env;

  private BinaryInput _in;
  private GZInputStream _gzIn;

  private ReadStream _rs;

  public ZlibInputStream(Env env, BinaryInput in) throws IOException
  {
    super(env);

    _env = env;

    init(in);
  }

  private void init(BinaryInput in)
    throws IOException
  {
    _in = in;

    _gzIn = new GZInputStream(in.getInputStream());

    ReadStream rs = new ReadStream(new VfsStream(_gzIn, null));
    _rs = rs;

    // calls parent init(ReadStream)
    init(rs);
  }

  /**
   * Opens a new copy.
   */
  public BinaryInput openCopy()
    throws IOException
  {
    return new ZlibInputStream(_env, _in.openCopy());
  }

  /**
   * Sets the position.
   */
  public boolean setPosition(long offset)
  {
    try {
      long pos = _rs.getPosition();

      if (pos <= offset) {
        if (_rs.setPosition(offset)) {
          return true;
        }
      }

      // temp workaround: need to close before open for Google
      close();
      //_in.close();

      BinaryInput newIn = _in.openCopy();
      init(newIn);

      long skipped = skip(offset);
      return skipped == offset;
    }
    catch (IOException e) {
      e.printStackTrace();

      throw new QuercusModuleException(e);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _in + "]";
  }
}
