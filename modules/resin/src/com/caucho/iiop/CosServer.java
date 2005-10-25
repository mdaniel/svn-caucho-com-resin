/*
 * Copyright (c) 1998-2000 Caucho Technology -- all rights reserved
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

package com.caucho.iiop;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.io.*;
import javax.naming.*;

import org.omg.CosNaming.*;
import org.omg.CosNaming.NamingContextPackage.*;

import com.caucho.log.Log;

import com.caucho.vfs.*;
import com.caucho.server.*;
import com.caucho.server.http.*;
import com.caucho.naming.*;

public class CosServer {
  private static final Logger log = Log.open(CosServer.class);
    
  private IiopProtocol _iiopServer;
  private String _host;
  private int _port;
  
  AbstractModel _model = new MemoryModel();

  CosServer(IiopProtocol iiopServer)
  {
    _iiopServer = iiopServer;
  }

  void setHost(String host)
  {
    _host = host;
  }

  void setPort(int port)
  {
    _port = port;
  }

  public org.omg.CORBA.Object resolve(NameComponent []n)
    throws NotFound, CannotProceed, InvalidName
  {
    Object value = null;
    String host = _host;
    String uri = "";

    try {
      if (log.isLoggable(Level.FINE)) {
	String name = "";
	  
	for (int i = 0; i < n.length; i++)
	  name += "/"  + n[i].id;

	log.fine("IIOP NameService lookup: " + name);
      }
      
      for (int i = 0; i < n.length; i++) {
        String name = n[i].id;
        String type = n[i].kind;

        value = _model.lookup(name);

        if (value != null)
          continue;

	/*
        if (i == 0) {
        }
        else if (i == 1) {
          host = name;
          continue;
        }
        else {
          uri += "/" + name;
          continue;
        }
	*/
	uri += "/" + name;
      }

      IiopSkeleton skel;
      
      if (value != null) {
      }
      else if (uri.equals("")) {
        String oid = "/NameService";
        skel = _iiopServer.getService(_host, _port, oid);

	return skel;
      }
      else if ((skel = _iiopServer.getService(_host, _port, uri)) != null) {
	return skel;
      }
    } catch (NamingException e) {
      log.log(Level.FINE, e.toString(), e);
      
      throw new NotFound(NotFoundReason.from_int(NotFoundReason._missing_node), n);
    }

    log.fine("IIOP COS NotFound: " + uri);

    throw new NotFound(NotFoundReason.from_int(NotFoundReason._missing_node), n);
  }
}
