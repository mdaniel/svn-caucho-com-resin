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

import java.util.*;
import java.util.logging.*;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnicodeValue;
import com.caucho.util.*;

public class Regexp {
  private static final Logger log
    = Logger.getLogger(Regexp.class.getName());
  
  private static final L10N L = new L10N(Regexp.class);
  
  public static final int FAIL = -1;
  public static final int SUCCESS = 0;

  UnicodeValue _pattern;
  UnicodeValue _subject;
  
  Node _prog;
  boolean _ignoreCase;
  boolean _isGlobal;

  int _first;
  int _start;

  StringCharCursor _stringCursor;

  int _nLoop;
  int []_loopCount;
  int []_loopTail;

  int _nGroup;
  int []_groupStart; // possibly not matching
  int _match;
  int _lexeme;

  CharBuffer _cb;
  
  // optim stuff
  CharBuffer _prefix; // initial string
  int _minLength; // minimum length possible for this regexp

  CharCursor _lastCursor;
  int _lastIndex;

  StringValue []_groupNames;
  
  boolean _isUnicode;
  boolean _isUTF8;
  boolean _isEval;
  
  GroupState _groupState = new GroupState();
  
  public Regexp(Env env, StringValue rawRegexp)
    throws IllegalRegexpException
  {
    if (rawRegexp.length() < 2) {
      throw new IllegalStateException(L.l(
          "Can't find delimiters in regexp '{0}'.",
          rawRegexp));
    }

    char delim = rawRegexp.charAt(0);

    if (delim == '{')
      delim = '}';
    else if (delim == '[')
      delim = ']';
    else if (delim == '(')
      delim = ')';
    else if (delim == '<')
      delim = '>';
    else if (delim == '\\' || Character.isLetterOrDigit(delim)) {
      throw new IllegalStateException(L.l(
          "Delimiter {0} in regexp '{1}' must not be backslash or alphanumeric.",
          String.valueOf(delim),
          rawRegexp));
    }

    int tail = rawRegexp.lastIndexOf(delim);

    if (tail <= 0)
      throw new IllegalStateException(L.l(
          "Can't find second {0} in regexp '{1}'.",
          String.valueOf(delim),
          rawRegexp));

    StringValue sflags = rawRegexp.substring(tail);
    StringValue pattern = rawRegexp.substring(1, tail); 
    
    int flags = 0;
    
    for (int i = 0; sflags != null && i < sflags.length(); i++) {
      switch (sflags.charAt(i)) {
        case 'm': flags |= Regcomp.MULTILINE; break;
        case 's': flags |= Regcomp.SINGLE_LINE; break;
        case 'i': flags |= Regcomp.IGNORE_CASE; break;
        case 'x': flags |= Regcomp.IGNORE_WS; break;
        case 'g': flags |= Regcomp.GLOBAL; break;
        
        case 'A': flags |= Regcomp.ANCHORED; break;
        case 'D': flags |= Regcomp.END_ONLY; break;
        case 'U': flags |= Regcomp.UNGREEDY; break;
        case 'X': flags |= Regcomp.STRICT; break;
        
        case 'u': _isUTF8 = true; break;
        case 'e': _isEval = true; break;
      }
    }
    
    if (_isUTF8)
      _pattern = pattern.toUnicodeValue(env, "UTF-8");
    else
      _pattern = pattern.toUnicodeValue(env);
    
    Regcomp comp = new Regcomp(flags);

    _prog = comp.parse(new PeekString(_pattern));

    compile(env, _prog, comp);
  }

  protected Regexp(Env env, Node prog, Regcomp comp)
  {
    _prog = prog;
    
    compile(env, _prog, comp);
  }
  
  private Regexp()
  {
  }
  
  public StringValue substring(Env env, int start)
  {
    StringValue result = _subject.substring(start);
    
    return encodeResultString(env, result);
  }
  
  public StringValue substring(Env env, int start, int end)
  {
    StringValue result = _subject.substring(start, end);
    
    return encodeResultString(env, result);
  }
  
  private StringValue encodeResultString(Env env, StringValue str)
  {
    if (_isUnicode) {
      if (_isUTF8)
        return str.toUnicodeValue(env, "UTF-8");
      else
        return str.toUnicodeValue(env);
    }
    else {
      if (_isUTF8)
        return str.toBinaryValue(env, "UTF-8");
      else
        return str.toBinaryValue(env);
    }
  }
  
  /*
  public Regexp(Node prog, Regcomp comp)
  {
    compile(prog, comp);
  }
  */

  public UnicodeValue getPattern()
  {
    return _pattern;
  }
  
  public boolean isUTF8()
  {
    return _isUTF8;
  }
  
  public boolean isEval()
  {
    return _isEval;
  }

  private void compile(Env env, Node prog, Regcomp comp)
  {
    _ignoreCase = (comp._flags & Regcomp.IGNORE_CASE) != 0;
    _isGlobal = (comp._flags & Regcomp.GLOBAL) != 0;

    if (_ignoreCase)
      RegOptim.ignoreCase(prog);

    if (! _ignoreCase)
      RegOptim.eliminateBacktrack(prog, null);

    _minLength = RegOptim.minLength(prog);
    _prefix = RegOptim.prefix(prog);

    //this._prog = RegOptim.linkLoops(prog);

    _nGroup = comp._maxGroup;
    _nLoop = comp._nLoop;
    
    _groupStart = new int[_nGroup + 1];
    for (int i = 0; i < _groupStart.length; i++) {
      _groupStart[i] = -1;
    }

    _loopCount = new int[_nLoop];
    _loopTail = new int[_nLoop];
    _cb = new CharBuffer();
    _stringCursor = new StringCharCursor("");
    
    _groupNames = new StringValue[_nGroup + 1];
    for (Map.Entry<Integer,UnicodeValue> entry : comp._groupNameMap.entrySet()) {
      StringValue groupName = entry.getValue();
      
      if (_isUnicode) {
      }
      else if (_isUTF8) {
        groupName.toBinaryValue(env, "UTF-8");
      }
      else {
        groupName.toBinaryValue(env);
      }
      
      _groupNames[entry.getKey().intValue()] = groupName;
    }
  }

  public void init(Env env, StringValue subject)
  {
    _isUnicode = subject.isUnicode();
    
    if (_isUTF8)
      _subject = subject.toUnicodeValue(env, "UTF-8");
    else
      _subject = subject.toUnicodeValue(env);

    _stringCursor.init(_subject);
    
    _lastIndex = 0;
    
    for (int i = 0; i < _groupStart.length; i++) {
      _groupStart[i] = -1;
    }
    
    for (int i = 0; i < _loopTail.length; i++) {
      _loopTail[i] = -1;
    }
  }
  
  public boolean isGlobal() { return _isGlobal; }
  public boolean ignoreCase() { return _ignoreCase; }

  /**
   * XXX: not proper behaviour with /g
   */
  public int exec(CharCursor cursor, int start, int first)
  { 
    _groupState.clear();
    
    this._start = start;
    this._first = first;

    cursor.setIndex(first);

    int begin = cursor.getIndex();
    int value = FAIL;
    if (begin >= first) {
      while (true) {
        if (cursor.current() == cursor.DONE) {
          value = match(_prog, cursor);
          break;
        }
        
        _groupState.setLength(0);
        
        if ((value = match(_prog, cursor)) != FAIL)
          break;
        
        cursor.setIndex(begin + 1);
        begin = cursor.getIndex();
      }
      
      /*
      do {
	_group.setLength(0);
	if ((value = match(_prog, cursor)) != FAIL)
	  break;

	cursor.setIndex(begin + 1);
	begin = cursor.getIndex();
      } while (cursor.current() != cursor.DONE);
      */
    }

    int pos = cursor.getIndex();
    if (pos < begin)
      begin = pos;
    if (_groupState.size() < 2)
      _groupState.setLength(2);
    _groupState.set(0, begin);
    _groupState.set(1, pos);
    
    _groupState.setMatched(0);

    return value;
  }

  /**
   * XXX: not proper behaviour with /g
   */
  public int exec(String string, int first)
  {
    _stringCursor.init(string);

    return exec(_stringCursor, 0, first);
  }

  /**
   * XXX: not proper behaviour with /g
   */
  public int exec(CharBuffer buffer, int first)
  {
    _stringCursor.init(buffer.toString());

    return exec(_stringCursor, 0, first);
  }

  /**
   * XXX: not proper behaviour with /g
   */
  public int exec()
  {
    return exec(_stringCursor);
  }
  
  /**
   * XXX: not proper behaviour with /g
   */
  public int exec(int first)
  {
    _lastIndex = first;
    
    return exec(_stringCursor);
  }
  
  /**
   * XXX: not proper behaviour with /g
   */
  public int exec(CharCursor cursor)
  {
    int first = 0;
    
    if (cursor == _lastCursor)
      first = _lastIndex;
    _lastCursor = cursor;

    int value = exec(cursor, 0, first);

    if (value == FAIL)
      _lastIndex = 0;
    else if (getBegin(0) == getEnd(0))
      _lastIndex = getEnd(0) + 1;
    else
      _lastIndex = getEnd(0);

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
    char ch;
    int value;
    
    int i;
    
    GroupState oldState;
    
    while (prog != null) {
      /*
      System.err.print("Regexp->match(): " + Node.code(prog._code) + " " + prog._string);
      if (prog._branch != null)
        System.err.print(" . " + Node.code(prog._branch._code) + " " + prog._branch._string);
      if (prog._rest != null)
        System.err.print(" : " + Node.code(prog._rest._code) + " " + prog._rest._string);
      
      System.err.println(" " + cursor.getIndex() + " . " + cursor.current());
      */
      
      switch (prog._code) {
      case Node.RC_NULL:
	prog = prog._rest;
	break;

      case Node.RC_LEXEME:
      case Node.RC_END:
	_lexeme = prog._index;
	return prog._index;

      case Node.RC_STRING:
   
	int length = prog._string.length();

	if (cursor.regionMatches(prog._string.getBuffer(), 0, length)) {
	  
	  prog = prog._rest;
        }
	else {
	  return FAIL;
        }
	break;
    
      case Node.RC_STRING_I:
	length = prog._string.length();

	if (cursor.regionMatchesIgnoreCase(prog._string.getBuffer(), 
					   0, length)) {
	  prog = prog._rest;
	}
	else
	  return FAIL;
	break;

      case Node.RC_SET:
	if ((ch = cursor.read()) != cursor.DONE && prog._set.match(ch))
	  prog = prog._rest;
	else
	  return FAIL;
	break;

      case Node.RC_SET_I:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
	int lch = Character.toLowerCase((char) ch);
	int uch = Character.toUpperCase((char) lch);
	if (prog._set.match(lch) || prog._set.match(uch))
	  prog = prog._rest;
	else
	  return FAIL;
	break;

      case Node.RC_NSET:
	if ((ch = cursor.read()) != cursor.DONE && ! prog._set.match(ch))
	  prog = prog._rest;
	else
	  return FAIL;
	break;

      case Node.RC_NSET_I:
	if ((ch = cursor.read()) == cursor.DONE)
	  return FAIL;
	lch = Character.toLowerCase((char) ch);
	uch = Character.toUpperCase((char) lch);
	if (! prog._set.match(lch) && ! prog._set.match(uch))
	  prog = prog._rest;
	else
	  return FAIL;
	break;

	// '('
      case Node.RC_BEG_GROUP:
	_groupStart[prog._index] = cursor.getIndex();
	
	prog = prog._rest;

	break;

	// ')'
      case Node.RC_END_GROUP:
	int index = 2 * prog._index;
	if (_groupState.size() <= index + 1)
	  _groupState.setLength(index + 2);
	_groupState.set(2 * prog._index, _groupStart[prog._index]);
	_groupState.set(2 * prog._index + 1, cursor.getIndex());
	
	_groupState.setMatched(prog._index);
	
	prog = prog._rest;
	break;

	// '\nn'
      case Node.RC_GROUP_REF:
        if (! _groupState.isMatched(prog._index))
          return FAIL;
        else {
          int begin = _groupState.get(2 * prog._index);
          length = (_groupState.get(2 * prog._index + 1) - 
                   _groupState.get(2 * prog._index));
          _cb.setLength(0);
          cursor.subseq(_cb, begin, begin + length);
          if (cursor.regionMatches(_cb.getBuffer(), 0, length)) {
            prog = prog._rest;
          } else
            return FAIL;
        }
	    break;

	// '\nn'
      case Node.RC_GROUP_REF_I:
        if (! _groupState.isMatched(prog._index))
          return FAIL;
        else {
          int begin = _groupState.get(2 * prog._index);
          length = (_groupState.get(2 * prog._index + 1) - 
                   _groupState.get(2 * prog._index));

          _cb.setLength(0);
          cursor.subseq(_cb, begin, begin + length);
          if (cursor.regionMatchesIgnoreCase(_cb.getBuffer(), 0, length)) {
            cursor.skip(length);
            prog = prog._rest;
          } else
            return FAIL;
        }
	    break;

      case Node.RC_LOOP_INIT:
	_loopCount[prog._rest._index] = 0;
	_loopTail[prog._rest._index] = -1;
	prog = prog._rest;
	break;

	// '*' '{n,m}' '+' '?' matches as much as possible
      case Node.RC_LOOP:
        oldState = _groupState.copy();
        tail = cursor.getIndex();

        int matchedCount = -1;
        int matchedTail = tail;
        GroupState matchedGroupState = oldState;
        
        int loopTail = -1;
        
        for (i = 0; i < prog._max; i++) {
          if (cursor.current() == cursor.DONE)
            break;

          if (loopTail == cursor.getIndex())
            break;

          loopTail = cursor.getIndex();
          
          if ((value = match(prog._branch, cursor)) == FAIL) {
            break;
          }
          
          int lastPos = cursor.getIndex();
          GroupState innerState = _groupState.copy();
          
          value = match(prog._rest, cursor);

          if (value == FAIL || i + 1 < prog._min) {
          }
          else {
            matchedCount = i + 1;
            matchedTail = cursor.getIndex();
            
            freeGroupState(matchedGroupState);
            matchedGroupState = _groupState.copy();
          }
          
          cursor.setIndex(lastPos);
          setGroupState(innerState);
        }

        if (prog._min <= matchedCount) {
          cursor.setIndex(matchedTail);

          freeGroupState(oldState);
          setGroupState(matchedGroupState);
          
          return SUCCESS;
        }
        else if (prog._min == 0) {
          cursor.setIndex(tail);
          setGroupState(oldState);
          
          prog = prog._rest;
        }
        else {
          cursor.setIndex(tail);
          setGroupState(oldState);
          
          return FAIL;
        }


        /*
	tail = cursor.getIndex();
	if (_loopCount[prog._index]++ < prog._min)
	  prog = prog._branch;
	else if (_loopCount[prog._index] > prog._max)
	  prog = prog._rest;
	else if (_loopTail[prog._index] == tail)
	  return FAIL;
	else {
	  _loopTail[prog._index] = tail;
	  int match = _group.size();

	  if ((ch = cursor.current()) == cursor.DONE)
	    prog = prog._rest;
	  else if (prog._set != null && prog._set.match(ch))
	    prog = prog._branch;
	  else {
	     oldState = _groupState.copy();

	    if ((value = match(prog._branch, cursor)) != FAIL) {
	      return value;
	    }
        else {
          _groupState = oldState;
          
          cursor.setIndex(tail);
          _group.setLength(match);
          prog = prog._rest;
        }
	  }
	}
	
	*/
        break;

    
    // '*' '{n,m}' '+' '?' possessively matches as much as possible
      case Node.RC_LOOP_LONG:
        oldState = _groupState.copy();
        tail = cursor.getIndex();
        
        for (i = 0; i < prog._max; i++) {
          if (cursor.current() == cursor.DONE)
            break;

          int lastPos = cursor.getIndex();
          GroupState innerState = _groupState.copy();
          
          if ((value = match(prog._branch, cursor)) == FAIL) {
            cursor.setIndex(lastPos);
            setGroupState(innerState);
            
            break;
          }
          else
            freeGroupState(innerState);
        }

        if (prog._min <= i) {
          freeGroupState(oldState);
          prog = prog._rest;
        }
        else {
          cursor.setIndex(tail);
          setGroupState(oldState);
          
          return FAIL;
        }
        
        /*
        tail = cursor.getIndex();

        if (_loopCount[prog._index] > prog._max)
          prog = prog._rest;
        else if (_loopTail[prog._index] == tail)
          return FAIL;
        else {
          _loopTail[prog._index] = tail;
          int match = _group.size();

          oldState = _groupState.copy();
          
          if (match(prog._branch, cursor) != FAIL) {
            cursor.setIndex(tail);
          }
          else {
            _groupState = oldState;
            
            if ((ch = cursor.current()) == cursor.DONE)
              prog = prog._rest;
            else if (prog._set != null && prog._set.match(ch))
              prog = prog._branch;
            else {
              cursor.setIndex(tail);
              _group.setLength(match);
              prog = prog._rest;
            }
          }
        }
        */
    break;

	// '*' '{n,m}' '+' '?' matches as little as possible
      case Node.RC_LOOP_SHORT:
        oldState = _groupState.copy();
        tail = cursor.getIndex();
        
        for (i = 0; i < prog._max; i++) {
          if (cursor.current() == cursor.DONE)
            break;

          if ((value = match(prog._branch, cursor)) == FAIL) {
            break;
          }
          
          int lastPos = cursor.getIndex();
          GroupState innerState = _groupState.copy();
          
          value = match(prog._rest, cursor);

          if (value == FAIL || i + 1 < prog._min) {
          }
          else {
            // matched
            freeGroupState(oldState);
            freeGroupState(innerState);
            return SUCCESS;
          }
          
          cursor.setIndex(lastPos);
          setGroupState(innerState);
        }

        if (prog._min == 0) {
          cursor.setIndex(tail);
          setGroupState(oldState);
          
          prog = prog._rest;
        }
        else {
          cursor.setIndex(tail);
          setGroupState(oldState);
          
          return FAIL;
        }
        
        /*
	tail = cursor.getIndex();
	if (_loopCount[prog._index]++ < prog._min)
	  prog = prog._branch;
	else if (_loopCount[prog._index] > prog._max)
	  prog = prog._rest;
	else {
	  oldState = _groupState.copy();
	  
	  if ((value = match(prog._rest, cursor)) != FAIL) {
	      return value;
      }

	  _groupState = oldState;
	  
      if (_loopTail[prog._index] == tail)
        return FAIL;
      else {
        _loopTail[prog._index] = tail;
        cursor.setIndex(tail);

        prog = prog._branch;
      }
	}
	*/
	break;

	// The first mismatch for loop unique is necessarily a match
	// for the successor, e.g. a*b as opposed to a*ab
	// XXX: this needs to be changed to be like the or.
      case Node.RC_LOOP_UNIQUE:

	if (_loopCount[prog._index]++ < prog._min) {
	  prog = prog._branch;
	}
	else if (_loopCount[prog._index] > prog._max)
	  prog = prog._rest;
	else if ((ch = cursor.current()) == cursor.DONE)
	  prog = prog._rest;
	else if (prog._set.match(ch)) {
	  prog = prog._branch;
	}
	else
	  prog = prog._rest;
	break;

      case Node.RC_OR:
	_match = _groupState.size();
	tail = cursor.getIndex();
	if ((value = match(prog._branch, cursor)) != FAIL)
	  return value;
	cursor.setIndex(tail);
	_groupState.setLength(_match);
	prog = prog._rest;
	break;

	// Here we can tell by the first character if the match works
      case Node.RC_OR_UNIQUE:
	if ((ch = cursor.current()) == cursor.DONE)
	  prog = prog._rest;
	else if (prog._set.match(ch)) {
	  prog = prog._branch;
        }
	else {
	  prog = prog._rest;
        }
	break;

	// The peek pattern must match but isn't included in the real match
      case Node.RC_POS_LOOKAHEAD:
	tail = cursor.getIndex();
	if (match(prog._branch, cursor) == FAIL)
	  return FAIL;
	cursor.setIndex(tail);
	prog = prog._rest;
	break;

	// The peek pattern must not match and isn't included in the real match
      case Node.RC_NEG_LOOKAHEAD:
	tail = cursor.getIndex();
	if (match(prog._branch, cursor) != FAIL)
	  return FAIL;
	cursor.setIndex(tail);
	prog = prog._rest;
	break;

	 // The previous pattern must match and isn't included in the real match
      case Node.RC_POS_LOOKBEHIND:
        tail = cursor.getIndex();
        oldState = _groupState.copy();
        
        length = prog._length;
        
        if (tail < length)
          return FAIL;
        
        cursor.setIndex(tail - length);
        
        if (match(prog._branch, cursor) == FAIL) {
          cursor.setIndex(tail);
          setGroupState(oldState);

          return FAIL;
        }

        cursor.setIndex(tail);
        setGroupState(oldState);

        prog = prog._rest;
        break;
        
      // The previous pattern must not match and isn't included in the real match
      case Node.RC_NEG_LOOKBEHIND:
        tail = cursor.getIndex();
        oldState = _groupState.copy();
        
        length = prog._branch._length;
        
        if (tail >= length) {
          cursor.setIndex(tail - length);
          
          if (match(prog._branch, cursor) != FAIL) {
            cursor.setIndex(tail);
            setGroupState(oldState);
            
            return FAIL;
          }
        }
        
        cursor.setIndex(tail);
        setGroupState(oldState);

        prog = prog._rest;
        break;
        
      case Node.RC_LOOKBEHIND_OR:
        tail = cursor.getIndex();
        oldState = _groupState.copy();
        
        int defaultLength = prog._length;
        boolean isMatched = false;
        
        Node node = prog._branch;

        if ((value = match(node, cursor)) != FAIL) {
          return value;
        }
        else {
          setGroupState(oldState);
          
          for (node = prog._rest; node != null && node._code != Node.RC_END; node = node._rest) {
            cursor.setIndex(tail);
            oldState = _groupState.copy();
            
            cursor.setIndex(tail + defaultLength - node._length);
            
            if (match(node, cursor) != FAIL) {
              isMatched = true;
              break;
            }
            
            cursor.setIndex(tail);
            setGroupState(oldState);
          }
        }
        
        if (! isMatched)
          return FAIL;
        
        prog = node._rest;
        break;

	   // Conditional subpattern
      case Node.RC_COND:
        tail = cursor.getIndex();

        if (_groupState.isMatched(prog._index)) {
          if (match(prog._branch, cursor) == FAIL)
            return FAIL;
        }
        else if (prog._nBranch != null) {
          if (match(prog._nBranch, cursor) == FAIL)
            return FAIL;
        }

        prog = prog._rest;
        break;

	// Beginning of line
      case Node.RC_BLINE:
	if (cursor.getIndex() == _start)
	  prog = prog._rest;
	else if (cursor.previous() == '\n') {
	  cursor.next();
	  prog = prog._rest;
	}
	else {
	  cursor.next();
	  return FAIL;
	}
	break;

	// End of line
      case Node.RC_ELINE:
	if (cursor.current() == cursor.DONE || cursor.current() == '\n')
	  prog = prog._rest;	  // XXX: return on success?
	else
	  return FAIL;
	break;

	// Beginning of match
      case Node.RC_GSTRING:
	if (cursor.getIndex() == _first)
	  prog = prog._rest;
	else
	  return FAIL;
	break;

	// beginning of string
      case Node.RC_BSTRING:
	if (cursor.getIndex() == _start)
	  prog = prog._rest;
	else
	  return FAIL;
	break;

    // end of string
      case Node.RC_ESTRING:
    if (cursor.current() == cursor.DONE)
      prog = prog._rest;      // XXX: return on success?
    else
      return FAIL;
    break;
    
	// end of string or newline at end of string
      case Node.RC_ENSTRING:
        ch = cursor.current();
        tail = cursor.getIndex();
	if (ch == '\n' && tail == cursor.getEndIndex() - 1 ||
        ch == cursor.DONE)
	  prog = prog._rest;	  // XXX: return on success?
	else
	  return FAIL;
	break;

      case Node.RC_WORD:
	tail = cursor.getIndex();
	if ((tail != _start && RegexpSet.WORD.match(cursor.prev())) !=
	    (cursor.current() != cursor.DONE && 
	     RegexpSet.WORD.match(cursor.current())))
	  prog = prog._rest;
	else
	  return FAIL;
	break;

      case Node.RC_NWORD:
        tail = cursor.getIndex();
      
	if ((tail != _start && RegexpSet.WORD.match(cursor.prev())) ==
	    (cursor.current() != cursor.DONE &&
	     RegexpSet.WORD.match(cursor.current())))
	  prog = prog._rest;
	else
	  return FAIL;
	break;
    
      case Node.RC_UNICODE:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        if (Character.getType(ch) == prog._unicodeCategory)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_NUNICODE:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        if (Character.getType(ch) != prog._unicodeCategory)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_C:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value == Character.CONTROL ||
            value == Character.FORMAT ||
            value == Character.UNASSIGNED ||
            value == Character.PRIVATE_USE ||
            value == Character.SURROGATE)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_NC:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;

        value = Character.getType(ch);
        
        if (value != Character.CONTROL &&
            value != Character.FORMAT &&
            value != Character.UNASSIGNED &&
            value != Character.PRIVATE_USE &&
            value != Character.SURROGATE)
          prog = prog._rest;
        else
          return FAIL;
        break;

      case Node.RC_L:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value == Character.LOWERCASE_LETTER ||
            value == Character.MODIFIER_LETTER ||
            value == Character.OTHER_LETTER ||
            value == Character.TITLECASE_LETTER ||
            value == Character.UPPERCASE_LETTER)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_NL:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value != Character.LOWERCASE_LETTER &&
            value != Character.MODIFIER_LETTER &&
            value != Character.OTHER_LETTER &&
            value != Character.TITLECASE_LETTER &&
            value != Character.UPPERCASE_LETTER)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_M:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value == Character.COMBINING_SPACING_MARK ||
            value == Character.ENCLOSING_MARK ||
            value == Character.NON_SPACING_MARK)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_NM:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value != Character.COMBINING_SPACING_MARK &&
            value != Character.ENCLOSING_MARK &&
            value != Character.NON_SPACING_MARK)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_N:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value == Character.DECIMAL_DIGIT_NUMBER ||
            value == Character.LETTER_NUMBER ||
            value == Character.OTHER_NUMBER)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_NN:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value != Character.DECIMAL_DIGIT_NUMBER &&
            value != Character.LETTER_NUMBER &&
            value != Character.OTHER_NUMBER)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_P:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value == Character.CONNECTOR_PUNCTUATION ||
            value == Character.DASH_PUNCTUATION ||
            value == Character.END_PUNCTUATION ||
            value == Character.FINAL_QUOTE_PUNCTUATION ||
            value == Character.INITIAL_QUOTE_PUNCTUATION ||
            value == Character.OTHER_PUNCTUATION ||
            value == Character.START_PUNCTUATION)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_NP:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value != Character.CONNECTOR_PUNCTUATION &&
            value != Character.DASH_PUNCTUATION &&
            value != Character.END_PUNCTUATION &&
            value != Character.FINAL_QUOTE_PUNCTUATION &&
            value != Character.INITIAL_QUOTE_PUNCTUATION &&
            value != Character.OTHER_PUNCTUATION &&
            value != Character.START_PUNCTUATION)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_S:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value == Character.CURRENCY_SYMBOL ||
            value == Character.MODIFIER_SYMBOL ||
            value == Character.MATH_SYMBOL ||
            value == Character.OTHER_SYMBOL)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_NS:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value != Character.CURRENCY_SYMBOL &&
            value != Character.MODIFIER_SYMBOL &&
            value != Character.MATH_SYMBOL &&
            value != Character.OTHER_SYMBOL)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_Z:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value == Character.LINE_SEPARATOR ||
            value == Character.PARAGRAPH_SEPARATOR ||
            value == Character.SPACE_SEPARATOR)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_NZ:
        if ((ch = cursor.read()) == cursor.DONE)
          return FAIL;
        
        value = Character.getType(ch);
        
        if (value != Character.LINE_SEPARATOR &&
            value != Character.PARAGRAPH_SEPARATOR &&
            value != Character.SPACE_SEPARATOR)
          prog = prog._rest;
        else
          return FAIL;
        break;
        
      case Node.RC_CHAR_CLASS:
        switch (prog._branch._code) {
          case Node.RC_SPACE:
            if ((ch = cursor.read()) == cursor.DONE)
              return FAIL;
            
            //value 
            
            break;
          
        }
        return SUCCESS;
        
      default:
        throw new RuntimeException("Internal error: " + Node.code(prog._code));
      }
    }

    return SUCCESS;
  }
  
  private void setGroupState(GroupState newState)
  {
    newState.free(_groupState);
    
    _groupState = newState;
  }
  
  private void freeGroupState(GroupState oldState)
  {
    _groupState.free(oldState);
  }
  
  private GroupState copyGroupState()
  {
    return _groupState.copy();
  }
  
  public int getBegin(int i)
  {
    if (_groupState.size() < 2 * i)
      return 0;
    else
      return _groupState.get(2 * i);
  }

  public int getEnd(int i)
  {
    if (_groupState.size() < 2 * i + 1)
      return 0;
    else
      return _groupState.get(2 * i + 1);
  }

  public int length() { return _groupState.size() / 2; }

  public boolean match(String string)
  {
    return exec(string, 0) != FAIL;
  }

  /*
  public ArrayList split(String string)
  {
    ArrayList<String> list = new ArrayList<String>();

    int i = 0;
    int length = string.length();
    
    while (exec(string, i) != FAIL) {
      if (i < getBegin(0))
        list.add(string.substring(i, getBegin(0)));
      
      list.add(string.substring(getBegin(0), getEnd(0)));
      
      i = getEnd(0);
    }
    
    if (i < length)
      list.add(string.substring(i));

    return list;
  }
  */
  
  /*
  public String replace(String string, String replace)
  {
    int i = 0;
    int length = string.length();
    
    if (length == 0 && _pattern.length() == 0)
      return replace;
    
    _cb.setLength(0);
    
    init(string);
    
    while (exec() != FAIL) {
      _cb.append(string, i, getBegin(0) - i);
      
      _cb.append(replace);
      
      i = getEnd(0);
    }

    if (i < length)
      _cb.append(string, i, length - i);
    
    return _cb.toString();
  }
  */
  
  /*
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
    return replace(test, replace, _cb).toString();
  }
  */

  /*
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
  */

  /*
  public String fill(String test, String replace)
  {
    return replace(test, replace, _cb).toString();
  }
  */

  //
  // Matcher methods
  //
  
  public boolean find()
  {
    return exec() != FAIL;
  }
  
  public boolean find(int first)
  {
    return exec(first) != FAIL;
  }
  
  public int start()
  {
    return getBegin(0);
  }
  
  public int start(int i)
  {
    return getBegin(i);
  }
  
  public int end()
  {
    return getEnd(0);
  }
  
  public int end(int i)
  {
    return getEnd(i);
  }
  
  public int groupCount()
  {
    return _nGroup;
  }
  
  public boolean isMatchedGroup(int i)
  {
    return _groupState.isMatched(i);
  }
  
  public StringValue group(Env env)
  {
    return group(env, 0);
  }

  public StringValue group(Env env, int i)
  {
    int begin = getBegin(i);
    int end = getEnd(i);
    
    if (_isUnicode)
      return (UnicodeValue)_subject.substring(begin, end);
    else if (_isUTF8)
      return _subject.substring(begin, end).toBinaryValue(env, "UTF-8");
    else
      return _subject.substring(begin, end).toBinaryValue(env);
  }
  
  public StringValue getGroupName(int i)
  {
    if (i >= _groupNames.length)
      return null;
    else
      return _groupNames[i];
  }
  
  public Regexp clone()
  {
    Regexp regexp = new Regexp();
    
    regexp._pattern = _pattern;
    regexp._prog = _prog;
    
    regexp._ignoreCase = _ignoreCase;
    regexp._isGlobal = _isGlobal;

    regexp._minLength = _minLength;
    regexp._prefix = _prefix;

    regexp._nGroup = _nGroup;
    regexp._nLoop = _nLoop;
    regexp._groupStart = new int[_nGroup + 1];
    regexp._loopCount = new int[_nLoop];
    regexp._loopTail = new int[_nLoop];
    regexp._cb = new CharBuffer();
    regexp._stringCursor = new StringCharCursor("");
    regexp._groupState = new GroupState();
    
    regexp._groupNames = _groupNames;

    regexp._isUnicode = _isUnicode;
    regexp._isUTF8 = _isUTF8;
    regexp._isEval = _isEval;
    
    return regexp;
  }
  
  public String toString()
  {
    return "[Regexp " + _pattern + "]";
  }
}
