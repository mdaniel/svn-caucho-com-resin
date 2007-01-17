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

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

abstract public class AbstractMatchesCondition
  extends AbstractCondition
{
  private Pattern _regexp;
  private boolean _isIgnoreCase;

  /**
   * The regular expression to compare to the value, required.
   */
  public void setRegexp(String regexp)
  {
    _regexp = Pattern.compile(regexp);
  }

  public Pattern getRegexp()
  {
    return _regexp;
  }

  /**
   * Set's the ignoreCase, if true the case is unimportant in the match,
   * default false.
   */
  public void setIgnoreCase(boolean ignoreCase)
  {
    _isIgnoreCase = ignoreCase;
  }

  public boolean isIgnoreCase()
  {
    return _isIgnoreCase;
  }

  @PostConstruct
  public void init()
  {
    required(_regexp, "regexp");

    if (_isIgnoreCase)
      _regexp = Pattern.compile(_regexp.pattern(), Pattern.CASE_INSENSITIVE);
  }

  abstract protected String getValue(HttpServletRequest request);

  public boolean isMatch(HttpServletRequest request)
  {
    String value = getValue(request);

    if (value == null)
        value = "";

    return _regexp.matcher(value).find();
  }
}
