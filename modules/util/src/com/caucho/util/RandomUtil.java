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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * System-wide random number generator.
 */
public class RandomUtil {
  static private long _seed = System.currentTimeMillis();
  static private Random _random;
  static private boolean _isTest;

  /**
   * Returns the next random long.
   */
  public synchronized static long getRandomLong()
  {
    return getRandom().nextLong();
  }

  /**
   * Returns the next random int.
   */
  public synchronized static int nextInt(int n)
  {
    return getRandom().nextInt(n);
  }

  /**
   * Adds a random number based on a string.
   */
  public static void addRandom(String random)
  {
    if (random == null)
      return;
    
    for (int i = 0; i < random.length(); i++)
      addRandom(random.charAt(i));
  }

  /**
   * Adds a random number to the server seed.
   */
  public static void addRandom(long seed)
  {
    Random random = getRandom();
    if (random instanceof SecureRandom)
      ((SecureRandom) random).setSeed(seed);
  }

  /**
   * Returns the random generator.
   */
  private static Random getRandom()
  {
    if (_random == null) {
      _random = new SecureRandom();
      _random.setSeed(_seed);
    }

    return _random;
  }

  /**
   * Sets the specific seed.  Only for testing.
   */
  public static void setTestSeed(long seed)
  {
    _seed = seed;
    _isTest = true;
    _random = new Random(seed);
  }
}
