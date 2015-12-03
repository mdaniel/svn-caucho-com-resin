/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.boot;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.VersionFactory;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.core.ResinProperties;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.lib.ResinConfigLibrary;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.loader.Environment;
import com.caucho.loader.LibraryLoader;
import com.caucho.server.resin.ResinELContext;
import com.caucho.server.webbeans.ResinServerConfigLibrary;
import com.caucho.util.L10N;
import com.caucho.vfs.NullPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

public class ResinBootAgent
{
  private static String _premainArgs;
  private static String _agentArgs;
  private static Instrumentation _instrument;
  
  public static Instrumentation getInstrumentation()
  {
    return _instrument;
  }
  
  public static void premain(String agentArgs, Instrumentation instrument)
  {
    _premainArgs = agentArgs;
    _instrument = instrument;
  }
  
  public static void agentmain(String agentArgs, Instrumentation instrument)
  {
    _premainArgs = agentArgs;
    _instrument = instrument;
  }
}
