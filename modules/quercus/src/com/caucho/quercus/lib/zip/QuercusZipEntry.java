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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Charles Reich
 */

package com.caucho.quercus.lib.zip;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.BinaryBuilderValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.util.L10N;

import com.caucho.vfs.TempBuffer;

import java.io.InputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

public class QuercusZipEntry {
  private static final Logger log =
                             Logger.getLogger(QuercusZipEntry.class.getName());
  private static final L10N L = new L10N(QuercusZipEntry.class);

  private InputStream _in;
  private long _position;
  private ZipEntry _zipEntry;

  public QuercusZipEntry(long position, ZipEntry zipEntry)
  {
    _position = position;
    _zipEntry = zipEntry;
  }

  /**
   * Returns the file name.
   */
  public String zip_entry_name()
  {
    return _zipEntry.getName();
  }

  /**
   * Returns the file's uncompressed size.
   */
  public long zip_entry_filesize()
  {
    return _zipEntry.getSize();
  }

  /**
   * Opens this zip entry for reading.
   */
  public boolean zip_entry_open(Env env, Zip zip)
  {
    try {
      _in = zip.openInputStream(this);
      return true;

    } catch (IOException e) {
      env.warning(L.l(e.toString()));
      log.log(Level.FINE,  e.toString(),  e);
      return false;
    }
  }

  /**
   * Closes the zip entry.
   */
  public void zip_entry_close()
    throws IOException
  {
    if (_in != null)
      _in.close();
  }

  /**
   * Reads and decompresses entry's compressed data.
   *
   * @param entry
   * @param length
   * @return decompressed BinaryValue or FALSE on error
   */
  public Value zip_entry_read(Env env,
                                @Optional("1024") int length)
  {
    if (_in == null)
      return BooleanValue.FALSE;

    BinaryBuilderValue bbv = new BinaryBuilderValue();
    TempBuffer tb = TempBuffer.allocate();
    byte[] buffer = tb.getBuffer();

    int sublen;
    try {
      while (length > 0) {
        sublen = _in.read(buffer, 0, buffer.length);
        bbv.append(buffer, 0, sublen);
        length -= sublen;
      }
      if (bbv.length() < 0)
        return BooleanValue.FALSE;

      return bbv;

    } catch (IOException e) {
      env.warning(L.l(e.toString()));
      log.log(Level.FINE,  e.toString(),  e);
      return BooleanValue.FALSE;
    } finally {
      TempBuffer.free(tb);
    }
  }

  /**
   * Returns the size of the compressed data.
   *
   * @return -1, or compressed size
   */
  public Value zip_entry_compressedsize()
  {
    if (_zipEntry == null)
      return new LongValue(-1);

    return new LongValue(_zipEntry.getCompressedSize());
  }

  /**
   * Only "deflate" and "store" methods are supported.
   *
   * @return the compression method used for this entry
   */
  public String zip_entry_compressionmethod()
  {
    if (_zipEntry == null)
      return "";

    Integer method = _zipEntry.getMethod();

    switch(method) {
      case java.util.zip.ZipEntry.DEFLATED:
        return "deflated";
      case java.util.zip.ZipEntry.STORED:
        return "stored";
      default:
        return method.toString();
    }
  }

  /**
   * Returns the position fo this entry in the stream.
   */
  public long getPosition()
  {
    return _position;
  }

  public ZipEntry getZipEntry()
  {
    return _zipEntry;
  }

  public String toString()
  {
    return "QuercusZipEntry[]";
  }
}
