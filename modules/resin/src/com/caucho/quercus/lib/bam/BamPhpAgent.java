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

import com.caucho.config.*;
import com.caucho.hemp.broker.*;
import com.caucho.util.*;
import com.caucho.vfs.*;

import javax.annotation.*;
import javax.script.*;

/**
 * BAM agent that calls into a PHP script to handle messages/queries.
 **/
public class BamPhpAgent extends GenericService {
  private static final L10N L = new L10N(BamPhpAgent.class);
  private static final Logger log
    = Logger.getLogger(BamPhpAgent.class.getName());

  private CompiledScript _script;
  private Path _scriptPath;

  public Path getScript()
  {
    return _scriptPath;
  }

  public void setScript(Path scriptPath)
  {
    _scriptPath = scriptPath;
  }

  @PostConstruct
  public void init()
    throws ConfigException
  {
    if (_scriptPath == null)
      throw new ConfigException(L.l("script path not specified"));

    ReadStream stream = null;

    try {
      stream = _scriptPath.openRead();
      ScriptEngineManager manager = new ScriptEngineManager();
      ScriptEngine engine = manager.getEngineByName("quercus");
      Compilable compiler = (Compilable) engine;

      _script = compiler.compile(stream.getReader());
    }
    catch (IOException e) {
      throw new ConfigException(L.l("unable to open script {0}", _scriptPath), 
                                e);
    }
    catch (ScriptException e) {
      throw new ConfigException(L.l("unable to compile script {0}", 
                                    _scriptPath), 
                                e);
    }
    finally {
      if (stream != null)
        stream.close();
    }

    super.init();
  }

  @Override
  public void message(String to, String from, Serializable value)
  {
    try {
      ScriptContext context = new SimpleScriptContext();

      context.setAttribute("_quercus_bam_event_type", BamEventType.MESSAGE,
                           ScriptContext.ENGINE_SCOPE);
      context.setAttribute("_quercus_bam_message_to", to, 
                           ScriptContext.ENGINE_SCOPE);
      context.setAttribute("_quercus_bam_message_from", from,
                           ScriptContext.ENGINE_SCOPE);
      context.setAttribute("_quercus_bam_message_value", value,
                           ScriptContext.ENGINE_SCOPE);

      _script.eval(context);
    }
    catch (ScriptException e) {
      log.fine(L.l("sendMessage({0}, {1}, {2}) failed: {3}", 
                   to, from, value, e));
    }
  }
}
