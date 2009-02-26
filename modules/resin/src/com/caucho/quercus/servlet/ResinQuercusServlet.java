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

package com.caucho.quercus.servlet;

import com.caucho.quercus.*;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.QuercusValueException;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.server.connection.CauchoResponse;
import com.caucho.server.resin.Resin;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet to call PHP through javax.script.
 */
public class ResinQuercusServlet extends QuercusServletImpl
{
  private static final L10N L = new L10N(ResinQuercusServlet.class);
  private static final Logger log
    = Logger.getLogger(ResinQuercusServlet.class.getName());

  private WebApp _webApp;

  /**
   * initialize the script manager.
   */
  @Override
  public void init(ServletConfig config)
    throws ServletException
  {
    super.init(config);

    _webApp = (WebApp) config.getServletContext();

    ResinQuercus quercus = (ResinQuercus) getQuercus();
    
    quercus.setWebApp(_webApp);
    getQuercus().setPwd(Vfs.lookup());

    quercus.setIni("caucho.server_id", Resin.getLocal().getServerId());

    quercus.start();
  }

  /**
   * Service.
   */
  public void service(HttpServletRequest request,
                      HttpServletResponse response)
    throws ServletException, IOException
  {
    Env env = null;
    WriteStream ws = null;
    
    try {
      Path path = getPath(request);

      QuercusPage page;

      try {
        page = getQuercus().parse(path);
      }
      catch (FileNotFoundException ex) {
        // php/2001
        log.log(Level.FINER, ex.toString(), ex);

        response.sendError(HttpServletResponse.SC_NOT_FOUND);

        return;
      }


      // XXX: check if correct.  PHP doesn't expect the lower levels
      // to deal with the encoding, so this may be okay
      if (response instanceof CauchoResponse) {
        ws = Vfs.openWrite(((CauchoResponse) response).getResponseStream());
      } else {
        OutputStream out = response.getOutputStream();

        ws = Vfs.openWrite(out);
      }
      
      // php/6006
      ws.setNewlineString("\n");

      Quercus quercus = getQuercus();
      
      env = quercus.createEnv(page, ws, request, response);
      quercus.setServletContext(_servletContext);
      
      try {
        env.start();
        
        env.setGlobalValue("request", env.wrapJava(request));
        env.setGlobalValue("response", env.wrapJava(response));
        env.setGlobalValue("servletContext", env.wrapJava(_servletContext));

        Value prepend = quercus.getIniValue("auto_prepend_file");
        if (! prepend.isNull()) {
          Path prependPath = env.lookup(prepend.toStringValue(env));
          
          if (prependPath == null)
            env.error(L.l("auto_prepend_file '{0}' not found.", prepend));
          else {
            QuercusPage prependPage = getQuercus().parse(prependPath);
            prependPage.executeTop(env);
          }
        }

        env.executeTop();

        Value append = quercus.getIniValue("auto_append_file");
        if (! append.isNull()) {
          Path appendPath = env.lookup(append.toStringValue(env));
          
          if (appendPath == null)
            env.error(L.l("auto_append_file '{0}' not found.", append));
          else {
            QuercusPage appendPage = getQuercus().parse(appendPath);
            appendPage.executeTop(env);
          }
        }
        //   return;
      }
      catch (QuercusExitException e) {
        throw e;
      }
      catch (QuercusErrorException e) {
        throw e;
      }
      catch (QuercusLineRuntimeException e) {
        log.log(Level.FINE, e.toString(), e);

	ws.println(e.getMessage());
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
        if (env != null)
          env.close();
        
        // don't want a flush for an exception
        if (ws != null)
          ws.close();
      }
    }
    catch (QuercusDieException e) {
      // normal exit
      log.log(Level.FINE, e.toString(), e);
    }
    catch (QuercusExitException e) {
      // normal exit
      log.log(Level.FINER, e.toString(), e);
    }
    catch (QuercusErrorException e) {
      // error exit
      log.log(Level.FINE, e.toString(), e);
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
   * Returns the Quercus instance.
   */
  @Override
  protected Quercus getQuercus()
  {
    synchronized (this) {
      if (_quercus == null)
	_quercus = new ResinQuercus();
    }

    return _quercus;
  }
}

