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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.servlets.ssi;

import com.caucho.loader.DynamicClassLoader;
import com.caucho.log.Log;
import com.caucho.server.connection.*;
import com.caucho.filters.*;
import com.caucho.util.CompileException;
import com.caucho.util.L10N;
import com.caucho.vfs.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Filters the result as SSI.
 *
 * @since Resin 2.0.6
 */
public class SSIFilter implements Filter {
  private static final L10N L = new L10N(SSIFilter.class);
  private static final Logger log
    = Logger.getLogger(SSIFilter.class.getName());
  
  public void init(FilterConfig config)
    throws ServletException
  {
  }
  
  /**
   * Creates a wrapper to save the output.
   */
  public void doFilter(ServletRequest request, ServletResponse response,
                       FilterChain nextFilter)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    SSIResponse ssiResponse = new SSIResponse(req, res);

    nextFilter.doFilter(req, ssiResponse);
    ssiResponse.finish(req, res);
  }
  
  /**
   * Any cleanup for the filter.
   */
  public void destroy()
  {
  }

  class SSIResponse extends CauchoResponseWrapper {
    private HttpServletRequest _request;
    private TempStream _tempStream;
    private WriteStream _out;
    
    SSIResponse(HttpServletRequest request, HttpServletResponse response)
    {
      _request = request;
      
      _tempStream = new TempStream();
      _tempStream.openWrite();
      _out = new WriteStream(_tempStream);

      init(response);
    }

    /**
     * This needs to be bypassed because the file's content
     * length has nothing to do with the returned length.
     */
    public void setContentLength(int length)
    {
    }

    /**
     * Calculates and returns the proper stream.
     */
    protected OutputStream getStream() throws IOException
    {
      return _out;
    }

    /**
     * Complets the request.
     */
    public void finish(HttpServletRequest req,
                       HttpServletResponse res)
      throws IOException, ServletException
    {
      flushBuffer();
      
      _out.close();

      ReadStream is = _tempStream.openRead(true);
      Statement stmt = null;
      try {
	stmt = new SSIParser().parse(is);
      } finally {
	is.close();
      }

      try {
	WriteStream out = Vfs.openWrite(res.getOutputStream());
	stmt.apply(out, req, res);
	out.close();
      } catch (Exception e) {
	e.printStackTrace();
      }
    }
  }
}
