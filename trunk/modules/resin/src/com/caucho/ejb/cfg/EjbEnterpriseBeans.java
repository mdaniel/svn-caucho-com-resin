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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.cfg;

import java.util.logging.Logger;

import com.caucho.config.program.ConfigProgram;
import com.caucho.util.L10N;

/**
 * Configuration for an ejb bean.
 */
public class EjbEnterpriseBeans {
  private static final L10N L = new L10N(EjbEnterpriseBeans.class);
  private static final Logger log
    = Logger.getLogger(EjbEnterpriseBeans.class.getName());
  
  private final EjbConfig _config;
  private final EjbJar _jar;
  private final String _ejbModuleName;

  public EjbEnterpriseBeans(EjbConfig config, 
                            EjbJar jar,
                            String ejbModuleName)
  {
    _config = config;
    _jar = jar;
    _ejbModuleName = ejbModuleName;
  }

  public EjbSessionConfigProxy createSession()
  {
    return new EjbSessionConfigProxy(_config, _jar, _ejbModuleName);
  }

  public EjbBeanConfigProxy createEjbBean()
  {
    return new EjbBeanConfigProxy(_config, _jar, _ejbModuleName);
  }

  public EjbMessageConfigProxy createMessageDriven()
  {
    return new EjbMessageConfigProxy(_config, _jar, _ejbModuleName);
  }
  
  public void addBuilderProgram(ConfigProgram program)
  {
    if (! _jar.isSkip()) {
      _jar.setSkip(true);
      
      log.warning(L.l("Skipping EJB jar '{0}' jar because of unknown EJB lite config {1}",
                      _jar, program));
    }
  }
}
