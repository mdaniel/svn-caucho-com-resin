/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.server.admin;

import com.caucho.v5.amp.Amp;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.websocket.server.WebSocketServletDispatch.WebSocketContextDispatch;


/**
 * Admin access as a rest servlet.
 */
public class AdminServlet // extends JampServletBase
{
  private ServiceManagerAmp _ampManager;
  private WebSocketContextDispatch _wsCxt;
  
  public AdminServlet()
  {
    _ampManager = Amp.newManager();
    
    _ampManager.newService(new AdminRestServiceImpl())
               .address("public:///manager")
               .ref();
  }
  
  public void setRequireSecure(boolean isSecure)
  {
  }
  
  /*
  @Override
  protected String getServicePath(HttpServletRequest req,
                                  PodContext podContext)
  {
    System.out.println("PI0: " + req.getPathInfo());
    return "/manager";
  }
  
  @Override
  protected String getMethodName(HttpServletRequest req,
                                  PodContext podContext)
  {
    String pathInfo = req.getPathInfo();

    if (pathInfo == null) {
      return null;
    }
    else {
      return pathInfo.substring(1);
    }
  }
  */

  /*
  @Override
  protected WebSocketContext getContext(String pathInfo)
  {
    WebSocketContext wsCxt = _wsCxt;
    
    if (wsCxt == null) {
      PodContext podContext = getPodContextByName(podName);
      
      EndpointJampConfigServer config;
      
      config = new EndpointJampConfigServer(podName,
                                            podContext.getAmpManager(),
                                            getChannelContext(),
                                            podContext.getWsBrokerFactory(),
                                            _jsonFactory);
      
      _wsCxt = wsCxt = new WebSocketContext(config);

      System.out.println("WS: " + wsCxt);
    }

    return wsCxt;
  }
  */

  /*
  @Override
  protected ServiceManagerAmp createAmpManager(String podName)
  {
    return _ampManager;
  }
  */
}
