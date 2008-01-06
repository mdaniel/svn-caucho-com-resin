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
 * @author Scott Ferguson;
 */

package com.caucho.config.j2ee;

import com.caucho.config.BuilderProgram;
import com.caucho.config.LineConfigException;
import com.caucho.config.ConfigException;
import com.caucho.config.NodeBuilder;
import com.caucho.util.*;

import java.lang.reflect.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PostConstructProgram extends CallbackProgram
{
  private static final Logger log
    = Logger.getLogger(PostConstructProgram.class.getName());
  private static final L10N L = new L10N(PostConstructProgram.class);

  private Method _init;

  public PostConstructProgram(Method init)
  {
    _init = init;
    _init.setAccessible(true);
  }

  @Override
  public void configureImpl(NodeBuilder builder, Object bean)
    throws ConfigException
  {
    try {
      _init.invoke(bean);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  @Override
  public Object configureImpl(NodeBuilder builder, Class type)
    throws ConfigException
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public int hashCode()
  {
    return _init.getName().hashCode();
  }

  public boolean equals(Object o)
  {
    if (this == o)
      return true;
    else if (! (o instanceof PostConstructProgram))
      return false;

    PostConstructProgram program = (PostConstructProgram) o;
    Method init = program._init;

    if (! _init.getName().equals(init.getName()))
      return false;

    Class []aParam = _init.getParameterTypes();
    Class []bParam = init.getParameterTypes();

    if (aParam.length != bParam.length)
      return false;

    for (int i = 0; i < aParam.length; i++) {
      if (! aParam[i].equals(bParam[i]))
        return false;
    }

    return true;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _init + "]";
  }
}
