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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson;
 */

package com.caucho.config.program;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.spi.CreationalContext;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.type.ConfigType;
import com.caucho.loader.Environment;
import com.caucho.xml.QName;

/**
 * Stored configuration program for an attribute.
 */
public class RecoverableProgram extends FlowProgram {
  private static final Logger log
    = Logger.getLogger(RecoverableProgram.class.getName());
  
  public static final String ATTR = "resin.config.recoverable";
  
  private ConfigProgram _program;

  public RecoverableProgram(ConfigProgram program)
  {
    _program = program;
  }

  @Override
  public QName getQName()
  {
    return _program.getQName();
  }

  @Override
  public <T> void inject(T bean, CreationalContext<T> cxt)
    throws ConfigException
  {
    Object oldRecover = Config.getProperty(ATTR);
    
    try {
      Config.setProperty(ATTR, true);
      
      _program.inject(bean, cxt);
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.toString(), e);
      Environment.setConfigException(e);
    } finally {
      Config.setProperty(ATTR, oldRecover);
    }
  }

  @Override
  public <T> T create(ConfigType<T> type, CreationalContext<T> cxt)
    throws ConfigException
  {
    try {
      return _program.create(type, cxt);
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.toString(), e);
      Environment.setConfigException(e);
      
      return null;
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _program + "]";
  }
}
