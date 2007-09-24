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

  static final int ANCHORED = 0x20;
  static final int END_ONLY = 0x40;
  static final int UNGREEDY = 0x80;
  static final int STRICT = 0x100;
  
  static final HashMap<String,Integer> _characterClassMap
    = new HashMap<String,Integer>();
  
  int _nGroup;
  int _maxGroup;
  int _nLoop;
  int _flags;

  HashMap<Integer,StringValue> _groupNameMap
    = new HashMap<Integer,StringValue>();

  HashMap<StringValue,Integer> _groupNameReverseMap
    = new HashMap<StringValue,Integer>();
  
  boolean _isLookbehind;
  boolean _isOr;
  
  Regcomp(int flags)
  {
    _flags = flags;
  }

  Node parse(PeekStream pattern) throws IllegalRegexpException
  {
    _nGroup = 0;
    
    Node value = parseRec(pattern);

    if (_nGroup > _maxGroup)
      _maxGroup = _nGroup;

    if ((_flags & ANCHORED) != 0) {
      Node node = new Node(Node.RC_BSTRING);
      node._rest = value;
      
      value = node;
    }
    
    return value;
  }

  private Node parseRec(PeekStream pattern)
    throws IllegalRegexpException
  {
    return parseRec(pattern, false);
  }
  
  /**
   * @param isConditional true is parsing for (?(cond)yes|no)
   * 
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
  private Node parseRec(PeekStream pattern, boolean isConditional)
    throws IllegalRegexpException
  {
    Node head;
    Node last;
    Node node;

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
	    node = new Node(Node.RC_LOOP, _nLoop++, 0, Integer.MAX_VALUE);
	  else
	    node = new Node(Node.RC_LOOP_SHORT, _nLoop++, 0, Integer.MAX_VALUE);
	}
    else if (pattern.peek() == '+') {
      pattern.read();
      node = new Node(Node.RC_LOOP_LONG, _nLoop++, 0, Integer.MAX_VALUE);
    }
	else if ((_flags & UNGREEDY) != 0)
      node = new Node(Node.RC_LOOP_SHORT, _nLoop++, 0, Integer.MAX_VALUE);
	else
	  node = new Node(Node.RC_LOOP, _nLoop++, 0, Integer.MAX_VALUE);
	
	node._branch = last;
	last = node;
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
	      node = new Node(Node.RC_LOOP, _nLoop++, 1, Integer.MAX_VALUE);
	  else
	    node = new Node(Node.RC_LOOP_SHORT, _nLoop++, 1, Integer.MAX_VALUE);
	}
    else if (pattern.peek() == '+') {
      pattern.read();
      node = new Node(Node.RC_LOOP_LONG, _nLoop++, 1, Integer.MAX_VALUE);
    }
	else if ((_flags & UNGREEDY) != 0)
      node = new Node(Node.RC_LOOP_SHORT, _nLoop++, 1, Integer.MAX_VALUE);
	else
	  node = new Node(Node.RC_LOOP, _nLoop++, 1, Integer.MAX_VALUE);
	node._branch = last;
	last = node;

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
	    node = new Node(Node.RC_LOOP, _nLoop++, 0, 1);
	  else
	    node = new Node(Node.RC_LOOP_SHORT, _nLoop++, 0, 1);
	}
    else if (pattern.peek() == '+') {
      pattern.read();
      node = new Node(Node.RC_LOOP_LONG, _nLoop++, 0, 1);
    }
	else if ((_flags & UNGREEDY) != 0)
      node = new Node(Node.RC_LOOP_SHORT, _nLoop++, 0, 1);
	else
	  node = new Node(Node.RC_LOOP, _nLoop++, 0, 1);

	node._branch = last;
	last = node;
	break;

      case '{':
        ch = pattern.peek();
        if (ch < '0' || ch > '9') {
          Node next = parseString('{', pattern, last);
          if (next != last) {
            head = Node.concat(head, last);
            last = next;
          }
          
          break;
        }
        
	if (last == null)
	  throw new IllegalRegexpException("`{' must follow content expression");
	/*
	if (last._code == last.RC_LOOP)
	  throw new IllegalRegexpException("nested *?+");
	*/

	node = new Node(Node.RC_LOOP);
	node._index = _nLoop++;
	node._branch = last;
	node._length = node._branch._length * node._min;

	parseBrace(pattern, node);
	if ((ch = pattern.read()) != '}')
	  throw new IllegalRegexpException("expected `}' at " +
					   badChar(ch));
	  
	if (pattern.peek() == '?') {
	  pattern.read();
	  node._code = Node.RC_LOOP_SHORT;
	}
    else if (pattern.peek() == '+') {
      pattern.read();
      node._code = Node.RC_LOOP_LONG;
    }

	if (_isLookbehind) {
	  if (node._min != node._max)
	    throw new IllegalRegexpException("lookbehind strings must be fixed length: "
	                                     + node._min + " != " + node._max);
	}
	
    int length = node._branch._length * node._min;
    node._length = length;
	
	last = node;
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
            && (head._code == Node.RC_OR || head._code == Node.RC_LOOKBEHIND_OR))
          break;
        
        node = Node.concat(head, last);
	head = new Node(Node.RC_OR, node);
	
	if (_isLookbehind) {
	  head._code = Node.RC_LOOKBEHIND_OR;
	  //head = new Node(Node.RC_LOOKBEHIND_OR, head);
	}
	

	head._length = node._length;
	
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
	    
	    /*
	    if (last._rest == null || last._rest._code == Node.RC_NULL) {
	    }
	    else {
	      Node newLast = new Node(Node.RC_NULL);
	      newLast._rest = last;
	      
	      last = newLast;
	    }
	    */
	    
	    /*
	    // removes RC_END
	    Node.removeTail(last);
	    
	    if (last._rest != null) {
	      Node newLast = Node.removeTail(last);
	      head = Node.concat(head, last);
	        
	      last = newLast;
	    }
	    */
	    
	    break;
	    
	  case '>':
	    // (?>...) Atomic groups (once-only subpatterns). 
	    last = parseRec(pattern);
	    break;

	  case '=':
	    // (?=...) Positive lookahead assertion.
	    last = new Node(Node.RC_POS_LOOKAHEAD, parseRec(pattern));
	    break;

	  case '!':
	    // (?!...) Negative lookahead assertion.
	    last = new Node(Node.RC_NEG_LOOKAHEAD, parseRec(pattern));
	    break;
	    
	  case '<':
	    ch = pattern.read();
	    
	    // (?<=...) Positive lookbehind assertion.
        if (ch == '=')
          last = new Node(Node.RC_POS_LOOKBEHIND);
        // (?<!...) Negative lookbehind assertion.
	    else if (ch == '!')
          last = new Node(Node.RC_NEG_LOOKBEHIND);
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

	      last = new Node(Node.RC_BEG_GROUP, groupIndex);
	      last._rest = parseRec(pattern);
	      last._length = last._rest._length;

	      node = new Node(Node.RC_END_GROUP, groupIndex);
	      last = Node.replaceTail(last, node);
	      
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
	      
	      last = new Node(Node.RC_GROUP_REF, groupIndex.intValue());
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
	    
	    Node condition = null;;
	    
	    ch = pattern.peek();
	    if ('0' <= ch && ch <= '9')
	      condition = parseBackReference(pattern.read(), pattern, last);
	    else
	      condition = null;
	    ch = pattern.read();
	    

	    if (ch != ')')
	      throw new IllegalRegexpException("expected `)' at " +
                                           badChar(ch));

	    Node yesPattern = parseRec(pattern, true);
	    Node noPattern = null;
	    
	    if (pattern.peek() == '|') {
	      pattern.read();
	      
	      noPattern = parseRec(pattern, true);
	    }

	    last = new Node(Node.RC_COND, condition._index);
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

	  last = new Node(Node.RC_BEG_GROUP, groupIndex);
	  last._rest = parseRec(pattern);
	  
	  if (last._rest != null)
        last._length = last._rest._length;

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
	
	if (pattern.peek() == ':') {
      throw new IllegalRegexpException("POSIX [::] class outside []");
	}
	else {
	  last = parseSet(pattern);
	}

	if ((ch = pattern.read()) != ']') {
	  throw new IllegalRegexpException("expected `]' at " + 
					   badChar(ch));
	}
	break;

      case '.':
	head = Node.concat(head, last);

	if ((_flags & SINGLE_LINE) == 0)
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
	if ((_flags & MULTILINE) != 0)
	  last = new Node(Node.RC_BLINE);
	else
	  last = new Node(Node.RC_BSTRING);
	break;

      case '$':
	head = Node.concat(head, last);
	if ((_flags & MULTILINE) != 0)
	  last = new Node(Node.RC_ELINE);
	else if ((_flags & END_ONLY) != 0)
	  last = new Node(Node.RC_ESTRING);
	else
	  last = new Node(Node.RC_ENSTRING);
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
    int first = pattern.peek();
    
    if (first == '^') {
      pattern.read();
      node = new Node(Node.RC_NSET);
      node._length = 1;
    }
    else {
      node = new Node(Node.RC_SET);
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
    
    if (ch == ']')
      pattern.ungetc(ch);

    return node;
  }

  private Node parseString(int ch,
                           PeekStream pattern,
                           Node last)
    throws IllegalRegexpException
  {
    return parseString(ch, pattern, last, false);
  }
  
  /**
   * parseString
   */
  private Node parseString(int ch,
                           PeekStream pattern,
                           Node last,
                           boolean isEscaped)
    throws IllegalRegexpException
  {
    if ((_flags & IGNORE_WS) != 0 && ! isEscaped) {
      if (RegexpSet.SPACE.match(ch))
        return last;
      else if (ch == '#') {
        while ((ch = pattern.read()) != '\n' && ch >= 0) {
        }
        
        return last;
      }
    }

    int next = pattern.read();
    if (last == null
        || last._code != Node.RC_STRING
        || (last._rest != null && last._rest._code != Node.RC_END)
        || next == '*'
        || next == '?'
        || next == '{'
        || next == '+') {

      last = new Node(new CharBuffer());
    }
    last._string.append((char) ch);
    last._length++;
    
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

    case 'z':
      return new Node(Node.RC_ESTRING);
      
    case 'Z':
      return new Node(Node.RC_ENSTRING);

    case 'G':
      return new Node(Node.RC_GSTRING);

    case 'a':
      return parseString('\u0007', pattern, last);
    
    case 'c':
      ch = pattern.read();
      
      ch = Character.toUpperCase(ch);
      ch ^= 0x40;

      return parseString(ch, pattern, last);

    case 'e':
      return parseString('\u001B', pattern, last, true);
    case 'n':
      return parseString('\n', pattern, last, true);
    case 'r':
      return parseString('\r', pattern, last, true);
    case 'f':
      return parseString('\f', pattern, last, true);
    case 't':
      return parseString('\t', pattern, last, true);

    case 'x':
      int hex = parseHex(pattern);
      return parseString(hex, pattern, last, true);
    
    case '0':
      int oct = parseOctal(ch, pattern);
      return parseString(oct, pattern, last, true);

    case '1': case '2': case '3': case '4': 
    case '5': case '6': case '7': case '8': case '9':
      return parseBackReference(ch, pattern, last);

    case 'p':
      return parseUnicodeProperty(pattern, last, false);
    case 'P':
      return parseUnicodeProperty(pattern, last, true);
      
    case 'Q':
      while ((ch = pattern.read()) >= 0) {
        if (ch == '\\' && pattern.peek() == 'E') {
          pattern.read();
          break;
        }

        last = parseString(ch, pattern, last);
      }

      return last;
      
    case '#':
      return parseString('#', pattern, last, true);

    default:
      if ((_flags & STRICT) != 0)
        throw new IllegalRegexpException("unrecognized escape at " +
                                         badChar(ch));
      return parseString(ch, pattern, last);
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
  
  private Node parseBackReference(int ch,
                                  PeekStream pattern,
                                  Node last)
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
      return new Node(Node.RC_GROUP_REF, value);
    }
    else if (! ('0' <= ch2 && ch2 <= '7') &&
             ! ('0' <= ch3 && ch3 <= '7'))
      throw new IllegalRegexpException("back referencing to a non-existent group: " +
                                       value);
    
    if (value > 10)
      pattern.ungetc(ch2);
    
    if (ch == '8' || ch == '9' ||
        '0' <= ch3 && ch3 <= '9' && value * 10 + ch3 - '0' > 0xFF) {
      //out of byte range or not an octal,
      //need to parse backslash as the NULL character
      
      pattern.ungetc(ch);
      return parseString('\u0000', pattern, last);
    }
    
    int oct = parseOctal(ch, pattern);
    
    return parseString(oct, pattern, last, true);
  }
  
  private int parseOctal(int ch,
                         PeekStream pattern)
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
  
  private Node parseUnicodeProperty(PeekStream pattern,
                                   Node last,
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
    
    Node node;
    
    if (isBraced)
      node = parseBracedUnicodeProperty(ch, pattern, last, isNegated);
    else
      node = parseUnbracedUnicodeProperty(ch, pattern, last, isNegated);
    
    return node;
  }
  
  private Node parseBracedUnicodeProperty(int ch,
                                          PeekStream pattern,
                                          Node last,
                                          boolean isNegated)
    throws IllegalRegexpException
  {
    byte category = 0;
    
    int ch2 = pattern.read();
    
    switch (ch) {
    case 'C':
      switch (ch2) {
      case 'c':
        category = Character.CONTROL;
        break;
      case 'f':
        category = Character.FORMAT;
        break;
      case 'n':
        category = Character.UNASSIGNED;
        break;
      case 'o':
        category = Character.PRIVATE_USE;
        break;
      case 's':
        category = Character.SURROGATE;
        break;
      case '}':
        if (isNegated)
          return new Node(Node.RC_NC);
        else
          return new Node(Node.RC_C);
      default:
        throw new IllegalRegexpException("invalid Unicode category " +
                badChar(ch) + "" + badChar(ch2));
      }
      break;
    case 'L':
      switch (ch2) {
        case 'l':
          category = Character.LOWERCASE_LETTER;
          break;
        case 'm':
          category = Character.MODIFIER_LETTER;
          break;
        case 'o':
          category = Character.OTHER_LETTER;
          break;
        case 't':
          category = Character.TITLECASE_LETTER;
          break;
        case 'u':
          category = Character.UPPERCASE_LETTER;
          break;
        case '}':
          if (isNegated)
            return new Node(Node.RC_NL);
          else
            return new Node(Node.RC_L);
        default:
          throw new IllegalRegexpException("invalid Unicode category " +
                  badChar(ch) + "" + badChar(ch2));
      }
      break;
    case 'M':
      switch (ch2) {
      case 'c':
        category = Character.COMBINING_SPACING_MARK;
        break;
      case 'e':
        category = Character.ENCLOSING_MARK;
        break;
      case 'n':
        category = Character.NON_SPACING_MARK;
        break;
      case '}':
        if (isNegated)
          return new Node(Node.RC_NM);
        else
          return new Node(Node.RC_M);
      default:
        throw new IllegalRegexpException("invalid Unicode category " +
                badChar(ch) + "" + badChar(ch2));
      }
      break;
    case 'N':
      switch (ch2) {
      case 'd':
        category = Character.DECIMAL_DIGIT_NUMBER;
        break;
      case 'l':
        category = Character.LETTER_NUMBER;
        break;
      case 'o':
        category = Character.OTHER_NUMBER;
        break;
      case '}':
        if (isNegated)
          return new Node(Node.RC_NN);
        else
          return new Node(Node.RC_N);
      default:
        throw new IllegalRegexpException("invalid Unicode category " +
                badChar(ch) + "" + badChar(ch2));
      }
      break;
    case 'P':
      switch (ch2) {
      case 'c':
        category = Character.CONNECTOR_PUNCTUATION;
        break;
      case 'd':
        category = Character.DASH_PUNCTUATION;
        break;
      case 'e':
        category = Character.END_PUNCTUATION;
        break;
      case 'f':
        category = Character.FINAL_QUOTE_PUNCTUATION;
        break;
      case 'i':
        category = Character.INITIAL_QUOTE_PUNCTUATION;
        break;
      case 'o':
        category = Character.OTHER_PUNCTUATION;
        break;
      case 's':
        category = Character.START_PUNCTUATION;
        break;
      case '}':
        if (isNegated)
          return new Node(Node.RC_NP);
        else
          return new Node(Node.RC_P);
      default:
        throw new IllegalRegexpException("invalid Unicode category " +
                badChar(ch) + "" + badChar(ch2));
      }
      break;
    case 'S':
      switch (ch2) {
      case 'c':
        category = Character.CURRENCY_SYMBOL;
        break;
      case 'k':
        category = Character.MODIFIER_SYMBOL;
        break;
      case 'm':
        category = Character.MATH_SYMBOL;
        break;
      case 'o':
        category = Character.OTHER_SYMBOL;
        break;
      case '}':
        if (isNegated)
          return new Node(Node.RC_NS);
        else
          return new Node(Node.RC_S);
      default:
        throw new IllegalRegexpException("invalid Unicode category " +
                badChar(ch) + "" + badChar(ch2));
      }
      break;
    case 'Z':
      switch (ch2) {
        case 'l':
          category = Character.LINE_SEPARATOR;
          break;
        case 'p':
          category = Character.PARAGRAPH_SEPARATOR;
          break;
        case 's':
          category = Character.SPACE_SEPARATOR;
          break;
        case '}':
          if (isNegated)
            return new Node(Node.RC_NZ);
          else
            return new Node(Node.RC_Z);
        default:
          throw new IllegalRegexpException("invalid Unicode category " +
                  badChar(ch) + "" + badChar(ch2));
      }
      break;
    }

    if ((ch = pattern.read()) != '}')
      throw new IllegalRegexpException("expected '}' at " +
              badChar(ch));
    
    Node node;

    if (isNegated)
      node = new Node(Node.RC_NUNICODE);
    else
      node = new Node(Node.RC_UNICODE);
    
    node._unicodeCategory = category;
    
    return node;
  }
  
  private Node parseUnbracedUnicodeProperty(int ch,
                                            PeekStream pattern,
                                            Node last,
                                            boolean isNegated)
    throws IllegalRegexpException
  {
    switch (ch) {
      case 'C':
        if (isNegated)
          return new Node(Node.RC_NC);
        else
          return new Node(Node.RC_C);
      case 'L':
        if (isNegated)
          return new Node(Node.RC_NL);
        else
          return new Node(Node.RC_L);
      case 'M':
        if (isNegated)
          return new Node(Node.RC_NM);
        else
          return new Node(Node.RC_M);
      case 'N':
        if (isNegated)
          return new Node(Node.RC_NN);
        else
          return new Node(Node.RC_N);
      case 'P':
        if (isNegated)
          return new Node(Node.RC_NP);
        else
          return new Node(Node.RC_P);
      case 'S':
        if (isNegated)
          return new Node(Node.RC_NS);
        else
          return new Node(Node.RC_S);
      case 'Z':
        if (isNegated)
          return new Node(Node.RC_NZ);
        else
          return new Node(Node.RC_Z);
      default:
        throw new IllegalRegexpException("invalid Unicode property " +
                badChar(ch));
    }
  }
  
  /*
  static {
    _characterClassMap.put("alnum", Node.RC_ALNUM);
    _characterClassMap.put("alpha", Node.RC_ALPHA);
    _characterClassMap.put("blank", Node.RC_BLANK);
    _characterClassMap.put("cntrl", Node.RC_CNTRL);
    _characterClassMap.put("digit", Node.RC_DIGIT);
    _characterClassMap.put("graph", Node.RC_GRAPH);
    _characterClassMap.put("lower", Node.RC_LOWER);
    _characterClassMap.put("print", Node.RC_PRINT);
    _characterClassMap.put("punct", Node.RC_PUNCT);
    _characterClassMap.put("space", Node.RC_SPACE);
    _characterClassMap.put("upper", Node.RC_UPPER);
    _characterClassMap.put("xdigit", Node.RC_XDIGIT);
  }
  */
}
