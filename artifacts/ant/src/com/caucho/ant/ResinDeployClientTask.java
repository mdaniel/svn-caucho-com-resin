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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

public abstract class ResinDeployClientTask extends Task {
  private String _server;
  private int _port = -1;
  private String _user;
  private String _message;
  private String _password;

  private String _stage = "default";
  private String _virtualHost = "default";
  private String _contextRoot;
  private String _version;

  public void setServer(String server)
  {
    _server = server;
  }

  public String getServer()
  {
    return _server;
  }

  public void setPort(int port)
  {
    _port = port;
  }

  public int getPort()
  {
    return _port;
  }

  public void setUser(String user)
  {
    _user = user;
  }

  public String getUser()
  {
    return _user;
  }

  public void setPassword(String password)
  {
    _password = password;
  }

  public String getPassword()
  {
    return _password;
  }

  public void setCommitMessage(String message)
  {
    _message = message;
  }

  public String getCommitMessage()
  {
    return _message;
  }

  public void setStage(String stage)
  {
    _stage = stage;
  }

  public String getStage()
  {
    return _stage;
  }

  public void setVirtualHost(String virtualHost)
  {
    _virtualHost = virtualHost;
  }

  public String getVirtualHost()
  {
    return _virtualHost;
  }

  public void setContextRoot(String contextRoot)
  {
    _contextRoot = contextRoot;
  }

  public String getContextRoot()
  {
    return _contextRoot;
  }

  public void setVersion(String version)
  {
    _version = version;
  }

  public String getVersion()
  {
    return _version;
  }

  protected String buildWarTag()
  {
    return buildWarTag(_stage, _virtualHost, _contextRoot);
  }

  protected String buildWarTag(String stage, 
                               String host, 
                               String contextRoot)
  {
    return stage + "/wars/" + host + "/" + contextRoot;
  }

  protected String buildVersionedWarTag()
  {
    return buildVersionedWarTag(_stage, _virtualHost, _contextRoot, _version);
  }

  protected String buildVersionedWarTag(String stage, 
                                        String host, 
                                        String contextRoot, 
                                        String version)
  {
    if (version != null)
      return buildWarTag(stage, host, contextRoot) + '-' + version;

    return buildWarTag(stage, host, contextRoot);
  }

  protected void validate()
    throws BuildException
  {
    if (_server == null)
      throw new BuildException("server is required by " + getTaskName());

    if (_port == -1)
      throw new BuildException("port is required by " + getTaskName());

    if (_user == null)
      throw new BuildException("user is required by " + getTaskName());
  }

  protected abstract void doTask(DeployClient client)
    throws BuildException;

  /**
   * Executes the ant task.
   **/
  @Override
  public void execute()
    throws BuildException
  {
    validate();

    // fix the class loader

    ClassLoader loader = DeployClient.class.getClassLoader();

    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(loader);

      doTask(new DeployClient(_server, _port, _user, _password));
    }
    finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
}
