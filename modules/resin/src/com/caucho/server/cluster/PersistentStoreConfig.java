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

package com.caucho.server.cluster;

import com.caucho.config.ConfigException;
import com.caucho.log.Log;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration distributed stores.
 */
public class PersistentStoreConfig {
  static protected final Logger log = Log.open(PersistentStoreConfig.class);
  static final L10N L = new L10N(PersistentStoreConfig.class);

  private String _name = "caucho/persistent-store";

  private StoreManager _store;

  /**
   * Sets the persistent store name.
   */
  public void setJndiName(String name)
  {
    _name = name;
  }
  
  /**
   * Sets the persistent store type.
   */
  public void setType(String type)
    throws ConfigException
  {
    Cluster cluster = Cluster.getLocal();
    
    if (type.equals("jdbc")) {
      try {
	Class cl = Class.forName("com.caucho.server.cluster.JdbcStore");
	
	_store = (StoreManager) cl.newInstance();

	if (cluster != null)
	  cluster.setStore(_store);
      } catch (Throwable e) {
	log.log(Level.FINER, e.toString(), e);
      }

      if (_store == null)
	throw new ConfigException(L.l("'{0}' persistent sessions are available in Resin Professional.  See http://www.caucho.com for information and licensing.",
				      type));
    }
    else if (type.equals("file"))
      _store = new FileStore();
    else if (type.equals("cluster") || type.equals("tcp")) {
      if (cluster == null)
	throw new ConfigException(L.l("Cluster store needs a defined <cluster>.  Use 'file' for single-machine persistence."));
      
      try {
	Class cl = Class.forName("com.caucho.server.cluster.ClusterStore");

	_store = (StoreManager) cl.newInstance();

	if (cluster != null)
	  cluster.setStore(_store);
      } catch (Throwable e) {
	log.log(Level.FINER, e.toString(), e);
      }

      if (_store == null)
	throw new ConfigException(L.l("'{0}' persistent sessions are available in Resin Professional.  See http://www.caucho.com for information and licensing.",
				      type));
    }

    if (_store == null)
      throw new ConfigException(L.l("{0} is an unknown persistent-store type.  Only 'jdbc', 'file', and 'tcp' are allowed.",
				    type));
  }

  public StoreManager createInit()
  {
    return _store;
  }

  @PostConstruct
  public void init()
    throws Exception
  {
    if (_store == null)
      throw new ConfigException(L.l("type is a required attribute of persistent-store"));

    _store.init();

    if (_name.startsWith("java:comp"))
      Jndi.bindDeep(_name, _store);
    else
      Jndi.bindDeep("java:comp/env/" + _name, _store);
  }
}
