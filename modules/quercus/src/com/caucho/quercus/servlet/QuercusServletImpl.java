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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.servlet;

import com.caucho.config.ConfigException;
import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusDieException;
import com.caucho.quercus.QuercusExitException;
import com.caucho.quercus.QuercusLineRuntimeException;
import com.caucho.quercus.QuercusRequestAdapter;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusValueException;
import com.caucho.quercus.module.QuercusModule;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet to call PHP through javax.script.
 */
public class QuercusServletImpl
{
  private static final L10N L = new L10N(QuercusServletImpl.class);
  private static final Logger log
    = Logger.getLogger(QuercusServletImpl.class.getName());

  protected Quercus _quercus;

  private ServletConfig _config;
  private ServletContext _servletContext;

  /**
   * initialize the script manager.
   */
  public void init(ServletConfig config)
    throws ServletException
  {
    _config = config;
    _servletContext = config.getServletContext();

    getQuercus().setPwd(new FilePath(_servletContext.getRealPath("/")));
  }

  /**
   * Service.
   */
  public void service(HttpServletRequest request,
                      HttpServletResponse response)
    throws ServletException, IOException
  {
    try {
      Path path = getPath(request);

      QuercusPage page = getQuercus().parse(path);

      WriteStream ws;
      
      OutputStream out = response.getOutputStream();
      
      VfsStream s = new VfsStream(null, out);
      ws = new WriteStream(s);

      Env env = getQuercus().createEnv(page, ws, request, response);
      try {
        env.setGlobalValue("request", env.wrapJava(request));
        env.setGlobalValue("response", env.wrapJava(request));

        env.start();

	String prepend = env.getIniString("auto_prepend_file");
	if (prepend != null) {
	  QuercusPage prependPage = getQuercus().parse(env.lookup(prepend));
	  prependPage.executeTop(env);
	}

        page.executeTop(env);

	String append = env.getIniString("auto_append_file");
	if (append != null) {
	  QuercusPage appendPage = getQuercus().parse(env.lookup(append));
	  appendPage.executeTop(env);
	}
     //   return;
      }
      catch (QuercusExitException e) {
        throw e;
      }
      catch (QuercusLineRuntimeException e) {
        log.log(Level.FINE, e.toString(), e);

      //  return;
      }
      catch (QuercusValueException e) {
        log.log(Level.FINE, e.toString(), e);
	
	ws.println(e.toString());

      //  return;
      }
      catch (Throwable e) {
        if (response.isCommitted())
          e.printStackTrace(ws.getPrintWriter());

        ws = null;

        throw e;
      }
      finally {
        env.close();

        // don't want a flush for an exception
        if (ws != null)
          ws.close();
      }
    }
    catch (QuercusDieException e) {
      log.log(Level.FINE, e.toString(), e);
      // normal exit
    }
    catch (QuercusExitException e) {
      log.log(Level.FINER, e.toString(), e);
      // normal exit
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Throwable e) {
      throw new ServletException(e);
    }
  }

  Path getPath(HttpServletRequest req)
  {
    String scriptPath = QuercusRequestAdapter.getPageServletPath(req);
    String pathInfo = QuercusRequestAdapter.getPagePathInfo(req);

    Path pwd = new FilePath(System.getProperty("user.dir"));

    Path path = pwd.lookup(req.getRealPath(scriptPath));

    if (path.isFile())
      return path;

    // XXX: include

    String fullPath;
    if (pathInfo != null)
      fullPath = scriptPath + pathInfo;
    else
      fullPath = scriptPath;

    return pwd.lookup(req.getRealPath(fullPath));
  }

  /**
   * Returns the Quercus instance.
   */
  protected Quercus getQuercus()
  {
    synchronized (this) {
      if (_quercus == null)
	_quercus = new Quercus();
    }

    return _quercus;
  }

  /**
   * Gets the script manager.
   */
  public void destroy()
  {
    _quercus.close();
  }
}

