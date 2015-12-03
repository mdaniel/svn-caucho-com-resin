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
 * @author Alex Rojkov
 */

package com.caucho.boot;

import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.util.L10N;

public abstract class WebAppCommand extends AbstractRepositoryCommand
{
  private static final L10N L = new L10N(WebAppCommand.class);
  
  @Override
  protected void initBootOptions()
  {
    addValueOption("host", "host", "virtual host to make application available on");
    addValueOption("stage", "stage", "stage to deploy application to, defaults to production");
    addValueOption("version", "version", "version of application formatted as <major.minor.micro.qualifier>");
    
    super.initBootOptions();
  }

  @Override
  public final int doCommand(WatchdogArgs args,
                             WatchdogClient client,
                             WebAppDeployClient deployClient)
  {
    String tag = args.getArg("-tag");

    if (tag == null) {
      String name = getName(args, null);

      String stage = args.getArg("-stage");
      if (stage == null)
        stage = "production";

      String host = args.getArg("-host");
      if (host == null)
        host = "default";

      String version = args.getArg("-version");

      tag = stage + "/webapp/" + host + "/" + name;

      if (version != null)
        tag = tag + "-" + version;
    }

    return doCommand(deployClient, tag);
  }

  protected abstract int doCommand(WebAppDeployClient deployClient,
                                   String tag);
}
