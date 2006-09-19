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
 * @author Emil Ong
 */

package com.caucho.quercus.lib.jms;

import java.util.Map;
import java.util.HashMap;

import java.util.logging.Logger;

import javax.jms.*;
import javax.naming.*;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.quercus.QuercusModuleException;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.JavaValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.BooleanValue;

import com.caucho.quercus.program.JavaClassDef;

import com.caucho.quercus.module.NotNull;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.ReturnNullAsFalse;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.ModuleStartupListener;
import com.caucho.quercus.module.ClassImplementation;

/**
 * JMS functions
 */
@ClassImplementation
public class JMSModule extends AbstractQuercusModule 
  implements ModuleStartupListener {

  private static final Logger log =
    Logger.getLogger(JMSModule.class.getName());

  private static final L10N L = new L10N(JMSModule.class);

  private static final HashMap<String,StringValue> _iniMap
    = new HashMap<String,StringValue>();

  /**
   * Returns the default quercus.ini values.
   */
  public Map<String,StringValue> getDefaultIni()
  {
    return _iniMap;
  }

  public void startup(Env env)
  {
    try {
      env.getQuercus().addJavaClass("JMSQueue", 
                                    com.caucho.quercus.lib.jms.JMSQueue.class);
    } catch (ConfigException e) {
      env.warning(L.l("JMSQueue unavailable: {0}", e));
    }
  }

  static JMSQueue message_get_queue(Env env, String queueName, 
                                    ConnectionFactory connectionFactory)
  {
    if (connectionFactory == null)
      connectionFactory = getConnectionFactory(env);

    if (connectionFactory == null) {
      env.warning(L.l("No connection factory"));
      return null;
    }

    try {
      Context cxt = (Context) new InitialContext().lookup("java:comp/env");
      
      return new JMSQueue(cxt, connectionFactory, queueName);
    } catch (Exception e) {
      env.warning(e);

      return null;
    }
  }

  private static ConnectionFactory getConnectionFactory(Env env)
  {
    try {
      Context cxt = (Context) new InitialContext().lookup("java:comp/env");
      
      StringValue factoryName = env.getIni("jms.connection_factory");

      if (factoryName == null)
	log.fine("jms.connection_factory not set");

      return (ConnectionFactory) cxt.lookup(factoryName.toString());
    } catch (Exception e) {
      log.fine(e.toString());

      return null;
    }
  }

  static {
    addIni(_iniMap, "jms.connection_factory", 
                    "jms/ConnectionFactory", PHP_INI_SYSTEM);
  }
}

