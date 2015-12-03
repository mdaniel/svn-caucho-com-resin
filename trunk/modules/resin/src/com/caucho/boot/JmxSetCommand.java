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

import com.caucho.config.ConfigException;
import com.caucho.server.admin.JmxSetQueryReply;
import com.caucho.server.admin.ManagerClient;
import com.caucho.util.L10N;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class JmxSetCommand extends JmxCommand
{
  private static final L10N L = new L10N(JmxSetCommand.class);

  @Override
  protected void initBootOptions()
  {
    addValueOption("pattern", "pattern",
      "pattern to match MBean, adheres to the rules defined for javax.managment.ObjectName e.g. qa:type=Foo");
    addValueOption("attribute", "attribute", "name of the attribute");
    
    super.initBootOptions();
  }
  
  @Override
  public String getDescription()
  {
    return "sets a JMX value for a server MBean";
  }
  
  @Override
  public String getUsageArgs()
  {
    return " <value>";
  }

  @Override
  public boolean isDefaultArgsAccepted()
  {
    return true;
  }

  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       ManagerClient managerClient)
  {
    String pattern = args.getArg("-pattern");

    if (pattern == null)
      throw new ConfigException(L.l(
        "-pattern is required for jmx-set command"));

    try {
      ObjectName.getInstance(pattern);
    } catch (MalformedObjectNameException e) {
      throw new ConfigException(L.l("invalid pattern `{0}': `{1}'",
                                    pattern,
                                    e.getMessage()));
    }

    String attribute = args.getArg("-attribute");

    if (attribute == null)
      throw new ConfigException(L.l(
        "-attribute is required for jmx-set command"));

    String value = args.getDefaultArg();

    if (value == null)
      throw new ConfigException(L.l(
        "jmx-set requires <value> parameter be specified"));

    JmxSetQueryReply reply = managerClient.setJmx(pattern,
                                                    attribute,
                                                    value);

    String message = L.l(
      "value for attribute `{0}' on bean `{1}' is changed from `{2}' to `{3}'",
      reply.getAttribute(),
      reply.getBean(),
      reply.getOldValue(),
      reply.getNewValue());

    System.out.println(message);

    return 0;
  }
}
