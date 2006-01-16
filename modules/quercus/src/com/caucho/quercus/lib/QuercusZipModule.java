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

import java.util.logging.Logger;

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

  public ZipFileClass zip_open(String fileName)
    throws IOException
  {
    return new ZipFileClass(fileName);
  }

  public ZipEntryClass zip_read(ZipFileClass zipFile)
    throws IOException
  {
    return zipFile.zip_read();
  }

  // @todo zip_close()
  // @todo zip_entry_close()
  // @todo zip_entry_compressedsize()
  // @todo zip_entry_compressionmethod()
  // @todo zip_entry_filesize()
  // @todo zip_entry_name()
  // @todo zip_entry_open()
  // @todo zip_read()
}
