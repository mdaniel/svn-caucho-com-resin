/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.regexp;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;

public class RegexpWrapper
{
  private Regexp _regexp;
  private StringValue _regexpStr;
  
  // two so wordpress doesn't need to create a new regexp
  private Regexp _regexp2;
  private StringValue _regexpStr2;
  
  public RegexpWrapper()  
  {
  }
  
  public Regexp get(Env env, StringValue str)
  {
    if (_regexp == null) {
      _regexp = RegexpModule.createRegexpNoCache(env, str);
      _regexpStr = str;
      
      return _regexp;
    }
    else if (str == _regexpStr
             || (str.hashCode() == _regexpStr.hashCode()
                 && _regexpStr.equals(str))) {
      return _regexp;
    }
    else if (_regexp2 == null) {
      _regexp2 = RegexpModule.createRegexpNoCache(env, str);
      _regexpStr2 = str;
      
      return _regexp2;
    }
    else if (str == _regexpStr2
        || (str.hashCode() == _regexpStr2.hashCode()
            && _regexpStr2.equals(str))) {
      return _regexp2;
    }
    else {
      return RegexpModule.createRegexp(env, str);
    }
  }
}
