/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.ant;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.*;

/**
 * Command-line tool and ant task to compile jsp files
 */
public class Jspc extends Task {
  private File _rootDirectory;
  private File _resinHome;
  private Vector _classpath = new Vector();
  protected Level _level = Level.WARNING;

  /**
   * For ant.
   **/
  public Jspc()
  {
  }

  public void setRootDirectory(File root)
  {
    _rootDirectory = root;
  }

  public void addClasspath(Path path)
  {
    _classpath.add(path);
  }

  public File getResinHome()
  {
    return _resinHome;
  }

  public void setResinHome(File resinHome)
  {
    _resinHome = resinHome;
  }

  public Level getLevel()
  {
    return _level;
  }

  public void setLevel(Level level)
  {
    _level = level;
  }

  public void setLogLevel(String level)
  {
    if (level == null || level.isEmpty())
      return;

    level = level.toUpperCase();

    _level = Level.parse(level);
  }

  /**
   * Executes the ant task.
   **/
  public void execute()
    throws BuildException
  {
    if (_resinHome == null)
      throw new BuildException("resinHome is required by jspc");

    if (_rootDirectory == null)
      throw new BuildException("rootDirectory is required by jspc");

    File resinJar = new File(_resinHome,
                             "lib" + File.separatorChar + "resin.jar");

    if (! resinJar.exists())
      throw new BuildException("resinHome '"
                                 + _resinHome
                                 + "' does not appear to be valid");

    Java java = new Java(this);
    java.setFailonerror(true);
    java.setFork(true);
    java.setJar(resinJar);

    List<String> args = new ArrayList<String>();
    args.add("jspc");
    args.add("-app-dir");
    args.add(_rootDirectory.getPath());

    for (String arg : args)
      java.createArg().setLine(arg);

    log(java.getCommandLine().toString(), _level.intValue());

    java.executeJava();
  }
}
