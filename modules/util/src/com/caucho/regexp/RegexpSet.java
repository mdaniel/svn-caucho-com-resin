/*
 * Copyright (c) 1998-2002 Caucho Technology -- all rights reserved
 *
 * Caucho Technology permits modification and use of this file in
 * source and binary form ("the Software") subject to the Caucho
 * Developer Source License 1.1 ("the License") which accompanies
 * this file.  The License is also available at
 *   http://www.caucho.com/download/cdsl1-1.xtp
 *
 * In addition to the terms of the License, the following conditions
 * must be met:
 *
 * 1. Each copy or derived work of the Software must preserve the copyright
 *    notice and this notice unmodified.
 *
 * 2. Each copy of the Software in source or binary form must include 
 *    an unmodified copy of the License in a plain ASCII text file named
 *    LICENSE.
 *
 * 3. Caucho reserves all rights to its names, trademarks and logos.
 *    In particular, the names "Resin" and "Caucho" are trademarks of
 *    Caucho and may not be used to endorse products derived from
 *    this software.  "Resin" and "Caucho" may not appear in the names
 *    of products derived from this software.
 *
 * This Software is provided "AS IS," without a warranty of any kind. 
 * ALL EXPRESS OR IMPLIED REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED.
 *
 * CAUCHO TECHNOLOGY AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE OR ANY THIRD PARTY AS A RESULT OF USING OR
 * DISTRIBUTING SOFTWARE. IN NO EVENT WILL CAUCHO OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE SOFTWARE, EVEN IF HE HAS BEEN ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGES.      
 *
 * @author Scott Ferguson
 */

package com.caucho.regexp;

import java.util.*;
import com.caucho.util.*;

class RegexpSet {
  static final int BITSET_CHARS = 0x80;
  static final int BITSET_WIDTH = 16;

  static RegexpSet SPACE = null;
  static RegexpSet WORD = null;
  static RegexpSet DIGIT = null;
  static RegexpSet DOT = null;

  int bitset[];
  IntSet range;

  static {
    SPACE = new RegexpSet();
    SPACE.setRange(' ', ' ');
    SPACE.setRange(0x9, 0xd);

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
      bitset[i] |= b.bitset[i];

    range.union(b.range);
  }

  /**
   * Ors a set with the inverse of another.
   */
  void mergeOrInv(RegexpSet b)
  {
    for (int i = 0; i < BITSET_WIDTH; i++)
      bitset[i] |= ~b.bitset[i];

    range.unionNegate(b.range, 0, 0xffff);
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
	bitset[i >> 3] |= (1 << (i & 0x7));

      if (high < BITSET_CHARS)
	return;

      low = BITSET_CHARS;
    }

    range.union(low, high);
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
      if ((bitset[i] &= next.bitset[i]) != 0)
	isDisjoint = false;
    }

    if (range.intersection(next.range))
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
      bitset[i] &= ~next.bitset[i];
    }

    range.difference(next.range);
  }

  /*
   *   Returns true if the character is in the set.
   */
  boolean match(int ch)
  {
    if (ch < 0)
      return false;
    else if (ch < BITSET_CHARS)
      return (bitset[ch >> 3] & (1 << (ch & 7))) != 0;
    else {
      return range.contains(ch);
    }
  }

  /**
   * Create a new RegexpSet
   */
  RegexpSet()
  {
    bitset = new int[BITSET_WIDTH];
    range = new IntSet();
  }

  /**
   * Create a new RegexpSet
   */
  RegexpSet(RegexpSet old)
  {
    bitset = new int[BITSET_WIDTH];

    for (int i = 0; i < BITSET_WIDTH; i++)
      bitset[i] = old.bitset[i];

    range = (IntSet) old.range.clone();
  }
}
