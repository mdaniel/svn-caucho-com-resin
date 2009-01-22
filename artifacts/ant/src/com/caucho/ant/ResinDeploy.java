/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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

package com.caucho.ant;

import java.io.File;
import java.io.IOException;

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.server.admin.DeployClient;
import com.caucho.vfs.Vfs;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

/**
 * Ant task to deploy war files to resin
 */
public class ResinDeploy {
  private String _server;
  private int _port = -1;
  private String _warFile;
  private String _user;
  private String _message;
  private String _password;
  private String _version;
  private String _virtualHost = "default";

  /**
   * For ant.
   **/
  public ResinDeploy()
  {
  }

  public void setServer(String server)
  {
    _server = server;
  }

  public void setPort(int port)
  {
    _port = port;
  }

  public void setWarFile(String warFile)
  {
    _warFile = warFile;
  }

  public void setCommitMessage(String message)
  {
    _message = message;
  }

  public void setUser(String user)
  {
    _user = user;
  }

  public void setPassword(String password)
  {
    _password = password;
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  public void setVirtualHost(String virtualHost)
  {
    _virtualHost = virtualHost;
  }

  /**
   * Executes the ant task.
   **/
  public void execute()
    throws BuildException
  {
    if (_warFile == null)
      throw new BuildException("war-file is required by resin-deploy");

    if (! _warFile.endsWith(".war"))
      throw new BuildException("war-file must have .war extension");

    if (_server == null)
      throw new BuildException("server is required by resin-deploy");

    if (_port == -1)
      throw new BuildException("port is required by resin-deploy");

    if (_user == null)
      throw new BuildException("user is required by resin-deploy");

    // compute the git tag

    int lastSlash = _warFile.lastIndexOf("/");

    if (lastSlash < 0)
      lastSlash = 0;

    String name = _warFile.substring(lastSlash, 
                                     _warFile.length() - ".war".length());
    String tag = "wars/" + _virtualHost + "/" + name;

    // fix the class loader

    ClassLoader loader = DeployClient.class.getClassLoader();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(loader);

      DeployClient client = new DeployClient(_server, _port, _user, _password);
      com.caucho.vfs.Path path = Vfs.lookup(_warFile);
      client.deployJarContents(path, tag, _user, _message, _version, null);
    }
    catch (IOException e) {
      throw new BuildException(e);
    }
    finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
}
