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

/**
 * JMS functions
 */
public class JMSModule extends AbstractQuercusModule 
  implements ModuleStartupListener {

  private static final Logger log =
    Logger.getLogger(JMSModule.class.getName());

  private static final L10N L = new L10N(JMSModule.class);

  private static Context _context;
  private static ConnectionFactory _connectionFactory = null;

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
    StringValue factoryName = env.getIni("jms.connection_factory");

    if (factoryName == null)
      log.fine("jms.connection_factory not set");

    try {
      _context = (Context) new InitialContext().lookup("java:comp/env");

      _connectionFactory = 
        (ConnectionFactory) _context.lookup(factoryName.toString());
    } catch (Exception e) {
      log.fine(e.toString());
    }
  }

  @ReturnNullAsFalse
  public static JMSQueue message_get_queue(Env env, String queueName, 
                                           @Optional ConnectionFactory 
                                           connectionFactory)
  {
    if (connectionFactory == null)
      connectionFactory = _connectionFactory;

    if (connectionFactory == null) {
      env.warning(L.l("No connection factory"));
      return null;
    }

    try {
      return new JMSQueue(_context, connectionFactory, queueName);
    } catch (Exception e) {
      env.warning(e);

      return null;
    }
  }

  /**
   * Send a message to a Queue.
   */
  public static boolean message_send(Env env, 
                                     @NotNull JMSQueue queue, Value message)
  {
    try {
      return queue.send(message);
    } catch (JMSException e) {
      env.warning(e);

      return false;
    }
  }

  /**
   * Receive a message from a queue or topic.  Non-blocking.
   */
  public static Value message_receive(Env env, 
                                      @NotNull JMSQueue queue,
                                      @Optional("1") int timeout)
  {
    try {
      return queue.receive(env, timeout);
    } catch (JMSException e) {
      env.warning(e);

      return BooleanValue.FALSE;
    }
  }

  static {
    addIni(_iniMap, "jms.connection_factory", 
                    "jms/ConnectionFactory", PHP_INI_SYSTEM);
  }
}

