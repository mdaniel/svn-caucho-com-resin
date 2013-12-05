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
 * @author Scott Ferguson
 */

package com.caucho.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * System-wide random number generator.
 */
public class RandomUtil {
  private static FreeList<Random> _freeRandomList
    = new FreeList<Random>(64);
  
  private static Random _testRandom;

  /**
   * Returns the next random long.
   */
  public static long getRandomLong()
  {
    Random random = getRandom();

    long value = random.nextLong();

    freeRandom(random);

    return value;
  }

  /**
   * Returns the next random int.
   */
  public static int nextInt(int n)
  {
    Random random = getRandom();

    int value = random.nextInt(n);

    freeRandom(random);

    return value;
  }

  /**
   * Returns the next random double between 0 and 1
   */
  public static double nextDouble()
  {
    Random random = getRandom();

    double value = random.nextDouble();

    freeRandom(random);

    return value;
  }

  /**
   * Returns the random generator.
   */
  private static Random getRandom()
  {
    Random random = _freeRandomList.allocate();

    if (random != null) {
      return random;
    }
    else if (_testRandom != null) {
      return _testRandom;
    }
    else {
      return new SecureRandom();
    }
  }
  
  private static void freeRandom(Random random)
  {
    _freeRandomList.free(random);
  }

  /**
   * Sets the specific seed.  Only for testing.
   */
  public static void setTestSeed(long seed)
  {
    _testRandom = new Random(seed);
    
    while (_freeRandomList.allocate() != null) {
    }
  }
}
