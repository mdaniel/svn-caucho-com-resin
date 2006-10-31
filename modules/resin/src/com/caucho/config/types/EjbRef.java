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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.config.types;

import java.util.*;
import java.util.logging.*;
import java.rmi.*;

import javax.annotation.*;

import javax.ejb.*;
import javax.naming.*;

import com.caucho.util.*;
import com.caucho.naming.*;
import com.caucho.ejb.*;
import com.caucho.ejb.protocol.*;

/**
 * Configuration for the ejb-ref
 */
public class EjbRef implements ObjectProxy {
  private static final L10N L = new L10N(EjbRef.class);
  private static final Logger log
    = Logger.getLogger(EjbRef.class.getName());

  private String _name;
  private String _type;
  private Class _home;
  private Class _remote;

  private Hashtable _jndiEnv;
  private String _link;

  public EjbRef()
  {
  }

  public void setId(String id)
  {
  }

  public void setDescription(String description)
  {
  }
  
  public void setEjbRefName(String name)
  {
    _name = name;
  }

  /**
   * Returns the ejb name.
   */
  public String getEjbRefName()
  {
    return _name;
  }

  public void setEjbRefType(String type)
  {
    _type = type;
  }

  public void setHome(Class home)
  {
    _home = home;
  }

  /**
   * Returns the home class.
   */
  public Class getHome()
  {
    return _home;
  }

  public void setRemote(Class remote)
  {
    _remote = remote;
  }

  /**
   * Returns the remote class.
   */
  public Class getRemote()
  {
    return _remote;
  }

  public void setEjbLink(String link)
  {
    _link = link;
  }

  public void putJndiEnv(String key, String value)
  {
    if (_jndiEnv == null)
      _jndiEnv = new Hashtable();

    _jndiEnv.put(key, value);
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    log.warning("BINDING: " + _link + " " + _name);
    if (_link != null && ! _name.equals(_link))
      Jndi.bindDeepShort(_name, this);
  }
  
  /**
   * Creates the object from the proxy.
   *
   * @return the object named by the proxy.
   */
  public Object createObject(Hashtable env)
    throws NamingException
  {
    if (_jndiEnv != null && _link != null) {
      return new InitialContext(_jndiEnv).lookup(_link);
    }
    
    EJBServer server = EJBServer.getLocal();

    if (server != null) {
      try {
	EJBHome home = server.findRemoteEJB(_link);

	if (home != null)
	  return home;
      } catch (RemoteException e) {
	throw new NamingException(e.toString());
      }
    }

    EJBHome home = IiopProtocolContainer.findRemoteEJB(_link);

    if (home != null)
      return home;

    Context ic = new InitialContext(env);

    return ic.lookup(_link);
  }

  public String toString()
  {
    return "EjbRef[" + _name + "]";
  }
}
