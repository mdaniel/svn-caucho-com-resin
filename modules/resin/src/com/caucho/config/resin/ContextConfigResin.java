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
 */

package com.caucho.config.resin;

import com.caucho.v5.config.Config;
import com.caucho.v5.config.core.ContextConfig;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.config.type.TypeFactoryResin;
import com.caucho.v5.config.xml.ContextConfigXml;
import com.caucho.v5.inject.Module;

/**
 * The ConfigContext contains the state of the current configuration.
 */
@Module
public class ContextConfigResin extends ContextConfigXml
{
  public ContextConfigResin(ContextConfigResin parent)
  {
    super(parent);
  }

  public ContextConfigResin(Config config)
  {
    super(config);
  }

  public static ContextConfigResin getCurrent()
  {
    return (ContextConfigResin) ContextConfig.getCurrent();
  }
  
  @Override
  protected TypeFactoryConfig getTypeFactory()
  {
    return TypeFactoryResin.getFactory();
  }
}
