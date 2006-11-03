/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.net.*;

/**
 * Represents an internet network mask.
 */
public class InetNetwork {
  private long _address;
  private long _mask;
  private int _maskIndex;

  /**
   * Create a internet mask.
   *
   * @param inetAddress the main address
   * @param maskIndex the number of bits to match.
   */
  public InetNetwork(InetAddress inetAddress, int maskIndex)
  {
    byte []bytes = inetAddress.getAddress();

    long address = 0;
    for (int i = 0; i < bytes.length; i++)
      address = 256 * address + (bytes[i] & 0xff);

    _address = address;
    _maskIndex = maskIndex;
    _mask = -1L << (32 - maskIndex);
  }
  
  /**
   * Creates an inet network with a mask.
   */
  public InetNetwork(long address, int maskIndex)
  {
    _address = address;
    _maskIndex = maskIndex;
    _mask = -1L << (32 - maskIndex);
  }

  public static InetNetwork create(String network)
  {
    if (network == null)
      return null;
    
    int i = 0;
    int len = network.length();
    
    long address = 0;
    int digits = 0;

    int ch = 0;
    while (i < len) {
      if (network.charAt(i) == '/')
        break;

      int digit = 0;
      for (; i < len && (ch = network.charAt(i)) >= '0' && ch <= '9'; i++)
        digit = 10 * digit + ch - '0';

      address = 256 * address + digit;

      digits++;

      if (i < len && ch == '.')
        i++;
    }

    while (digits++ < 4) {
      address *= 256;
    }
      

    int mask;
    if (i < len && network.charAt(i) == '/') {
      mask = 0;
      for (i++; i < len && (ch = network.charAt(i)) >= '0' && ch <= '9'; i++)
        mask = 10 * mask + ch - '0';
    }
    else
      mask = 32;

    return new InetNetwork(address, mask);
  }

  /**
   * Returns true if the address is in this network.
   */
  public boolean isMatch(InetAddress inetAddress)
  {
    byte []bytes = inetAddress.getAddress();

    long address = 0;
    for (int i = 0; i < bytes.length; i++)
      address = 256 * address + (bytes[i] & 0xff);

    return (_address & _mask) == (address & _mask);
  }

  /**
   * Returns true if the address is in this network.
   */
  public boolean isMatch(byte []bytes)
  {
    long address = 0;
    for (int i = 0; i < bytes.length; i++)
      address = 256 * address + (bytes[i] & 0xff);

    return (_address & _mask) == (address & _mask);
  }

  /**
   * Returns true if the address is in this network.
   */
  public boolean isMatch(long address)
  {
    return (_address & _mask) == (address & _mask);
  }

  /**
   * Return a readable string.
   */
  public String toString()
  {
    CharBuffer cb = CharBuffer.allocate();

    for (int i = 0; i < 4; i++) {
      if (i != 0)
        cb.append('.');

      cb.append((_address >> (3 - i) * 8) & 0xff);
    }

    cb.append('/');
    cb.append(_maskIndex);

    return cb.close();
  }
}
