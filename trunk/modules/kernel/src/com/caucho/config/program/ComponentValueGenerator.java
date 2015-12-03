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

package com.caucho.config.program;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;

import com.caucho.config.inject.InjectManager;

/**
 * Generator for a component value.
 */
public class ComponentValueGenerator extends ValueGenerator {
  private final InjectManager _cdiManager;
  private final Bean<?> _bean;

  public ComponentValueGenerator(String location, Bean<?> bean)
  {
    if (bean == null)
      throw new NullPointerException();

    _cdiManager = InjectManager.create();
    _bean = bean;
  }

  /**
   * Creates the value.
   */
  @Override
  public Object create()
  {
    CreationalContext<?> env = _cdiManager.createCreationalContext(_bean);
    Class<?> type = _bean.getBeanClass();

    return _cdiManager.getReference(_bean, type, env);
  }
}
