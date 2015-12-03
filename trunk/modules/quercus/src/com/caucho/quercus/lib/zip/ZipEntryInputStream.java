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

package com.caucho.quercus.lib.zip;

import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.quercus.lib.file.ReadStreamInput;
import com.caucho.quercus.env.Env;
import com.caucho.vfs.*;
import com.caucho.util.L10N;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Input from a compressed stream.
 */
public class ZipEntryInputStream extends ReadStreamInput
{
  private static final L10N L = new L10N(ZipEntryInputStream.class);

  private final BinaryInput _in;
  private final long _position;

  public ZipEntryInputStream(BinaryInput in, long position)
    throws IOException
  {
    super(Env.getInstance());

    _in = in;
    _position = position;

    in.setPosition(_position);

    ZipInputStream zipInputStream = new ZipInputStream(in.getInputStream());

    ZipEntry curEntry = zipInputStream.getNextEntry();

    if (curEntry == null)
      throw new IOException(
          L.l("ZipEntry at position {0} not found", _position));

    init(new ReadStream(new VfsStream(zipInputStream, null)));
  }

  /**
   * Opens a copy.
   */
  public BinaryInput openCopy()
    throws IOException
  {
    return new ZipEntryInputStream(_in.openCopy(), _position);
  }

  public String toString()
  {
    return "ZipEntryInputStream[]";
  }
}
