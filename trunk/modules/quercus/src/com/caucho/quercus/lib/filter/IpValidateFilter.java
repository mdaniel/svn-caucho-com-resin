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

import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.BooleanValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpValidateFilter extends AbstractFilter implements ValidateFilter
{
  @Override
  protected Value filterImpl(Env env, Value value,
                             int flags, ArrayValue options)
  {
    if (! value.isString()) {
      return BooleanValue.FALSE;
    }

    boolean isIpV4 = (flags & FilterModule.FILTER_FLAG_IPV4) > 0;
    boolean isIpV6 = (flags & FilterModule.FILTER_FLAG_IPV6) > 0;

    if (! isIpV4 && ! isIpV6) {
      isIpV4 = true;
      isIpV6 = true;
    }

    boolean isRejectReserved = (flags & FilterModule.FILTER_FLAG_NO_RES_RANGE) > 0;
    boolean isRejectPrivate = (flags & FilterModule.FILTER_FLAG_NO_PRIV_RANGE) > 0;

    StringValue str = value.toStringValue(env);

    if (isIpV4) {
      int pos = str.indexOf('.');

      if (pos >= 0) {
        if (isValidIp4(str, isRejectReserved, isRejectPrivate)) {
          return value;
        }
        else {
          return BooleanValue.FALSE;
        }
      }
    }

    if (isIpV6) {
      int pos = str.indexOf(':');

      if (pos >= 0) {
        if (isValidIp6(str, isRejectReserved, isRejectPrivate)) {
          return value;
        }
        else {
          return BooleanValue.FALSE;
        }
      }
    }

    return BooleanValue.FALSE;
  }

  private boolean isValidIp4(StringValue str,
                             boolean isRejectReserved,
                             boolean isRejectPrivate)
  {
    InputStream is = str.toInputStream();

    int part0 = parseIp4Part(is, false);
    if (part0 < 0 || part0 > 255) {
      return false;
    }
    else if (isRejectReserved
             && (part0 == 0
                 //|| part0 == 10
                 //|| part0 == 127
                 || 224 <= part0 && part0 <= 255)) {
      return false;
    }
    else if (isRejectPrivate
             && (part0 == 10)) {
      return false;
    }

    int part1 = parseIp4Part(is, false);
    if (part1 < 0 || part1 > 255) {
      return false;
    }
    else if (isRejectReserved) {
      if (part0 == 169 && part1 == 254) {
          //|| part0 == 172 && 16 <= part1 && part1 <= 31
          //|| part0 == 192 && part1 == 168
          //|| part0 == 198 && 18 <= part1 && part1 <= 19) {
        return false;
      }
    }
    else if (isRejectPrivate
             && (part0 == 172 && 16 <= part1 && part1 <= 31
                 || part0 == 192 && part1 == 168)) {
      return false;
    }

    int part2 = parseIp4Part(is, false);
    if (part2 < 0 || part2 > 255) {
      return false;
    }
    else if (isRejectReserved
             && (part0 == 192 && part1 == 0 && part2 == 2)) {
          //|| part0 == 192 && part1 == 88 && part2 == 99
          //|| part0 == 198 && part1 == 51 && part2 == 100
          //|| part0 == 203 && part1 == 0 && part2 == 113) {
      return false;
    }

    int part3 = parseIp4Part(is, true);
    if (part3 < 0 || part3 > 255) {
      return false;
    }

    return true;
  }

  private int parseIp4Part(InputStream is, boolean isLast)
  {
    try {
      int value;

      int ch;

      if ('0' <= (ch = is.read()) && ch <= '9') {
        value = ch - '0';
      }
      else {
        return -1;
      }

      if ('0' <= (ch = is.read()) && ch <= '9') {
        value = value * 10 + ch - '0';
      }
      else if (! isLast && ch == '.') {
        return value;
      }
      else if (isLast && ch < 0) {
        return value;
      }
      else {
        return -1;
      }

      if ('0' <= (ch = is.read()) && ch <= '9') {
        value = value * 10 + ch - '0';
      }
      else if (! isLast && ch == '.') {
        return value;
      }
      else if (isLast && ch < 0) {
        return value;
      }
      else {
        return -1;
      }

      if ((ch = is.read()) == '.' || (isLast && ch < 0)) {
        return value;
      }
      else {
        return -1;
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isValidIp6(StringValue str,
                                    boolean isRejectReserved,
                                    boolean isRejectPrivate)
  {
    // contributed by SSN
    // XXX: reserved, private

    try {
      InetAddress ip = InetAddress.getByName(str.toString());

      return ip instanceof Inet6Address;
    }
    catch (UnknownHostException e) {
      return false;
    }
  }
}
