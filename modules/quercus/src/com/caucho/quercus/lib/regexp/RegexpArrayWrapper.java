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
  private RegexpCouple _couple;
  private RegexpCouple _couple2;
  private RegexpCouple _couple3;
  private RegexpCouple _couple4;
  private RegexpCouple _couple5;
  // six so mediawiki does not have to create a new regexp[]
  private RegexpCouple _couple6;
  
  public RegexpArrayWrapper()  
  {
  }
  
  public Regexp []get(Env env, Value pattern)
  {
    RegexpCouple couple = _couple;
    RegexpCouple couple2 = _couple2;
    RegexpCouple couple3 = _couple3;
    RegexpCouple couple4 = _couple4;
    RegexpCouple couple5 = _couple5;
    RegexpCouple couple6 = _couple6;
    
    pattern = pattern.toValue();

    if (couple == null) {
      Regexp []regexp = RegexpModule.createRegexpArray(env, pattern);
      
      _couple = new RegexpCouple(regexp, pattern);

      return regexp;
    }
    else if (couple.is(pattern)) {
      return couple._regexp;
    }
    else if (couple2 == null) {
      Regexp []regexp = RegexpModule.createRegexpArray(env, pattern);
      
      _couple2 = new RegexpCouple(regexp, pattern);

      return regexp;
    }
    else if (couple2.is(pattern)) {
      return couple2._regexp;
    }
    else if (couple3 == null) {
      Regexp []regexp = RegexpModule.createRegexpArray(env, pattern);
      
      _couple3 = new RegexpCouple(regexp, pattern);

      return regexp;
    }
    else if (couple3.is(pattern)) {
      return couple3._regexp;
    }
    else if (couple4 == null) {
      Regexp []regexp = RegexpModule.createRegexpArray(env, pattern);
      
      _couple4 = new RegexpCouple(regexp, pattern);

      return regexp;
    }
    else if (couple4.is(pattern)) {
      return couple4._regexp;
    }
    else if (couple5 == null) {
      Regexp []regexp = RegexpModule.createRegexpArray(env, pattern);
      
      _couple5 = new RegexpCouple(regexp, pattern);

      return regexp;
    }
    else if (couple5.is(pattern)) {
      return couple5._regexp;
    }
    else if (couple6 == null) {
      Regexp []regexp = RegexpModule.createRegexpArray(env, pattern);
      
      _couple6 = new RegexpCouple(regexp, pattern);

      return regexp;
    }
    else if (couple6.is(pattern)) {
      return couple6._regexp;
    }
    else {
      Regexp []regexp = RegexpModule.createRegexpArray(env, pattern);
      return regexp;
    }
  }
  
  static class RegexpCouple
  {
    Regexp []_regexp;
    Value _rawRegexp;
    
    RegexpCouple(Regexp []regexp, Value rawRegexp)
    {
      _regexp = regexp;
      _rawRegexp = rawRegexp;
    }
    
    boolean is(Value pattern)
    {
      return pattern == _rawRegexp
             || (pattern.hashCode() == _rawRegexp.hashCode()
                 && pattern.equals(_rawRegexp));
    }
  }
}
