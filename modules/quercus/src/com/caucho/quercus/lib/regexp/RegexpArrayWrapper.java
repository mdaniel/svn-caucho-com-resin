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
    if (_regexp == null) {
      _regexp = RegexpModule.createRegexpArrayNoCache(env, pattern);
      _regexpValue = pattern;

      return _regexp;
    }
    else if (pattern == _regexpValue
             || (pattern.isString()
                 && pattern.hashCode() == _regexpValue.hashCode()
                 && pattern.equals(_regexpValue))
             || (pattern.cmp(_regexpValue) == 0)) {
      return _regexp;
    }
    else if (_regexp2 == null) {
      _regexp2 = RegexpModule.createRegexpArrayNoCache(env, pattern);
      _regexpValue2 = pattern;
      
      return _regexp2;
    }
    else if (pattern == _regexpValue2
        || (pattern.isString()
            && pattern.hashCode() == _regexpValue2.hashCode()
            && pattern.equals(_regexpValue2))
        || (pattern.cmp(_regexpValue2) == 0)) {
      return _regexp2;
    }
    else if (_regexp3 == null) {
      _regexp3 = RegexpModule.createRegexpArrayNoCache(env, pattern);
      _regexpValue3 = pattern;
      
      return _regexp3;
    }
    else if (pattern == _regexpValue3
        || (pattern.isString()
            && pattern.hashCode() == _regexpValue3.hashCode()
            && pattern.equals(_regexpValue3))
        || (pattern.cmp(_regexpValue3) == 0)) {
      return _regexp3;
    }
    else if (_regexp4 == null) {
      _regexp4 = RegexpModule.createRegexpArrayNoCache(env, pattern);
      _regexpValue4 = pattern;
      
      return _regexp4;
    }
    else if (pattern == _regexpValue4
        || (pattern.isString()
            && pattern.hashCode() == _regexpValue4.hashCode()
            && pattern.equals(_regexpValue4))
        || (pattern.cmp(_regexpValue4) == 0)) {
      return _regexp4;
    }
    else if (_regexp5 == null) {
      _regexp5 = RegexpModule.createRegexpArrayNoCache(env, pattern);
      _regexpValue5 = pattern;
      
      return _regexp5;
    }
    else if (pattern == _regexpValue5
        || (pattern.isString()
            && pattern.hashCode() == _regexpValue5.hashCode()
            && pattern.equals(_regexpValue5))
        || (pattern.cmp(_regexpValue5) == 0)) {
      return _regexp5;
    }
    else if (_regexp6 == null) {
      _regexp6 = RegexpModule.createRegexpArrayNoCache(env, pattern);
      _regexpValue6 = pattern;
      
      return _regexp6;
    }
    else if (pattern == _regexpValue6
        || (pattern.isString()
            && pattern.hashCode() == _regexpValue6.hashCode()
            && pattern.equals(_regexpValue6))
        || (pattern.cmp(_regexpValue6) == 0)) {
      return _regexp6;
    }
    else {
      return RegexpModule.createRegexpArray(env, pattern);
    }
  }
}
