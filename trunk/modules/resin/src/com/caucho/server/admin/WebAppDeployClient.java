/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.server.admin;

import com.caucho.bam.actor.ActorSender;
import com.caucho.server.deploy.DeployClient;
import com.caucho.server.resin.Resin;
import com.caucho.util.QDate;

/**
 * Deploy Client API
 */
public class WebAppDeployClient extends DeployClient
{
  public WebAppDeployClient()
  {
    super(Resin.getCurrentServerId());
  }
  
  public WebAppDeployClient(String serverId)
  {
    super(serverId);
  }

  public WebAppDeployClient(String url, ActorSender sender)
  {
    super(url, sender);
  }

  public WebAppDeployClient(String host, int port,
                            String userName, String password)
  {
    super(host, port, userName, password);
  }
  
  /*
  public String []listWebApps(String host)
  {
    return listTags("production/webapps/host");
  }
  */

  //
  // low-level routines
  //

  //
  // tag construction
  //

  public static String createTag(String stage, 
                                 String host, 
                                 String name)
  {
    while (name.startsWith("/"))
      name = name.substring(1);
    
    return stage + "/webapp/" + host + "/" + name;
  }

  public static String createTag(String stage, 
                                 String host,
                                 String name,
                                 String version)
  {
    if (version != null)
      return createTag(stage, host, name) + "-" + version;

    return createTag(stage, host, name);
  }

  public static String createArchiveTag(String host,
                                        String name, 
                                        String version)
  {
    QDate qDate = new QDate();
    long time = qDate.getTimeOfDay() / 1000;

    StringBuilder sb = new StringBuilder();

    sb.append(createTag("archive", host, name, version));

    sb.append('/');

    sb.append(qDate.printISO8601Date());

    sb.append('T');
    sb.append((time / 36000) % 10);
    sb.append((time / 3600) % 10);

    sb.append(':');
    sb.append((time / 600) % 6);
    sb.append((time / 60) % 10);

    sb.append(':');
    sb.append((time / 10) % 6);
    sb.append((time / 1) % 10);

    return sb.toString();
  }
}

