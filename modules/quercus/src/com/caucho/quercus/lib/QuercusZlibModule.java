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

import java.util.logging.Logger;

/**
 * PHP ZLib
 */
public class QuercusZlibModule extends AbstractQuercusModule {

  private static final Logger log = Log.open(QuercusMysqlModule.class);
  private static final L10N L = new L10N(QuercusMysqlModule.class);

  /**
   * Returns true for the Zlib extension.
   */
  public boolean isExtensionLoaded(String name)
  {
    return "zlib".equals(name);
  }

  // @todo gzclose()
  // @todo gzcompress()
  // @todo gzdeflate()
  // @todo gzencode()
  // @todo gzeof()
  // @todo gzfile()
  // @todo gzgetc()
  // @todo gzgets()
  // @todo gzgetss()
  // @todo gzinflate()
  // @todo gzopen()
  // @todo gzpassthru()
  // @todo gzputs()
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
