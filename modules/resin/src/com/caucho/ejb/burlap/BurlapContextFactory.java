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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.burlap;

import java.util.*;
import java.io.*;

import javax.naming.*;
import javax.naming.spi.*;

import com.caucho.util.*;
import com.caucho.naming.*;
import com.caucho.vfs.*;
import com.caucho.jsp.*;
import com.caucho.server.*;
import com.caucho.sql.*;
import com.caucho.ejb.*;
import com.caucho.config.ConfigException;

/**
 * The root context factory.
 */
public class BurlapContextFactory implements InitialContextFactory  {
  private static final L10N L = new L10N(BurlapContextFactory.class);

  private AbstractModel _model;
  
  /**
   * Returns the initial context for the current thread.
   */
  public Context getInitialContext(Hashtable<?,?> environment)
    throws NamingException
  {
    String prefix = (String) environment.get(Context.PROVIDER_URL);

    String user = (String) environment.get(Context.SECURITY_PRINCIPAL);
    String pw = (String) environment.get(Context.SECURITY_CREDENTIALS);

    if (user != null) {
      String auth = Base64.encode(user + ':' + pw);

      if (! prefix.endsWith("/"))
	prefix = prefix + '/';
      
      BurlapModel model = new BurlapModel(prefix);
      BurlapClientContainer client;
      try {
	client = new BurlapClientContainer(model.getURLPrefix());
	client.setBasicAuthentication(auth);
      } catch (ConfigException e) {
	throw new NamingException(e.toString());
      }

      model.setClientContainer(client);
      
      return new ContextImpl(model, environment);
    }

    if (_model == null)
      _model = new BurlapModel(prefix);

    return new ContextImpl(_model, environment);
  }
}
