/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.io.IOException;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.env.*;
import com.caucho.vfs.Path;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * PHP Zip
 */

public class QuercusZipModule extends AbstractQuercusModule {

  private static final Logger log = Log.open(QuercusZipModule.class);
  private static final L10N L = new L10N(QuercusZipModule.class);

  /**
   * Returns true for the Zip extension.
   */
  public boolean isExtensionLoaded(String name)
  {
    return "zip".equals(name);
  }

  public Value zip_open(Env env,
                        @NotNull Path path)
    throws IOException
  {
    Value value = BooleanValue.FALSE;

    if (path == null)
      return value;

    ZipClass zip = new ZipClass(path);

    if (zip.getZipInputStream() == null)
      return BooleanValue.FALSE;
    else
      return env.wrapJava(zip);
  }

  public Value zip_read(Env env,
                        @NotNull ZipClass zipFile)
    throws IOException
  {
    if (zipFile == null)
      return NullValue.NULL;

    return zipFile.zip_read(env);
  }

  /**
   *
   * @param zipEntry
   * @return false if zipEntry is null
   */
  public Value zip_entry_name(@NotNull ZipEntryClass zipEntry)
  {
    if (zipEntry == null)
      return BooleanValue.FALSE;

    return new StringValue(zipEntry.zip_entry_name());
  }

  /**
   *
   * @param zipEntry
   * @return false if zipEntry is null
   */
  public Value zip_entry_filesize(@NotNull ZipEntryClass zipEntry)
  {
    if (zipEntry == null)
      return BooleanValue.FALSE;

    return new LongValue(zipEntry.zip_entry_filesize());
  }

  public boolean zip_close(@NotNull ZipClass zipFile)
    throws IOException
  {
    if (zipFile != null)
      zipFile.zip_close();

    return true;
  }

  /**
   *
   * @param file
   * @param entry
   * @param mode ignored - always "rb" from fopen()
   * @return true on success or false on failure
   */
  public boolean zip_entry_open(@NotNull ZipClass file,
                                @NotNull ZipEntryClass entry,
                                @Optional String mode)
  {
    if ((file == null) || (entry == null))
      return false;

    return entry.zip_entry_open(file);
  }

  /**
   *
   * @param entry
   * @return always true.  This has no meaning.
   */
  public boolean zip_entry_close(@NotNull ZipEntryClass entry)
  {
    if (entry != null)
      entry.zip_entry_close();

    return true;
  }

  /**
   *
   *
   * @param entry
   * @param length
   * @return false or string
   */
  public Value zip_entry_read(@NotNull ZipEntryClass entry,
                              @Optional("1024") int length)
  {
    if (entry == null)
      return BooleanValue.FALSE;

    return entry.zip_entry_read(length);
  }

  /**
   *
   * @param entry
   * @return empty string, stored or deflated
   */
  public String zip_entry_compressionmethod(@NotNull ZipEntryClass entry)
  {
    if (entry == null)
      return "";

    return entry.zip_entry_compressionmethod();
  }

  /**
   *
   * @param entry
   * @return -1, or compressed size
   */
  public long zip_entry_compressedsize(@NotNull ZipEntryClass entry)
  {
    if (entry == null)
      return -1;

    return entry.zip_entry_compressedsize();
  }
}
