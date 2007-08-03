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

/*
 * XXX: anchored expressions should have flags for quick matching.
 */

package com.caucho.regexp;

import java.util.*;
import com.caucho.util.*;


class RegOptim {
  /*
   * The following are really optimization things.
   */
  static void ignoreCase(Node node)
  {
    for (; node != null; node = node.rest) {
      switch (node.code) {
      case Node.RC_SET:
	node.code = Node.RC_SET_I;
	break;
      case Node.RC_NSET:
	node.code = Node.RC_NSET_I;
	break;
      case Node.RC_STRING:
	node.code = Node.RC_STRING_I;
	break;
      case Node.RC_GROUP_REF:
	node.code = Node.RC_GROUP_REF_I;
	break;
      }

      ignoreCase(node.branch);
    }
  }

  /**
   *
   * Returns the minimum length of a matching string.
   */
  static int minLength(Node node)
  {
    if (node == null)
      return 0;

    switch (node.code) {
    case Node.RC_SET:
    case Node.RC_NSET:
    case Node.RC_SET_I:
    case Node.RC_NSET_I:
      return 1 + minLength(node.rest);

    case Node.RC_STRING:
    case Node.RC_STRING_I:
      return node.string.length() + minLength(node.rest);

    case Node.RC_OR:
    case Node.RC_OR_UNIQUE:
      return Math.min(minLength(node.branch), minLength(node.rest));

    case Node.RC_LOOP:
    case Node.RC_LOOP_SHORT:
    case Node.RC_LOOP_UNIQUE:
    case Node.RC_LOOP_SHORT_UNIQUE:
      return (node.min * minLength(node.branch) + minLength(node.rest));

    default:
      return minLength(node.rest);
    }
  }

  static CharBuffer prefix(Node node)
  {
    if (node == null)
      return null;

    switch (node.code) {
    case Node.RC_BEG_GROUP:
    case Node.RC_END_GROUP:
    case Node.RC_WORD:
    case Node.RC_NWORD:
    case Node.RC_BLINE:
    case Node.RC_ELINE:
    case Node.RC_BSTRING:
    case Node.RC_ESTRING:
    case Node.RC_GSTRING:
      return prefix(node.rest);

    case Node.RC_POS_PEEK:
      return prefix(node.branch);

    case Node.RC_STRING:
    case Node.RC_STRING_I:
      return node.string;

    case Node.RC_LOOP:
    case Node.RC_LOOP_SHORT:
    case Node.RC_LOOP_UNIQUE:
    case Node.RC_LOOP_SHORT_UNIQUE:
      if (node.min > 0)
	return prefix(node.branch);
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
  static private CharBuffer findMust(Node node)
  {
    CharBuffer string1;
    CharBuffer string2;

    if (node == null)
      return null;

    switch (node.code) {
    case Node.RC_STRING:
      string1 = findMust(node.rest);
      return string1 != null ? string1 : node.string;

    case Node.RC_OR:
    case Node.RC_OR_UNIQUE:
      string1 = findMust(node.branch);
      string2 = findMust(node.rest);

      if (string1 != null && string2 != null && string1.equals(string2))
	return string1;
      else
	return null;

    case Node.RC_LOOP:
    case Node.RC_LOOP_UNIQUE:
    case Node.RC_LOOP_SHORT:
      string1 = findMust(node.rest);
      if (string1 != null)
	return string1;
      else if (node.min > 0)
	return findMust(node.branch);
      else
	return null;

    case Node.RC_POS_PEEK:
      string1 = findMust(node.rest);
      if (string1 != null)
	return string1;
      else
	return findMust(node.branch);

    default:
      return findMust(node.rest);
    }
  }

  /**
   * Link the loop branches and eliminate null derivations.
   */
  static private Node linkLoops(Node node, Node loop, boolean canDeriveNull)
  {
    if (node == null && loop != null && canDeriveNull) {
      if (loop.min > 0)
	loop.min = 1;

      return loop;
    }
    else if (node == null)
      return loop;

    switch (node.code) {
    case Node.RC_END:
    case Node.RC_NULL:
      return linkLoops(node.rest, loop, canDeriveNull);

    case Node.RC_OR:
    case Node.RC_OR_UNIQUE:
      if (node.mark)
	return node;

      node.mark = true;
      node.branch = linkLoops(node.branch, loop, canDeriveNull);
      node.rest = linkLoops(node.rest, loop, canDeriveNull);

      return node;

    case Node.RC_LOOP:
    case Node.RC_LOOP_SHORT:
    case Node.RC_LOOP_UNIQUE:
      if (node.mark) {
	if (canDeriveNull && node.min > 0)
	  node.min = 1;

	return node;
      }
      node.mark = true;
      node.branch = linkLoops(node.branch, node, true);
      node.rest = linkLoops(node.rest, loop, canDeriveNull);
      Node init = new Node(Node.RC_LOOP_INIT);
      init.rest = node;
      return init;

    case Node.RC_STRING:
    case Node.RC_SET:
    case Node.RC_NSET:
    case Node.RC_STRING_I:
    case Node.RC_SET_I:
    case Node.RC_NSET_I:
      node.rest = linkLoops(node.rest, loop, false);
      return node;

    default:
      node.rest = linkLoops(node.rest, loop, canDeriveNull);
      return node;
    }
  }

  static Node linkLoops(Node node)
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

  static void eliminateBacktrack(Node node, Node rest)
  {
    RegexpSet left;
    RegexpSet right;

    if (node == null)
      return;

    switch (node.code) {
    case Node.RC_LOOP_SHORT:
      eliminateBacktrack(node.branch, node.rest);
      eliminateBacktrack(node.rest, rest);

      if (firstset(node.branch, null)) {
	if (node.min > 0)
	  node.min = 1;
      }
      return;

    case Node.RC_LOOP:
      eliminateBacktrack(node.branch, node.rest);
      eliminateBacktrack(node.rest, rest);

      left = new RegexpSet();
      if (firstset(node.branch, left)) {
	if (node.min > 0)
	  node.min = 0;
      }

      right = new RegexpSet();
      boolean emptyLast = false;
      if (firstset(node.rest, right)) {
        emptyLast = true;
	firstset(rest, right);
      }

      // If the overlap is disjoint and the right can't derive null, then
      // it's unique
      if (emptyLast) {
        // If the right can derive null, then we can't take a shortcut
      }
      else if (right.mergeOverlap(left)) {
	node.code = Node.RC_LOOP_UNIQUE;
	node.set = left;
      }
      else {
	left.difference(right);
	node.set = left;
      }

      return;

    case Node.RC_OR: 
    case Node.RC_OR_UNIQUE:
      eliminateBacktrack(node.branch, rest);
      eliminateBacktrack(node.rest, rest);

      boolean emptyFirst = false;
      left = new RegexpSet();
      if (firstset(node.branch, left))
	emptyFirst = firstset(rest, left);

      right = new RegexpSet();
      if (firstset(node.rest, right))
	firstset(rest, right);
      
      if (! emptyFirst && right.mergeOverlap(left)) {
	node.code = Node.RC_OR_UNIQUE;
	node.set = left;
      }
      return;

    case Node.RC_POS_PEEK:
    case Node.RC_NEG_PEEK:
      eliminateBacktrack(node.branch, rest);
      eliminateBacktrack(node.rest, rest);
      return;

    default:
      eliminateBacktrack(node.rest, rest);
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
  static private boolean firstset(Node node, RegexpSet set)
  {
    int i;

    if (node == null)
      return true;

    switch (node.code) {
    case Node.RC_STRING:
      int ch = node.string.charAt(0);
      if (set != null)
	set.setRange(ch, ch);
      return false;

    case Node.RC_SET:
      if (set != null)
	set.mergeOr(node.set);
      return false;

    case Node.RC_NSET:
      if (set != null) {
	set.mergeOrInv(node.set);
      }
      return false;

    case Node.RC_LOOP:
    case Node.RC_LOOP_SHORT:
    case Node.RC_LOOP_SHORT_UNIQUE:
    case Node.RC_LOOP_UNIQUE:
      if (firstset(node.branch, set)) {
	if (node.min > 0)
	  node.min = 1;

	return firstset(node.rest, set);
      }
      else if (node.min == 0) {
	// XXX: ibm-jdk needs this split
	boolean isFirst = firstset(node.rest, set);
	return isFirst;
      }
      else
	return false;

    case Node.RC_OR:
    case Node.RC_OR_UNIQUE:
      if (firstset(node.branch, set)) {
	firstset(node.rest, set);
	return true;
      }
      else
	return firstset(node.rest, set);

    case Node.RC_POS_PEEK:
      RegexpSet lookahead = new RegexpSet();

      boolean result = firstset(node.rest, set);
      if (set != null && firstset(node.branch, lookahead)) {
	set.mergeOr(lookahead);
      }
      else {
	set.mergeOverlap(lookahead);
      }

      return result;

    default:
      return firstset(node.rest, set);
    }
  }

  static Node appendLexemeValue(Node node, int lexeme)
  {
    if (node == null || node.code == Node.RC_END || 
	node.code == Node.RC_LEXEME) {
      node = new Node(Node.RC_LEXEME, lexeme);
      node.rest = null;
      return node;
    }

    node.rest = appendLexemeValue(node.rest, lexeme);
    if (node.code == Node.RC_OR)
      node.branch = appendLexemeValue(node.branch, lexeme);

    return node;
  }

  static Node appendLexeme(Node parent, Node child, int lexeme)
  {
    child = appendLexemeValue(child, lexeme);

    if (parent == null)
      return child;
    
    parent = new Node(Node.RC_OR, parent);
    parent.rest = child;

    return parent;
  }
}
