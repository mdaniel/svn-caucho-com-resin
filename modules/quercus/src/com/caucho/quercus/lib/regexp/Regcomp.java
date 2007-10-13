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

import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.util.*;

/**
 * Regular expression compilation.
 */
class Regcomp {
  private static final L10N L = new L10N(RegexpNode.class);
  
  static final int MULTILINE = 0x1;
  static final int SINGLE_LINE = 0x2;
  static final int IGNORE_CASE = 0x4;
  static final int IGNORE_WS = 0x8;
  static final int GLOBAL = 0x10;

  static final int ANCHORED = 0x20;
  static final int END_ONLY = 0x40;
  static final int UNGREEDY = 0x80;
  static final int STRICT = 0x100;
  
  static final HashMap<String,Integer> _characterClassMap
    = new HashMap<String,Integer>();
  
  int _nGroup;
  int _nLoop;
  int _maxGroup;
  int _flags;

  HashMap<Integer,StringValue> _groupNameMap
    = new HashMap<Integer,StringValue>();

  HashMap<StringValue,Integer> _groupNameReverseMap
    = new HashMap<StringValue,Integer>();

  RegexpNode _groupTail;
  
  boolean _isLookbehind;
  boolean _isOr;
  
  Regcomp(int flags)
  {
    _flags = flags;
  }

  boolean isGreedy()
  {
    return (_flags & UNGREEDY) != UNGREEDY;
  }

  int nextLoopIndex()
  {
    return _nLoop++;
  }

  RegexpNode parse(PeekStream pattern) throws IllegalRegexpException
  {
    _nGroup = 1;
    
    RegexpNode value = parseRec(pattern, null);

    int ch;
    while ((ch = pattern.read()) == '|') {
      value = new RegexpNode.Or(value, parseRec(pattern, null));
    }
    
    value = value.getHead();

    if (_maxGroup < _nGroup)
      _maxGroup = _nGroup;

    /*
    if ((_flags & ANCHORED) != 0) {
      RegexpNode node = RegexpNode.create(RegexpNode.RC_BSTRING);
      node._rest = value;
      
      value = node;
    }
    */
    
    return value;
  }

  private RegexpNode parseRec(PeekStream pattern)
    throws IllegalRegexpException
  {
    return parseRec(pattern, false);
  }
  
  /**
   * @param isConditional true is parsing for (?(cond)yes|no)
   * 
   *   Recursively compile a RegexpNode.
   *
   * first      -- The first node of this sub-RegexpNode
   * prev       -- The previous node of this sub-RegexpNode
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
  private RegexpNode parseRec(PeekStream pattern, boolean isConditional)
    throws IllegalRegexpException
  {
    RegexpNode head;
    RegexpNode.Compat last;
    RegexpNode node;
    RegexpNode.Compat compat;

    head = null;
    last = null;

    int ch;
    int lastNonWhitespaceChar = -1;
    
    while ((ch = pattern.read()) >= 0) {
      if ((_flags & IGNORE_WS) != 0 &&
          (RegexpSet.SPACE.match(ch) || ch == '#')) {
      }
      else
        lastNonWhitespaceChar = ch;
      
      switch (ch) {
      case '*':
	if (last == null)
	  throw new IllegalRegexpException("`*' must follow content expression");
	
	/*
	if (last._code == last.RC_LOOP)
	  throw new IllegalRegexpException("nested *?+");
    */

	if (pattern.peek() == '?') {
	  pattern.read();
	  
	  if ((_flags & UNGREEDY) != 0)
	    node = RegexpNode.create(RegexpNode.RC_LOOP, _nLoop++, 0, Integer.MAX_VALUE);
	  else
	    node = RegexpNode.create(RegexpNode.RC_LOOP_SHORT, _nLoop++, 0, Integer.MAX_VALUE);
	}
	else if (pattern.peek() == '+') {
	  pattern.read();
	  node = RegexpNode.create(RegexpNode.RC_LOOP_LONG, _nLoop++, 0, Integer.MAX_VALUE);
	}
	else if ((_flags & UNGREEDY) != 0)
      node = RegexpNode.create(RegexpNode.RC_LOOP_SHORT, _nLoop++, 0, Integer.MAX_VALUE);
	else
	  node = RegexpNode.create(RegexpNode.RC_LOOP, _nLoop++, 0, Integer.MAX_VALUE);
	
	((RegexpNode.Compat) node)._branch = last;
	last = (RegexpNode.Compat) node;
	break;

      case '+':
	if (last == null)
	  throw new IllegalRegexpException("`+' must follow content expression");
	
	// php/4e59
	/*
	if (last._code == last.RC_LOOP)
	  throw new IllegalRegexpException("nested *?+");
    */

	if (pattern.peek() == '?') {
	  pattern.read();
	  
	  if (((_flags & UNGREEDY) != 0))
	      node = RegexpNode.create(RegexpNode.RC_LOOP, _nLoop++, 1, Integer.MAX_VALUE);
	  else
	    node = RegexpNode.create(RegexpNode.RC_LOOP_SHORT, _nLoop++, 1, Integer.MAX_VALUE);
	}
	else if (pattern.peek() == '+') {
	  pattern.read();
	  node = RegexpNode.create(RegexpNode.RC_LOOP_LONG, _nLoop++, 1, Integer.MAX_VALUE);
	}
	else if ((_flags & UNGREEDY) != 0)
      node = RegexpNode.create(RegexpNode.RC_LOOP_SHORT, _nLoop++, 1, Integer.MAX_VALUE);
	else
	  node = RegexpNode.create(RegexpNode.RC_LOOP, _nLoop++, 1, Integer.MAX_VALUE);
	((RegexpNode.Compat) node)._branch = last;
	last = (RegexpNode.Compat) node;

	break;

      case '?':
	if (last == null)
	  throw new IllegalRegexpException("`?' must follow content expression");
	/*
	if (last._code == last.RC_LOOP)
	  throw new IllegalRegexpException("nested *?+");
	*/

	if (pattern.peek() == '?') {
	  pattern.read();
	  
	  if ((_flags & UNGREEDY) != 0)
	    node = RegexpNode.create(RegexpNode.RC_LOOP, _nLoop++, 0, 1);
	  else
	    node = RegexpNode.create(RegexpNode.RC_LOOP_SHORT, _nLoop++, 0, 1);
	}
	else if (pattern.peek() == '+') {
	  pattern.read();
	  node = RegexpNode.create(RegexpNode.RC_LOOP_LONG, _nLoop++, 0, 1);
	}
	else if ((_flags & UNGREEDY) != 0)
	  node = RegexpNode.create(RegexpNode.RC_LOOP_SHORT, _nLoop++, 0, 1);
	else
	  node = RegexpNode.create(RegexpNode.RC_LOOP, _nLoop++, 0, 1);

	((RegexpNode.Compat) node)._branch = last;
	last = (RegexpNode.Compat) node;
	break;

      case '{':
        ch = pattern.peek();
        if (! ('0' <= ch && ch <= '9')) {
          RegexpNode next = parseString('{', pattern);
          if (next != last) {
            head = RegexpNode.concat(head, last);
            last = (RegexpNode.Compat) next;
          }
          
          break;
        }
        
	if (last == null)
	  throw new IllegalRegexpException("`{' must follow content expression");
	/*
	if (last._code == last.RC_LOOP)
	  throw new IllegalRegexpException("nested *?+");
	*/

	node = RegexpNode.create(RegexpNode.RC_LOOP);
	compat = (RegexpNode.Compat) node;
	compat._index = _nLoop++;
	compat._branch = last;
	compat._length = compat._branch._length * compat._min;

	parseBrace(pattern, node);
	if ((ch = pattern.read()) != '}')
	  throw new IllegalRegexpException("expected `}' at " +
					   badChar(ch));
	  
	if (pattern.peek() == '?') {
	  pattern.read();
	  compat._code = RegexpNode.RC_LOOP_SHORT;
	}
	else if (pattern.peek() == '+') {
	  pattern.read();
	  compat._code = RegexpNode.RC_LOOP_LONG;
	}

	if (_isLookbehind) {
	  if (compat._min != compat._max)
	    throw new IllegalRegexpException("lookbehind strings must be fixed length: "
	                                     + compat._min + " != " + compat._max);
	}
	
	int length = compat._branch._length * compat._min;
	compat._length = length;
	
	last = (RegexpNode.Compat) node;
	break;

      case '|':
        if (isConditional) {
          pattern.ungetc(ch);
          
          return last;
        }
        
        /*
        //php/152o
        if ((_flags & IGNORE_WS) != 0) {
          while ((ch = pattern.peek()) == '|' || RegexpSet.SPACE.match(ch)) {
            pattern.read();
          }
        }
        else {
          while ((ch = pattern.peek()) == '|') {
            pattern.read();
          }
        }
        */

        if (last == null && head != null
            && (((RegexpNode.Compat) head)._code == RegexpNode.RC_OR
		|| ((RegexpNode.Compat) head)._code == RegexpNode.RC_LOOKBEHIND_OR))
          break;
        
        node = RegexpNode.concat(head, last);
	head = RegexpNode.create(RegexpNode.RC_OR, node);
	
	if (_isLookbehind) {
	  ((RegexpNode.Compat) head)._code = RegexpNode.RC_LOOKBEHIND_OR;
	  //head = RegexpNode.create(RegexpNode.RC_LOOKBEHIND_OR, head);
	}
	

	head._length = node._length;
	
	last = null;
	break;

      case '(':
	head = RegexpNode.concat(head, last);

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
	    last = (RegexpNode.Compat) parseRec(pattern);
	    
	    last = (RegexpNode.Compat) RegexpNode.replaceTail(last, RegexpNode.create(RegexpNode.RC_NULL));
	    
	    /*
	    if (last._rest == null || last._rest._code == RegexpNode.RC_NULL) {
	    }
	    else {
	      RegexpNode newLast = RegexpNode.create(RegexpNode.RC_NULL);
	      newLast._rest = last;
	      
	      last = newLast;
	    }
	    */
	    
	    /*
	    // removes RC_END
	    RegexpNode.removeTail(last);
	    
	    if (last._rest != null) {
	      RegexpNode newLast = RegexpNode.removeTail(last);
	      head = RegexpNode.concat(head, last);
	        
	      last = newLast;
	    }
	    */
	    
	    break;
	    
	  case '>':
	    // (?>...) Atomic groups (once-only subpatterns). 
	    last = (RegexpNode.Compat) parseRec(pattern);
	    break;

	  case '=':
	    // (?=...) Positive lookahead assertion.
	    last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_POS_LOOKAHEAD, parseRec(pattern));
	    break;

	  case '!':
	    // (?!...) Negative lookahead assertion.
	    last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_NEG_LOOKAHEAD, parseRec(pattern));
	    break;
	    
	  case '<':
	    ch = pattern.read();
	    
	    // (?<=...) Positive lookbehind assertion.
        if (ch == '=')
          last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_POS_LOOKBEHIND);
        // (?<!...) Negative lookbehind assertion.
	    else if (ch == '!')
          last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_NEG_LOOKBEHIND);
        else
          throw new IllegalRegexpException("expected `}' at " +
                                           badChar(ch));
        
        _isLookbehind = true;
        node = parseRec(pattern);
        _isLookbehind = false;
        
        last._branch = node;
        
        if (node != null)
          last._length = node._length;
	    break;
	    
	  case 'P':
	    // (?P<name>group), (?P=name) named groups

	    StringValue name = pattern.createStringBuilder();

	    if ((ch = pattern.read()) == '<') {
	      // this is a named group definition

	      while ((ch = pattern.read()) != '>' && ch >= 0) {
	        name.append((char)ch);
	      }
	      
	      int groupIndex = ++_nGroup;

	      last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_BEG_GROUP, groupIndex);
	      last._rest = parseRec(pattern);
	      last._length = last._rest._length;

	      node = RegexpNode.create(RegexpNode.RC_END_GROUP, groupIndex);
	      last = (RegexpNode.Compat) RegexpNode.replaceTail(last, node);
	      
	       _groupNameMap.put(Integer.valueOf(groupIndex), name);
	       _groupNameReverseMap.put(name, Integer.valueOf(groupIndex));
	    }
	    else if (ch == '=') {
	      // this is a named group reference

	      while ((ch = pattern.peek()) != ')' && ch >= 0) {
	        pattern.read();
	        name.append((char)ch);
	      }
	      
	      Integer groupIndex = _groupNameReverseMap.get(name);
	      
	      if (groupIndex == null) {
	          throw new IllegalRegexpException("undeclared group reference '" +
                                               name + "'");
	      }
	      
	      last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_GROUP_REF, groupIndex.intValue());
	    }
	    else
          throw new IllegalRegexpException("expected '<' or '=' at " +
                                           badChar(ch));

	    break;

	  case '(':
	    // (?(cond)yes|no) Conditional subpattern.
	    
	    /*
	    int index = 0;
	    while ('0' <= (ch = pattern.read()) && ch <= '9') {
	      index = index * 10 + ch - '0';
	    }
	    */
	    
	    RegexpNode condition = null;;

	    ch = pattern.peek();
	    if ('0' <= ch && ch <= '9')
	      condition = parseBackReference(pattern.read(), pattern);
	    else
	      condition = null;
	    ch = pattern.read();
	    

	    if (ch != ')')
	      throw new IllegalRegexpException("expected `)' at " +
                                           badChar(ch));

	    RegexpNode yesPattern = parseRec(pattern, true);
	    RegexpNode noPattern = null;
	    
	    if (pattern.peek() == '|') {
	      pattern.read();
	      
	      noPattern = parseRec(pattern, true);
	    }

	    last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_COND, ((RegexpNode.Compat) condition)._index);
	    last._condition = condition;
	    last._branch = yesPattern;
	    last._nBranch = noPattern;
	    
	    break;

	  case 'm': case 'i': case 's': case 'x': case 'g':
	    do {
	      switch (ch) {
	      case 'm': _flags |= MULTILINE; break;
	      case 'i': _flags |= IGNORE_CASE; break;
	      case 's': _flags |= SINGLE_LINE; break;
	      case 'x': _flags |= IGNORE_WS; break;
	      case 'g': _flags |= GLOBAL; break;
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
	  int groupIndex = ++_nGroup;

	  last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_BEG_GROUP, groupIndex);
	  last._rest = parseRec(pattern);
	  
	  if (last._rest != null)
	    last._length = last._rest._length;

	  node = RegexpNode.create(RegexpNode.RC_END_GROUP, groupIndex);
	  last = (RegexpNode.Compat) RegexpNode.replaceTail(last, node);
	}

	if ((ch = pattern.read()) != ')')
	  throw new IllegalRegexpException("expected `)' at " +
					   badChar(ch));

	break;

      case ')':
	pattern.ungetc(ch);

	return RegexpNode.concat(head, last);

      case '[':
	head = RegexpNode.concat(head, last);
	
	if (pattern.peek() == ':') {
	  throw new IllegalRegexpException("POSIX [::] class outside []");
	}
	else {
	  last = (RegexpNode.Compat) parseSet(pattern);
	}

	if ((ch = pattern.read()) != ']') {
	  throw new IllegalRegexpException("expected `]' at " + 
					   badChar(ch));
	}
	break;

      case '.':
	head = RegexpNode.concat(head, last);

	if ((_flags & SINGLE_LINE) == 0)
	  last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_NSET, RegexpSet.DOT);
	else
	  last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_NSET, new RegexpSet());
	break;

      case '\\':
	RegexpNode next = parseSlash(pattern);
	/*
	if (next != last)
	  head = RegexpNode.concat(head, last);
	*/

	last = (RegexpNode.Compat) next;
	break;
            
      case '^':
	head = RegexpNode.concat(head, last);
	if ((_flags & MULTILINE) != 0)
	  last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_BLINE);
	else
	  last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_BSTRING);
	break;

      case '$':
	head = RegexpNode.concat(head, last);
	if ((_flags & MULTILINE) != 0)
	  last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_ELINE);
	else if ((_flags & END_ONLY) != 0)
	  last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_ESTRING);
	else
	  last = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_ENSTRING);
	break;
            
      default:
	next = parseString(ch, pattern);
	head = RegexpNode.concat(head, last);
	last = (RegexpNode.Compat) next;
	break;
      }
    }

    return RegexpNode.concat(head, last);
  }
  
  /**
   * @param isConditional true is parsing for (?(cond)yes|no)
   * 
   *   Recursively compile a RegexpNode.
   *
   * first      -- The first node of this sub-RegexpNode
   * prev       -- The previous node of this sub-RegexpNode
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
  private RegexpNode parseRec(PeekStream pattern, RegexpNode tail)
    throws IllegalRegexpException
  {
    int ch = pattern.read();
    RegexpNode next;
    RegexpNode groupTail;

    switch (ch) {
    case -1:
      return tail;

    case '?':
      if (tail == null)
	throw error(L.l("'?' requires a preceeding regexp"));

      tail = createLoop(pattern, tail, 0, 1);
      
      return parseRec(pattern, tail.getTail());

    case '*':
      if (tail == null)
	throw error(L.l("'*' requires a preceeding regexp"));

      tail = createLoop(pattern, tail, 0, Integer.MAX_VALUE);
      
      return parseRec(pattern, tail.getTail());

    case '+':
      if (tail == null)
	throw error(L.l("'+' requires a preceeding regexp"));

      tail = createLoop(pattern, tail, 1, Integer.MAX_VALUE);
      
      return parseRec(pattern, tail.getTail());

    case '{':
      if (tail == null)
	throw error(L.l("'{' requires a preceeding regexp"));

      return parseRec(pattern, parseBrace(pattern, tail));

    case '.':
      if ((_flags & SINGLE_LINE) == 0)
	next = RegexpNode.DOT;
      else
	next = RegexpNode.ANY_CHAR;
	
      return concat(tail, parseRec(pattern, next));

    case '|':
      pattern.ungetc(ch);

      if (_groupTail != null)
	return concat(tail, _groupTail);
      else
	return tail;

    case '(':
      {
	switch (pattern.peek()) {
	case '?':
	  pattern.read();

	  switch (pattern.peek()) {
	  case ':':
	    pattern.read();
	    return parseGroup(pattern, tail, 0);
	    
	  case '#':
	    parseCommentGroup(pattern);
	    
	    return parseRec(pattern, tail);
	    
	  case '(':
	    return parseConditional(pattern, tail);
	    
	  case '=':
	  case '!':
	    pattern.read();

	    boolean isPositive = (ch == '=');

	    groupTail = _groupTail;
	    _groupTail = null;

	    next = parseRec(pattern, null);
	    
	    while ((ch = pattern.read()) == '|') {
	      next = next.createOr(parseRec(pattern, null));
	    }

	    if (isPositive)
	      next = new RegexpNode.Lookahead(next);
	    else
	      next = new RegexpNode.NotLookahead(next);

	    if (ch != ')')
	      throw error(L.l("expected ')' at '{0}'",
			      String.valueOf((char) ch)));

	    _groupTail = groupTail;

	    return concat(tail, parseRec(pattern, next));
	    
	  case '<':
	    pattern.read();

	    switch (pattern.read()) {
	    case '=':
	      isPositive = true;
	      break;
	    case '!':
	      isPositive = false;
	      break;
	    default:
	      throw error(L.l("expected '=' or '!'"));
	    }

	    groupTail = _groupTail;
	    _groupTail = null;

	    next = parseRec(pattern, null);

	    if (next == null) {
	    }
	    else if (isPositive)
	      next = new RegexpNode.Lookbehind(next);
	    else
	      next = new RegexpNode.NotLookbehind(next);
	    
	    while ((ch = pattern.read()) == '|') {
	      RegexpNode second = parseRec(pattern, null);

	      if (second == null) {
	      }
	      else if (isPositive)
		second = new RegexpNode.Lookbehind(second);
	      else
		second = new RegexpNode.NotLookbehind(second);

	      if (second != null)
		next = next.createOr(second);
	    }

	    if (ch != ')')
	      throw error(L.l("expected ')' at '{0}'",
			      String.valueOf((char) ch)));

	    _groupTail = groupTail;

	    return concat(tail, parseRec(pattern, next));
	    
	  case 'P':
	    pattern.read();
	    return parseNamedGroup(pattern, tail);
	    
	  default:
	    throw error(L.l("'{0}' is an unknown (? code", String.valueOf((char) pattern.peek())));
	  }
	  
	default:
	  return parseGroup(pattern, tail, _nGroup++);
	}
      }

    case ')':
      pattern.ungetc(ch);

      if (_groupTail != null)
	return concat(tail, _groupTail);
      else
	return tail;

    case '[':
      next = parseSet(pattern);

      return concat(tail, parseRec(pattern, next));
      
    case '\\':
      next = parseSlash(pattern);
      
      return concat(tail, parseRec(pattern, next));
      
    case '^':
      next = RegexpNode.ANCHOR_BEGIN;
      
      return concat(tail, parseRec(pattern, next));
      
    case '$':
      next = RegexpNode.ANCHOR_END;
      
      return concat(tail, parseRec(pattern, next));
      
    default:
      next = parseString(ch, pattern);
      
      return concat(tail, parseRec(pattern, next));
    }
  }

  private void parseCommentGroup(PeekStream pattern)
  {
    int ch;
    
    // (?#...) Comment
    while ((ch = pattern.read()) >= 0 && ch != ')') {
    }
  }
  
  private RegexpNode parseNamedGroup(PeekStream pattern, RegexpNode tail)
    throws IllegalRegexpException
  {
    int ch = pattern.read();

    if (ch == '=') {
      StringBuilder sb = new StringBuilder();

      while ((ch = pattern.read()) != ')' && ch >= 0) {
	sb.append((char) ch);
      }

      if (ch != ')')
	throw error(L.l("expected ')'"));

      String name = sb.toString();
      
      Integer v = _groupNameReverseMap.get(new StringBuilderValue(name));

      if (v != null) {
	RegexpNode next = new RegexpNode.GroupRef(v);
      
	return concat(tail, parseRec(pattern, next));
      }
      else
	throw error(L.l("'{0}' is an unknown regexp group", name));
    }
    else if (ch == '<') {
      StringBuilder sb = new StringBuilder();

      while ((ch = pattern.read()) != '>' && ch >= 0) {
	sb.append((char) ch);
      }

      if (ch != '>')
	throw error(L.l("expected '>'"));

      String name = sb.toString();

      int group = _nGroup++;

      _groupNameMap.put(group, new StringBuilderValue(name));
      _groupNameReverseMap.put(new StringBuilderValue(name), group);

      return parseGroup(pattern, tail, group);
    }
    else
      throw error(L.l("Expected '(?:P=name' or '(?:P<name' for named group"));
  }

  private RegexpNode parseConditional(PeekStream pattern, RegexpNode tail)
    throws IllegalRegexpException
  {
    int ch = pattern.read();

    if (ch != '(')
      throw error(L.l("expected '('"));
    
    RegexpNode.ConditionalHead groupHead = null;;
    RegexpNode groupTail = null;

    if ('1' <= (ch = pattern.peek()) && ch <= '9') {
      int value = 0;

      while ('0' <= (ch = pattern.read()) && ch <= '9') {
	value = 10 * value + ch - '0';
      }

      if (ch != ')')
	throw error(L.l("expected ')'"));

      if (_nGroup <= value)
	throw error(L.l("conditional value less than number of groups"));

      groupHead = new RegexpNode.ConditionalHead(value);
      groupTail = groupHead.getTail();
    }
    else
      throw error(L.l("conditional requires number"));

    RegexpNode oldTail = _groupTail;

    _groupTail = groupTail;
	
    RegexpNode first = parseRec(pattern, null);
    RegexpNode second = null;

    if ((ch = pattern.read()) == '|') {
      second = parseRec(pattern, null);

      ch = pattern.read();
    }

    if (ch != ')')
      throw error(L.l("expected ')' at '{0}'", String.valueOf((char) ch)));

    _groupTail = oldTail;

    groupHead.setFirst(first);
    groupHead.setSecond(second);
	
    return concat(tail, parseRec(pattern, groupHead));
  }

  private RegexpNode parseGroup(PeekStream pattern, RegexpNode tail,
				int group)
    throws IllegalRegexpException
  {
    RegexpNode.GroupHead groupHead = new RegexpNode.GroupHead(group);
    RegexpNode.GroupTail groupTail
      = new RegexpNode.GroupTail(group, groupHead);

    RegexpNode oldTail = _groupTail;

    _groupTail = groupTail;
	
    RegexpNode body = parseRec(pattern, null);

    int ch;
    while ((ch = pattern.read()) == '|') {
      body = body.createOr(parseRec(pattern, null));
    }

    if (ch != ')')
      throw error(L.l("expected ')'"));

    _groupTail = oldTail;

    groupHead.setNode(body);
	
    return concat(tail, parseRec(pattern, groupHead));
  }

  private void expect(char test, int value)
    throws IllegalRegexpException
  {
    if (test != value)
      throw error(L.l("expected '{0}'", test));
  }

  private IllegalRegexpException error(String msg)
  {
    return new IllegalRegexpException(msg);
  }

  /**
   *   Parse the repetition construct.
   *
   *   {n}    -- exactly n
   *   {n,}   -- at least n
   *   {n,m}  -- from n to m
   *   {,m}   -- at most m
   */
  /*
  private void parseBrace(PeekStream pattern, RegexpNode loopNode)
  {
    RegexpNode.Compat loop = (RegexpNode.Compat) loopNode;
    
    loop._min = 0;
    int ch;

    while ((ch = pattern.read()) >= '0' && ch <= '9') {
      loop._min = 10 * loop._min + ch - '0';
    }

    if (ch != ',') {
      loop._max = loop._min;
      pattern.ungetc(ch);
      return;
    }

    loop._max = Integer.MAX_VALUE;
    while ((ch = pattern.read()) >= '0' && ch <= '9') {
      loop._max = loop._max == Integer.MAX_VALUE ? 0 : loop._max;
      loop._max = 10 * loop._max + ch - '0';
    }

    pattern.ungetc(ch);
  }
  */

  /**
   *   Parse the repetition construct.
   *
   *   {n}    -- exactly n
   *   {n,}   -- at least n
   *   {n,m}  -- from n to m
   *   {,m}   -- at most m
   */
  private RegexpNode parseBrace(PeekStream pattern, RegexpNode node)
    throws IllegalRegexpException
  {
    int ch;
    int min = 0;
    int max = Integer.MAX_VALUE;

    while ((ch = pattern.read()) >= '0' && ch <= '9') {
      min = 10 * min + ch - '0';
    }

    if (ch == ',') {
      while ('0' <= (ch = pattern.read()) && ch <= '9') {
	if (max == Integer.MAX_VALUE)
	  max = 0;
	
	max = 10 * max + ch - '0';
      }
    }
    else
      max = min;

    if (ch != '}')
      throw error(L.l("Expected '}'"));

    return createLoop(pattern, node, min, max);
  }

  private RegexpNode createLoop(PeekStream pattern, RegexpNode node,
				int min, int max)
  {
    if (pattern.peek() == '+') {
      pattern.read();
      
      return node.createPossessiveLoop(min, max);
    }
    else if (pattern.peek() == '?') {
      pattern.read();

      if (isGreedy())
	return node.createLoopUngreedy(this, min, max);
      else
	return node.createLoop(this, min, max);
    }
    else {
      if (isGreedy())
	return node.createLoop(this, min, max);
      else
	return node.createLoopUngreedy(this, min, max);
    }
  }

  static RegexpNode concat(RegexpNode prev, RegexpNode next)
  {
    if (prev != null) {
      return prev.concat(next).getHead();
    }
    else
      return next;
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
    if (0x20 <= ch && ch <= 0x7f)
      return "'" + (char) ch + "'";
    else if ((ch & 0xffff) == 0xffff)
      return "end of expression";
    else
      return "'" + (char) ch + "' (\\u" + hex(ch) + ")";
  }

  /**
   *   Collect the characters in a set, e.g. [a-z@@^!"]
   *
   * Variables:
   *
   *   last     -- Contains last read character.
   *   lastdash -- Contains character before dash or -1 if not after dash.
   */
  private RegexpNode parseSet(PeekStream pattern) 
    throws IllegalRegexpException
  {
    RegexpNode.Compat node;
    int first = pattern.peek();
    boolean isNot = false;
    
    if (first == '^') {
      pattern.read();
      isNot = true;
      node = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_NSET);
      node._length = 1;
    }
    else {
      node = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_SET);
      node._length = 1;
    }
    
    RegexpSet set = new RegexpSet();
    node._set = set;

    int last = -1;
    int lastdash = -1;
    int ch;

    int charRead = 0;
    
    while ((ch = pattern.read()) >= 0) {
      charRead++;

      // php/4e3o
      // first literal closing bracket need not be escaped
      if (ch == ']') {
        if (charRead == 1 && first == '^') {
          pattern.ungetc(ch);
          ch = '\\';
        }
        else
          break;
      }
      
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
	case 'n':
	  ch = '\n';
	  isChar = true;
	  break;
	case 't':
	  ch = '\t';
	  isChar = true;
	  break;
	case 'r':
	  ch = '\r';
	  isChar = true;
	  break;
	case 'f':
	  ch = '\f';
	  isChar = true;
	  break;

	case 'x':
	  ch = parseHex(pattern);
	  isChar = true;
	  break;

	case '0': case '1': case '2': case '3':
	case '4': case '5': case '6': case '7':
	  ch = parseOctal(ch, pattern);
	  isChar = true;
	  break;

	default:
	  isChar = true;
	}
      }
      else if (ch == '[') {
	if (pattern.peek() == ':') {
	  isChar = false;
	  pattern.read();
	  set.mergeOr(parseCharacterClass(pattern));
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
    
    if (ch != ']')
      throw error(L.l("Expected ']'"));

    if (isNot)
      return set.createNotNode();
    else
      return set.createNode();
  }

  /**
   * Returns a node for sequences starting with a backslash.
   */
  private RegexpNode parseSlash(PeekStream pattern)
    throws IllegalRegexpException
  {
    int ch;
    switch (ch = pattern.read()) {
    case 's':
      return RegexpNode.SPACE;

    case 'S':
      return RegexpNode.NOT_SPACE;

    case 'd':
      return RegexpNode.DIGIT;

    case 'D':
      return RegexpNode.NOT_DIGIT;

    case 'w':
      return RegexpNode.S_WORD;

    case 'W':
      return RegexpNode.NOT_S_WORD;

    case 'b':
      return RegexpNode.WORD;

    case 'B':
      return RegexpNode.NOT_WORD;

    case 'A':
      return RegexpNode.STRING_BEGIN;

    case 'z':
      return RegexpNode.STRING_END;
      
    case 'Z':
      return RegexpNode.STRING_NEWLINE;

    case 'G':
      return RegexpNode.STRING_FIRST;

    case 'a':
      return parseString('\u0007', pattern);
    
    case 'c':
      ch = pattern.read();
      
      ch = Character.toUpperCase(ch);
      ch ^= 0x40;

      return parseString(ch, pattern);

    case 'e':
      return parseString('\u001B', pattern, true);
    case 'n':
      return parseString('\n', pattern, true);
    case 'r':
      return parseString('\r', pattern, true);
    case 'f':
      return parseString('\f', pattern, true);
    case 't':
      return parseString('\t', pattern, true);

    case 'x':
      int hex = parseHex(pattern);
      return parseString(hex, pattern, true);
    
    case '0':
      int oct = parseOctal(ch, pattern);
      return parseString(oct, pattern, true);

    case '1': case '2': case '3': case '4': 
    case '5': case '6': case '7': case '8': case '9':
      return parseBackReference(ch, pattern);

    case 'p':
      return parseUnicodeProperty(pattern, false);
    case 'P':
      return parseUnicodeProperty(pattern, true);
      
    case 'Q':
      throw new UnsupportedOperationException();
      /*
      while ((ch = pattern.read()) >= 0) {
        if (ch == '\\' && pattern.peek() == 'E') {
          pattern.read();
          break;
        }

        last = parseString(ch, pattern);
      }

      return last;
      */
      
    case '#':
      return parseString('#', pattern, true);

    default:
      if ((_flags & STRICT) != 0)
        throw new IllegalRegexpException("unrecognized escape at " +
                                         badChar(ch));
      return parseString(ch, pattern);
    }
  }
  
  /**
   * Returns a node for sequences starting with a '[:'.
   */
  private RegexpSet parseCharacterClass(PeekStream pattern)
    throws IllegalRegexpException
  {
    StringBuilder sb = new StringBuilder();
    
    int ch;
    while ((ch = pattern.read()) != ':' && ch >= 0) {
      sb.append((char)ch);
    }
    
    if (ch != ':') {
      throw new IllegalRegexpException("expected character class closing colon ':' at " + badChar(ch));
    }  
     
    if ((ch = pattern.read()) != ']') {
      throw new IllegalRegexpException("expected character class closing bracket ']' at " + badChar(ch));
    }

    String name = sb.toString();
    
    RegexpSet set = RegexpSet.CLASS_MAP.get(name);
    
    if (set == null) {
      throw new IllegalRegexpException("unrecognized POSIX character class " +
                                       name);
    }
 
    return set;
  }

  private int parseHex(PeekStream pattern)
    throws IllegalRegexpException
  {
    int ch = pattern.read();
    
    int hex = 0;
    
    StringBuilder sb = new StringBuilder();
    
    if (ch == '{') {
      while ((ch = pattern.read()) != '}') {
        if (ch < 0)
          throw new IllegalRegexpException("no more input; expected '}'");
        
        sb.append((char)ch);
      }
    }
    else {
      if (ch < 0)
        throw new IllegalRegexpException("expected hex digit at " +
                badChar(ch));
      
      sb.append((char)ch);
      ch = pattern.read();
      
      if (ch < 0) {
        throw new IllegalRegexpException("expected hex digit at " +
                                         badChar(ch));
      }

      sb.append((char)ch);
    }
    
    int len = sb.length();
    
    for (int i = 0; i < len; i++) {
      ch = sb.charAt(i);

      if ('0' <= ch && ch <= '9')
        hex = hex * 16 + ch - '0';
      else if ('a' <= ch && ch <= 'f')
        hex = hex * 16 + ch - 'a' + 10;
      else if ('A' <= ch && ch <= 'F')
        hex = hex * 16 + ch - 'A' + 10;
      else
        throw new IllegalRegexpException("expected hex digit at " +
                                         badChar(ch));
    }
    
    return hex;
  }
  
  private RegexpNode parseBackReference(int ch, PeekStream pattern)
    throws IllegalRegexpException
  {
    int value = ch - '0';
    int ch2 = pattern.peek();
    
    if ('0' <= ch2 && ch2 <= '9') {
      pattern.read();
      value = value * 10 + ch2 - '0';
    }

    int ch3 = pattern.peek();
    
    if (value < 10 || value <= _nGroup && ! ('0' <= ch3 && ch3 <= '7')) {
      return new RegexpNode.GroupRef(value);
    }
    else if (! ('0' <= ch2 && ch2 <= '7')
	     && ! ('0' <= ch3 && ch3 <= '7'))
      throw new IllegalRegexpException("back referencing to a non-existent group: " +
                                       value);
    
    if (value > 10)
      pattern.ungetc(ch2);
    
    if (ch == '8' || ch == '9'
	|| '0' <= ch3 && ch3 <= '9' && value * 10 + ch3 - '0' > 0xFF) {
      //out of byte range or not an octal,
      //need to parse backslash as the NULL character
      
      pattern.ungetc(ch);
      return parseString('\u0000', pattern);
    }
    
    int oct = parseOctal(ch, pattern);
    
    return parseString(oct, pattern, true);
  }

  private RegexpNode parseString(int ch,
				 PeekStream pattern)
    throws IllegalRegexpException
  {
    return parseString(ch, pattern, false);
  }
  
  /**
   * parseString
   */
  private RegexpNode parseString(int ch,
				 PeekStream pattern,
				 boolean isEscaped)
    throws IllegalRegexpException
  {
    CharBuffer cb = new CharBuffer();
    
    for (; ch >= 0; ch = pattern.read()) {
      switch (ch) {
      case ' ': case '\t': case '\n': case '\r':
	if ((_flags & IGNORE_WS) == 0 || isEscaped)
	  cb.append((char) ch);
	break;

      case '#':
	if ((_flags & IGNORE_WS) == 0 || isEscaped)
	  cb.append((char) ch);
	else {
	  while ((ch = pattern.read()) != '\n' && ch >= 0) {
	  }
	}
	break;

      case '{': case '}': case '(': case ')': case '[': case ']':
      case '+': case '?': case '*': case '.':
      case '$': case '^': case '|':
	pattern.ungetc(ch);
	return new RegexpNode.StringNode(cb);

      case '\\':
	ch = pattern.read();
	
	switch (ch) {
	case -1:
	  cb.append('\\');
	  return new RegexpNode.StringNode(cb);
	  
	case 's': case 'S': case 'd': case 'D':
	case 'w': case 'W': case 'b': case 'B':
	case 'A': case 'z': case 'Z': case 'G':
	case 'p': case 'P':
	  pattern.ungetc(ch);
	  pattern.ungetc('\\');
	  return new RegexpNode.StringNode(cb);

	case 'a':
	  cb.append('\u0007');
	  break;
	  
	case 'c':
	  ch = pattern.read();
      
	  ch = Character.toUpperCase(ch);
	  ch ^= 0x40;
	  cb.append((char) ch);
	  break;
	case 'e':
	  cb.append('\u001b');
	  break;
	case 'f':
	  cb.append('\f');
	  break;

	case 'x':
	  int hex = parseHex(pattern);
	  cb.append((char) hex);
	  break;
      
	case 'Q':
	  throw new UnsupportedOperationException();
	  /*
	    while ((ch = pattern.read()) >= 0) {
	    if (ch == '\\' && pattern.peek() == 'E') {
	    pattern.read();
	    break;
	    }

	    last = parseString(ch, pattern);
	    }

	    return last;
	  */
    
	case '0':
	  int oct = parseOctal(ch, pattern);
	  cb.append((char) oct);
	  break;

	case '1': case '2': case '3': case '4': 
	case '5': case '6': case '7': case '8': case '9':
	  if (ch - '0' <= _nGroup) {
	    pattern.ungetc(ch);
	    pattern.ungetc('\\');
	    return new RegexpNode.StringNode(cb);
	  }
	  else {
	    oct = parseOctal(ch, pattern);
	    cb.append((char) oct);
	  }
	  break;
	case '#':
	  cb.append('#');
	  break;

	default:
	  if ((_flags & STRICT) != 0)
	    throw error(L.l("unrecognized escape at " + badChar(ch)));

	  cb.append((char) ch);
	  break;
	}
	break;

      default:
	cb.append((char) ch);
      }
    }

    return new RegexpNode.StringNode(cb);
  }
  
  private int parseOctal(int ch, PeekStream pattern)
    throws IllegalRegexpException
  {
    if ('0' > ch || ch > '7')
      throw new IllegalRegexpException("expected octal digit at " +
                                       badChar(ch));
    
    int oct = ch - '0';
    
    int ch2 = pattern.peek();
    
    if ('0' <= ch2 && ch2 <= '7') {
      pattern.read();
      
      oct = oct * 8 + ch2 - '0';
      
      ch = pattern.peek();
      
      if ('0' <= ch && ch <= '7') {
        pattern.read();
        
        oct = oct * 8 + ch - '0';
      }
    }
    
    return oct;
  }
  
  private RegexpNode parseUnicodeProperty(PeekStream pattern,
					  boolean isNegated)
    throws IllegalRegexpException
  {
    int ch = pattern.read();

    boolean isBraced = false;

    if (ch == '{') {
      isBraced = true;
      ch = pattern.read();
      
      if (ch == '^') {
        isNegated = ! isNegated;
        ch = pattern.read();
      }
    }
    
    RegexpNode node;
    
    if (isBraced)
      node = parseBracedUnicodeProperty(ch, pattern, isNegated);
    else
      node = parseUnbracedUnicodeProperty(ch, pattern, isNegated);
    
    return node;
  }
  
  private RegexpNode parseBracedUnicodeProperty(int ch,
						PeekStream pattern,
						boolean isNegated)
    throws IllegalRegexpException
  {
    byte category = 0;
    
    int ch2 = pattern.read();
    
    switch (ch) {
    case 'C':
      switch (ch2) {
      case 'c':
	expect('}', pattern.read());
	       
        return isNegated ? RegexpNode.PROP_NOT_Cc : RegexpNode.PROP_Cc;
      case 'f':
	expect('}', pattern.read());
	       
        return isNegated ? RegexpNode.PROP_NOT_Cf : RegexpNode.PROP_Cf;
      case 'n':
	expect('}', pattern.read());
	       
        return isNegated ? RegexpNode.PROP_NOT_Cn : RegexpNode.PROP_Cn;
      case 'o':
	expect('}', pattern.read());
	       
        return isNegated ? RegexpNode.PROP_NOT_Co : RegexpNode.PROP_Co;
      case 's':
	expect('}', pattern.read());
	       
        return isNegated ? RegexpNode.PROP_NOT_Cs : RegexpNode.PROP_Cs;
      case '}':
        return isNegated ? RegexpNode.PROP_NOT_C : RegexpNode.PROP_C;
	
      default:
        throw error(L.l("invalid Unicode category "
			+ badChar(ch) + "" + badChar(ch2)));
      }

    case 'L':
      switch (ch2) {
      case 'l':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Ll : RegexpNode.PROP_Ll;
      case 'm':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Lm : RegexpNode.PROP_Lm;
      case 'o':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Lo : RegexpNode.PROP_Lo;
      case 't':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Lt : RegexpNode.PROP_Lt;
      case 'u':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Lu : RegexpNode.PROP_Lu;
	
      case '}':
        return isNegated ? RegexpNode.PROP_NOT_L : RegexpNode.PROP_L;
	
      default:
	throw error(L.l("invalid Unicode category "
			+ badChar(ch) + "" + badChar(ch2)));
      }
    case 'M':
      switch (ch2) {
      case 'c':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Mc : RegexpNode.PROP_Mc;
      case 'e':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Me : RegexpNode.PROP_Me;
      case 'n':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Mn : RegexpNode.PROP_Mn;
	
      case '}':
        return isNegated ? RegexpNode.PROP_NOT_M : RegexpNode.PROP_M;
	
      default:
        throw error(L.l("invalid Unicode category " +
			badChar(ch) + "" + badChar(ch2)));
      }

    case 'N':
      switch (ch2) {
      case 'd':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Nd : RegexpNode.PROP_Nd;
      case 'l':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Nl : RegexpNode.PROP_Nl;
      case 'o':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_No : RegexpNode.PROP_No;
	
      case '}':
        return isNegated ? RegexpNode.PROP_NOT_N : RegexpNode.PROP_N;
	
      default:
        throw error(L.l("invalid Unicode category " +
			badChar(ch) + "" + badChar(ch2)));
      }

    case 'P':
      switch (ch2) {
      case 'c':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Pc : RegexpNode.PROP_Pc;
      case 'd':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Pd : RegexpNode.PROP_Pd;
      case 'e':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Pe : RegexpNode.PROP_Pe;
      case 'f':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Pf : RegexpNode.PROP_Pf;
      case 'i':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Pi : RegexpNode.PROP_Pi;
      case 'o':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Po : RegexpNode.PROP_Po;
      case 's':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Ps : RegexpNode.PROP_Ps;
	
      case '}':
        return isNegated ? RegexpNode.PROP_NOT_P : RegexpNode.PROP_P;
	
      default:
        throw error(L.l("invalid Unicode category "
			+ badChar(ch) + "" + badChar(ch2)));
      }

    case 'S':
      switch (ch2) {
      case 'c':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Sc : RegexpNode.PROP_Sc;
      case 'k':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Sk : RegexpNode.PROP_Sk;
      case 'm':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Sm : RegexpNode.PROP_Sm;
      case 'o':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_So : RegexpNode.PROP_So;
	
      case '}':
        return isNegated ? RegexpNode.PROP_NOT_S : RegexpNode.PROP_S;
	
      default:
        throw error(L.l("invalid Unicode category "
			+ badChar(ch) + "" + badChar(ch2)));
      }

    case 'Z':
      switch (ch2) {
      case 'l':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Zl : RegexpNode.PROP_Zl;
      case 'p':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Zp : RegexpNode.PROP_Zp;
      case 's':
	expect('}', pattern.read());
	       
	return isNegated ? RegexpNode.PROP_NOT_Zs : RegexpNode.PROP_Zs;
	
      case '}':
        return isNegated ? RegexpNode.PROP_NOT_Z : RegexpNode.PROP_Z;
	
      default:
	throw error(L.l("invalid Unicode category " +
			badChar(ch) + "" + badChar(ch2)));
      }
    }

    if ((ch = pattern.read()) != '}')
      throw error(L.l("expected '}' at " + badChar(ch)));
    
    RegexpNode.Compat node;

    if (isNegated)
      node = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_NUNICODE);
    else
      node = (RegexpNode.Compat) RegexpNode.create(RegexpNode.RC_UNICODE);
    
    node._unicodeCategory = category;
    
    return node;
  }
  
  private RegexpNode parseUnbracedUnicodeProperty(int ch,
						  PeekStream pattern,
						  boolean isNegated)
    throws IllegalRegexpException
  {
    switch (ch) {
      case 'C':
        if (isNegated)
          return RegexpNode.create(RegexpNode.RC_NC);
        else
          return RegexpNode.create(RegexpNode.RC_C);
      case 'L':
        if (isNegated)
          return RegexpNode.create(RegexpNode.RC_NL);
        else
          return RegexpNode.create(RegexpNode.RC_L);
      case 'M':
        if (isNegated)
          return RegexpNode.create(RegexpNode.RC_NM);
        else
          return RegexpNode.create(RegexpNode.RC_M);
      case 'N':
        if (isNegated)
          return RegexpNode.create(RegexpNode.RC_NN);
        else
          return RegexpNode.create(RegexpNode.RC_N);
      case 'P':
        if (isNegated)
          return RegexpNode.create(RegexpNode.RC_NP);
        else
          return RegexpNode.create(RegexpNode.RC_P);
      case 'S':
        if (isNegated)
          return RegexpNode.create(RegexpNode.RC_NS);
        else
          return RegexpNode.create(RegexpNode.RC_S);
      case 'Z':
        if (isNegated)
          return RegexpNode.create(RegexpNode.RC_NZ);
        else
          return RegexpNode.create(RegexpNode.RC_Z);
      default:
        throw new IllegalRegexpException("invalid Unicode property " +
                badChar(ch));
    }
  }
  
  /*
  static {
    _characterClassMap.put("alnum", RegexpNode.RC_ALNUM);
    _characterClassMap.put("alpha", RegexpNode.RC_ALPHA);
    _characterClassMap.put("blank", RegexpNode.RC_BLANK);
    _characterClassMap.put("cntrl", RegexpNode.RC_CNTRL);
    _characterClassMap.put("digit", RegexpNode.RC_DIGIT);
    _characterClassMap.put("graph", RegexpNode.RC_GRAPH);
    _characterClassMap.put("lower", RegexpNode.RC_LOWER);
    _characterClassMap.put("print", RegexpNode.RC_PRINT);
    _characterClassMap.put("punct", RegexpNode.RC_PUNCT);
    _characterClassMap.put("space", RegexpNode.RC_SPACE);
    _characterClassMap.put("upper", RegexpNode.RC_UPPER);
    _characterClassMap.put("xdigit", RegexpNode.RC_XDIGIT);
  }
  */
}
