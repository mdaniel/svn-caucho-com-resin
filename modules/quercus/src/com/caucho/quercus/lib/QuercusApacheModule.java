/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib;

import java.text.Collator;

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Enumeration;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.lang.Math;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;

import com.caucho.server.connection.CauchoResponse;

import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.quercus.module.Optional;
import com.caucho.quercus.module.Reference;
import com.caucho.quercus.module.UsesSymbolTable;

import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.NullValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.DefaultValue;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.ArrayValueImpl;
import com.caucho.quercus.env.DoubleValue;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Callback;

import com.caucho.quercus.env.ArrayValue.ValueComparator;
import com.caucho.quercus.env.ArrayValue.KeyComparator;
import com.caucho.quercus.env.ArrayValue.GetKey;
import com.caucho.quercus.env.ArrayValue.AbstractGet;

import com.caucho.quercus.program.AbstractFunction;

/**
 * PHP apache routines.
 */
public class QuercusApacheModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(QuercusApacheModule.class);

  private static final Logger log =
    Logger.getLogger(QuercusApacheModule.class.getName());

  /**
   * Stub for insisting the apache process should terminate.
   */
  public boolean apache_child_terminate()
  {
    return false;
  }

  // XXX: apache_get_modules
  // XXX: apache_get_version
  // XXX: apache_getenv
  // XXX: apache_lookup_uri
  
  /**
   * Gets and sets apache notes
   */
  public String apache_note(Env env,
			    String name,
			    @Optional Value value)
  {
    HttpServletRequest req = env.getRequest();

    Object oldValue = req.getAttribute(name);

    if (value.isset())
      req.setAttribute(name, value.toString());

    if (oldValue != null)
      return oldValue.toString();
    else
      return null;
  }

  /**
   * Returns all the request headers
   */
  public Value apache_request_headers(Env env)
  {
    HttpServletRequest req = env.getRequest();

    ArrayValue result = new ArrayValueImpl();
    
    Enumeration e = req.getHeaderNames();

    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();

      result.put(new StringValue(key), new StringValue(req.getHeader(key)));
    }

    return result;
  }

  // XXX: apache_response_headers

  /**
   * Stub for resetting the output timeout.
   */
  public boolean apache_reset_timeout()
  {
    return false;
  }

  // XXX: apache_setenv
  // XXX: ascii2ebcdic
  // XXX: ebcdic2ascii

  /**
   * Returns all the request headers
   */
  public Value getallheaders(Env env)
  {
    return apache_request_headers(env);
  }

  /**
   * Include request.
   */
  public boolean virtual(Env env, String url)
    throws IOException, ServletException
  {
    HttpServletRequest req = env.getRequest();
    CauchoResponse res = (CauchoResponse) env.getResponse();

    // XXX: need to put the output, so the included stream gets the
    // buffer, too
    env.getOut().flushBuffer();

    req.getRequestDispatcher(url).include(req, res);

    return true;
  }
}
