/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib;

import javax.servlet.http.HttpServletRequest;

import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;

/**
 * Quercus functions to get information about the Quercus environment.
 */
public class QuercusModule extends AbstractQuercusModule
{
  /**
   * Returns true if this is the Professional version.
   */
  public static boolean quercus_is_pro(Env env)
  {
    return env.isPro();
  }
  
  /**
   * Returns true if pages will be compiled.
   */
  public static boolean quercus_is_compile(Env env)
  {
    return env.isCompile();
  }
  
  /*
   * Returns true if a JDBC database has been explicitly set.
   */
  public static boolean quercus_has_database(Env env)
  {
    return env.hasDatabase();
  }
  
  /*
   * Returns true if there is an HttpRequest associated with this Env.
   */
  public static boolean quercus_has_request(Env env)
  {
    return env.hasRequest();
  }
  
  /**
   * Returns the HttpServletRequest associated with this Env.
   */
  public static HttpServletRequest quercus_get_request(Env env)
  {
    return env.getRequest();
  }
}
