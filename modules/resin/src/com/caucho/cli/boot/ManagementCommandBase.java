/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

import com.caucho.amp.remote.ClientAmp;
import com.caucho.cli.baratine.ArgsCli;
import com.caucho.cli.server.BootArgumentException;
import com.caucho.cli.server.RemoteCommandBase;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.server.admin.ManagerClientApi;
import com.caucho.server.config.ServerConfigBoot;

public abstract class ManagementCommandBase extends RemoteCommandBase
{
  public static final int RETURN_CODE_SERVER_ERROR = 32;

  @Override
  public ExitCode doCommand(ArgsCli args,
                            ServerConfigBoot server,
                            ClientAmp client)
    throws BootArgumentException
  {
    ManagerClientApi managerClient
      = client.lookup("remote:///manager").as(ManagerClientApi.class);

    return doCommand(args, server, managerClient);
  }

  protected abstract ExitCode doCommand(ArgsCli args,
                                        ServerConfigBoot server,
                                        ManagerClientApi client);
}