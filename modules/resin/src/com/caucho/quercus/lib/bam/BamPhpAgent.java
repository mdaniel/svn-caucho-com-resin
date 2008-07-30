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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.BamError;
import com.caucho.bam.BamStream;
import com.caucho.config.ConfigException;
import com.caucho.hemp.broker.GenericService;
import com.caucho.quercus.Quercus;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.JavaValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.quercus.page.InterpretedPage;
import com.caucho.quercus.parser.QuercusParser;
import com.caucho.quercus.program.JavaClassDef;
import com.caucho.quercus.program.QuercusProgram;
import com.caucho.util.L10N;
import com.caucho.vfs.NullWriteStream;
import com.caucho.vfs.Path;
import com.caucho.vfs.WriteStream;
import com.caucho.xmpp.disco.DiscoInfoQuery;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * BAM agent that calls into a PHP script to handle messages/queries.
 **/
public class BamPhpAgent extends GenericService {
  private static final L10N L = new L10N(BamPhpAgent.class);
  private static final Logger log
    = Logger.getLogger(BamPhpAgent.class.getName());

  private final Quercus _quercus = new Quercus();

  private final HashMap<String,BamPhpAgent> _children
    = new HashMap<String,BamPhpAgent>();

  private ArrayList<String> _featureNames = new ArrayList<String>();

  private Path _script;
  private String _encoding = "ISO-8859-1";

  public BamPhpAgent()
  {
  }

  public BamPhpAgent(Path script, String encoding)
  {
    _script = script;
    _encoding = encoding;
  }

  public Path getScript()
  {
    return _script;
  }

  public void setScript(Path script)
  {
    _script = script;
  }

  public String getEncoding()
  {
    return _encoding;
  }

  public void setEncoding(String encoding)
  {
    _encoding = encoding;
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_script == null)
      throw new ConfigException(L.l("script path not specified"));

    super.init();
  }

  @Override
  public boolean startAgent(String jid)
  {
    if (log.isLoggable(Level.FINE)) 
      log.fine(L.l("{0}.startAgent({1})", toString(), jid));

    return hasChild(jid);
  }

  boolean hasChild(String jid)
  {
    synchronized(_children) {
      return _children.containsKey(jid);
    }
  }

  void addChild(String jid, BamPhpAgent child)
  {
    synchronized(_children) {
      _children.put(jid, child);
    }
  }

  private Env createEnv(QuercusPage page, BamEventType type, 
                        String to, String from, Serializable value)
  {
    WriteStream out = new NullWriteStream();

    Env env = new Env(_quercus, page, out, null, null);

    JavaClassDef agentClassDef = env.getJavaClassDefinition(BamPhpAgent.class);
    env.setGlobalValue("_quercus_bam_agent", agentClassDef.wrap(env, this));

    env.start();

    JavaClassDef eventClassDef = env.getJavaClassDefinition(BamEventType.class);
    Value typeValue = eventClassDef.wrap(env, type);

    env.setGlobalValue("_quercus_bam_event_type", typeValue);

    env.setGlobalValue("_quercus_bam_to", StringValue.create(to));
    env.setGlobalValue("_quercus_bam_from", StringValue.create(from));

    Value javaValue = NullValue.NULL;

    if (value != null) {
      JavaClassDef classDef = env.getJavaClassDefinition(value.getClass());
      javaValue = classDef.wrap(env, value);
    }

    env.setGlobalValue("_quercus_bam_value", javaValue);

    return env;
  }

  private void setId(Env env, long id)
  {
    env.setGlobalValue("_quercus_bam_id", LongValue.create(id));
  }

  private void setError(Env env, BamError error)
  {
    Value errorValue = NullValue.NULL;
    if (error != null) {
      JavaClassDef errorClassDef = env.getJavaClassDefinition(BamError.class);
      errorValue = errorClassDef.wrap(env, error);
    }

    env.setGlobalValue("_quercus_bam_error", errorValue);
  }

  @Override
  public void message(String to, String from, Serializable value)
  {
    Env env = null;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.MESSAGE, to, from, value);
      page.executeTop(env);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();
    }
  }

  @Override
  public void messageError(String to, String from, Serializable value,
                           BamError error)
  {
    Env env = null;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.MESSAGE_ERROR, to, from, value);
      setError(env, error);

      page.executeTop(env);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();
    }
  }

  @Override
  public boolean queryGet(long id, String to, String from, Serializable value)
  {
    BamEventType eventType = BamEventType.QUERY_GET;

    // XXX move to override of introspected method
    if (value instanceof DiscoInfoQuery)
      eventType = BamEventType.GET_DISCO_FEATURES;

    Env env = null;
    boolean understood = false;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, eventType, to, from, value);
      setId(env, id);

      page.executeTop(env);

      if (eventType == BamEventType.GET_DISCO_FEATURES) {
        _featureNames.clear();

        Value returnValue = env.getGlobalValue("_quercus_bam_function_return");

        if (returnValue.isArray()) {
          _featureNames = 
            (ArrayList) returnValue.toJavaList(env, ArrayList.class);
        }

        understood = handleDiscoInfoQuery(id, to, from, (DiscoInfoQuery) value);
      }
      else {
        understood = 
          env.getGlobalValue("_quercus_bam_function_return").toBoolean();
      }
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();

      return understood;
    }
  }

  @Override
  protected void getDiscoFeatureNames(ArrayList<String> featureNames)
  {
    featureNames.addAll(_featureNames);
  }

  @Override
  public boolean querySet(long id, String to, String from, Serializable value)
  {
    Env env = null;
    boolean understood = false;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.QUERY_SET, to, from, value);
      setId(env, id);

      page.executeTop(env);

      understood = 
        env.getGlobalValue("_quercus_bam_function_return").toBoolean();
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();

      return understood;
    }
  }

  @Override
  public void queryResult(long id, String to, String from, Serializable value)
  {
    Env env = null;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.QUERY_RESULT, to, from, value);
      setId(env, id);

      page.executeTop(env);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();
    }
  }

  @Override
  public void queryError(long id, String to, String from, 
                         Serializable value, BamError error)
  {
    Env env = null;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.QUERY_ERROR, to, from, value);
      setId(env, id);
      setError(env, error);

      page.executeTop(env);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();
    }
  }

  @Override
  public void presence(String to, String from, Serializable value)
  {
    Env env = null;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.PRESENCE, to, from, value);

      page.executeTop(env);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();
    }
  }

  @Override
  public void presenceUnavailable(String to, String from, Serializable value)
  {
    Env env = null;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.PRESENCE_UNAVAILABLE, to, from, value);

      page.executeTop(env);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();
    }
  }

  @Override
  public void presenceProbe(String to, String from, Serializable value)
  {
    Env env = null;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.PRESENCE_PROBE, to, from, value);

      page.executeTop(env);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();
    }
  }

  @Override
  public void presenceSubscribe(String to, String from, Serializable value)
  {
    Env env = null;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.PRESENCE_SUBSCRIBE, to, from, value);

      page.executeTop(env);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();
    }
  }

  @Override
  public void presenceSubscribed(String to, String from, Serializable value)
  {
    Env env = null;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.PRESENCE_SUBSCRIBED, to, from, value);

      page.executeTop(env);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();
    }
  }

  @Override
  public void presenceUnsubscribe(String to, String from, Serializable value)
  {
    Env env = null;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.PRESENCE_UNSUBSCRIBE, to, from, value);

      page.executeTop(env);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();
    }
  }

  @Override
  public void presenceUnsubscribed(String to, String from, Serializable value)
  {
    Env env = null;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.PRESENCE_UNSUBSCRIBED, 
                      to, from, value);

      page.executeTop(env);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();
    }
  }

  @Override
  public void presenceError(String to, String from, 
                            Serializable value, BamError error)
  {
    Env env = null;

    try {
      QuercusPage page = _quercus.parse(_script);
      env = createEnv(page, BamEventType.PRESENCE_ERROR, to, from, value);
      setError(env, error);

      page.executeTop(env);
    }
    catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
    finally {
      if (env != null)
        env.close();
    }
  }

  public String toString()
  {
    return "BamPhpAgent[jid=" + getJid() + ",script=" + _script + "]";
  }
}
