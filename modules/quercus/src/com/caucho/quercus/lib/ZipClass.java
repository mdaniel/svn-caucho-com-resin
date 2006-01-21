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

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Enumeration;

import java.io.IOException;

import com.caucho.util.L10N;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.vfs.Path;

/**
 * Zip object oriented API facade
 */

public class ZipClass {
  private static final Logger log = Logger.getLogger(ZipClass.class.getName());
  private static final L10N L = new L10N(ZipClass.class);

  private ZipInputStream _zipInputStream;

  public ZipClass(Path zipPath)
  {
    try {
      _zipInputStream = new ZipInputStream(zipPath.openRead());
    } catch (IOException ex) {
      log.log(Level.FINE,  ex.toString(),  ex);
    }
  }

  /**
   *
   *
   * @return next zip_entry or null
   */
  public Value zip_read(Env env)
    throws IOException
  {
    ZipEntry entry;

    if ((entry = _zipInputStream.getNextEntry()) != null)
      return env.wrapJava(new ZipEntryClass(entry));
    else
      return BooleanValue.FALSE;
  }

  /**
   *
   * @return always true, just there so Quercus can see the function
   * @throws IOException
   */
  public boolean zip_close()
    throws IOException
  {
    _zipInputStream.close();
    return true;
  }

  public ZipInputStream getZipInputStream()
  {
    return _zipInputStream;
  }
}
