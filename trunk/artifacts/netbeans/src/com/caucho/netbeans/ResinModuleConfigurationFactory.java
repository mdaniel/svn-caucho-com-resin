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
 * @author Scott Ferguson
 */
package com.caucho.netbeans;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.j2ee.deployment.common.api.ConfigurationException;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eeModule;
import org.netbeans.modules.j2ee.deployment.plugins.spi.config.ModuleConfiguration;
import org.netbeans.modules.j2ee.deployment.plugins.spi.config.ModuleConfigurationFactory;

import java.util.logging.*;

public class ResinModuleConfigurationFactory
        implements ModuleConfigurationFactory {

  private static final Logger log = Logger.getLogger(ResinModuleConfigurationFactory.class.getName());
  private static ResinModuleConfigurationFactory _instance;
  private List<WarConfiguration> _warConfigurations = new ArrayList<WarConfiguration>();

  private ResinModuleConfigurationFactory() {
  }

  public synchronized static ResinModuleConfigurationFactory create() {
    if (_instance == null) {
      _instance = new ResinModuleConfigurationFactory();
    }

    return _instance;
  }

  public ModuleConfiguration create(J2eeModule module)
          throws ConfigurationException {
    if (J2eeModule.WAR == module.getModuleType()) {
      WarConfiguration configuration = new WarConfiguration(module);
      _warConfigurations.add(configuration);

      return configuration;
    } else {
      return null;
    }
  }

  public WarConfiguration find(File file) {
    String name = file.getPath();

    for (WarConfiguration config : _warConfigurations) {
      try {
        String path = config.getJ2eeModule().getArchive().getPath();
        if (name.equals(path)) {
          return config;
        }
      } catch (Exception e) {
        log.log(Level.WARNING, "Can't find configuration for " + file, e);
      }
    }

    return null;
  }
}
