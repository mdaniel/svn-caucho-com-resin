/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import java.io.OutputStream;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.sql.DataSource;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.server.connection.CauchoResponse;

import com.caucho.quercus.Quercus;
import com.caucho.quercus.QuercusExitException;

import com.caucho.quercus.env.Env;

import com.caucho.quercus.module.QuercusModule;

import com.caucho.quercus.page.PhpPage;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * Servlet to call PHP through javax.script.
 */
public class QuercusServlet extends HttpServlet {
  private static final L10N L = new L10N(QuercusServlet.class);
  private static final Logger log
    = Logger.getLogger(QuercusServlet.class.getName());

  private final Quercus _quercus = new Quercus();

  private boolean _isCompileSet;

  /**
   * Set true if quercus should be compiled into Java.
   */
  public void setCompile(String isCompile)
    throws ConfigException
  {
    _isCompileSet = true;

    if ("true".equals(isCompile) || "".equals(isCompile)) {
      _quercus.setCompile(true);
      _quercus.setLazyCompile(false);
    }
    else if ("false".equals(isCompile)) {
      _quercus.setCompile(false);
      _quercus.setLazyCompile(false);
    }
    else if ("lazy".equals(isCompile)) {
      _quercus.setLazyCompile(true);
    }
    else
      throw new ConfigException(L.l("'{0}' is an unknown compile value.  Values are 'true', 'false', or 'lazy'.",
				    isCompile));
  }

  /**
   * Set the default data source.
   */
  public void setDatabase(DataSource database)
  {
    _quercus.setDatabase(database);
  }

  /**
   * Adds a quercus module.
   */
  public void addModule(QuercusModule module)
    throws ConfigException
  {
    _quercus.addModule(module);
  }
  
  /**
   * Adds a quercus class.
   */
  public void addClass(PhpClassConfig classConfig)
    throws ConfigException
  {
    _quercus.addJavaClass(classConfig.getName(), classConfig.getType());
  }

  /**
   * Adds a quercus.ini configuration
   */
  public PhpIni createPhpIni()
    throws ConfigException
  {
    return new PhpIni(_quercus);
  }
  
  /**
   * Gets the script manager.
   */
  public void init()
    throws ServletException
  {
    if (! _isCompileSet) {
      _quercus.setLazyCompile(true);
      
      // XXX: for validating QA
      // throw new ServletException("compile must be set.");
    }
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

      PhpPage page = _quercus.parse(path);

      // quercus/1b06
      response.setContentType("text/html");

      WriteStream ws;

      // XXX: check if correct.  PHP doesn't expect the lower levels
      // to deal with the encoding, so this may be okay
      if (response instanceof CauchoResponse) {
	ws = Vfs.openWrite(((CauchoResponse) response).getResponseStream());
      }
      else {
	OutputStream out = response.getOutputStream();

	ws = Vfs.openWrite(out);
      }
	
      Env env = new Env(_quercus, page, ws, request, response);
      try {
	page.executeTop(env);

	return;
      } catch (QuercusExitException e) {
	throw e;
      } catch (Throwable e) {
	if (response.isCommitted())
	  e.printStackTrace(ws.getPrintWriter());
	
	ws = null;

	throw e;
      } finally {
	env.close();

	// don't want a flush for an exception
	if (ws != null)
	  ws.close();
      }
    } catch (QuercusExitException e) {
      log.log(Level.FINEST, e.toString(), e);
      // normal exit
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new ServletException(e);
    }
  }

  Path getPath(HttpServletRequest req)
  {
    String scriptPath = req.getServletPath();
    String pathInfo = req.getPathInfo();

    Path pwd = Vfs.lookup();
      
    Path path = pwd.lookup(req.getRealPath(scriptPath));

    if (path.isFile())
      return path;
    
    // XXX: include

    String fullPath;
    if (pathInfo != null)
      fullPath = scriptPath + pathInfo;
    else
      fullPath = scriptPath;

    return Vfs.lookup().lookup(req.getRealPath(fullPath));
  }
  
  /**
   * Gets the script manager.
   */
  public void destroy()
  {
    _quercus.close();
  }

  public static class PhpIni {
    private Quercus _quercus;

    PhpIni(Quercus quercus)
    {
      _quercus = quercus;
    }

    /**
     * Sets an arbitrary property.
     */
    public void put(String key, String value)
    {
      _quercus.setIni(key, value);
    }
  }
}

