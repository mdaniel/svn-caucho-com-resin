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
 * @author Scott Ferguson
 */

package com.caucho.quercus.servlet;

import com.caucho.quercus.GoogleQuercus;
import com.caucho.quercus.QuercusContext;
import com.caucho.util.L10N;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet to call PHP through javax.script.
 */
public class GoogleQuercusServletImpl extends QuercusServletImpl
{
  private static final L10N L = new L10N(GoogleQuercusServletImpl.class);
  private static final Logger log
    = Logger.getLogger(GoogleQuercusServletImpl.class.getName());

  protected final String _gsBucket;

  public GoogleQuercusServletImpl(String gsBucket)
  {
    super();

    _gsBucket = gsBucket;
  }

  /**
   * Returns the Quercus instance.
   */
  @Override
  protected QuercusContext getQuercus()
  {
    if (_quercus == null) {
      _quercus = new GoogleQuercus();

      if (_gsBucket != null) {
        _quercus.setIni("google.cloud_storage_bucket", _gsBucket);
      }
    }

    return _quercus;
  }

  @Override
  protected void handleThrowable(HttpServletResponse response, Throwable e)
    throws IOException, ServletException
  {
    log.log(Level.WARNING, e.toString(), e);

    OutputStream os = response.getOutputStream();
    WriteStream out = Vfs.openWrite(os);
    out.println(e);
    out.close();
  }
}

