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

package com.caucho.ejb.hessian;

import java.io.*;
import java.util.*;
import java.rmi.*;

import javax.ejb.*;
import javax.naming.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.config.ConfigException;

import com.caucho.hessian.io.HessianRemoteResolver;

import com.caucho.ejb.*;
import com.caucho.ejb.protocol.*;
import com.caucho.ejb.message.MessageServer;

/**
 * Server containing all the EJBs for a given configuration.
 *
 * <p>Each protocol will extend the container to override Handle creation.
 */
public class HessianProtocol extends ProtocolContainer {
  static L10N L = new L10N(HessianProtocol.class);
  
  private Class _objectSkelClass; 
  private Class _homeSkelClass;

  private HashMap<AbstractServer,Class> _homeSkeletonMap
    = new HashMap<AbstractServer,Class>();
  
  private HashMap<AbstractServer,Class> _objectSkeletonMap
    = new HashMap<AbstractServer,Class>();

  private HessianRemoteResolver _resolver;

  /**
   * Create a server with the given prefix name.
   */
  public HessianProtocol()
  {
    _resolver = new HessianStubFactory();
  }

  public String getName()
  {
    return "hessian";
  }

  /**
   * Returns the handle encoder for burlap.
   */
  protected HandleEncoder createHandleEncoder(AbstractServer server,
                                              Class primaryKeyClass)
    throws ConfigException
  {
    return new HessianHandleEncoder(server,
                                    getURLPrefix() + server.getEJBName(),
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
    }
    else if (objectId != null) {
      Object key = server.getHandleEncoder("hessian").objectIdToKey(objectId);
        
      EJBObject obj = server.getContext(key, false).getRemoteView();

      Class objectSkelClass = getObjectSkelClass(server);

      HessianSkeleton skel = (HessianSkeleton) objectSkelClass.newInstance();
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

	HessianSkeleton skel = (HessianSkeleton) homeSkelClass.newInstance();
	skel._setServer(server);
	skel._setResolver(_resolver);
	skel._setObject(remote);

	return skel;
      }
      else if (remote instanceof EJBObject) {
	Class skelClass = getObjectSkelClass(server);

	HessianSkeleton skel = (HessianSkeleton) skelClass.newInstance();
	skel._setServer(server);
	skel._setResolver(_resolver);
	skel._setObject(remote);

	return skel;
      }

      return null;
    }
  }

  /**
   * Returns the class for home skeletons.
   */
  protected Class getHomeSkelClass(AbstractServer server)
    throws Exception
  {
    Class homeSkelClass = _homeSkeletonMap.get(server);
    
    if (homeSkelClass != null)
      return homeSkelClass;
    
    Class remoteHomeClass = server.getRemoteHomeClass();

    homeSkelClass = HessianSkeletonGenerator.generate(remoteHomeClass,
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
    Class objectSkelClass = _objectSkeletonMap.get(server);
    
    if (objectSkelClass != null)
      return objectSkelClass;
    
    Class remoteObjectClass = server.getRemoteObjectClass();

    objectSkelClass = HessianSkeletonGenerator.generate(remoteObjectClass,
                                                        getWorkPath());

    _objectSkeletonMap.put(server, objectSkelClass);

    return objectSkelClass;
  }
}
