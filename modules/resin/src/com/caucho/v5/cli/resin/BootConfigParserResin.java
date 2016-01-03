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
 * @author Scott Ferguson
 */

package com.caucho.v5.cli.resin;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.cli.server.BootConfigParser;
import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.lib.ResinConfigLibrary;
import com.caucho.v5.config.xml.ConfigXml;
import com.caucho.v5.server.cdi.ResinServerConfigLibrary;
import com.caucho.v5.server.config.RootConfigBoot;
import com.caucho.v5.vfs.PathImpl;

public class BootConfigParserResin extends BootConfigParser
{
  private static final Logger log
    = Logger.getLogger(BootConfigParserResin.class.getName());
  
  @Override
  protected ConfigXml createConfig()
  {
    ConfigXml config = new ConfigXml();

    //CandiManager cdiManager = CandiManager.create();

    ResinConfigLibrary.configure();
    ResinServerConfigLibrary.configure();
    
    return config;
  }
  
  @Override
  protected void configure(ConfigContext config, RootConfigBoot bean, PathImpl path)
  {
    if (path == null) {
      return;
    }
    
    ConfigXml configXml = (ConfigXml) config;

    if (log.isLoggable(Level.FINER)) {
      log.fine("CLI parsing " + path.getNativePath());
    }
    
    if (path.getTail().endsWith(".cf")) {
      config.configure2(bean, path);
    }
    else {
      bean.setConfigTemplate(null);
      
      configXml.configure(bean, path, 
                       "com/caucho/v5/server/resin/resin.rnc");
    }
  }
}
