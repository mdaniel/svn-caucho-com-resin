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

import com.caucho.hmpp.packet.QuerySet;
import com.caucho.hmpp.packet.QueryResult;
import com.caucho.hmpp.packet.QueryGet;
import com.caucho.hmpp.packet.QueryError;
import com.caucho.hmpp.packet.Message;
import com.caucho.hmpp.HmppError;
import java.io.*;
import java.util.logging.*;

import com.caucho.hessian.io.*;
import com.caucho.hemp.*;
import com.caucho.hmpp.MessageHandler;
import com.caucho.hmpp.QueryHandler;

/**
 * Handles callbacks for a hmpp service
 */
public class HmppServiceHandler
  implements MessageHandler, QueryHandler
{
  private static final Logger log
    = Logger.getLogger(HmppServiceHandler.class.getName());

  private ServerPacketHandler _packetHandler;
  private Hessian2StreamingOutput _out;

  HmppServiceHandler(ServerPacketHandler packetHandler,
		     Hessian2StreamingOutput out)
  {
    _packetHandler = packetHandler;
    _out = out;
  }
  
  public void onMessage(String to, String from, Serializable value)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send message to=" + to
		  + " from=" + from);
      }
      
      _out.writeObject(new Message(to, from, value));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  public Serializable onQuery(String to,
			      String from,
			      Serializable query)
  {
    return null;
  }
  
  public boolean onQueryGet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " queryGet to=" + to
		  + " from=" + from);
      }
      
      _out.writeObject(new QueryGet(id, to, from, query));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
    
    return true;
  }
  
  public boolean onQuerySet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " querySet to=" + to
		  + " from=" + from);
      }
      
      _out.writeObject(new QuerySet(id, to, from, query));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
    
    return true;
  }
  
  public void onQueryResult(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send query result to=" + to
		  + " from=" + from);
      }
      
      _out.writeObject(new QueryResult(id, to, from, value));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
  
  public void onQueryError(long id,
			   String to,
			   String from,
			   Serializable query,
			   HmppError error)
  {
    try {
      if (log.isLoggable(Level.FINER)) {
	log.finer(_packetHandler + " send " + error + " to=" + to
		  + " from=" + from);
      }
      
      _out.writeObject(new QueryError(id, to, from, query, error));
      _out.flush();
    } catch (IOException e) {
      _packetHandler.close();
      
      log.log(Level.FINE, e.toString(), e);
    }
  }
}
