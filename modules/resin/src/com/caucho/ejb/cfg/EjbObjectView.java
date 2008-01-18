/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.ejb.cfg;

import com.caucho.ejb.cfg21.EjbView;
import com.caucho.config.ConfigException;
import com.caucho.ejb.gen21.BeanAssembler;
import com.caucho.ejb.gen21.ViewClass;
import com.caucho.log.Log;
import com.caucho.util.L10N;

import java.util.logging.Logger;
import java.util.ArrayList;


/**
 * Configuration for a particular view.
 */
public class EjbObjectView extends EjbView
{
  private static final Logger log = Log.open(EjbObjectView.class);
  private static final L10N L = new L10N(EjbObjectView.class);

  private boolean _isRemote;
  
  /**
   * Creates a new entity bean configuration.
   */
  public EjbObjectView(EjbBean bean,
		       ArrayList<ApiClass> apiList,
		       String prefix,
		       String suffix,
		       boolean isRemote)
    throws ConfigException
  {
    super(bean, apiList, prefix, suffix);

    _isRemote = isRemote;
  }

  /**
   * Assembles the generator.
   */
  protected void assembleView(BeanAssembler assembler,
                              String fullClassName)
    throws ConfigException
  {
    ViewClass viewClass;

    if (_isRemote) {
      viewClass = assembler.createRemoteView(getApiList(),
					     fullClassName,
					     getPrefix(),
					     getSuffix());
    }
    else {
      viewClass = assembler.createView(getApiList(),
				       fullClassName,
				       getPrefix(),
				       getSuffix());
    }

    assembleMethods(assembler, viewClass, fullClassName);
  }
}
