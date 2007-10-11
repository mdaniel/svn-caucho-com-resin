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

/*
 * XXX: anchored expressions should have flags for quick matching.
 */

package com.caucho.quercus.lib.regexp;

import java.util.*;
import com.caucho.util.*;


class RegOptim {
  /*
   * The following are really optimization things.
   */
  static void ignoreCase(RegexpNode nodeArg)
  {
    RegexpNode.Compat node = (RegexpNode.Compat) nodeArg;
    
    for (; node != null; node = (RegexpNode.Compat) node._rest) {
      switch (node._code) {
      case RegexpNode.RC_SET:
	node._code = RegexpNode.RC_SET_I;
	break;
      case RegexpNode.RC_NSET:
	node._code = RegexpNode.RC_NSET_I;
	break;
      case RegexpNode.RC_STRING:
	node._code = RegexpNode.RC_STRING_I;
	break;
      case RegexpNode.RC_GROUP_REF:
	node._code = RegexpNode.RC_GROUP_REF_I;
	break;
      }

      ignoreCase(node._branch);
    }
  }

  /**
   *
   * Returns the minimum length of a matching string.
   */
  static int minLength(RegexpNode nodeArg)
  {
    RegexpNode.Compat node = (RegexpNode.Compat) nodeArg;
    
    if (node == null)
      return 0;

    switch (node._code) {
    case RegexpNode.RC_SET:
    case RegexpNode.RC_NSET:
    case RegexpNode.RC_SET_I:
    case RegexpNode.RC_NSET_I:
      return 1 + minLength(node._rest);

    case RegexpNode.RC_STRING:
    case RegexpNode.RC_STRING_I:
      return node._string.length() + minLength(node._rest);

    case RegexpNode.RC_OR:
    case RegexpNode.RC_OR_UNIQUE:
      return Math.min(minLength(node._branch), minLength(node._rest));

    case RegexpNode.RC_LOOP:
    case RegexpNode.RC_LOOP_SHORT:
    case RegexpNode.RC_LOOP_UNIQUE:
    case RegexpNode.RC_LOOP_SHORT_UNIQUE:
      return (node._min * minLength(node._branch) + minLength(node._rest));
    default:
      return minLength(node._rest);
    }
  }

  static CharBuffer prefix(RegexpNode nodeArg)
  {
    RegexpNode.Compat node = (RegexpNode.Compat) nodeArg;
    
    if (node == null)
      return null;

    switch (node._code) {
    case RegexpNode.RC_BEG_GROUP:
    case RegexpNode.RC_END_GROUP:
    case RegexpNode.RC_WORD:
    case RegexpNode.RC_NWORD:
    case RegexpNode.RC_BLINE:
    case RegexpNode.RC_ELINE:
    case RegexpNode.RC_BSTRING:
    case RegexpNode.RC_ESTRING:
    case RegexpNode.RC_GSTRING:
      return prefix(node._rest);

    case RegexpNode.RC_POS_LOOKAHEAD:
      return prefix(node._branch);

    case RegexpNode.RC_STRING:
    case RegexpNode.RC_STRING_I:
      return node._string;

    case RegexpNode.RC_LOOP:
    case RegexpNode.RC_LOOP_SHORT:
    case RegexpNode.RC_LOOP_UNIQUE:
    case RegexpNode.RC_LOOP_SHORT_UNIQUE:
      if (node._min > 0)
	return prefix(node._branch);
      else
	return null;

    default:
      return null;
    }
  }

  /**
   * Returns a fixed string that must be matched somewhere in the node.
   *
   * Returns the _last_ string so or-branches will choose the same string.
   * (Remember the tails of the or jump directly to the continuation.)
   */
  static private CharBuffer findMust(RegexpNode nodeArg)
  {
    RegexpNode.Compat node = (RegexpNode.Compat) nodeArg;
    
    CharBuffer string1;
    CharBuffer string2;

    if (node == null)
      return null;

    switch (node._code) {
    case RegexpNode.RC_STRING:
      string1 = findMust(node._rest);
      return string1 != null ? string1 : node._string;

    case RegexpNode.RC_OR:
    case RegexpNode.RC_OR_UNIQUE:
      string1 = findMust(node._branch);
      string2 = findMust(node._rest);

      if (string1 != null && string2 != null && string1.equals(string2))
	return string1;
      else
	return null;

    case RegexpNode.RC_LOOP:
    case RegexpNode.RC_LOOP_UNIQUE:
    case RegexpNode.RC_LOOP_SHORT:
      string1 = findMust(node._rest);
      if (string1 != null)
	return string1;
      else if (node._min > 0)
	return findMust(node._branch);
      else
	return null;

    case RegexpNode.RC_POS_LOOKAHEAD:
      string1 = findMust(node._rest);
      if (string1 != null)
	return string1;
      else
	return findMust(node._branch);

    default:
      return findMust(node._rest);
    }
  }

  /**
   * Link the loop branches and eliminate null derivations.
   */
  static private RegexpNode linkLoops(RegexpNode nodeArg,
				      RegexpNode loopArg,
				      boolean canDeriveNull)
  {
    RegexpNode.Compat node = (RegexpNode.Compat) nodeArg;
    RegexpNode.Compat loop = (RegexpNode.Compat) loopArg;
    
    if (node == null && loop != null && canDeriveNull) {
      if (loop._min > 0)
	loop._min = 1;

      return loop;
    }
    else if (node == null)
      return loop;

    switch (node._code) {
    case RegexpNode.RC_END:
    case RegexpNode.RC_NULL:
      return linkLoops(node._rest, loop, canDeriveNull);

    case RegexpNode.RC_OR:
    case RegexpNode.RC_OR_UNIQUE:
      if (node._mark)
	return node;

      node._mark = true;
      node._branch = linkLoops(node._branch, loop, canDeriveNull);
      node._rest = linkLoops(node._rest, loop, canDeriveNull);

      return node;

    case RegexpNode.RC_LOOP:
    case RegexpNode.RC_LOOP_SHORT:
    case RegexpNode.RC_LOOP_UNIQUE:

      if (node._mark) {
	if (canDeriveNull && node._min > 0)
	  node._min = 1;

	return node;
      }
      node._mark = true;
      node._branch = linkLoops(node._branch, node, true);
      node._rest = linkLoops(node._rest, loop, canDeriveNull);
      RegexpNode init = RegexpNode.create(RegexpNode.RC_LOOP_INIT);
      init._rest = node;
      return init;

    case RegexpNode.RC_GROUP_REF:
    case RegexpNode.RC_GROUP_REF_I:
    case RegexpNode.RC_STRING:
    case RegexpNode.RC_SET:
    case RegexpNode.RC_NSET:
    case RegexpNode.RC_STRING_I:
    case RegexpNode.RC_SET_I:
    case RegexpNode.RC_NSET_I:
      node._rest = linkLoops(node._rest, loop, false);
      return node;

    case RegexpNode.RC_POS_LOOKBEHIND:
    case RegexpNode.RC_NEG_LOOKBEHIND:
      node._branch = linkLoops(node._branch, loop, canDeriveNull);
      return node;
      
    default:
      node._rest = linkLoops(node._rest, loop, canDeriveNull);
      return node;
    }
  }

  static RegexpNode linkLoops(RegexpNode node)
  {    
    return linkLoops(node, null, true);
  }

  /**
   * Any branch ("|" or loop) splits into four cases based on the next char:
   *   1) Mismatch, e.g. `d' in [ab]aab|[bc]aaa  or  [ab]*[bc]aaa
   *   2) Left branch, e.g. `a' in [ab]aab|[bc]aaa  or  [ab]*[bc]aaa
   *   3) Right branch, e.g. `c' in [ab]aab|[bc]aaa  or  [ab]*[bc]aaa
   *   4) Either branch, e.g. `b' in [ab]aab|[bc]aaa  or  [ab]*[bc]aaa
   *
   * Only the 4th case requires a non-deterministic decision (implemented in
   * noder by recursive matching with backtracking.)
   *
   * RC_LOOP_UNIQUE -- The firstsets are disjoint.
   * RC_LOOP        -- The loop firstset is for branch
   *
   * RC_OR_UNIQUE   -- The firstsets are disjoint.
   *                   The or firstset is for the left branch.
   * RC_OR
   *
   * Null loops are like ()* or (a*)+.  A null loop automatically has
   * a minimum match of 0.  The null derivation for the loop body is
   * then removed with re_eliminate_null.
   */

  static void eliminateBacktrack(RegexpNode nodeArg, RegexpNode rest)
  {
    RegexpNode.Compat node = (RegexpNode.Compat) nodeArg;
    
    RegexpSet left;
    RegexpSet right;

    if (node == null)
      return;

    switch (node._code) {
    case RegexpNode.RC_LOOP_SHORT:
      eliminateBacktrack(node._branch, node._rest);
      eliminateBacktrack(node._rest, rest);

      if (firstset(node._branch, null)) {
	if (node._min > 0)
	  node._min = 1;
      }
      return;

    case RegexpNode.RC_LOOP:
      //XXX: disable loop optimizations for now
      // php/4ebc
      if (true)
        break;
      
      eliminateBacktrack(node._branch, node._rest);
      eliminateBacktrack(node._rest, rest);

      left = new RegexpSet();
      if (firstset(node._branch, left)) {
	if (node._min > 0)
	  node._min = 0;
      }

      right = new RegexpSet();
      boolean emptyLast = false;
      if (firstset(node._rest, right)) {
        emptyLast = true;
	firstset(rest, right);
      }

      // If the overlap is disjoint and the right can't derive null, then
      // it's unique
      if (emptyLast) {
        // If the right can derive null, then we can't take a shortcut
      }
      else if (right.mergeOverlap(left)) {
	node._code = RegexpNode.RC_LOOP_UNIQUE;
	node._set = left;
      }
      else {
	left.difference(right);
	node._set = left;
      }

      return;

    case RegexpNode.RC_OR: 
    case RegexpNode.RC_OR_UNIQUE:
      eliminateBacktrack(node._branch, rest);
      eliminateBacktrack(node._rest, rest);

      boolean emptyFirst = false;
      left = new RegexpSet();
      if (firstset(node._branch, left))
	emptyFirst = firstset(rest, left);

      right = new RegexpSet();
      if (firstset(node._rest, right))
	firstset(rest, right);
      
      if (! emptyFirst && right.mergeOverlap(left)) {
	node._code = RegexpNode.RC_OR_UNIQUE;
	node._set = left;
      }
      return;

    case RegexpNode.RC_POS_LOOKAHEAD:
    case RegexpNode.RC_NEG_LOOKAHEAD:
      eliminateBacktrack(node._branch, rest);
      eliminateBacktrack(node._rest, rest);
      return;

    default:
      eliminateBacktrack(node._rest, rest);
      return;
    }
  }
  
  /**
   * Calculate a node's firstset.
   *
   * The firstset is the set of characters which begin a successful match.
   *
   * @return true if the node can derive null
   */
  static private boolean firstset(RegexpNode nodeArg, RegexpSet set)
  {
    int i;

    RegexpNode.Compat node = (RegexpNode.Compat) nodeArg;

    if (node == null)
      return true;

    switch (node._code) {
    //XXX: optimize group references
    case RegexpNode.RC_GROUP_REF_I:
    case RegexpNode.RC_GROUP_REF:
      return false;
    case RegexpNode.RC_STRING:
      int ch = node._string.charAt(0);
      if (set != null)
	set.setRange(ch, ch);
      return false;

    case RegexpNode.RC_SET:
      if (set != null)
	set.mergeOr(node._set);
      return false;

    case RegexpNode.RC_NSET:
      if (set != null) {
	set.mergeOrInv(node._set);
      }
      return false;

    case RegexpNode.RC_LOOP:
    case RegexpNode.RC_LOOP_SHORT:
    case RegexpNode.RC_LOOP_SHORT_UNIQUE:
    case RegexpNode.RC_LOOP_UNIQUE:
      if (firstset(node._branch, set)) {
	if (node._min > 0)
	  node._min = 1;

	return firstset(node._rest, set);
      }
      else if (node._min == 0) {
	// XXX: ibm-jdk needs this split
	boolean isFirst = firstset(node._rest, set);
	return isFirst;
      }
      else
	return false;

    case RegexpNode.RC_OR:
    case RegexpNode.RC_OR_UNIQUE:
      if (firstset(node._branch, set)) {
	firstset(node._rest, set);
	return true;
      }
      else
	return firstset(node._rest, set);

    case RegexpNode.RC_POS_LOOKAHEAD:
      RegexpSet lookahead = new RegexpSet();

      boolean result = firstset(node._rest, set);
      if (set != null && firstset(node._branch, lookahead)) {
	set.mergeOr(lookahead);
      }
      else {
	set.mergeOverlap(lookahead);
      }

      return result;

    default:
      return firstset(node._rest, set);
    }
  }

  static RegexpNode appendLexemeValue(RegexpNode node, int lexeme)
  {
    RegexpNode.Compat compat = (RegexpNode.Compat) node;
    
    if (compat == null || compat == RegexpNode.END
	|| compat._code == RegexpNode.RC_LEXEME) {
      node = RegexpNode.create(RegexpNode.RC_LEXEME, lexeme);
      node._rest = null;
      return node;
    }

    compat._rest = appendLexemeValue(compat._rest, lexeme);
    if (compat._code == RegexpNode.RC_OR)
      compat._branch = appendLexemeValue(compat._branch, lexeme);

    return compat;
  }

  static RegexpNode appendLexeme(RegexpNode parent,
				 RegexpNode child,
				 int lexeme)
  {
    child = appendLexemeValue(child, lexeme);

    if (parent == null)
      return child;
    
    parent = RegexpNode.create(RegexpNode.RC_OR, parent);
    parent._rest = child;

    return parent;
  }
}
