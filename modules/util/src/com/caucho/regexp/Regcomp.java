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

/**
 * Regular expression compilation.
 */
class Regcomp {
  static final int MULTILINE = 0x1;
  static final int SINGLE_LINE = 0x2;
  static final int IGNORE_CASE = 0x4;
  static final int IGNORE_WS = 0x8;
  static final int GLOBAL = 0x10;

  int nGroup;
  int maxGroup;
  int nLoop;
  int flags;

  Regcomp(int flags)
  {
    this.flags = flags;
  }

  Node parse(PeekStream pattern) throws IllegalRegexpException
  {
    nGroup = 0;
    
    Node value = parseRec(pattern);

    if (nGroup > maxGroup)
      maxGroup = nGroup;

    return value;
  }

  /**
   *   Recursively compile a Node.
   *
   * first      -- The first node of this sub-Node
   * prev       -- The previous node of this sub-Node
   * last_begin -- When the last grouping began
   * last_end   -- When the last grouping ended
   *
   * head       ->  node
   *                 v -- rest
   *                ...
   *                 v -- rest
   *                node
   *
   * last       ->  node
   *                 v -- rest
   *                ...
   *                 v -- rest
   *                node
   */
  private Node parseRec(PeekStream pattern) throws IllegalRegexpException
  {
    Node head;
    Node last;
    Node node;

    head = null;
    last = null;

    int ch;
    while ((ch = pattern.read()) >= 0) {
      switch (ch) {
      case '*':
	if (last == null)
	  throw new IllegalRegexpException("`*' must follow content expression");
	if (last.code == last.RC_LOOP)
	  throw new IllegalRegexpException("nested *?+");

	if (pattern.peek() == '?') {
	  pattern.read();
	  node = new Node(Node.RC_LOOP_SHORT, nLoop++, 0, Integer.MAX_VALUE);
	}
	else
	  node = new Node(Node.RC_LOOP, nLoop++, 0, Integer.MAX_VALUE);
	node.branch = last;
	last = node;
	break;

      case '+':
	if (last == null)
	  throw new IllegalRegexpException("`+' must follow content expression");
	if (last.code == last.RC_LOOP)
	  throw new IllegalRegexpException("nested *?+");

	if (pattern.peek() == '?') {
	  pattern.read();
	  node = new Node(Node.RC_LOOP_SHORT, nLoop++, 1, Integer.MAX_VALUE);
	}
	else
	  node = new Node(Node.RC_LOOP, nLoop++, 1, Integer.MAX_VALUE);
	node.branch = last;
	last = node;
	break;

      case '?':
	if (last == null)
	  throw new IllegalRegexpException("`?' must follow content expression");
	if (last.code == last.RC_LOOP)
	  throw new IllegalRegexpException("nested *?+");

	if (pattern.peek() == '?') {
	  pattern.read();
	  node = new Node(Node.RC_LOOP_SHORT, nLoop++, 0, 1);
	}
	else
	  node = new Node(Node.RC_LOOP, nLoop++, 0, 1);
	node.branch = last;
	last = node;
	break;

      case '{':
	if (last == null)
	  throw new IllegalRegexpException("`{' must follow content expression");
	if (last.code == last.RC_LOOP)
	  throw new IllegalRegexpException("nested *?+");

	node = new Node(Node.RC_LOOP);
	node.index = nLoop++;
	node.branch = last;

	parseBrace(pattern, node);
	if ((ch = pattern.read()) != '}')
	  throw new IllegalRegexpException("expected `}' at " +
					   badChar(ch));

	if (pattern.peek() == '?') {
	  pattern.read();
	  node.code = Node.RC_LOOP_SHORT;
	}

	last = node;
	break;

      case '|':
	head = new Node(Node.RC_OR, Node.concat(head, last));
	last = null;
	break;

      case '(':
	head = Node.concat(head, last);

	if (pattern.peek() == '?') {
	  pattern.read();
	  switch ((ch = pattern.read())) {
	  case '#':
	    // (?#...) Comment
	    while ((ch = pattern.read()) >= 0 && ch != ')') {
	    }
	    pattern.ungetc(ch);

	    last = null;
	    break;

	  case ':':
	    // (?:...) No grouping, only for precedence.
	    last = parseRec(pattern);
	    last = Node.replaceTail(last, new Node(Node.RC_NULL));
	    break;

	  case '=':
	    // (?=...) Positive lookahead assertion.
	    last = new Node(Node.RC_POS_PEEK, parseRec(pattern));
	    break;

	  case '!':
	    // (?!...) Negative lookahead assertion.
	    last = new Node(Node.RC_NEG_PEEK, parseRec(pattern));
	    break;

	  case 'm': case 'i': case 's': case 'x': case 'g':
	    do {
	      switch (ch) {
	      case 'm': flags |= MULTILINE; break;
	      case 'i': flags |= IGNORE_CASE; break;
	      case 's': flags |= SINGLE_LINE; break;
	      case 'x': flags |= IGNORE_WS; break;
	      case 'g': flags |= GLOBAL; break;
	      default:
		throw new IllegalRegexpException("expected one of `misxg' at "
						 + badChar(ch));
	      }
	    } while ((ch = pattern.read()) >= 0 && ch != ')');
	    pattern.ungetc(ch);
	    last = null;
	    break;

	  default:
	    throw new IllegalRegexpException("expected `(?' code at "
					     + badChar(ch));
	  }
	}
	else {
	  int groupIndex = ++nGroup;

	  last = new Node(Node.RC_BEG_GROUP, groupIndex);
	  last.rest = parseRec(pattern);

	  node = new Node(Node.RC_END_GROUP, groupIndex);
	  last = Node.replaceTail(last, node);
	}

	if ((ch = pattern.read()) != ')')
	  throw new IllegalRegexpException("expected `)' at " +
					   badChar(ch));

	break;

      case ')':
	pattern.ungetc(ch);

	return Node.concat(head, last);

      case '[':
	head = Node.concat(head, last);

	last = parseSet(pattern);

	if ((ch = pattern.read()) != ']')
	  throw new IllegalRegexpException("expected `]' at " + 
					   badChar(ch));
	break;

      case '.':
	head = Node.concat(head, last);

	if ((flags & SINGLE_LINE) == 0)
	  last = new Node(Node.RC_NSET, RegexpSet.DOT);
	else
	  last = new Node(Node.RC_NSET, new RegexpSet());
	break;

      case '\\':
	Node next = parseSlash(pattern, last);
	if (next != last)
	  head = Node.concat(head, last);

	last = next;
	break;
            
      case '^':
	head = Node.concat(head, last);
	if ((flags & MULTILINE) != 0)
	  last = new Node(Node.RC_BLINE);
	else
	  last = new Node(Node.RC_BSTRING);
	break;

      case '$':
	head = Node.concat(head, last);
	if ((flags & MULTILINE) != 0)
	  last = new Node(Node.RC_ELINE);
	else
	  last = new Node(Node.RC_ESTRING);
	break;
            
      default:
	next = parseString(ch, pattern, last);
	if (next != last) {
	  head = Node.concat(head, last);
	  last = next;
	}
	break;
      }
    }

    return Node.concat(head, last);
  }

  /**
   *   Parse the repetition construct.
   *
   *   {n}    -- exactly n
   *   {n,}   -- at least n
   *   {n,m}  -- from n to m
   *   {,m}   -- at most m
   */
  private void
  parseBrace(PeekStream pattern, Node loop)
  {
    loop.min = 0;
    int ch;

    while ((ch = pattern.read()) >= '0' && ch <= '9') {
      loop.min = 10 * loop.min + ch - '0';
    }

    if (ch != ',') {
      loop.max = loop.min;
      pattern.ungetc(ch);
      return;
    }

    loop.max = Integer.MAX_VALUE;
    while ((ch = pattern.read()) >= '0' && ch <= '9') {
      loop.max = loop.max == Integer.MAX_VALUE ? 0 : loop.max;
      loop.max = 10 * loop.max + ch - '0';
    }

    pattern.ungetc(ch);
  }

  private String hex(int value)
  {
    CharBuffer cb = new CharBuffer();

    for (int b = 3; b >= 0; b--) {
      int v = (value >> (4 * b)) & 0xf;
      if (v < 10)
	cb.append((char) (v + '0'));
      else
	cb.append((char) (v - 10 + 'a'));
    }

    return cb.toString();
  }

  private String badChar(int ch)
  {
    if (ch >= 0x20 && ch <= 0x7f)
      return "`" + (char) ch + "'";
    else if ((ch & 0xffff) == 0xffff)
      return "end of expression";
    else
      return "`" + (char) ch + "' (\\u" + hex(ch) + ")";
  }

  /**
   *   Collect the characters in a set, e.g. [a-z@@^!"]
   *
   * Variables:
   *
   *   last     -- Contains last read character.
   *   lastdash -- Contains character before dash or -1 if not after dash.
   */
  private Node parseSet(PeekStream pattern) 
    throws IllegalRegexpException
  {
    Node node;
    if (pattern.peek() == '^') {
      pattern.read();
      node = new Node(Node.RC_NSET);
    }
    else {
      node = new Node(Node.RC_SET);
    }

    RegexpSet set = new RegexpSet();
    node.set = set;

    int last = -1;
    int lastdash = -1;
    int ch;
    while ((ch = pattern.read()) != ']' && ch >= 0) {
      boolean isChar = true;
      boolean isDash = ch == '-';

      if (ch == '\\') {
	isChar = false;

	switch ((ch = pattern.read())) {
	case 's':
	  set.mergeOr(RegexpSet.SPACE);
	  break;

	case 'S':
	  set.mergeOrInv(RegexpSet.SPACE);
	  break;

	case 'd':
	  set.mergeOr(RegexpSet.DIGIT);
	  break;

	case 'D':
	  set.mergeOrInv(RegexpSet.DIGIT);
	  break;

	case 'w':
	  set.mergeOr(RegexpSet.WORD);
	  break;

	case 'W':
	  set.mergeOrInv(RegexpSet.WORD);
	  break;

	case 'b':
	  ch = '\b';
	  isChar = true;
	  break;

	default:
	  isChar = true;
	}
      }

      if (isDash && last != -1 && lastdash == -1) {
	lastdash = last;
      }
      // c1-c2
      else if (isChar && lastdash != -1) {
	if (lastdash > ch)
	  throw new IllegalRegexpException("expected increasing range at " +
					   badChar(ch));

	set.setRange(lastdash, ch);

	last = -1;
	lastdash = -1;
      }
      else if (lastdash != -1) {
	set.setRange(lastdash, lastdash);
	set.setRange('-', '-');

	last = -1;
	lastdash = -1;
      }
      else if (last != -1) {
	set.setRange(last, last);

	if (isChar)
	  last = ch;
      }
      else if (isChar)
	last = ch;
    }

    // Dash at end of set: [a-z1-]
    if (lastdash != -1) {
      set.setRange(lastdash, lastdash);
      set.setRange('-', '-');
    }
    else if (last != -1)
      set.setRange(last, last);

    if (ch == ']')
      pattern.ungetc(ch);

    return node;
  }

  /**
   * parseString
   */
  private Node parseString(int ch, PeekStream pattern, Node last)
    throws IllegalRegexpException
  {
    if ((flags & IGNORE_WS) != 0 && RegexpSet.SPACE.match(ch))
      return last;

    int next = pattern.read();
    if (last == null || last.code != Node.RC_STRING ||
	next == '*' || next == '?' || next == '{' || next == '+') {
      last = new Node(new CharBuffer());
    }
    last.string.append((char) ch);

    if (next != -1)
      pattern.ungetc(next);

    return last;
  }

  /**
   * Returns a node for sequences starting with a backslash.
   */
  private Node parseSlash(PeekStream pattern, Node last)
    throws IllegalRegexpException
  {
    Node node;
    int i;

    int ch;
    switch (ch = pattern.read()) {
    case 's':
      return new Node(Node.RC_SET, new RegexpSet(RegexpSet.SPACE));

    case 'S':
      return new Node(Node.RC_NSET, new RegexpSet(RegexpSet.SPACE));

    case 'd':
      return new Node(Node.RC_SET, new RegexpSet(RegexpSet.DIGIT));

    case 'D':
      return new Node(Node.RC_NSET, new RegexpSet(RegexpSet.DIGIT));

    case 'w':
      return new Node(Node.RC_SET, new RegexpSet(RegexpSet.WORD));

    case 'W':
      return new Node(Node.RC_NSET, new RegexpSet(RegexpSet.WORD));

    case 'b':
      return new Node(Node.RC_WORD);

    case 'B':
      return new Node(Node.RC_NWORD);

    case 'A':
      return new Node(Node.RC_BSTRING);

    case 'Z':
      return new Node(Node.RC_ESTRING);

    case 'G':
      return new Node(Node.RC_GSTRING);

    case '1': case '2': case '3': case '4': 
    case '5': case '6': case '7': case '8': case '9':
      int value = ch - '0';
      int oct = ch - '0';
      boolean badOct = ch >= '8';
      while ((ch = pattern.read()) >= '0' && ch <= '9') {
	value = 10 * value + ch - '0';
	oct = 8 * oct + ch - '0';
	if (ch >= '8')
	  badOct = true;
      }
      if (ch != -1)
	pattern.ungetc(ch);
      if (value <= nGroup)
	return new Node(Node.RC_GROUP_REF, value);
      else if (badOct)
	throw new IllegalRegexpException("expected octal digit at " +
					 badChar(ch));
      else
	return parseString(oct, pattern, last);
    
    default:
      return parseString(ch, pattern, last);
    }
  }
}
