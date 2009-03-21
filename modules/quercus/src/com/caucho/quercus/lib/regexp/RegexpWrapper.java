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
    Regexp regexp = _regexp;
    StringValue regexpStr = _regexpStr;
    
    Regexp regexp2 = _regexp2;
    StringValue regexpStr2 = _regexpStr2;
    
    if (regexp == null || regexpStr == null) {
      regexp = RegexpModule.createRegexpNoCache(env, str);
      _regexp = regexp;
      _regexpStr = str;
      
      return regexp;
    }
    else if (str == regexpStr
             || (str.hashCode() == regexpStr.hashCode()
                 && str.equals(regexpStr))) {
      return regexp;
    }
    else if (regexp2 == null || regexpStr2 == null) {
      regexp2 = RegexpModule.createRegexpNoCache(env, str);
      _regexp2 = regexp2;
      _regexpStr2 = str;
      
      return regexp2;
    }
    else if (str == regexpStr2
        || (str.hashCode() == regexpStr2.hashCode()
            && str.equals(regexpStr2))) {
      return regexp2;
    }
    else {
      return RegexpModule.createRegexp(env, str);
    }
  }
}
