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
 *
 * @author Sam
 */

package com.caucho.v5.rewrite;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.el.ELException;
import javax.servlet.http.HttpServletRequest;

import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.expr.ExprCfg;
import com.caucho.v5.http.rewrite.RequestPredicate;
import com.caucho.v5.util.L10N;

/**
 * Passes if the named header exists and has a value
 * that matches a regular expression.
 *
 * <pre>
 * &lt;web-app xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:Forbidden regexp="^/local/">
 *     &lt;resin:IfHeader name="foo" regexp="bar"/>
 *   &lt;/resin:Forbidden>
 *
 * &lt;/web-app>
 * </pre>
 *
 * <p>RequestPredicates may be used for both security and rewrite conditions.
 */
@Configurable
public class IfExpr implements RequestPredicate
{
  private static final L10N L = new L10N(IfExpr.class);
  private static final Logger log = Logger.getLogger(IfExpr.class.getName());
  
  private ExprCfg _test;

  public IfExpr()
  {
  }

  /**
   * Sets the EL expression to compare against.
   */
  // @Configurable
  public void setTest(ExprCfg expr)
  {
    setExpr(expr);
  }
  
  @Configurable
  public void setExpr(ExprCfg expr)
  {
    _test = expr;
  }
  
  public void setValue(ExprCfg expr)
  {
    setTest(expr);
  }

  @PostConstruct
  public void init()
  {
    if (_test == null)
      throw new ConfigException(L.l("'test' is a required attribute for {0}",
                                    getClass().getSimpleName()));
  }

  /**
   * True if the predicate matches.
   *
   * @param request the servlet request to test
   */
  @Override
  public boolean isMatch(HttpServletRequest request)
  {
    try {
      return _test.evalBoolean(ConfigContext.getEnvironment());
    } catch (ELException e) {
      log.log(Level.FINER, e.toString(), e);
      
      return false;
    }
  }
}
