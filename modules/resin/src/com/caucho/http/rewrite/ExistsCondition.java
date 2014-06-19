/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

package com.caucho.http.rewrite;

import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;
import com.caucho.vfs.*;

/**
* A rewrite condition that passes if the target exists.
*/
public class ExistsCondition
  extends AbstractCondition
{
  private static final L10N L = new L10N(ExistsCondition.class);

  private Path _pwd;
  
  private String _value;
  private Pattern _regexp;

  ExistsCondition(String value)
  {
    _value = value;

    _pwd = Vfs.lookup();
  }
  
  public String getTagName()
  {
    return "exists";
  }

  public void setRegexp(Pattern pattern)
  {
    _regexp = pattern;
  }

  public boolean isMatch(HttpServletRequest request,
                         HttpServletResponse response)
  {
    String servletPath = request.getServletPath();
    String realPath = request.getRealPath(servletPath);

    Path path = _pwd.lookup(realPath);
    
    return path.canRead();
  }
}
