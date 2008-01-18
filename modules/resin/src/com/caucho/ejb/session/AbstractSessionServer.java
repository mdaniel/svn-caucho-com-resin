/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.session;

import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.util.Log;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.SessionBean;
import java.util.logging.Logger;

/**
 * Server container for a session bean.
 */
abstract public class AbstractSessionServer extends AbstractServer {
  protected final static Logger log
    = Logger.getLogger(AbstractSessionServer.class.getName());

  private AbstractSessionContext _homeContext;

  public AbstractSessionServer(EjbContainer container)
  {
    super(container);
  }

  /**
   * Initialize the server
   */
  @Override
  public void init()
    throws Exception
  {
    try {
      super.init();
      
      //generate();

      /*
      _homeContext = (QSessionContext) _beanSkelClass.newInstance();
      _homeContext.init(this);
        
      if (_config.getLocalHomeClass() != null)
        _localHome = _homeContext.createLocalHome();

      if (_homeStubClass != null) {
        _remoteHomeView = _homeContext.createRemoteHomeView();

	if (_config.getJndiName() != null) {
	  Context ic = new InitialContext();
	  ic.rebind(_config.getJndiName(), this);
	}
      }
      */
      
      log.config("initialized session bean: " + this);
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Returns the object key from a handle.
   */
  public Class getPrimaryKeyClass()
  {
    return String.class;
  }

  /**
   * Returns the EJBLocalHome stub for the container
   */
  public EJBLocalHome getEJBLocalHome()
  {
    return _localHome;
  }

  /**
   * Returns the EJBHome stub for the container
   */
  public EJBHome getEJBHome()
  {
    if (_remoteHome == null) {
      try {
        _remoteHome = _jvmClient.createHomeStub();
      } catch (Exception e) {
        EJBExceptionWrapper.createRuntime(e);
      }
    }
    
    return _remoteHome;
  }

  public Object getHomeObject()
  {
    return _remoteHomeView;
  }

  /**
   * Creates the local stub for the object in the context.
   */
  SessionObject getEJBLocalObject(SessionBean bean)
  {
    try {
      SessionObject obj = null;

      /*
      obj = (SessionObject) bean.getLocal();
      obj._setObject(bean);
      */
      if (obj == null)
        throw new IllegalStateException("bean has no local interface");

      return obj;
    } catch (Exception e) {
      throw new EJBExceptionWrapper(e);
    }
  }
}
