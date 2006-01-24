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

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Reference;

import com.caucho.quercus.env.*;
import com.caucho.vfs.Path;

import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.io.IOException;

/**
 * PHP ZLib
 */
public class QuercusZlibModule extends AbstractQuercusModule {

  private static final Logger log = Log.open(QuercusZlibModule.class);
  private static final L10N L = new L10N(QuercusZlibModule.class);

  /**
   * Returns true for the Zlib extension.
   */
  public boolean isExtensionLoaded(String name)
  {
    return "zlib".equals(name);
  }

  /**
   *
   * @param env
   * @param fileName
   * @param mode
   * @param useIncludePath always on
   * @return ZlibClass
   */
  public Value gzopen(Env env,
                      @NotNull String fileName,
                      @NotNull String mode,
                      @Optional("0") int useIncludePath)
  {
    if (fileName == null)
      return BooleanValue.FALSE;

    ZlibClass zlib = new ZlibClass(env, fileName, mode, useIncludePath);
    return env.wrapJava(zlib);
  }

  public int gzwrite(Env env,
                     @NotNull ZlibClass zp,
                     @NotNull String s,
                     @Optional("0") int length)
  {
    if ((zp == null) || (s == null) || (s == ""))
      return 0;

    return zp.gzwrite(env, s,length);
  }

  /**
   *
   * @param env
   * @param zp
   * @param s
   * @param length
   * @return alias of gzwrite
   */
  public int gzputs(Env env,
                    @NotNull ZlibClass zp,
                    @NotNull String s,
                    @Optional("0") int length)
  {
    return gzwrite(env, zp, s, length);
  }

  public boolean gzclose(@NotNull ZlibClass zp)
  {
    if (zp == null)
      return false;

    return zp.gzclose();
  }

  public Value gzgetc(Env env,
                      @NotNull ZlibClass zp)
    throws IOException, DataFormatException
  {
    if (zp == null)
      return BooleanValue.FALSE;

    return zp.gzgetc(env);
  }
  // @todo gzcompress()
  // @todo gzdeflate()
  // @todo gzencode()
  // @todo gzeof()
  // @todo gzfile()
  // @todo gzgets()
  // @todo gzgetss()
  // @todo gzinflate()
  // @todo gzpassthru()
  // @todo gzread()
  // @todo gzread()
  // @todo gzrewind()
  // @todo gzseek()
  // @todo gztell()
  // @todo gzuncompress()
  // @todo gzwrite()
  // @todo readgzfile()
  // @todo zlib_get_coding_type()
}
