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

package com.caucho.ejb.cfg21;

import com.caucho.config.*;
import com.caucho.config.types.InjectionTarget;
import com.caucho.ejb.cfg.*;
import com.caucho.ejb.gen.*;
import com.caucho.java.*;
import com.caucho.java.gen.*;
import com.caucho.util.L10N;

import javax.ejb.*;
import java.io.IOException;
import java.util.*;

/**
 * Generates the skeleton for an entity bean.
 */
public class EntityGenerator extends BeanGenerator {
  private static final L10N L = new L10N(EntityGenerator.class);

  private EjbEntityBean _entityBean;

  private ArrayList<View> _views = new ArrayList<View>();

  public EntityGenerator(EjbEntityBean entityBean)
  {
    super(toFullClassName(entityBean.getEJBName(),
			  entityBean.getEJBClass().getSimpleName()),
          entityBean.getEJBClassWrapper());

    _entityBean = entityBean;
  }

  private static String toFullClassName(String ejbName, String className)
  {
    StringBuilder sb = new StringBuilder();

    sb.append("_ejb.");

    if (! Character.isJavaIdentifierStart(ejbName.charAt(0)))
      sb.append('_');

    for (int i = 0; i < ejbName.length(); i++) {
      char ch = ejbName.charAt(i);

      if (ch == '/')
	sb.append('.');
      else if (Character.isJavaIdentifierPart(ch))
	sb.append(ch);
      else
	sb.append('_');
    }

    sb.append(".");
    sb.append(className);
    sb.append("__EJB");

    return sb.toString();
  }

  /**
   * Sets the local home
   */
  public void setLocalHome(ApiClass homeApi)
  {
  }

  /**
   * Sets the remote home
   */
  public void setRemoteHome(ApiClass homeApi)
  {
  }
  
  /**
   * Sets the local object
   */
  public void setLocalObject(ApiClass objectApi)
  {
  }

  /**
   * Sets the remote object
   */
  public void setRemoteObject(ApiClass objectApi)
  {
  }

  /**
   * Adds a local
   */
  public void addLocal(ApiClass localApi)
  {
  }

  /**
   * Adds a remote
   */
  @Override
  public void addRemote(ApiClass remoteApi)
  {
  }

  /**
   * Introspects the bean.
   */
  public void introspect()
  {
    _entityBean.initIntrospect();
      
    _entityBean.assembleBeanMethods();

    _entityBean.createViews21();
  }

  /**
   * Returns the views
   */
  public ArrayList<View> getViews()
  {
    return _views;
  }

  @Override
  public void generate(JavaWriter out)
    throws IOException
  {
    try {
      GenClass genClass = _entityBean.assembleGenerator(getFullClassName());

      if (genClass != null)
	genClass.generate(out);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
}
