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
 * @author Sam
 */

package com.caucho.server.rewrite;

import com.caucho.server.dispatch.Invocation;
import com.caucho.config.types.RawString;
import com.caucho.util.InetNetwork;

import java.util.regex.*;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;
import javax.servlet.ServletRequest;
import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;

public class RequireConfig
{
  private AbstractCondition _condition;
  
  private String _regexp;
  private boolean _isIgnoreCase;

  /**
   * Sets the el expression.
   */
  public void setExpr(RawString expr)
  {
    _condition = new ExprCondition(expr.getValue());
  }
  
  public void setHeader(String header)
  {
    _condition = new HeaderCondition(header);
  }
  
  public void setCookie(String cookie)
  {
    _condition = new CookieCondition(cookie);
  }
  
  public void setParam(String cookie)
  {
    _condition = new ParamCondition(cookie);
  }
  
  public void setRemoteAddr(String addr)
  {
    _condition = new RemoteAddrCondition(InetNetwork.create(addr));
  }

  public void setRegexp(String regexp)
  {
    _regexp = regexp;
  }

  public void setIgnoreCase(boolean ignoreCase)
  {
    _isIgnoreCase = ignoreCase;
  }
  
  Condition getCondition()
  {
    if (_regexp == null) {
    }
    else if (_isIgnoreCase)
      _condition.setRegexp(Pattern.compile(_regexp, Pattern.CASE_INSENSITIVE));
    else
      _condition.setRegexp(Pattern.compile(_regexp));

    return _condition;
  }
}
