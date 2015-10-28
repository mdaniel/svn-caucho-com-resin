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

package com.caucho.rewrite;

import java.util.Locale;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.http.rewrite.RequestPredicateVary;
import com.caucho.v5.util.L10N;

/**
 * Match if the request's locale matches an expression.
 *
 * <pre>
 * &lt;resin:Allow url-pattern="/admin/*"&gt;
 *                  xmlns:resin="urn:java:com.caucho.resin"&gt;
 *   &lt;resin:IfLocale value="fr"/>
 * &lt;/resin:Allow>
 * </pre>
 *
 * <p>RequestPredicates may be used for security and rewrite actions.
 */
@Configurable
public class IfLocale implements RequestPredicateVary
{
  private static final L10N L = new L10N(IfLocale.class);

  private Pattern _regexp;
  private boolean _isSendVary = true;

  /**
   * The locale value to test against
   */
  @ConfigArg(0)
  public void setValue(Pattern regexp)
  {
    _regexp = regexp;
  }
  
  public void setSendVary(boolean isVary)
  {
    _isSendVary = isVary;
  }

  @PostConstruct
  public void init()
  {
    if (_regexp == null)
      throw new ConfigException(L.l("'value' is a required attribute for {0}",
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
    Locale locale = request.getLocale();

    if (locale == null)
      return false;
    else
      return _regexp.matcher(locale.toString()).find();
  }
  
  @Override
  public void addVaryHeader(HttpServletResponse response)
  {
    if (_isSendVary) {
      response.addHeader("Vary", "Accept-Language");
    }
  }
}
