/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.servlets.naming;

import com.caucho.naming.*;

import javax.naming.*;

import java.util.*;
import java.util.logging.*;

/**
 * Hessian based model for JNDI.
 */
public class RemoteJndiService implements NamingProxy
{
  private static final Logger log
    = Logger.getLogger(RemoteJndiService.class.getName());
  
  private String _jndiPath;
  
  public void setJndiPath(String jndiPath)
  {
    _jndiPath = jndiPath;
  }
    
  /**
   * Looks up a child
   */
  public Object lookup(String name)
  {
    try {
      if (name.startsWith("/"))
        name = name.substring(1);
      
      InitialContext ic = new InitialContext();
      Object v = ic.lookup(_jndiPath);

      if (v == null || name.equals(""))
        return v;
      else if (v instanceof Context)
        v = ((Context) v).lookup(name);

      if (log.isLoggable(Level.FINER))
        log.finer(this + " lookup '" + name + "' -> " + v);

      if (v instanceof Context)
        return new RemoteContext(name);
      else
        return v;
    } catch (NamingException e) {
      e.printStackTrace();
      
      log.log(Level.FINEST, e.toString(), e);

      return null;
    }
  }
}
