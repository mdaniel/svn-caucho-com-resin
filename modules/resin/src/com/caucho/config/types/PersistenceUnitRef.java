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
 * @author Sam
 */

package com.caucho.config.types;

import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EJBServer;
import com.caucho.naming.Jndi;
import com.caucho.naming.ObjectProxy;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.annotation.PostConstruct;
import javax.naming.*;
import javax.persistence.*;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for the persistence-unit-ref.
 */
public class PersistenceUnitRef implements ObjectProxy {
  private static final L10N L = new L10N(EjbRef.class);
  private static final Logger log
    = Logger.getLogger(EjbRef.class.getName());

  private String _location = "";

  private String _persistenceUnitRefName;
  private String _persistenceUnitName;
  private String _mappedName;

  // XXX: missing inject stuff, too

  private EntityManagerFactory _emf;

  public PersistenceUnitRef()
  {
  }

  public void setConfigLocation(String location)
  {
    _location = location;
  }

  protected String getTagName()
  {
    return "<persistence-unit-ref>";
  }

  public void setId(String id)
  {
  }

  public void setDescription(String description)
  {
  }

  public void setDisplayName(String displayName)
  {
  }

  /**
   * Sets the name to use in the local jndi context.
   * This is the jndi lookup name that code uses to obtain the home for
   * the bean when doing a jndi lookup.
   *
   * <pre>
   *   <persistence-unit-ref-name>persistence/Gryffindor</persistence-unit-ref-name>
   *   ...
   *   (new InitialContext()).lookup("java:comp/env/ejb/Gryffindor");
   * </pre>
   */
  public void setPersistenceUnitRefName(String name)
  {
    _persistenceUnitRefName = name;
  }

  /**
   * Returns the name.
   */
  public String getPersistenceUnitRefName()
  {
    return _persistenceUnitRefName;
  }

  /**
   * Sets the name from the persistence.xml
   */
  public void setPersistenceUnitName(String name)
  {
    _persistenceUnitName = name;
  }

  /**
   * Returns the name.
   */
  public String getPersistenceUnitName()
  {
    return _persistenceUnitName;
  }

  /**
   * Sets the mapped-name
   */
  // (XXX: not needed?)
  public void setMappedName(String name)
  {
    _mappedName = name;
  }

  /**
   * Returns the name.
   */
  public String getMappedName()
  {
    return _mappedName;
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    if (_persistenceUnitRefName == null)
      return;

    // jpa/0s2m
    if (_persistenceUnitName == null)
      _persistenceUnitName = "default";

    String fullJndiName = Jndi.getFullName(_persistenceUnitRefName);

    try {
      Object oldValue = new InitialContext().lookup(fullJndiName);

      if (oldValue != null)
        return;
    } catch (NamingException e) {
    }

    Jndi.rebindDeep(fullJndiName, this);
  }

  /**
   * Creates the object from the proxy.
   *
   * @return the object named by the proxy.
   */
  public Object createObject(Hashtable env)
    throws NamingException
  {
    if (_emf == null) {
      _emf = Persistence.createEntityManagerFactory(_persistenceUnitName);

      if (_emf == null)
        log.warning(L.l(_location + "'{0}' is an unknown persistence-unit-name",
                        _persistenceUnitName));
    }

    return _emf;
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _persistenceUnitName + "]";
  }
}
