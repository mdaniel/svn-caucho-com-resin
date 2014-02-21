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

import com.caucho.cloud.scaling.ResinScalingClient;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.lang.reflect.Constructor;

public abstract class AbstractScalingCommand extends AbstractBootCommand
{
  private static final L10N L = new L10N(AbstractScalingCommand.class);
  private static Class<?> _scalingClientClass;

  @Override
  protected void initBootOptions()
  {
    addValueOption("address", "address", "ip or host name of the server");
    addIntValueOption("port", "port", "server http port");
    
    addSpacerOption();
    
    addValueOption("user", "user", "user name used for authentication to the server");
    addValueOption("password", "password", "password used for authentication to the server");
    
    super.initBootOptions();
  }

  protected ResinScalingClient getScalingClient(WatchdogArgs args,
                                                WatchdogClient client)
  {
    String address = args.getArg("-address");

    if (address == null || address.isEmpty())
      address = client.getConfig().getAddress();

    int port = -1;

    String portArg = args.getArg("-port");

    try {
      if (portArg != null && ! portArg.isEmpty())
        port = Integer.parseInt(portArg);
    } catch (NumberFormatException e) {
      NumberFormatException e1 = new NumberFormatException(
        "-port argument is not a number '" + portArg + "'");
      e1.setStackTrace(e.getStackTrace());

      throw e;
    }

    if (port == -1)
      port = client.getConfig().getPort();

    if (port == 0) {
      throw new ConfigException(L.l("HTTP listener {0}:{1} was not found",
                                    address, port));
    }

    String user = args.getArg("-user");
    String password = args.getArg("-password");

    try {
      Constructor c = _scalingClientClass.getConstructor(String.class,
                                                         int.class,
                                                         String.class,
                                                         String.class);

      ResinScalingClient scaingClient;
      scaingClient = (ResinScalingClient) c.newInstance(address,
                                                        port,
                                                        user,
                                                        password);

      return scaingClient;
    } catch (Exception e) {
      throw new ConfigException(e.getMessage(), e);
    }
  }

  @Override
  public boolean isProOnly()
  {
    return true;
  }

  public boolean isPro()
  {
    return (_scalingClientClass != null);
  }

  static {
    try {
      _scalingClientClass = Class.forName(
        "com.caucho.cloud.elastic.ElasticCloudClient");
    } catch (ClassNotFoundException e) {
    }
  }
}
