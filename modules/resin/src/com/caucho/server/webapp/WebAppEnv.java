/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.server.webapp;

import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.validation.Validation;

import com.caucho.naming.Jndi;
import com.caucho.naming.ObjectProxy;

/**
 * Initializes JNDI environment
 */
public class WebAppEnv
{
  private static final Logger log
    = Logger.getLogger(WebAppEnv.class.getName());
  
  public void init()
  {
    try {
      initBeanValidation();
    } catch (Throwable e) {
      e.printStackTrace();
      log.log(Level.FINEST, e.toString(), e);
    }
  }
  
  private void initBeanValidation()
    throws NamingException
  {
    Jndi.bindDeep("java:comp/BeanValidation", new JndiFactoryBeanValidation());
  }
  
  private static class JndiFactoryBeanValidation implements ObjectProxy
  {
    @Override
    public Object createObject(Hashtable<?, ?> env) throws NamingException
    {
      return Validation.buildDefaultValidatorFactory();
    }
  }
}
