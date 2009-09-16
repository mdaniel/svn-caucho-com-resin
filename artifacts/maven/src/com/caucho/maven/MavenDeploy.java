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

package com.caucho.maven;

import com.caucho.server.admin.DeployClient;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.server.admin.StatusQuery;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.io.IOException;
import java.util.HashMap;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * The MavenDeploy
 * @goal deploy
 */
public class MavenDeploy extends AbstractMojo
{
  private int _port = 8080;
  private String _server = "127.0.0.1";
  private String _warFile;
  private String _virtualHost = "default";
  private String _contextPath;
  private String _user;
  private String _password;
  private String _message;
  private String _version;

  /**
   * Sets the ip address of the host that resin is running on
   */
  public void setServer(String server)
  {
    _server = server;
  }

  /**
   * Sets the HTTP port that the resin instance is listening to
   */
  public void setPort(int port)
  {
    _port = port;
  }

  /**
   * Sets the path of the WAR file (defaults to target/${finalName}.war)
   */
  public void setWarFile(String warFile)
  {
    _warFile = warFile;
  }

  /**
   * Sets the context path of the webapp (defaults to /${finalName})
   */
  public void setContextPath(String contextPath)
  {
    _contextPath = contextPath;
  }

  /**
   * Sets the virtual host to which to deploy the webapp (defaults to default)
   */
  public void setVirtualHost(String virtualHost)
  {
    _virtualHost = virtualHost;
  }

  /**
   * Sets the user name for the deployment service
   */
  public void setUser(String user)
  {
    _user = user;
  }

  /**
   * Sets the password for the deployment service
   */
  public void setPassword(String password)
  {
    _password = password;
  }

  /**
   * Sets the commit message for the deploy
   */
  public void setCommitMessage(String message)
  {
    _message = message;
  }

  /**
   * Sets the version for the deploy
   */
  public void setVersion(String version)
  {
    _version = version;
  }

  /**
   * Executes the maven resin:run task
   */
  public void execute() 
    throws MojoExecutionException
  {
    WebAppDeployClient client = null;

    try {
      client = new WebAppDeployClient(_server, _port, _user, _password);

      String tag = 
        WebAppDeployClient.createTag("default", _virtualHost, _contextPath);

      Path path = Vfs.lookup(_warFile);

      HashMap<String,String> attributes = new HashMap<String,String>();
      attributes.put(WebAppDeployClient.USER_ATTRIBUTE, _user);
      attributes.put(WebAppDeployClient.MESSAGE_ATTRIBUTE, _message);
      attributes.put(WebAppDeployClient.VERSION_ATTRIBUTE, _version);

      client.deployJarContents(tag, path, attributes);
    }
    catch (IOException e) {
      throw new MojoExecutionException("Unable to connect to server", e);
    }
  }
}
