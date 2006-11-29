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

package com.caucho.naming.java;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.log.Log;
import com.caucho.naming.AbstractModel;
import com.caucho.naming.ContextImpl;
import com.caucho.naming.MemoryModel;
import com.caucho.util.L10N;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Create a remote object
 */
public class javaURLContextFactory implements ObjectFactory {
  private static L10N L = new L10N(javaURLContextFactory.class);
  private static Logger dbg = Log.open(javaURLContextFactory.class);

  private static EnvironmentLocal<AbstractModel> _javaModel =
  new EnvironmentLocal<AbstractModel>("caucho.naming.model.java");

  /**
   * Sets the model for the current class loader.
   */
  public static AbstractModel getContextModel()
  {
    return _javaModel.get();
  }

  /**
   * Sets the model for the current class loader.
   */
  public static void setContextModel(AbstractModel model)
  {
    _javaModel.set(model);
  }
  
  public Object getObjectInstance(Object obj,
                                  Name name,
                                  Context parentContext,
                                  Hashtable<?,?> env)
    throws NamingException
  {
    AbstractModel model = _javaModel.getLevel();

    if (model == null) {
      if (dbg.isLoggable(Level.FINER)) {
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	
        dbg.finer(L.l("creating JNDI java: model for {0} parent:{1}",
		      loader,
		      (loader != null ? loader.getParent() : null)));
      }
        
      model = _javaModel.get();

      if (model != null)
        model = model.copy();
      else
        model = new MemoryModel();

      if (model.lookup("java:comp") == null)
        model.createSubcontext("java:comp");

      if (model.lookup("java:") == null)
        model.createSubcontext("java:");

      // XXX: java: is not a context itself.  So you can't do a lookup on
      // java: and then get a list.

      _javaModel.set(model);
    }

    Context context = new ContextImpl(model, env);

    if (obj != null)
      return context.lookup((String) obj);
    else
      return context;
  }
}
