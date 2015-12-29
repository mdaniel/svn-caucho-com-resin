/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.websocket;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a server endpoint that can receive new connections.
 */
public abstract class Endpoint
{
  private static final Logger log
    = Logger.getLogger(Endpoint.class.getName());
  
  public abstract void onOpen(Session session, EndpointConfig config);
  
  public void onClose(Session session, CloseReason closeReason)
  {
    try {
      session.close();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }
  
  public void onError(Session session, Throwable throwable)
  {
  }
}
