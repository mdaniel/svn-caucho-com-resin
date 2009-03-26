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

public class EregWrapper
{
  private Ereg _regexp;
  private Ereg _regexp2;
  
  public EregWrapper()  
  {
  }
  
  public Ereg get(Env env, StringValue str)
  {
    Ereg regexp = _regexp;
    Ereg regexp2 = _regexp2;
    
    if (regexp == null) {
      regexp = RegexpModule.createEreg(env, str);
      _regexp = regexp;
      
      return regexp;
    }
    else if (str == regexp._rawRegexp
             || str.equals(regexp._rawRegexp)) {
      return regexp;
    }
    else if (regexp2 == null) {
      regexp2 = RegexpModule.createEreg(env, str);
      _regexp2 = regexp2;
      
      return regexp2;
    }
    else if (str == regexp2._rawRegexp
        || str.equals(regexp2._rawRegexp)) {
      return regexp2;
    }
    else {
      return RegexpModule.createEreg(env, str);
    }
  }
}
