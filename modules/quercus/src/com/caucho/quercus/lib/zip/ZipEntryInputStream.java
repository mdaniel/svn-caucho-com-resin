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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.zip;

import com.caucho.quercus.lib.file.BinaryInput;
import com.caucho.quercus.lib.file.ReadStreamInput;
import com.caucho.vfs.Vfs;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Input from a compressed stream.
 * XXX: might want to replace ZipInputStream with an Inflater so
 *     that we don't read this entry's header twice
 */
public class ZipEntryInputStream extends ReadStreamInput
{
  private BinaryInput _in;
  private ZipInputStream _zipIn;
  private QuercusZipEntry _entry;
  
  public ZipEntryInputStream(BinaryInput in, QuercusZipEntry entry)
    throws IOException
  {
    init(in, entry);
  }

  protected void init(BinaryInput in, QuercusZipEntry entry)
    throws IOException
  {
    in.setPosition(entry.getPosition());

    _in = in;
    _zipIn = new ZipInputStream(in.getInputStream());
    _entry = entry;

    ZipEntry curEntry = _zipIn.getNextEntry();

    if (curEntry == null)
      throw new IOException("Zip entry " +
          entry.getZipEntry().getName() + " not found.");

    init(Vfs.openRead(_zipIn));
  }

  /**
   * Opens a copy.
   */
  public BinaryInput openCopy()
    throws IOException
  {
    return new ZipEntryInputStream(_in.openCopy(), _entry);
  }

  public String toString()
  {
    return "ZipEntryInputStream[]";
  }
}
