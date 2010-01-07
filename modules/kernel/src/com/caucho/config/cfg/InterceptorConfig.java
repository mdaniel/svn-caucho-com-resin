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

package com.caucho.config.cfg;

import com.caucho.config.*;
import com.caucho.config.j2ee.*;
import com.caucho.config.types.*;
import com.caucho.util.*;
import com.caucho.naming.*;
import com.caucho.config.inject.*;

import java.lang.reflect.*;
import java.lang.annotation.*;

import javax.annotation.*;

/**
 * Configuration for the xml interceptor component.
 */
public class InterceptorConfig {
  private static final L10N L = new L10N(InterceptorConfig.class);

  private Class _class;

  public void setClass(Class cl)
  {
    _class = cl;
  }

  @PostConstruct
  public void init()
  {
    if (_class == null)
      throw new ConfigException(L.l("'class' is a required attribute of <interceptor>"));
    
    InjectManager webBeans = InjectManager.create();
    webBeans.addInterceptor(new InterceptorBean(_class));
  }
}
