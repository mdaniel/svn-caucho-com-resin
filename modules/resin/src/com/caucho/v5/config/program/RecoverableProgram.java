/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson;
 */

package com.caucho.v5.config.program;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.config.Config;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.config.type.ConfigType;
import com.caucho.v5.inject.InjectContext;
import com.caucho.v5.loader.EnvLoader;

/**
 * Stored configuration program for an attribute.
 */
public class RecoverableProgram extends FlowProgram {
  private static final Logger log
    = Logger.getLogger(RecoverableProgram.class.getName());
  
  public static final String ATTR = "caucho.config.recoverable";
  
  private ConfigProgram _program;

  public RecoverableProgram(Config config,
                            ConfigProgram program)
  {
    super(config);
    
    _program = program;
  }

  @Override
  public NameCfg getQName()
  {
    return _program.getQName();
  }

  @Override
  public <T> void inject(T bean, InjectContext cxt)
    throws ConfigException
  {
    Object oldRecover = Config.getProperty(ATTR);
    
    try {
      Config.setProperty(ATTR, true);
      
      _program.inject(bean, cxt);
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.toString(), e);
      EnvLoader.setConfigException(e);
    } finally {
      Config.setProperty(ATTR, oldRecover);
    }
  }

  @Override
  public <T> T create(ConfigType<T> type, InjectContext cxt)
    throws ConfigException
  {
    try {
      return _program.create(type, cxt);
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.toString(), e);
      EnvLoader.setConfigException(e);
      
      return null;
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _program + "]";
  }
}
