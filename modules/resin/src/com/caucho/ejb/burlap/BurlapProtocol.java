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

import com.caucho.config.ConfigException;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.message.MessageServer;
import com.caucho.ejb.protocol.HandleEncoder;
import com.caucho.ejb.protocol.ProtocolContainer;
import com.caucho.ejb.protocol.Skeleton;
import com.caucho.hessian.io.HessianRemoteResolver;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
/**
 * Server containing all the EJBs for a given configuration.
 *
 * <p>Each protocol will extend the container to override Handle creation.
 */
public class BurlapProtocol extends ProtocolContainer {
  private static final L10N L = new L10N(BurlapProtocol.class);
  private static final Logger log = Log.open(BurlapProtocol.class);

  private Class _objectSkelClass;
  private Class _homeSkelClass;

  private HashMap<AbstractServer,Class> _homeSkeletonMap =
    new HashMap<AbstractServer,Class>();

  private HashMap<AbstractServer,Class> _objectSkeletonMap =
    new HashMap<AbstractServer,Class>();

  private HashMap<String,AbstractServer> _serverMap =
    new HashMap<String,AbstractServer>();

  private HessianRemoteResolver _resolver;

  /**
   * Create a server with the given prefix name.
   */
  public BurlapProtocol()
  {
    _resolver = new BurlapStubFactory();
  }

  public String getName()
  {
    return "burlap";
  }

  /**
   * Adds a server to the protocol.
   */
  public void addServer(AbstractServer server)
  {
    log.finer("Burlap[" + server + "] added");

    _serverMap.put(server.getServerId(), server);
  }

  /**
   * Removes a server from the protocol.
   */
  public void removeServer(AbstractServer server)
  {
    _serverMap.remove(server.getServerId());
  }

  protected HandleEncoder createHandleEncoder(AbstractServer server,
                                              Class primaryKeyClass)
    throws ConfigException
  {
    return new BurlapHandleEncoder(server,
                                   getURLPrefix() + server.getServerId(),
                                   primaryKeyClass);
  }

  /**
   * Returns the skeleton
   */
  public Skeleton getSkeleton(String uri, String queryString)
    throws Exception
  {
    String serverId = uri;
    String objectId = null;

    // decode ?id=my-instance-id
    if (queryString != null) {
      int p = queryString.indexOf('=');

      if (p >= 0)
	objectId = queryString.substring(p + 1);
      else
	objectId = queryString;
    }

    AbstractServer server = getProtocolManager().getServerByEJBName(serverId);

    if (server == null) {
      ArrayList children = getProtocolManager().getRemoteChildren(serverId);

      if (children != null && children.size() > 0)
        return new NameContextSkeleton(this, serverId);
      else
        return null; // XXX: should return error skeleton
      /*
	ArrayList children = getServerContainer().getRemoteChildren(serverId);

	if (children != null && children.size() > 0)
	return new NameContextSkeleton(this, serverId);
	else
	return null; // XXX: should return error skeleton
      */
    }
    else if (objectId != null) {
      Object key = server.getHandleEncoder("burlap").objectIdToKey(objectId);

      EJBObject obj = server.getContext(key, false).getRemoteView();

      Class objectSkelClass = getObjectSkelClass(server);

      BurlapSkeleton skel = (BurlapSkeleton) objectSkelClass.newInstance();
      skel._setServer(server);
      skel._setResolver(_resolver);
      skel._setObject(obj);
      return skel;
    }
    else if (server instanceof MessageServer) {
      return new MessageSkeleton((MessageServer) server);
    }
    else {
      Object remote = server.getRemoteObject();

      if (remote instanceof EJBHome) {
	Class homeSkelClass = getHomeSkelClass(server);

	BurlapSkeleton skel = (BurlapSkeleton) homeSkelClass.newInstance();
	skel._setServer(server);
	skel._setResolver(_resolver);
	skel._setObject(remote);

	return skel;
      }
      else if (remote instanceof EJBObject) {
	Class skelClass = getObjectSkelClass(server);

	BurlapSkeleton skel = (BurlapSkeleton) skelClass.newInstance();
	skel._setServer(server);
	skel._setResolver(_resolver);
	skel._setObject(remote);

	return skel;
      }
    }

    return null;
  }

  /**
   * Returns the skeleton
   */
  public Skeleton getExceptionSkeleton()
    throws Exception
  {
    return new ExceptionSkeleton();
  }

  /**
   * Returns the class for home skeletons.
   */
  protected Class getHomeSkelClass(AbstractServer server)
    throws Exception
  {
    Class homeSkelClass = (Class) _homeSkeletonMap.get(server);
    
    if (homeSkelClass != null)
      return homeSkelClass;
    
    Class remoteHomeClass = server.getRemoteHomeClass();

    homeSkelClass = BurlapSkeletonGenerator.generate(remoteHomeClass,
                                                        getWorkPath());

    _homeSkeletonMap.put(server, homeSkelClass);

    return homeSkelClass;
  }

  /**
   * Returns the class for home skeletons.
   */
  protected Class getObjectSkelClass(AbstractServer server)
    throws Exception
  {
    Class objectSkelClass = (Class) _objectSkeletonMap.get(server);
    
    if (objectSkelClass != null)
      return objectSkelClass;
    
    Class remoteObjectClass = server.getRemoteObjectClass();

    objectSkelClass = BurlapSkeletonGenerator.generate(remoteObjectClass,
                                                        getWorkPath());

    _objectSkeletonMap.put(server, objectSkelClass);

    return objectSkelClass;
  }
}
