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
 * @author Scott Ferguson;
 */

package com.caucho.config.j2ee;

import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.NodeBuilder;
import com.caucho.config.types.*;
import com.caucho.soa.client.WebServiceClient;
import com.caucho.ejb.EjbServerManager;
import com.caucho.util.L10N;
import com.caucho.server.util.ScheduledThreadPool;
import com.caucho.naming.*;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GeneratorInjectProgram extends BuilderProgram
{
  private static final Logger log
    = Logger.getLogger(GeneratorInjectProgram.class.getName());
  
  private AccessibleInject _inject;
  private ValueGenerator _generator;

  GeneratorInjectProgram(AccessibleInject inject, ValueGenerator generator)
  {
    _inject = inject;
    _generator = generator;
  }

  public void configureImpl(NodeBuilder builder, Object bean)
    throws ConfigException
  {
    try {
      _inject.inject(bean, _generator.create());
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  public Object configure(NodeBuilder builder, Class type)
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _inject + ", " + _generator + "]";
  }
}
