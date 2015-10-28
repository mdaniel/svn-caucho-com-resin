/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.config.inject;

import javax.annotation.sql.DataSourceDefinition;

import com.caucho.v5.config.inject.ScanClassInject;
import com.caucho.v5.config.inject.ScanListenerInject;
import com.caucho.v5.config.inject.ScanRootInject;
import com.caucho.v5.inject.Jndi;
import com.caucho.v5.inject.MBean;

/**
 * The web beans container for a given environment.
 */
class ScanClassInjectJavaee extends ScanClassInject
{
  ScanClassInjectJavaee(String className,
                        ScanListenerInject manager,
                        ScanRootInject context)
  {
    super(className, manager, context);
  }

  static {
    //addAnnotation(Startup.class);
    addAnnotation(Jndi.class);
    addAnnotation(MBean.class);
    //addAnnotation(Stateless.class);
    //addAnnotation(Stateful.class);
    //addAnnotation(Singleton.class);
    //addAnnotation(MessageDriven.class);
    
    addImmediate(DataSourceDefinition.class);
  }
}