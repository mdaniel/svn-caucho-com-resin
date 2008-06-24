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
 * @author Emil Ong
 */

package com.caucho.quercus.lib.bam;

import com.caucho.bam.BamStream;
import com.caucho.bam.BamError;
import com.caucho.hemp.client.HempClient;
import com.caucho.xmpp.im.ImMessage;
import com.caucho.xmpp.im.ImPresence;
import com.caucho.xmpp.im.RosterItem;
import com.caucho.xmpp.im.RosterQuery;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.annotation.ClassImplementation;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.module.IniDefinitions;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.JavaValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.IniDefinition;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.program.AbstractFunction;
import com.caucho.util.L10N;
import com.caucho.webbeans.manager.WebBeansContainer;
import com.caucho.vfs.Path;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * BAM functions
 */
@ClassImplementation
public class BamModule extends AbstractQuercusModule
{
  private static final Logger log
    = Logger.getLogger(BamModule.class.getName());

  private static final L10N L = new L10N(BamModule.class);

  private static BamPhpAgent getAgent(Env env)
  {
    Value agentValue = env.getGlobalValue("_quercus_bam_agent");

    if (agentValue != null && ! agentValue.isNull())
      return (BamPhpAgent) agentValue.toJavaObject();

    return null;
  }

  private static BamStream getBrokerStream(Env env)
  {
    BamPhpAgent agent = getAgent(env);

    if (agent != null)
      return agent.getBrokerStream();

    HempClient client = env.getSpecialValue("_quercus_bam_client");

    return client.getBrokerStream();
  }

  private static String getJid(Env env)
  {
    BamPhpAgent agent = getAgent(env);

    if (agent != null)
      return agent.getJid();
    
    HempClient client = env.getSpecialValue("_quercus_bam_client");

    return client.getJid();
  }

  // XXX does bam_login make sense?  should this really be a persistent bean?
  public static Value bam_login(Env env, 
                                String url, String username, String password)
  {
    BamPhpAgent agent = getAgent(env);

    if (agent != null)
      return env.error("bam_login not available from agent script");

    BamClientResource resource = new BamClientResource(url);
    env.addCleanup(resource);

    HempClient client = resource.getClient();

    try {
      client.connect();
    }
    catch (IOException e) {
      return env.error("Unable to connect to BAM server", e);
    }

    client.login(username, password);

    env.setSpecialValue("_quercus_bam_client", client);

    return BooleanValue.TRUE;
  }

  /**
   * Registers a "child" agent that is represented by the given script.
   **/
  public static Value bam_register_agent(Env env, String jid, String script)
  {
    BamPhpAgent agent = getAgent(env);

    if (agent == null)
      return env.error("bam_register_agent must be called from agent script");

    BamPhpAgent child = new BamPhpAgent();
    child.setName(jid);

    Path path = env.getSelfDirectory().lookup(script);

    if (! path.exists())
      return env.error("script not found: " + script);

    child.setScript(path);

    WebBeansContainer container = WebBeansContainer.getCurrent();
    container.injectObject(child);

    agent.addChild(jid, child);

    return BooleanValue.TRUE;
  }

  public static Value bam_agent_exists(Env env, String jid)
  {
    BamPhpAgent agent = getAgent(env);

    if (agent == null)
      return env.error("bam_agent_exists must be called from agent script");

    return BooleanValue.create(agent.findAgent(jid) != null);
  }

  public static String bam_my_jid(Env env)
  {
    return getJid(env);
  }

  //
  // Utilities
  //

  public static String bam_get_bare_jid(Env env, String uri)
  {
    int slash = uri.indexOf('/');

    if (slash < 0)
      return uri;

    return uri.substring(0, slash);
  }

  public static String bam_get_jid_resource(Env env, String uri)
  {
    int slash = uri.indexOf('/');

    if (slash < 0 || slash == uri.length() - 1)
      return "";

    return uri.substring(slash + 1);
  }

  //
  // Transmit
  //

  public static void bam_send_message(Env env, String to, Serializable value)
  {
    getBrokerStream(env).message(to, null, value);
  }

  public static void bam_send_message_error(Env env, 
                                            String to, 
                                            Serializable value, BamError error)
  {
    getBrokerStream(env).messageError(to, null, value, error);
  }

  public static Value bam_send_query_get(Env env, 
                                         long id, String to, Serializable value)
  {
    boolean understood = getBrokerStream(env).queryGet(id, to, null, value);

    return BooleanValue.create(understood);
  }

  public static Value bam_send_query_set(Env env, 
                                         long id, String to, Serializable value)
  {
    boolean understood = getBrokerStream(env).querySet(id, to, null, value);

    return BooleanValue.create(understood);
  }

  public static void bam_send_query_result(Env env, 
                                           long id, String to,
                                           Serializable value)
  {
    getBrokerStream(env).queryResult(id, to, null, value);
  }

  public static void bam_send_query_error(Env env, 
                                          long id, String to,
                                          Serializable value, BamError error)
  {
    getBrokerStream(env).queryError(id, to, null, value, error);
  }

  public static void bam_send_presence(Env env, String to, Serializable value)
  {
    String jid = getJid(env);
    getBrokerStream(env).presence(to, jid, value);
  }

  public static void bam_send_presence_unavailable(Env env, String to, 
                                                   Serializable value)
  {
    getBrokerStream(env).presenceUnavailable(to, null, value);
  }

  public static void bam_send_presence_probe(Env env, String to, 
                                             Serializable value)
  {
    getBrokerStream(env).presenceProbe(to, null, value);
  }

  public static void bam_send_presence_subscribe(Env env, String to, 
                                                 Serializable value)
  {
    getBrokerStream(env).presenceSubscribe(to, null, value);
  }

  public static void bam_send_presence_subscribed(Env env, String to, 
                                                  Serializable value)
  {
    getBrokerStream(env).presenceSubscribed(to, null, value);
  }

  public static void bam_send_presence_unsubscribe(Env env, String to, 
                                                   Serializable value)
  {
    getBrokerStream(env).presenceUnsubscribe(to, null, value);
  }

  public static void bam_send_presence_unsubscribed(Env env, String to, 
                                                    Serializable value)
  {
    getBrokerStream(env).presenceUnsubscribed(to, null, value);
  }

  public static void bam_send_presence_error(Env env, String to, 
                                             Serializable value,
                                             BamError error)
  {
    getBrokerStream(env).presenceError(to, null, value, error);
  }

  /**
   * Dispatches messages, queries, and presences to handler functions based
   * on their prefixes.
   **/
  public static Value bam_dispatch(Env env)
  {
    Value eventTypeValue = env.getGlobalValue("_quercus_bam_event_type");

    if (eventTypeValue == null)
      return BooleanValue.FALSE;

    BamEventType eventType = (BamEventType) eventTypeValue.toJavaObject();

    Value to = env.getGlobalValue("_quercus_bam_to");
    Value from = env.getGlobalValue("_quercus_bam_from");
    Value value = env.getGlobalValue("_quercus_bam_value");

    AbstractFunction function = findFunction(env, eventType.getPrefix(), value);

    if (function == null) {
      log.fine(L.l("bam handler function not found for {0}", eventType));

      return BooleanValue.FALSE;
    }

    Value functionReturn = BooleanValue.FALSE;

    if (eventType.hasId() && eventType.hasError()) {
      Value id = env.getGlobalValue("_quercus_bam_id");
      Value error = env.getGlobalValue("_quercus_bam_error");

      functionReturn = function.call(env, id, to, from, value, error);
    } 
    else if (! eventType.hasId() && eventType.hasError()) {
      Value error = env.getGlobalValue("_quercus_bam_error");

      functionReturn = function.call(env, to, from, value, error);
    }
    else if (eventType.hasId() && ! eventType.hasError()) {
      Value id = env.getGlobalValue("_quercus_bam_id");

      functionReturn = function.call(env, id, to, from, value);
    }
    else {
      functionReturn = function.call(env, to, from, value);
    }

    env.setGlobalValue("_quercus_bam_function_return", functionReturn);

    return functionReturn;
  }

  /**
   * Finds the handler function for a value with the given prefix.  If there
   * is a specific handler for a specific value type, that is returned 
   * otherwise the generic handler (with the name of the prefix) is returned
   * if found.
   **/
  private static AbstractFunction findFunction(Env env, 
                                               String prefix, 
                                               Value value)
  {
    if (value == null)
      return env.findFunction(prefix);

    Object obj = value.toJavaObject();

    if (obj == null)
      return env.findFunction(prefix);

    String typeName = obj.getClass().getSimpleName().toLowerCase();
    String functionName = prefix + '_' + typeName;

    AbstractFunction function = env.findFunction(functionName);

    if (function == null)
      function = env.findFunction(prefix);

    return function;
  }

  public static void im_send_message(Env env, 
                                     String to, 
                                     String from, 
                                     String body)
  {
    bam_send_message(env, to, new ImMessage(to, from, body));
  }

  public static RosterItem im_create_roster_item(Env env, 
                                                 String jid,
                                                 @Optional String name,
                                                 @Optional String subscription,
                                                 @Optional 
                                                 ArrayList<String> groupList)
  {
    if ("".equals(subscription))
      subscription = "both";

    return new RosterItem(null, jid, name, subscription, groupList);
  }

  public static void im_send_roster(Env env, 
                                    long id, String to, 
                                    ArrayList<RosterItem> roster)
  {
    bam_send_query_result(env, id, to, new RosterQuery(roster));
  }

  public static void im_send_presence(Env env, 
                                      String to, 
                                      String status)
  {
    bam_send_presence(env, to, new ImPresence(status));
  }
}

