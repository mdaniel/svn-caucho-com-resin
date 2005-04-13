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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.naming;

import java.io.*;
import java.util.*;

import javax.naming.*;
import javax.ejb.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.naming.*;

import com.caucho.ejb.EnvServerManager;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;

import com.caucho.ejb.protocol.EjbProtocolManager;

/**
 * JNDI context for Local requests.
 */
public class LocalModel extends AbstractModel {
  protected EjbProtocolManager _protocolManager;
  protected String _prefix;

  /**
   * Creates a new Local Context.
   */
  public LocalModel(EjbProtocolManager protocolManager)
  {
    _protocolManager = protocolManager;
    _prefix = "";
  }

  /**
   * Creates a subcontext.
   *
   * @param env the JNDI environment
   * @param prefix the URL prefix
   */
  public LocalModel(EjbProtocolManager protocolManager, String prefix)
  {
    _protocolManager = protocolManager;
    
    if (! prefix.endsWith("/"))
      prefix = prefix + "/";
    
    if (! prefix.startsWith("/"))
      prefix = "/" + prefix;
    
    _prefix = prefix;
  }

  /**
   * Creates a new instance of MemoryModel.
   */
  protected AbstractModel create()
  {
    return new LocalModel(_protocolManager, _prefix);
  }

  public EnvServerManager getServerContainer()
  {
    return _protocolManager.getServerManager();
  }

  /**
   * Looks up the named bean.  Since we're assuming only a single level,
   * just try to look it up directly.
   *
   * @param name the segment name to lookup
   *
   * @return the home stub.
   */
  public Object lookup(String name)
    throws NamingException
  {
    try {
      // _protocolManager.initJdbc();
      
      AbstractServer server = _protocolManager.getServerByEJBName(_prefix + name);

      if (server != null)
        return server.getEJBLocalHome();
      else {
        String newPrefix = _prefix + name + "/";

        ArrayList list = _protocolManager.getLocalChildren(newPrefix);

        if (list != null && list.size() > 0)
          return new LocalModel(_protocolManager, newPrefix);

        if (newPrefix.equals("caucho-ejb-admin/"))
          return _protocolManager.getServerManager().getAdmin();
        
        return null;
      }
    } catch (Exception e) {
      throw new NamingExceptionWrapper(e);
    }
  }

  /**
   * Can't currently bind.
   */
  public void bind(String name, Object value)
    throws NamingException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Can't currently unbind.
   */
  public void unbind(String name)
    throws NamingException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Can't currently destroy a subcontext
   */
  public void destroySubcontext(String name)
    throws NamingException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Return the names.
   */
  public List list()
  {
    ArrayList list = _protocolManager.getLocalChildren(_prefix);

    return list;
  }

  public AbstractModel createSubcontext(String name)
    throws NamingException
  {
    throw new UnsupportedOperationException();
  }

  public String toString()
  {
    return "[LocalModel " + _prefix + "]";
  }
}
