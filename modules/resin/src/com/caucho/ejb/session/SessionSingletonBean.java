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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.session;

import com.caucho.config.*;
import com.caucho.config.inject.ComponentImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.inject.SingletonBean;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.cfg.*;
import com.caucho.ejb.manager.EjbContainer;

import javax.ejb.*;
import javax.enterprise.inject.Named;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Server container for a session bean.
 */
public class SessionSingletonBean extends SingletonBean
{
  private Class _beanType;
  
  public SessionSingletonBean(Object value, Class beanType)
  {
    super(InjectManager.create());
    
    setValue(value);
    setTargetType(value.getClass());

    addType(SessionContext.class);

    _beanType = beanType;

    init();
  }

  @Override
  protected Class getIntrospectionClass()
  {
    return _beanType;
  }
}
