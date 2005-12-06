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

package com.caucho.php.servlet;

import java.io.PrintWriter;
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

import com.caucho.php.Php;
import com.caucho.php.PhpExitException;
import com.caucho.php.PhpRuntimeException;

import com.caucho.php.gen.PhpGenerator;

import com.caucho.php.env.Env;

import com.caucho.php.parser.PhpParser;

import com.caucho.php.program.PhpProgram;

import com.caucho.php.module.PhpModule;

import com.caucho.php.page.PhpPage;

import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * Servlet to call PHP through javax.script.
 */
public class PhpServlet extends HttpServlet {
  private static final L10N L = new L10N(PhpServlet.class);
  private static final Logger log
    = Logger.getLogger(PhpServlet.class.getName());

  private final Php _php = new Php();

  private boolean _isCompileSet;

  /**
   * Set true if php should be compiled into Java.
   */
  public void setCompile(String isCompile)
    throws ConfigException
  {
    _isCompileSet = true;

    if ("true".equals(isCompile) || "".equals(isCompile)) {
      _php.setCompile(true);
      _php.setLazyCompile(false);
    }
    else if ("false".equals(isCompile)) {
      _php.setCompile(false);
      _php.setLazyCompile(false);
    }
    else if ("lazy".equals(isCompile)) {
      _php.setLazyCompile(true);
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
    _php.setDatabase(database);
  }

  /**
   * Adds a php module.
   */
  public void addModule(PhpModule module)
    throws ConfigException
  {
    _php.addModule(module);
  }
  
  /**
   * Adds a php class.
   */
  public void addClass(PhpClassConfig classConfig)
    throws ConfigException
  {
    _php.addJavaClass(classConfig.getName(), classConfig.getType());
  }

  /**
   * Adds a php.ini configuration
   */
  public PhpIni createPhpIni()
    throws ConfigException
  {
    return new PhpIni(_php);
  }
  
  /**
   * Gets the script manager.
   */
  public void init()
    throws ServletException
  {
    if (! _isCompileSet) {
      _php.setLazyCompile(true);
      
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

      PhpPage page = _php.parse(path);

      // php/1b06
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
	
      Env env = new Env(_php, page, ws, request, response);
      try {
	page.executeTop(env);

	return;
      } catch (PhpExitException e) {
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
    } catch (PhpExitException e) {
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
    _php.close();
  }

  public static class PhpIni {
    private Php _php;

    PhpIni(Php php)
    {
      _php = php;
    }

    /**
     * Sets an arbitrary property.
     */
    public void put(String key, String value)
    {
      _php.setIni(key, value);
    }
  }
}

