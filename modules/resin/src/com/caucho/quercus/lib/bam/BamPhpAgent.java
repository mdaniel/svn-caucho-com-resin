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

import java.io.*;
import java.util.logging.*;

import com.caucho.bam.BamError;
import com.caucho.config.*;
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
import com.caucho.util.*;
import com.caucho.vfs.*;

import javax.annotation.*;

/**
 * BAM agent that calls into a PHP script to handle messages/queries.
 **/
public class BamPhpAgent extends GenericService {
  private static final L10N L = new L10N(BamPhpAgent.class);
  private static final Logger log
    = Logger.getLogger(BamPhpAgent.class.getName());

  private Quercus _quercus = new Quercus();
  private QuercusProgram _program;
  private Path _scriptPath;
  private String _encoding = "ISO-8859-1";

  public Path getScript()
  {
    return _scriptPath;
  }

  public void setScript(Path scriptPath)
  {
    _scriptPath = scriptPath;
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
    if (_scriptPath == null)
      throw new ConfigException(L.l("script path not specified"));

    try {
      _program = QuercusParser.parse(_quercus, _scriptPath, _encoding);
    }
    catch (IOException e) {
      throw new ConfigException(L.l("unable to open script {0}", _scriptPath), 
                                e);
    }

    super.init();
  }

  private Env createEnv(BamEventType type, 
                        String to, String from, Serializable value)
  {
    WriteStream out = new NullWriteStream();

    QuercusPage page = new InterpretedPage(_program);

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
    _program.execute(createEnv(BamEventType.MESSAGE, to, from, value));
  }

  @Override
  public void messageError(String to, String from, Serializable value,
                           BamError error)
  {
    Env env = createEnv(BamEventType.MESSAGE_ERROR, to, from, value);
    setError(env, error);

    _program.execute(env);
  }

  @Override
  public boolean queryGet(long id, String to, String from, Serializable value)
  {
    Env env = createEnv(BamEventType.QUERY_GET, to, from, value);
    setId(env, id);

    return _program.execute(env).toBoolean();
  }


  @Override
  public boolean querySet(long id, String to, String from, Serializable value)
  {
    Env env = createEnv(BamEventType.QUERY_SET, to, from, value);
    setId(env, id);

    return _program.execute(env).toBoolean();
  }

  @Override
  public void queryResult(long id, String to, String from, Serializable value)
  {
    Env env = createEnv(BamEventType.QUERY_RESULT, to, from, value);
    setId(env, id);

    _program.execute(env);
  }

  @Override
  public void queryError(long id, String to, String from, 
                         Serializable value, BamError error)
  {
    Env env = createEnv(BamEventType.QUERY_ERROR, to, from, value);
    setId(env, id);
    setError(env, error);

    _program.execute(env);
  }

  @Override
  public void presence(String to, String from, Serializable value)
  {
    _program.execute(createEnv(BamEventType.PRESENCE, to, from, value));
  }

  @Override
  public void presenceUnavailable(String to, String from, Serializable value)
  {
    _program.execute(createEnv(BamEventType.PRESENCE_UNAVAILABLE, 
                               to, from, value));
  }

  @Override
  public void presenceProbe(String to, String from, Serializable value)
  {
    _program.execute(createEnv(BamEventType.PRESENCE_PROBE, to, from, value));
  }

  @Override
  public void presenceSubscribe(String to, String from, Serializable value)
  {
    _program.execute(createEnv(BamEventType.PRESENCE_SUBSCRIBE, 
                               to, from, value));
  }

  @Override
  public void presenceSubscribed(String to, String from, Serializable value)
  {
    _program.execute(createEnv(BamEventType.PRESENCE_SUBSCRIBED, 
                               to, from, value));
  }

  @Override
  public void presenceUnsubscribe(String to, String from, Serializable value)
  {
    _program.execute(createEnv(BamEventType.PRESENCE_UNSUBSCRIBE, 
                               to, from, value));
  }

  @Override
  public void presenceUnsubscribed(String to, String from, Serializable value)
  {
    _program.execute(createEnv(BamEventType.PRESENCE_UNSUBSCRIBED, 
                               to, from, value));
  }

  @Override
  public void presenceError(String to, String from, 
                            Serializable value, BamError error)
  {
    Env env = createEnv(BamEventType.PRESENCE_ERROR, to, from, value);
    setError(env, error);

    _program.execute(env);
  }
}
