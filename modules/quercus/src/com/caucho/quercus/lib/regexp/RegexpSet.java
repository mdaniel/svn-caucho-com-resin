/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib.regexp;

import com.caucho.util.*;

class RegexpSet {
  static final int BITSET_CHARS = 0x80;
  static final int BITSET_WIDTH = 16;

  static RegexpSet SPACE = null;
  static RegexpSet WORD = null;
  static RegexpSet DIGIT = null;
  static RegexpSet DOT = null;
  
  int _bitset[];
  IntSet _range;

  static {
    SPACE = new RegexpSet();
    SPACE.setRange(' ', ' ');
    SPACE.setRange(0x9, 0xa); //tab to newline
    SPACE.setRange(0xc, 0xd); //form feed to carriage return

    DOT = new RegexpSet();
    DOT.setRange('\n', '\n');

    DIGIT = new RegexpSet();
    DIGIT.setRange('0', '9');

    WORD = new RegexpSet();
    WORD.setRange('a', 'z');
    WORD.setRange('A', 'Z');
    WORD.setRange('0', '9');
    WORD.setRange('_', '_');
  }
  /**
   * Ors two character sets.
   */
  void mergeOr(RegexpSet b)
  {
    for (int i = 0; i < BITSET_WIDTH; i++)
      _bitset[i] |= b._bitset[i];

    _range.union(b._range);
  }

  /**
   * Ors a set with the inverse of another.
   */
  void mergeOrInv(RegexpSet b)
  {
    for (int i = 0; i < BITSET_WIDTH; i++)
      _bitset[i] |= ~b._bitset[i];

    _range.unionNegate(b._range, 0, 0xffff);
  }

  /**
   * Set a range of characters in a character set.
   */
  void setRange(int low, int high)
  {
    if (low > high || low < 0 || high > 0xffff)
	throw new RuntimeException("Range out of range");

    if (low < BITSET_CHARS) {
      for (int i = low; i < Math.min(high + 1, BITSET_CHARS); i++)
	_bitset[i >> 3] |= (1 << (i & 0x7));

      if (high < BITSET_CHARS)
	return;

      low = BITSET_CHARS;
    }

    _range.union(low, high);
  }

  /**
   * Calculate the intersection of two sets.
   *
   * @return true if disjoint
   */
  boolean mergeOverlap(RegexpSet next)
  {
    boolean isDisjoint = true;

    for (int i = 0; i < BITSET_WIDTH; i++) {
      if ((_bitset[i] &= next._bitset[i]) != 0)
	isDisjoint = false;
    }

    if (_range.intersection(next._range))
      isDisjoint = false;

    return isDisjoint;
  }

  /**
   * Calculate the difference of two sets.
   *
   * @return true if disjoint
   */
  void difference(RegexpSet next)
  {
    for (int i = 0; i < BITSET_WIDTH; i++) {
      _bitset[i] &= ~next._bitset[i];
    }

    _range.difference(next._range);
  }

  /*
   *   Returns true if the character is in the set.
   */
  boolean match(int ch)
  {
    if (ch < 0)
      return false;
    else if (ch < BITSET_CHARS)
      return (_bitset[ch >> 3] & (1 << (ch & 7))) != 0;
    else {
      return _range.contains(ch);
    }
  }

  /**
   * Create a new RegexpSet
   */
  RegexpSet()
  {
    _bitset = new int[BITSET_WIDTH];
    _range = new IntSet();
  }

  /**
   * Create a new RegexpSet
   */
  RegexpSet(RegexpSet old)
  {
    _bitset = new int[BITSET_WIDTH];

    for (int i = 0; i < BITSET_WIDTH; i++)
      _bitset[i] = old._bitset[i];

    _range = (IntSet) old._range.clone();
  }
}
