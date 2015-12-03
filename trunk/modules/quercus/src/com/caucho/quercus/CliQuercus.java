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

package com.caucho.quercus;

import com.caucho.quercus.env.CliEnv;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.servlet.api.QuercusHttpServletRequest;
import com.caucho.quercus.servlet.api.QuercusHttpServletResponse;
import com.caucho.vfs.WriteStream;

import java.io.IOException;

public class CliQuercus extends Quercus
{
  @Override
  public Env createEnv(QuercusPage page,
                       WriteStream out,
                       QuercusHttpServletRequest request,
                       QuercusHttpServletResponse response)
  {
    return new CliEnv(this, page, out, getArgv());
  }

  public static void main(String []args)
    throws IOException
  {
    CliQuercus quercus = new CliQuercus();

    startMain(args, quercus);
  }

  /**
   * Hard-coded to true for CLI according to php.net.
   */
  @Override
  public boolean isRegisterArgv() {
    return true;
  }
}
