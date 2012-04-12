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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.filter;

import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReadOnly;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;

import java.util.HashMap;
import java.util.logging.Logger;

public class FilterModule extends AbstractQuercusModule
{
  private static final Logger log
    = Logger.getLogger(FilterModule.class.getName());

  private static final L10N L = new L10N(FilterModule.class);

  public static final int FILTER_VALIDATE_EMAIL = 256 + 16 + 2; // 274
  public static final int FILTER_VALIDATE_IP = 256 + 16 + 2 + 1; // 275
  public static final int FILTER_DEFAULT = 512 + 4; // 516

  public static final int FILTER_FLAG_IPV4 = 1 << 20;
  public static final int FILTER_FLAG_IPV6 = FILTER_FLAG_IPV4 * 2;
  public static final int FILTER_FLAG_NO_RES_RANGE = FILTER_FLAG_IPV4 * 4;
  public static final int FILTER_FLAG_NO_PRIV_RANGE = FILTER_FLAG_IPV4 * 8;


  public static HashMap<Integer,Filter> _filterMap
   =  new HashMap<Integer,Filter>();

  public String []getLoadedExtensions()
  {
    return new String[] { "filter" };
  }

  public static Value filter_var(Env env,
                                 @ReadOnly Value value,
                                 @Optional("FILTER_DEFAULT") int filterId,
                                 @Optional Value flagV)
  {
    if (value.isArray()) {
      return BooleanValue.FALSE;
    }

    Filter filter = _filterMap.get(filterId);

    if (filter == null) {
      env.warning(L.l("Unknown filter or filter not implemented: {0}", filterId));

      return BooleanValue.FALSE;
    }

    return filter.filter(env, value, flagV);
  }

  static {
    _filterMap.put(FILTER_DEFAULT, new DefaultFilter());
    _filterMap.put(FILTER_VALIDATE_EMAIL, new EmailValidateFilter());
    _filterMap.put(FILTER_VALIDATE_IP, new IpValidateFilter());
  }
}
