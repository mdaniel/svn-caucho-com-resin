/*
< * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
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

package com.caucho.v5.config.type;

import java.util.HashMap;

import com.caucho.v5.config.cf.QName;
import com.caucho.v5.config.custom.ConfigCustomBean;
import com.caucho.v5.config.custom.TypeCustomBean;
import com.caucho.v5.config.resin.ConfigCustomBeanResin;
import com.caucho.v5.config.type.InlineBeanType;
import com.caucho.v5.config.type.TypeFactoryConfig;
import com.caucho.v5.loader.EnvironmentLocal;

/**
 * Factory for returning type strategies.
 */
public class TypeFactoryResin extends TypeFactoryConfig
{
  private static final EnvironmentLocal<TypeFactoryResin> _localFactory
    = new EnvironmentLocal<>();
    
  private final HashMap<String,TypeCustomBean<?>> _customBeanMap
    = new HashMap<>();
  
  public TypeFactoryResin(ClassLoader loader)
  {
    super(loader);
  }

  public static TypeFactoryResin getFactory()
  {
    return getFactory(Thread.currentThread().getContextClassLoader());
  }

  public static TypeFactoryResin getFactory(ClassLoader loader)
  {
    if (loader == null) {
      loader = getSystemClassLoader();
    }

    TypeFactoryResin factory = _localFactory.getLevel(loader);

    if (factory == null) {
      factory = new TypeFactoryResin(loader);
      _localFactory.set(factory, loader);
      factory.init(loader);
    }

    return factory;
  }
  
  @Override
  protected void initNamespaces()
  {
    super.initNamespaces();
    
    addNamespace(NamespaceConfigResin.URN_RESIN);
    addNamespace(NamespaceConfigResin.NS_RESIN);
    addNamespace(NamespaceConfigResin.NS_RESIN_CORE);
    
    addNamespace(NamespaceConfigResin.NS_JAVAEE);
    addNamespace(NamespaceConfigResin.NS_J2EE);
  }
  
  
  protected InlineBeanType createInlineBean(Class<?> type)
  {
    return new InlineBeanTypeResin(this, type);
  }
  
  @Override
  protected ConfigCustomBean<?> createCustomBeanImpl(QName qName,
                                                  Class<?> cl,
                                                  Object parent)
  {
    return new ConfigCustomBeanResin(qName, cl, parent);
  }
}
