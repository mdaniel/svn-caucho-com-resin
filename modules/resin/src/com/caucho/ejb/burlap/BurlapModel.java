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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.burlap;

import java.io.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.naming.*;
import javax.ejb.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.naming.*;
import com.caucho.services.name.NameServerRemote;

/**
 * JNDI context for Burlap home objects.
 *
 * <p>For now, only allow single level calls to the EJB.
 */
public class BurlapModel extends AbstractModel {
  private static final L10N L = new L10N(BurlapModel.class);
  private static final Logger log
    = Logger.getLogger(BurlapModel.class.getName());
  
  private String _urlPrefix;
  private String _namePrefix;
  private BurlapModel _root;
  private Hashtable _cache;
  private BurlapClientContainer _client;
  private NameServerRemote _remoteRoot;
  private NameServerRemote _remote;
  
  /**
   * Creates a new Burlap model
   */
  public BurlapModel(String prefix)
  {
    if (! prefix.endsWith("/"))
      prefix = prefix + '/';
    
    _urlPrefix = prefix;
    _namePrefix = "/";
    _root = this;
    _cache = new Hashtable();
  }
  
  /**
   * Returns the root Burlap model
   */
  public BurlapModel(String namePrefix, BurlapModel root)
  {
    if (! namePrefix.endsWith("/"))
      namePrefix = namePrefix + '/';
    
    _namePrefix = namePrefix;
    _root = root;
  }

  void setRemote(NameServerRemote remote)
  {
    _remote = remote;
  }

  void setClientContainer(BurlapClientContainer client)
  {
    _root._client = client;
  }

  /**
   * Creates a new instance of BurlapModel.
   */
  public AbstractModel copy()
  {
    return this;
  }

  /**
   * Returns the full url prefix.
   */
  String getURLPrefix()
  {
    return _root._urlPrefix;
  }

  /**
   * Returns the full name.
   */
  String getURL()
  {
    return getURLPrefix() + _namePrefix;
  }

  /**
   * Looks up the named bean.  Since we're assuming only a single level,
   * just try to look it up directly.
   *
   * <p>Burlap to find all the valid names.
   *
   * @param name the segment name to lookup
   *
   * @return the home stub.
   */
  public Object lookup(String name)
    throws NamingException
  {
    try {
      Object obj = _root._cache.get(getURLPrefix() + _namePrefix + name);
      if (obj != null)
        return obj;

      if (_root._remoteRoot == null) {
        if (_root._client == null)
          _root._client = BurlapClientContainer.find(getURLPrefix());
      
        _root._remoteRoot =
          (NameServerRemote) _client.createObjectStub(getURLPrefix());
      }

      obj = _root._remoteRoot.lookup(_namePrefix + name);

      if (obj instanceof EJBHome)
        _root._cache.put(name, obj);
      else if (obj instanceof NameServerRemote) {
        BurlapModel model = new BurlapModel(_namePrefix + name, _root);
        _remote = (NameServerRemote) obj;
        model.setRemote(_remote);
        obj = model;
        _root._cache.put(name, obj);
      }

      if (log.isLoggable(Level.FINE))
	log.fine(this + " lookup(" + name + ")->" + obj);

      return obj;
    } catch (Exception e) {
      throw new NamingExceptionWrapper(e);
    }
  }

  /**
   * Returns a list of children of the named bean.
   *
   * @return array of the children.
   */
  public List list()
    throws NamingException
  {
    try {
      if (_remote == null) {
        if (_root._remoteRoot == null) {
          if (_root._client == null)
            _root._client = BurlapClientContainer.find(getURLPrefix());
      
          _root._remoteRoot =
            (NameServerRemote) _client.createObjectStub(getURLPrefix());
        }

        Object obj = _root._remoteRoot.lookup(_namePrefix);
        if (obj instanceof NameServerRemote)
          _remote = (NameServerRemote) obj;
      }

      if (_remote == null)
        throw new NamingException(L.l("Burlap object `{0}' is not a context.",
                                      getURLPrefix() + _namePrefix));

      String []list = _remote.list();

      ArrayList value = new ArrayList();
      for (int i = 0; list != null && i < list.length; i++)
        value.add(list[i]);
      
      return value;
    } catch (NamingException e) {
      throw e;
    } catch (Exception e) {
      throw new NamingExceptionWrapper(e);
    }
  }

  public String toString()
  {
    return "BurlapModel[url=" + " " + getURLPrefix() + ",name=" + _namePrefix + "]";
  }
}
