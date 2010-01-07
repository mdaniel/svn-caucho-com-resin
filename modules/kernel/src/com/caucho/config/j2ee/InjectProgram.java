/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.config.j2ee;

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.ConfigContext;
import com.caucho.config.program.ConfigProgram;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Analyzes a bean for @Inject tags.
 */
public class InjectProgram {
  private static final L10N L = new L10N(InjectProgram.class);
  private static final Logger log = Log.open(InjectProgram.class);

  private final ConfigProgram []_program;

  public InjectProgram(ArrayList<ConfigProgram> program)
  {
    _program = new ConfigProgram[program.size()];
    program.toArray(_program);
  }

  /**
   * Analyzes a bean for @Inject tags, building an init program for them.
   */
  public void configure(Object bean)
  {
    if (bean != null) {
      ConfigContext env = new ConfigContext();
      
      for (int i = 0; i < _program.length; i++)
	_program[i].inject(bean, env);
    }
  }
}

