/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

import com.caucho.config.ConfigException;
import com.caucho.loader.*;
import com.caucho.quercus.annotation.ClassImplementation;
import com.caucho.quercus.env.*;
import com.caucho.quercus.lib.file.FileModule;
import com.caucho.quercus.lib.session.QuercusSessionManager;
import com.caucho.quercus.module.ModuleContext;
import com.caucho.quercus.module.ResinModuleContext;
import com.caucho.quercus.module.ModuleInfo;
import com.caucho.quercus.module.ModuleStartupListener;
import com.caucho.quercus.module.QuercusModule;
import com.caucho.quercus.module.StaticFunction;
import com.caucho.quercus.page.InterpretedPage;
import com.caucho.quercus.page.PageManager;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.ClassDef;
import com.caucho.quercus.program.InterpretedClassDef;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.server.webapp.*;
import com.caucho.server.session.*;
import com.caucho.sql.*;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.util.LruCache;
import com.caucho.util.TimedCache;
import com.caucho.vfs.*;
import com.caucho.java.*;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.sql.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Facade for the PHP language.
 */
public class ResinQuercus extends Quercus
{
  private static L10N L = new L10N(ResinQuercus.class);
  private static final Logger log = Log.open(ResinQuercus.class);
  
  private static EnvironmentLocal<ModuleContext> _localModuleContext
    = new EnvironmentLocal<ModuleContext>();

  private WebApp _webApp;
  
  /**
   * Constructor.
   */
  public ResinQuercus()
  {
    super();

    setPwd(Vfs.lookup());
    setWorkDir(WorkDir.getLocalWorkDir());
  }

  public void setWebApp(WebApp webApp)
  {
    _webApp = webApp;
  }

  public WebApp getWebApp()
  {
    return _webApp;
  }

  @Override
  public ModuleContext getLocalContext(ClassLoader loader)
  {
    synchronized (_localModuleContext) {
      ModuleContext context = _localModuleContext.getLevel(loader);

      if (context == null) {
	context = createModuleContext(loader);
	_localModuleContext.set(context, loader);
      }

      return context;
    }
  }

  @Override
  protected ModuleContext createModuleContext(ClassLoader loader)
  {
    return new ResinModuleContext(loader);
  }

  public String getCookieName()
  {
    SessionManager sm = getSessionManager();

    if (sm != null)
      return sm.getCookieName();
    else
      return "JSESSIONID";
  }

  public SessionManager getSessionManager()
  {
    if (_webApp != null)
      return _webApp.getSessionManager();
    else
      return null;
  }

  @Override
  public String getVersion()
  {
    return com.caucho.Version.VERSION;
  }

  @Override
  public String getVersionDate()
  {
    return com.caucho.Version.VERSION_DATE;
  }

  @Override
  public DataSource findDatabase(String driver, String url)
  {
    try {
      if (getDatabase() != null)
	return getDatabase();
      else
	return DatabaseManager.findDatabase(url, driver);
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Unwrap connection if necessary.
   */
  @Override
  public Connection getConnection(Connection conn)
  {
    try {
      return ((UserConnection) conn).getConnection();
    } catch (Exception e) {
      throw new QuercusModuleException(e);
    }
  }

  /**
   * Unwrap statement if necessary.
   */
  public Statement getStatement(Statement stmt)
  {
    return ((UserStatement) stmt).getStatement();
  }
  
  /*
   * Returns true if Quercus is running under Resin.
   */
  @Override
  public boolean isResin()
  {
    return true;
  }
}

