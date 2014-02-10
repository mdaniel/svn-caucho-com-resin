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

package com.caucho.quercus;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.GoogleEnv;
import com.caucho.quercus.lib.db.JdbcDriverContext;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.servlet.api.QuercusHttpServletRequest;
import com.caucho.quercus.servlet.api.QuercusHttpServletResponse;
import com.caucho.vfs.GoogleMergePath;
import com.caucho.vfs.MergePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;

/**
 * Facade for the PHP language.
 */
public class GoogleQuercus extends QuercusContext
{
  /**
   * Constructor.
   */
  public GoogleQuercus()
  {
  }

  @Override
  public void init()
  {
    String mode
      = System.getProperty("com.google.appengine.tools.development.ApplicationPreparationMode");

    boolean isGsDisabled = "true".equals(mode);

    if (! isGsDisabled) {
      String gsBucket = getIniString("google.cloud_storage_bucket");

      if (gsBucket != null) {
        Path stdPwd = getPwd();

        GoogleMergePath mergePwd = new GoogleMergePath(stdPwd, gsBucket, true);
        setPwd(mergePwd);

        Path webInfDir = getWebInfDir();
        Path gsWebInfDir = mergePwd.getGooglePath().lookup("WEB-INF");
        MergePath mergeWebInf = new MergePath(gsWebInfDir, webInfDir);

        setWebInfDir(mergeWebInf);
      }
    }

    super.init();

    JdbcDriverContext jdbcDriverContext = getJdbcDriverContext();

    String driver = getIniString("google.jdbc_driver");

    if (driver == null) {
      driver = "com.google.appengine.api.rdbms.AppEngineDriver";
    }

    jdbcDriverContext.setDefaultDriver(driver);
    jdbcDriverContext.setDefaultUrlPrefix("jdbc:google:rdbms://");
    jdbcDriverContext.setDefaultEncoding(null);
    jdbcDriverContext.setProtocol("mysql", driver);
    jdbcDriverContext.setProtocol("google:rdbms", driver);
  }

  @Override
  public Env createEnv(QuercusPage page,
                       WriteStream out,
                       QuercusHttpServletRequest request,
                       QuercusHttpServletResponse response)
  {
    return new GoogleEnv(this, page, out, request, response);
  }
}

