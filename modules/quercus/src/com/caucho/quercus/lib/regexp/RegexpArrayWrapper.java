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
import com.caucho.quercus.env.Value;

public class RegexpArrayWrapper
{
  private Regexp []_regexp;
  private Value _regexpValue;

  private Regexp []_regexp2;
  private Value _regexpValue2;
  
  private Regexp []_regexp3;
  private Value _regexpValue3;
  
  private Regexp []_regexp4;
  private Value _regexpValue4;
  
  private Regexp []_regexp5;
  private Value _regexpValue5;
  
  // six so mediawiki does not need to create a new regexp
  private Regexp []_regexp6;
  private Value _regexpValue6;
  
  public RegexpArrayWrapper()  
  {
  }
  
  public Regexp []get(Env env, Value pattern)
  {
    Regexp []regexp = _regexp;
    Value regexpValue = _regexpValue;
    
    Regexp []regexp2 = _regexp2;
    Value regexpValue2 = _regexpValue2;
    
    Regexp []regexp3 = _regexp3;
    Value regexpValue3 = _regexpValue3;
    
    Regexp []regexp4 = _regexp4;
    Value regexpValue4 = _regexpValue4;
    
    Regexp []regexp5 = _regexp5;
    Value regexpValue5 = _regexpValue5;
    
    Regexp []regexp6 = _regexp6;
    Value regexpValue6 = _regexpValue6;
    
    if (regexp == null) {
      regexp = RegexpModule.createRegexpArrayNoCache(env, pattern);
      _regexp = regexp;
      _regexpValue = pattern;

      return regexp;
    }
    else if (pattern == regexpValue
             || (pattern.isString()
                 && pattern.hashCode() == regexpValue.hashCode()
                 && pattern.equals(regexpValue))
             || (pattern.cmp(regexpValue) == 0)) {
      return regexp;
    }
    else if (regexp2 == null) {
      regexp2 = RegexpModule.createRegexpArrayNoCache(env, pattern);
      _regexp2 = regexp2;
      _regexpValue2 = pattern;
      
      return regexp2;
    }
    else if (pattern == regexpValue2
        || (pattern.isString()
            && pattern.hashCode() == regexpValue2.hashCode()
            && pattern.equals(regexpValue2))
        || (pattern.cmp(regexpValue2) == 0)) {
      return regexp2;
    }
    else if (regexp3 == null) {
      regexp3 = RegexpModule.createRegexpArrayNoCache(env, pattern);
      _regexp3 = regexp;
      _regexpValue3 = pattern;
      
      return regexp3;
    }
    else if (pattern == regexpValue3
        || (pattern.isString()
            && pattern.hashCode() == regexpValue3.hashCode()
            && pattern.equals(regexpValue3))
        || (pattern.cmp(regexpValue3) == 0)) {
      return regexp3;
    }
    else if (regexp4 == null) {
      regexp4 = RegexpModule.createRegexpArrayNoCache(env, pattern);
      _regexp4 = regexp4;
      _regexpValue4 = pattern;
      
      return regexp4;
    }
    else if (pattern == regexpValue4
        || (pattern.isString()
            && pattern.hashCode() == regexpValue4.hashCode()
            && pattern.equals(regexpValue4))
        || (pattern.cmp(regexpValue4) == 0)) {
      return regexp4;
    }
    else if (regexp5 == null) {
      regexp5 = RegexpModule.createRegexpArrayNoCache(env, pattern);
      _regexp5 = regexp5;
      _regexpValue5 = pattern;
      
      return regexp5;
    }
    else if (pattern == regexpValue5
        || (pattern.isString()
            && pattern.hashCode() == regexpValue5.hashCode()
            && pattern.equals(regexpValue5))
        || (pattern.cmp(regexpValue5) == 0)) {
      return regexp5;
    }
    else if (regexp6 == null) {
      regexp6 = RegexpModule.createRegexpArrayNoCache(env, pattern);
      _regexp6 = regexp6;
      _regexpValue6 = pattern;
      
      return regexp6;
    }
    else if (pattern == regexpValue6
        || (pattern.isString()
            && pattern.hashCode() == regexpValue6.hashCode()
            && pattern.equals(regexpValue6))
        || (pattern.cmp(regexpValue6) == 0)) {
      return regexp6;
    }
    else {
      return RegexpModule.createRegexpArray(env, pattern);
    }
  }
}
