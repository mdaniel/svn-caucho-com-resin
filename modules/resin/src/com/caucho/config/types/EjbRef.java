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

import com.caucho.config.ConfigException;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EJBServer;
import com.caucho.naming.Jndi;
import com.caucho.naming.ObjectProxy;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration for the ejb-ref.
 *
 * An ejb-ref is used to make an ejb available within the environment
 * in which the ejb-ref is declared.
 */
public class EjbRef implements ObjectProxy {
  private static final L10N L = new L10N(EjbRef.class);
  private static final Logger log
    = Logger.getLogger(EjbRef.class.getName());

  private String _ejbRefName;
  private String _type;
  private Class _home;
  private Class _remote;
  private String _jndiName;
  private String _ejbLink;

  private Object _target;

  public EjbRef()
  {
  }

  public void setId(String id)
  {
  }

  public void setDescription(String description)
  {
  }

  /**
   * Sets the name to use in the local jndi context.
   * This is the jndi lookup name that code uses to obtain the home for
   * the bean when doing a jndi lookup.
   *
   * <pre>
   *   <ejb-ref-name>ejb/Gryffindor</ejb-ref-name>
   *   ...
   *   (new InitialContext()).lookup("java:comp/env/ejb/Gryffindor");
   * </pre>
   */
  public void setEjbRefName(String name)
  {
    _ejbRefName = name;
  }

  /**
   * Returns the ejb name.
   */
  public String getEjbRefName()
  {
    return _ejbRefName;
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

  /**
   * Sets the canonical jndi name to use to find the bean that
   * is the target of the reference.
   * For remote beans, a &lt;jndi-link> {@link com.caucho.naming.LinkProxy} is
   * used to link the local jndi context referred to in this name to
   * a remote context.
   */
  public void setJndiName(String jndiName)
  {
    _jndiName = jndiName;
  }

  /**
   * Set the target of the reference, an alternative to {@link #setJndiName(String)}.
   * The format of the ejbLink is "bean", or "jarname#bean", where <i>bean</i> is the
   * ejb-name of a bean within the same enterprise application, and <i>jarname</i>
   * further qualifies the identity of the target.
   */
  public void setEjbLink(String ejbLink)
  {
    _ejbLink = ejbLink;
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    boolean bind = false;

    if (_ejbRefName == null)
      throw new ConfigException(L.l("{0} is required", "<ejb-ref-name>"));

    if (_ejbLink != null &&  _jndiName != null)
        throw new ConfigException(L.l("either {0} or {1} can be used, but not both", "<ejb-link>", "<jndi-name>"));

    if (_ejbLink == null && _jndiName == null) {
      EJBServer server = EJBServer.getLocal();

      if (server != null)
        _ejbLink = _ejbRefName;
      else
        _jndiName = _ejbRefName;
    }

    _ejbRefName = Jndi.getFullName(_ejbRefName);

    if (_ejbLink != null) {
      EJBServer server = EJBServer.getLocal();

      if (server == null)
        throw new ConfigException(L.l("{0} requires a local {1}", "<ejb-link>", "<ejb-server>"));

      bind = true;
    }
    else {
      if (_jndiName != null) {
        _jndiName = Jndi.getFullName(_jndiName);

        if (! _jndiName.equals(_ejbRefName))
          bind = true;
      }
    }

    if (bind) {
      try {
        (new InitialContext()).unbind(_ejbRefName);
      }
      catch (Exception ex) {
        log.log(Level.FINEST, ex.toString(), ex);
      }

      Jndi.bindDeep(_ejbRefName, this);
    }

    if (log.isLoggable(Level.FINER))
      log.log(Level.FINER, L.l("{0} init", this));
  }

  /**
   * Creates the object from the proxy.
   *
   * @return the object named by the proxy.
   */
  public Object createObject(Hashtable env)
    throws NamingException
  {
    if (_target == null)
      resolve();

    return _target;
  }

  private void resolve()
    throws NamingException
  {
    if (log.isLoggable(Level.FINEST))
      log.log(Level.FINEST, L.l("{0} resolving", this));

    if (_jndiName != null) {
      _target = Jndi.lookup(_jndiName);

      if (_target == null && log.isLoggable(Level.FINE))
        log.log(Level.FINE, L.l("no ejb bean with jndi-name '{0}' could be found", _jndiName));
    }
    else {
      String archiveName;
      String ejbName;

      int hashIndex = _ejbLink.indexOf('#');

      if (hashIndex < 0) {
        archiveName = null;
        ejbName = _ejbLink;
      }
      else {
        archiveName = _ejbLink.substring(0, hashIndex);
        ejbName = _ejbLink.substring(hashIndex + 1);
      }

      try {
        Path path = archiveName == null ? Vfs.getPwd() : Vfs.lookup(archiveName);

        AbstractServer server = EJBServer.getLocal().getServer(path, ejbName);

        if (server == null) {
          if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, L.l("no ejb bean with path '{0}' and name '{1}' could be found", path, ejbName));
        }
        else {
          Object localHome = server.getEJBLocalHome();

          if (localHome != null)
            _target = localHome;

          if (_target == null) {
            Object remoteHome = server.getEJBHome();

            if (remoteHome != null)
              _target = remoteHome;
          }

          if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, L.l("no home interface is available for '{0}'", server));
        }
      }
      catch (Exception e) {
        throw new NamingException(L.l("invalid {0} '{1}': {2}", "<ejb-link>", _ejbLink, e));
      }

      if (_target == null)
        throw new NamingException(L.l("{0} '{1}' cannot be resolved", "<ejb-ref>", _ejbRefName));

      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, L.l("{0} resolved", this));
    }
  }

  public String toString()
  {
    return "EjbRef[" + _ejbRefName + ", " + _ejbLink + ", " +  _jndiName + "]";
  }
}
