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
import java.util.logging.*;

import com.caucho.util.*;
import com.caucho.vfs.*;

public class Regexp {
  private static final Logger log
    = Logger.getLogger(Regexp.class.getName());
  
  public static final int FAIL = -1;

  String pattern;
  
  Node prog;
  boolean ignoreCase;
  boolean isGlobal;

  int first;
  int start;

  StringCharCursor stringCursor;

  int nLoop;
  int []loopCount;
  int []loopTail;

  int nGroup;
  int []groupStart; // possibly not matching
  IntArray group;
  int match;
  int lexeme;

  CharBuffer cb;
  
  // optim stuff
  CharBuffer prefix; // initial string
  int minLength; // minimum length possible for this regexp

  CharCursor lastCursor;
  int lastIndex;

  public Regexp(String pattern, String sflags) throws IllegalRegexpException
  {
    this.pattern = pattern;
    
    int flags = 0;

    for (int i = 0; sflags != null && i < sflags.length(); i++) {
      switch (sflags.charAt(i)) {
      case 'm': flags |= Regcomp.MULTILINE; break;
      case 's': flags |= Regcomp.SINGLE_LINE; break;
      case 'i': flags |= Regcomp.IGNORE_CASE; break;
      case 'x': flags |= Regcomp.IGNORE_WS; break;
      case 'g': flags |= Regcomp.GLOBAL; break;
      }
    }

    Regcomp comp = new Regcomp(flags);

    prog = comp.parse(new PeekString(pattern));

    compile(prog, comp);

    /*
    if (dbg.canWrite())
      dbg.log(pattern + " -> " + prog);
    */
  }

  public Regexp(String pattern) throws IllegalRegexpException
  {
    this(pattern, null);
  }

  public Regexp(Node prog, Regcomp comp)
  {
    compile(prog, comp);
  }

  public String getPattern()
  {
    return pattern;
  }

  private void compile(Node prog, Regcomp comp)
  {
    ignoreCase = (comp.flags & Regcomp.IGNORE_CASE) != 0;
    isGlobal = (comp.flags & Regcomp.GLOBAL) != 0;

    if (ignoreCase)
      RegOptim.ignoreCase(prog);

    if (! ignoreCase)
      RegOptim.eliminateBacktrack(prog, null);

    minLength = RegOptim.minLength(prog);
    prefix = RegOptim.prefix(prog);

    this.prog = RegOptim.linkLoops(prog);

    nGroup = comp.maxGroup;
    nLoop = comp.nLoop;
    groupStart = new int[nGroup + 1];
    loopCount = new int[nLoop];
    loopTail = new int[nLoop];
    cb = new CharBuffer();
    stringCursor = new StringCharCursor("");
    group = new IntArray();
  }

  public boolean isGlobal() { return isGlobal; }
  public boolean ignoreCase() { return ignoreCase; }

  /**
   * XXX: not proper behaviour with /g
   */
  public int exec(CharCursor cursor, int start, int first)
  { 
    this.start = start;
    this.first = first;

    cursor.setIndex(first);

    int begin = cursor.getIndex();
    int value = FAIL;
    if (begin >= first) {
      do {
	group.setLength(0);
	if ((value = match(prog, cursor)) != FAIL)
	  break;

	cursor.setIndex(begin + 1);
	begin = cursor.getIndex();
      } while (cursor.current() != cursor.DONE);
    }

    int pos = cursor.getIndex();
    if (pos < begin)
      begin = pos;
    if (group.size() < 2)
      group.setLength(2);
    group.set(0, begin);
    group.set(1, pos);

    return value;
  }

  /**
   * XXX: not proper behaviour with /g
   */
  public int exec(String string, int first)
  {
    stringCursor.init(string);

    return exec(stringCursor, 0, first);
  }

  /**
   * XXX: not proper behaviour with /g
   */
  public int exec(CharBuffer buffer, int first)
  {
    stringCursor.init(buffer.toString());

    return exec(stringCursor, 0, first);
  }

  /**
   * XXX: not proper behaviour with /g
   */
  public int exec(CharCursor cursor)
  {
    int first = 0;
    
    if (cursor == lastCursor)
      first = lastIndex;
    lastCursor = cursor;

    int value = exec(cursor, 0, first);

    if (value == FAIL)
      lastIndex = 0;
    else if (getBegin(0) == getEnd(0))
      lastIndex = getEnd(0) + 1;
    else
      lastIndex = getEnd(0);

    return value;
  }

  /**
   * Tries to match the program.
   *
   * @return the tail of the match
   */
  private int match(Node prog, CharCursor cursor)
  {
    int tail;
    int ch;
    int value;

    while (prog != null) {
      switch (prog.code) {
      case Node.RC_NULL:
	prog = prog.rest;
	break;

      case Node.RC_LEXEME:
      case Node.RC_END:
	lexeme = prog.index;
	return prog.index;

      case Node.RC_STRING:
	int length = prog.string.length();

	if (cursor.regionMatches(prog.string.getBuffer(), 0, length)) {
	  prog = prog.rest;
        }
	else {
	  return FAIL;
        }
	break;

      case Node.RC_STRING_I:
	length = prog.string.length();

	if (cursor.regionMatchesIgnoreCase(prog.string.getBuffer(), 
					   0, length)) {
	  prog = prog.rest;
	}
	else
	  return FAIL;
	break;

      case Node.RC_SET:
	if ((ch = cursor.read()) != cursor.DONE && prog.set.match(ch))
	  prog = prog.rest;
	else
	  return FAIL;
	break;

      case Node.RC_SET_I:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
	int lch = Character.toLowerCase((char) ch);
	int uch = Character.toUpperCase((char) lch);
	if (prog.set.match(lch) || prog.set.match(uch))
	  prog = prog.rest;
	else
	  return FAIL;
	break;

      case Node.RC_NSET:
	if ((ch = cursor.read()) != cursor.DONE && ! prog.set.match(ch))
	  prog = prog.rest;
	else
	  return FAIL;
	break;

      case Node.RC_NSET_I:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
	lch = Character.toLowerCase((char) ch);
	uch = Character.toUpperCase((char) lch);
	if (! prog.set.match(lch) && ! prog.set.match(uch))
	  prog = prog.rest;
	else
	  return FAIL;
	break;

	// '('
      case Node.RC_BEG_GROUP:
	groupStart[prog.index] = cursor.getIndex();
	prog = prog.rest;
	break;

	// ')'
      case Node.RC_END_GROUP:
	int index = 2 * prog.index;
	if (group.size() <= index + 1)
	  group.setLength(index + 2);
	group.set(2 * prog.index, groupStart[prog.index]);
	group.set(2 * prog.index + 1, cursor.getIndex());
	prog = prog.rest;
	break;

	// '\nn'
      case Node.RC_GROUP_REF:
	int begin = group.get(2 * prog.index);
	length = (group.get(2 * prog.index + 1) - 
		  group.get(2 * prog.index));
	cb.setLength(0);
	cursor.subseq(cb, begin, begin + length);
	if (cursor.regionMatches(cb.getBuffer(), 0, length)) {
	  prog = prog.rest;
	} else
	  return FAIL;
	break;

	// '\nn'
      case Node.RC_GROUP_REF_I:
	begin = group.get(2 * prog.index);
	length = (group.get(2 * prog.index + 1) - 
		  group.get(2 * prog.index));

	cb.setLength(0);
	cursor.subseq(cb, begin, begin + length);
	if (cursor.regionMatchesIgnoreCase(cb.getBuffer(), 0, length)) {
	  cursor.skip(length);
	  prog = prog.rest;
	} else
	  return FAIL;
	break;

      case Node.RC_LOOP_INIT:
	loopCount[prog.rest.index] = 0;
	loopTail[prog.rest.index] = -1;
	prog = prog.rest;
	break;

	// '*' '{n,m}' '+' '?' matches as much as possible
      case Node.RC_LOOP:
	tail = cursor.getIndex();
	if (loopCount[prog.index]++ < prog.min)
	  prog = prog.branch;
	else if (loopCount[prog.index] > prog.max)
	  prog = prog.rest;
	else if (loopTail[prog.index] == tail)
	  return FAIL;
	else {
	  loopTail[prog.index] = tail;
	  int match = group.size();

	  if ((ch = cursor.current()) == cursor.DONE)
	    prog = prog.rest;
	  else if (prog.set != null && prog.set.match(ch))
	    prog = prog.branch;
	  else if ((value = match(prog.branch, cursor)) != FAIL)
	    return value;
	  else {
	    cursor.setIndex(tail);
	    group.setLength(match);
	    prog = prog.rest;
	  }
	}
	break;

	// '*' '{n,m}' '+' '?' matches as little as possible
      case Node.RC_LOOP_SHORT:
	tail = cursor.getIndex();
	if (loopCount[prog.index]++ < prog.min)
	  prog = prog.branch;
	else if (loopCount[prog.index] > prog.max)
	  prog = prog.rest;
	else if ((value = match(prog.rest, cursor)) != FAIL)
	  return value;
	else if (loopTail[prog.index] == tail)
	  return FAIL;
	else {
	  loopTail[prog.index] = tail;
	  cursor.setIndex(tail);

	  prog = prog.branch;
	}
	break;

	// The first mismatch for loop unique is necessarily a match
	// for the successor, e.g. a*b as opposed to a*ab
	// XXX: this needs to be changed to be like the or.
      case Node.RC_LOOP_UNIQUE:
	if (loopCount[prog.index]++ < prog.min)
	  prog = prog.branch;
	else if (loopCount[prog.index] > prog.max)
	  prog = prog.rest;
	else if ((ch = cursor.current()) == cursor.DONE)
	  prog = prog.rest;
	else if (prog.set.match(ch))
	  prog = prog.branch;
	else
	  prog = prog.rest;
	break;

      case Node.RC_OR:
	match = group.size();
	tail = cursor.getIndex();
	if ((value = match(prog.branch, cursor)) != FAIL)
	  return value;
	cursor.setIndex(tail);
	group.setLength(match);
	prog = prog.rest;
	break;

	// Here we can tell by the first character if the match works
      case Node.RC_OR_UNIQUE:
	if ((ch = cursor.current()) == cursor.DONE)
	  prog = prog.rest;
	else if (prog.set.match(ch)) {
	  prog = prog.branch;
        }
	else {
	  prog = prog.rest;
        }
	break;

	// The peek pattern must match but isn't included in the real match
      case Node.RC_POS_PEEK:
	tail = cursor.getIndex();
	if (match(prog.branch, cursor) == FAIL)
	  return FAIL;
	cursor.setIndex(tail);
	prog = prog.rest;
	break;

	// The peek pattern must not match and isn't included in the real match
      case Node.RC_NEG_PEEK:
	tail = cursor.getIndex();
	if (match(prog.branch, cursor) != FAIL)
	  return FAIL;
	cursor.setIndex(tail);
	prog = prog.rest;
	break;

	// Beginning of line
      case Node.RC_BLINE:
	if (cursor.getIndex() == start)
	  prog = prog.rest;
	else if (cursor.previous() == '\n') {
	  cursor.next();
	  prog = prog.rest;
	}
	else {
	  cursor.next();
	  return FAIL;
	}
	break;

	// End of line
      case Node.RC_ELINE:
	if (cursor.current() == cursor.DONE || cursor.current() == '\n')
	  prog = prog.rest;	  // XXX: return on success?
	else
	  return FAIL;
	break;

	// Beginning of match
      case Node.RC_GSTRING:
	if (cursor.getIndex() == first)
	  prog = prog.rest;
	else
	  return FAIL;
	break;

	// beginning of string
      case Node.RC_BSTRING:
	if (cursor.getIndex() == start)
	  prog = prog.rest;
	else
	  return FAIL;
	break;

	// end of string
      case Node.RC_ESTRING:
	if (cursor.current() == cursor.DONE)
	  prog = prog.rest;	  // XXX: return on success?
	else
	  return FAIL;
	break;

      case Node.RC_WORD:
	tail = cursor.getIndex();
	if ((tail != start && RegexpSet.WORD.match(cursor.prev())) !=
	    (cursor.current() != cursor.DONE && 
	     RegexpSet.WORD.match(cursor.current())))
	  prog = prog.rest;
	else
	  return FAIL;
	break;

      case Node.RC_NWORD:
	tail = cursor.getIndex();
	if ((tail != start && RegexpSet.WORD.match(cursor.prev())) ==
	    (cursor.current() != cursor.DONE &&
	     RegexpSet.WORD.match(cursor.current())))
	  prog = prog.rest;
	else
	  return FAIL;
	break;

      default:
	throw new RuntimeException("Internal error");
      }
    }

    return 0;
  }

  public int getBegin(int i)
  {
    if (group.size() < 2 * i)
      return 0;
    else
      return group.get(2 * i);
  }

  public int getEnd(int i)
  {
    if (group.size() < 2 * i + 1)
      return 0;
    else
      return group.get(2 * i + 1);
  }

  public int length() { return group.size() / 2; }

  public boolean match(String string)
  {
    return exec(string, 0) != FAIL;
  }

  public ArrayList split(String string)
  {
    ArrayList vector = new ArrayList();

    int i = 0;
    int length = string.length();
    while (i < length) {
      if (exec(string, i) == FAIL) {
	vector.add(string.substring(i));
	break;
      }

      vector.add(string.substring(i, getBegin(0)));
      if (getBegin(0) != getEnd(0))
	i = getEnd(0);
      else
	i = getEnd(0) + 1;
    }

    return vector;
  }

  public CharBuffer replace(String test, String replace, CharBuffer cb)
  {
    cb.clear();
    // cb.append(test, 0, getBegin(0));

    for (int i = 0; i < replace.length(); i++) {
      char ch = replace.charAt(i);
      if (ch != '$' || i == replace.length() - 1)
        cb.append(ch);
      else {
        ch = replace.charAt(i + 1);
        if (ch >= '0' && ch <= '9') {
          int group = ch - '0';
          if (group < length()) {
            cb.append(test.substring(getBegin(group), getEnd(group)));
          }
          i++;
        }
        else if (ch == '$') {
          cb.append('$');
          i++;
        }
        else
          cb.append('$');
      }
    }

    // cb.append(test, getEnd(0), test.length() - getEnd(0));

    return cb;
  }

  public String replace(String test, String replace)
  {
    return replace(test, replace, cb).toString();
  }

  public CharBuffer fill(String test, String replace, CharBuffer cb)
  {
    cb.clear();

    for (int i = 0; i < replace.length(); i++) {
      char ch = replace.charAt(i);
      if (ch != '$' || i == replace.length() - 1)
        cb.append(ch);
      else {
        ch = replace.charAt(i + 1);
        if (ch >= '0' && ch <= '9') {
          int group = ch - '0';
          if (group < length()) {
            cb.append(test.substring(getBegin(group), getEnd(group)));
          }
          i++;
        }
        else if (ch == '$') {
          cb.append('$');
          i++;
        }
        else
          cb.append('$');
      }
    }

    return cb;
  }

  public String fill(String test, String replace)
  {
    return replace(test, replace, cb).toString();
  }

  public String toString()
  {
    return "[Regexp " + pattern + "]";
  }
}
