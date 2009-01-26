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

package com.caucho.ejb.session;

import com.caucho.config.ConfigContext;
import com.caucho.config.inject.ComponentImpl;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.scope.ScopeContext;

import java.lang.annotation.*;
import javax.context.CreationalContext;

/**
 * Component for session beans
 */
public class StatelessComponent extends ComponentImpl
{
  private final StatelessProvider _provider;

  public StatelessComponent(StatelessProvider provider, Class api)
  {
    super(InjectManager.create());

    _provider = provider;
  }

  @Override
  public void setScope(ScopeContext scope)
  {
  }

  @Override
  public Object get()
  {
    return _provider.__caucho_get();
  }

  @Override
  public Object create(CreationalContext env)
  {
    return _provider.__caucho_get();
  }

  @Override
  public Object create()
  {
    return _provider.__caucho_get();
  }
}
