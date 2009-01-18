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

package com.caucho.hemp.servlet;

import java.io.*;
import javax.servlet.*;

import com.caucho.config.ConfigException;
import com.caucho.hemp.*;
import com.caucho.bam.BamBroker;
import com.caucho.server.connection.*;
import com.caucho.util.L10N;
import com.caucho.vfs.*;
import com.caucho.webbeans.manager.*;
import javax.servlet.http.HttpServletResponse;
import javax.webbeans.In;

/**
 * Main protocol handler for the HTTP version of HeMPP.
 */
public class HempServlet extends GenericServlet {
  private static final L10N L = new L10N(HempServlet.class);
  
  @In(optional=true)
  private BamBroker _broker;

  public void setBroker(BamBroker broker)
  {
    if (broker != null)
      _broker = broker;
  }

  @Override
  public void init()
  {
    if (_broker == null) {
      throw new ConfigException(L.l("{0}: broker is required",
				    this));
    }
  }
  
  /**
   * Service handling
   */
  public void service(ServletRequest request, ServletResponse response)
    throws IOException, ServletException
  {
    HttpServletRequestImpl req = (HttpServletRequestImpl) request;
    HttpServletResponseImpl res = (HttpServletResponseImpl) response;

    String upgrade = req.getHeader("Upgrade");

    if (! "HMTP/0.9".equals(upgrade)) {
      // eventually can use alt method
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    ReadStream is = req.getConnection().getReadStream();
    WriteStream os = req.getConnection().getWriteStream();

    TcpDuplexController controller
      = res.upgradeProtocol(new FromClientLinkStream(_broker, is, os));
    
    controller.setIdleTimeMax(30 * 60 * 1000L);
  }
}
