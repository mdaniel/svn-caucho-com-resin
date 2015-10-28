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

package com.caucho.v5.cli.boot;

import com.caucho.v5.cli.baratine.ArgsCli;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.env.shutdown.ExitCode;
import com.caucho.v5.server.admin.ManagerClientApi;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.StreamSource;
import com.caucho.v5.vfs.StreamSourcePath;
import com.caucho.v5.vfs.Vfs;

public class StoreLoadCommand extends ManagementCommandBase
{
  private static final L10N L = new L10N(StoreLoadCommand.class);
  
  private String _context = "web-app:production/webapp/default/ROOT";
  
  protected void setContext(String context)
  {
    _context = context;
  }
  
  protected String getContext()
  {
    return _context;
  }
  
  @Override
  protected void initBootOptions()
  {
    addValueOption("input", "file", "file name of the archive").tiny("i");
    
    super.initBootOptions();
  }
  
  @Override
  public String getDescription()
  {
    return "restores the store from an archive";
  }

  @Override
  public ExitCode doCommand(ArgsCli args,
                            ServerConfigBoot server,
                            ManagerClientApi managerClient)
  {
    String name = "test";
    
    String context = getContext();
    
    String fileName = args.getArg("input");
    
    if (fileName == null) {
      throw new ConfigException(L.l("--input is a required option"));
    }
    
    String value = null;
    
    Path path = Vfs.lookup(fileName);
    
    if (! path.canRead()) {
      throw new ConfigException(L.l("{0} is an unreadable path", path));
    }
    
    StreamSourcePath sourcePath = new StreamSourcePath(path);
    StreamSource ss = new StreamSource(sourcePath);
    
    managerClient.doStoreRestore(name + ":" + context, ss);
    
    return ExitCode.OK;
  }
}