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

package com.caucho.naming;

import java.io.*;
import java.util.*;

import javax.annotation.*;

import javax.naming.*;
import javax.naming.spi.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.config.ConfigException;
import com.caucho.config.types.InitParam;

/**
 * An object proxy for a foreign JNDI factory.
 */
public class LinkProxy implements ObjectProxy, java.io.Serializable {
  private static L10N L = new L10N(LinkProxy.class);

  // The foreign factory
  protected InitialContextFactory _factory;
  // The foreign factory
  protected Class _factoryClass;
  // Properties for the object
  protected Hashtable<String,String> _props;
  // The jndi-link path
  protected String _name;
  // The foreign name
  protected String _foreignName;

  /**
   * Creates a new LinkProxy.
   *
   * @param name the jndi-link path in the foreign namespace
   */
  public LinkProxy()
    throws NamingException
  {
  }

  /**
   * Creates a new LinkProxy.
   *
   * @param factory the foreign factory
   * @param props the properties for the object
   * @param name the jndi-link path in the foreign namespace
   */
  public LinkProxy(InitialContextFactory factory,
                   Hashtable<String,String> props,
                   String name)
    throws NamingException
  {
    if (factory == null)
      throw new NullPointerException();
    
    _factory = factory;
    _props = props;
    _foreignName = name;
  }

  /**
   * Creates a new LinkProxy.
   *
   * @param name the jndi-link path in the foreign namespace
   */
  public LinkProxy(String name)
    throws NamingException
  {
    _foreignName = name;
  }

  /**
   * Sets the jndi name.
   */
  public void setJndiName(String name)
  {
    _name = name;
  }

  /**
   * Sets the jndi name.
   */
  public void setName(String name)
  {
    setName(name);
  }

  /**
   * Sets the factory
   */
  public void setFactory(Class factoryClass)
  {
    _factoryClass = factoryClass;
  }

  /**
   * Sets the factory
   */
  public void setJndiFactory(Class factoryClass)
  {
    setFactory(factoryClass);
  }

  /**
   * Adds init param.
   */
  public void addInitParam(InitParam initParam)
  {
    if (_props == null)
      _props = new Hashtable<String,String>();
    
    _props.putAll(initParam.getParameters());
  }

  /**
   * Sets the foreign-name
   */
  public void setForeignName(String name)
  {
    _foreignName = name;
  }

  /**
   * Creates the object from the proxy.
   *
   * @param env the calling environment
   *
   * @return the object named by the proxy.
   */
  public Object createObject(Hashtable env)
    throws NamingException
  {
    Context context;
    Hashtable<String,String> mergeEnv;
    
    if (env == null || env.size() == 0)
      mergeEnv = _props;
    else if (_props == null || _props.size() == 0)
      mergeEnv = env;
    else {
      mergeEnv = new Hashtable<String,String>();
      mergeEnv.putAll(_props);
      mergeEnv.putAll(env);
    }

    if (_factory != null)
      context = _factory.getInitialContext(mergeEnv);
    else
      context = new InitialContext(mergeEnv);
    
    if (_foreignName != null) {
      try {
	Object value = context.lookup(_foreignName);
      
	return value;
      } catch (RuntimeException e) {
	e.printStackTrace();
	throw e;
      } catch (NamingException e) {
	e.printStackTrace();
	throw e;
      }
    }
    else
      return context;
  }

  /**
   * Initialize the resource.
   */
  @PostConstruct
  public void init()
    throws Exception
  {
    if (_name == null)
      throw new ConfigException(L.l("<jndi-link> configuration needs a <name>.  The <name> is the JNDI name where the context will be linked."));
    
    Class factoryClass = _factoryClass;

    if (factoryClass != null)
      _factory = (InitialContextFactory) factoryClass.newInstance();

    Jndi.bindDeep(_name, this);
  }

  public String toString()
  {
    if (_factoryClass != null)
      return "LinkProxy[name=" + _name + ",factory=" + _factoryClass.getName() + "]";
    else
      return "LinkProxy[name=" + _name + ",foreign=" + _foreignName + "]";
  }
}
