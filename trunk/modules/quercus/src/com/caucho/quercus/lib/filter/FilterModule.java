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

import com.caucho.quercus.UnimplementedException;
import com.caucho.quercus.annotation.Optional;
import com.caucho.quercus.annotation.ReadOnly;
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;
import com.caucho.util.L10N;

import java.util.HashMap;

public class FilterModule extends AbstractQuercusModule
{
  private static final L10N L = new L10N(FilterModule.class);

  public static final int INPUT_POST = 0;
  public static final int INPUT_GET = 1;
  public static final int INPUT_COOKIE = 2;
  public static final int INPUT_ENV = 4;
  public static final int INPUT_SERVER = 5;

  public static final int FILTER_VALIDATE_INT = 0x101;     // 257
  public static final int FILTER_VALIDATE_BOOLEAN = 0x102; // 258
  public static final int FILTER_VALIDATE_FLOAT = 0x103;   // 259
  public static final int FILTER_VALIDATE_EMAIL = 0x112;   // 274
  public static final int FILTER_VALIDATE_IP = 0x113;      // 275
  public static final int FILTER_SANITIZE_STRING = 0x201;  // 513
  public static final int FILTER_DEFAULT = 0x204;          // 516
  public static final int FILTER_SANITIZE_EMAIL = 0x205;   // 517

  public static final int FILTER_FLAG_ALLOW_OCTAL = 0x01;      // 1
  public static final int FILTER_FLAG_STRIP_LOW = 0x04;        // 4
  public static final int FILTER_FLAG_STRIP_HIGH = 0x08;       // 8
  public static final int FILTER_FLAG_ENCODE_LOW = 0x10;       // 16
  public static final int FILTER_FLAG_ENCODE_HIGH = 0x20;      // 32
  public static final int FILTER_FLAG_ENCODE_AMP = 0x40;       // 64
  public static final int FILTER_FLAG_NO_ENCODE_QUOTES = 0x80; // 128

  public static final int FILTER_FLAG_ALLOW_THOUSAND = 0x2000; // 8192

  public static final int FILTER_NULL_ON_FAILURE = 0x8000000;  // 134217728

  public static final int FILTER_FLAG_IPV4 = 0x100000;         // 1048576
  public static final int FILTER_FLAG_IPV6 = 0x200000;
  public static final int FILTER_FLAG_NO_RES_RANGE = 0x400000;
  public static final int FILTER_FLAG_NO_PRIV_RANGE = 0x800000;

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
      return BooleanValue.FALSE;
    }

    return filter.filter(env, value, flagV);
  }

  public static Value filter_input(Env env,
                                   int type,
                                   StringValue name,
                                   @Optional("FILTER_DEFAULT") int filterId,
                                   @Optional Value flagV)
  {
    ArrayValue array;

    switch (type) {
      case INPUT_POST:
        array = env.getInputPostArray();
        break;
      case INPUT_GET:
        array = env.getInputGetArray();
        break;
      case INPUT_COOKIE:
        array = env.getInputCookieArray();
        break;
      case INPUT_ENV:
        array = env.getInputEnvArray();
        break;
      default:
        return env.warning(L.l("filter input type is unknown: {0}", type));
    }

    Filter filter = _filterMap.get(filterId);

    if (filter == null) {
      throw new UnimplementedException(L.l("filter not implemented: {0}", filterId));
    }

    Value value = array.get(name);

    return filter.filter(env, value, flagV);
  }

  static {
    _filterMap.put(FILTER_DEFAULT, new DefaultFilter());
    _filterMap.put(FILTER_VALIDATE_INT, new IntValidateFilter());
    _filterMap.put(FILTER_VALIDATE_BOOLEAN, new BooleanValidateFilter());
    _filterMap.put(FILTER_VALIDATE_FLOAT, new FloatValidateFilter());
    _filterMap.put(FILTER_VALIDATE_EMAIL, new EmailValidateFilter());
    _filterMap.put(FILTER_VALIDATE_IP, new IpValidateFilter());
    _filterMap.put(FILTER_SANITIZE_STRING, new StringSanitizeFilter());
    _filterMap.put(FILTER_SANITIZE_EMAIL, new EmailSanitizeFilter());
  }
}
