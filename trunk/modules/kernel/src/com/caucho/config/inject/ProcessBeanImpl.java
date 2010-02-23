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

package com.caucho.config.inject;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.annotation.ServiceBinding;
import com.caucho.config.ServiceStartup;
import com.caucho.config.cfg.BeansConfig;
import com.caucho.vfs.Path;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;

public class ProcessBeanImpl<X> implements ProcessBean<X>
{
  private InjectManager _manager;
  private Bean<X> _bean;
  private boolean _isVeto;

  ProcessBeanImpl(InjectManager manager, Bean<X> bean)
  {
    _manager = manager;
    _bean = bean;
  }

  public InjectManager getManager()
  {
    return _manager;
  }

  public Annotated getAnnotated()
  {
    if (_bean instanceof AbstractBean)
      return ((AbstractBean) _bean).getAnnotated();
    else
      return null;
  }

  public Bean<X> getBean()
  {
    return _bean;
  }
    
  public void setBean(Bean<X> bean)
  {
    _bean = bean;
  }

  public void addDefinitionError(Throwable t)
  {
  }

  public void veto()
  {
    _isVeto = true;
  }

  public boolean isVeto()
  {
    return _isVeto;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _bean + "]";
  }
}
