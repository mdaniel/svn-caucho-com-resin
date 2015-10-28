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

package com.caucho.v5.naming.java;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import com.caucho.v5.naming.AbstractModel;
import com.caucho.v5.naming.ContextImpl;
import com.caucho.v5.naming.InitialContextFactoryImpl;

/**
 * Create a remote object
 */
public class javaURLContextFactory implements ObjectFactory {
  public Object getObjectInstance(Object obj,
                                  Name name,
                                  Context parentContext,
                                  Hashtable<?,?> env)
    throws NamingException
  {
    AbstractModel model = InitialContextFactoryImpl.createRoot();

    Context context = new ContextImpl(model, (Hashtable) env);

    if (obj != null)
      return context.lookup((String) obj);
    else
      return context;
  }
}