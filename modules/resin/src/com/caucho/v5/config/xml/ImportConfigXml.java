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

package com.caucho.v5.config.xml;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.NoAspect;
import com.caucho.v5.config.core.ImportConfig;
import com.caucho.v5.vfs.PathImpl;

/**
 * Imports values from a separate file.
 */
// XXX: FlowBean is from ioc/04c1 and server/1ac2
@NoAspect
public class ImportConfigXml extends ImportConfig
{
  @Override
  protected void configure(Object bean, PathImpl path)
  {
    ConfigXml config = new ConfigXml();
    // server/10hc
    // config.setResinInclude(true);
    
    String tail = path.getTail();
    
    if (tail.endsWith(".cf")) {
      config.configure2(bean, path);
    }
    else {
      String schema = null;
      // Use the relax schema for beans with schema.
      if (bean instanceof XmlSchemaBean) {
        schema = ((XmlSchemaBean) bean).getSchema();
      }
      
      try {
        config.configureBean(bean, path, schema);
      } catch (Exception e) {
        throw ConfigException.wrap(e);
      }
    }
  }
}

