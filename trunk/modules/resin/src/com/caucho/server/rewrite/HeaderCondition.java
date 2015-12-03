/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
* A rewrite condition that passes if the value of a named header matches a regexp.
*/
public class HeaderCondition
  extends AbstractCondition
{
  private final String _header;
  private Pattern _regexp;
  private boolean _caseInsensitive = false;
  private boolean _sendVary = true;

  HeaderCondition(String header)
  {
    _header = header;
  }
  
  public String getTagName()
  {
    return "header";
  }

  public void setRegexp(Pattern pattern)
  {
    _regexp = pattern;
  }

  public void setCaseInsensitive(boolean caseInsensitive)
  {
    _caseInsensitive = caseInsensitive;
  }

  public void setSendVary(boolean sendVary)
  {
    _sendVary = sendVary;
  }

  @PostConstruct
  public void init()
  {
    if (_regexp != null && _caseInsensitive)
      _regexp = Pattern.compile(_regexp.pattern(), Pattern.CASE_INSENSITIVE);
  }

  public boolean isMatch(HttpServletRequest request,
                         HttpServletResponse response)
  {
    if (_sendVary)
      addHeaderValue(response, "Vary", _header);

    String value = request.getHeader(_header);

    if (value == null)
      return false;
    else
      return _regexp == null || _regexp.matcher(value).find();
  }
}
