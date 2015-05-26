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
 * @author Alex Rojkov
 */

package com.caucho.cli.boot;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.cli.baratine.ArgsCli;
import com.caucho.cli.server.BootArgumentException;
import com.caucho.cli.server.ServerCommandBase;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.jsp.JspCompiler;
import com.caucho.server.config.ConfigBoot;

public class JspcCommand extends ServerCommandBase<ArgsCli>
{
  private final static Logger log
    = Logger.getLogger(JspcCommand.class.getName());
  
  @Override
  protected void initBootOptions()
  {
    addValueOption("app-dir", "dir", "Directory root of the web-app");
    addValueOption("class-dir", "dir", "The work directory to compile as output");
    
    addSpacerOption();
    
    addValueOption("compiler", "value", "The java compiler for javac");
    
    super.initBootOptions();
  }
  
  @Override
  public String getDescription()
  {
    return "pre-compiles JSP files";
  }
  
  public String getUsageTailArgs()
  {
    return " <jsp1> <jsp2> ...";
  }

  @Override
  public boolean isTailArgsAccepted()
  {
    return true;
  }

  @Override
  public ExitCode doCommand(ArgsCli args, ConfigBoot boot)
    throws BootArgumentException
  {
    List<String> jspcArgs = new ArrayList<String>();

    final String appDir = args.getArg("-app-dir");
    if (appDir != null && ! "".equals(appDir)) {
      jspcArgs.add("-app-dir");
      jspcArgs.add(appDir);
    }

    final String classDir = args.getArg("-class-dir");
    if (classDir != null && ! "".equals(classDir)) {
      jspcArgs.add("-class-dir");
      jspcArgs.add(classDir);
    }

    final String compiler = args.getArg("-compiler");
    if (compiler != null && ! "".equals(compiler)) {
      jspcArgs.add("-compiler");
      jspcArgs.add(compiler);
    }

    final String conf = args.getArg("-conf");
    if (conf != null && ! "".equals(conf)) {
      jspcArgs.add("-conf");
      jspcArgs.add(conf);
    }

    jspcArgs.addAll(args.getTailArgs());

    try {
      JspCompiler.main(jspcArgs.toArray(new String[jspcArgs.size()]));

      StringBuilder builder = new StringBuilder();
      if (appDir != null) {
        builder.append(appDir);
      }
      else {
        for (String tailArg : args.getTailArgs()) {
          builder.append(tailArg).append(' ');
        }
      }

      System.out.println("resin jspc finished compiling " + builder.toString());

      return ExitCode.OK;
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage(), e);

      return ExitCode.EXIT_1;
    }
  }

  /*
  @Override
  public void usage()
  {
    try {
      JspCompiler.main(new String[]{});
    } catch (Exception e) {
      log.log(Level.SEVERE, e.getMessage(), e);
    }
  }
  */
}
