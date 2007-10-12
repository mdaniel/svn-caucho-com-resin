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

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.*;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.StringBuilderValue;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.UnicodeBuilderValue;
import com.caucho.util.*;

public class RegexpState {
  private static final Logger log
    = Logger.getLogger(RegexpState.class.getName());
  
  private static final L10N L = new L10N(Regexp.class);
  
  public static final int FAIL = -1;
  public static final int SUCCESS = 0;

  StringValue _pattern;
  StringValue _subject;
  
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
  boolean _isPHP5String;
  
  boolean _isUTF8;
  boolean _isEval;
  
  GroupState _groupState = new GroupState();

  int _groupLength;
  int []_groupBegin;
  int []_groupEnd;

  private Regexp _regexp;
  
  public RegexpState(Regexp regexp, StringValue subject)
  {
    this(regexp);
    
    _subject = subject;
  }
  
  public RegexpState(Regexp regexp)
  {
    _regexp = regexp;

    _nGroup = _regexp._nGroup;
    _groupBegin = new int[_nGroup];
    _groupEnd = new int[_nGroup];

    _nLoop = _regexp._nLoop;
    _loopCount = new int[_nLoop];
  }

  public boolean find()
  {
    int minLength = _regexp._minLength;
    int length = _subject.length();

    for (; _first + minLength <= length; _first++) {
      clearGroup();

      int offset = _regexp._prog.match(_subject, _first, this);

      if (offset >= 0) {
	setBegin(0, _first);
	setEnd(0, offset);

	if (_first < offset)
	  _first = offset;
	else
	  _first += 1;
      
	return true;
      }
    }

    _first = _subject.length() + 1;
    return false;
  }

  public boolean find(StringValue subject)
  {
    _subject = subject;
    _first = 0;

    return find();
  }

  public int find(StringValue subject, int first)
  {
    _subject = subject;
    
    clearGroup();

    return _regexp._prog.match(_subject, first, this);
  }
  
  /**
   * XXX: not proper behaviour with /g
   */
  public int exec(StringValue subject, int start)
  { 
    clearGroup();
    
    _start = start;
    _first = start;

    int minLength = _regexp._minLength;
    int end = subject.length() - minLength;

    for (; start <= end; start++) {
      int value = _regexp._prog.match(subject, start, this);

      if (value >= 0) {
	setBegin(0, start);
	setEnd(0, value);
    
	_groupState.setMatched(0);
	
	return start;
      }
    }

    return -1;
  }

  private void clearGroup()
  {
    _groupLength = 0;
  }
  
  void setGroupState(GroupState newState)
  {
    newState.free(_groupState);
    
    _groupState = newState;
  }
  
  void freeGroupState(GroupState oldState)
  {
    _groupState.free(oldState);
  }
  
  GroupState copyGroupState()
  {
    return _groupState.copy();
  }
  
  public int getBegin(int i)
  {
    return _groupBegin[i];
  }

  public int getEnd(int i)
  {
    return _groupEnd[i];
  }
  
  public void setBegin(int i, int v)
  {
    _groupBegin[i] = v;
  }

  public void setEnd(int i, int v)
  {
    _groupEnd[i] = v;
  }

  public int getLength()
  {
    return _groupLength;
  }

  public void setLength(int length)
  {
    _groupLength = length;
  }

  public int length()
  {
    return _groupLength;
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
    return i <= _groupLength;
    // return _groupState.isMatched(i);
  }
  
  public StringValue group(Env env)
  {
    return group(env, 0);
  }

  public StringValue group(Env env, int i)
  {
    int begin = getBegin(i);
    int end = getEnd(i);

    StringValue s = _subject.substring(begin, end);

    return encodeResultString(env, s);
  }
  
  public StringValue getGroupName(int i)
  {
    if (_groupNames == null || _groupNames.length <= i)
      return null;
    else
      return _groupNames[i];
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
    if (_isUnicode)
      return str;
    else if (_isPHP5String)
      return encodePHP5ResultString(env, str);
    else if (_isUTF8)
      return str.toBinaryValue(env, "UTF-8");
    else
      return str.toBinaryValue(env);
  }
  
  private StringValue encodePHP5ResultString(Env env, StringValue str)
  {
    StringBuilderValue sb = new StringBuilderValue();
    
    try {
      byte []bytes;

      if (_isUTF8)
        bytes = str.toString().getBytes("UTF-8");
      else
        bytes = str.toBytes();
      
      sb.append(bytes);
      
      return sb;
    }
    catch (UnsupportedEncodingException e) {
      log.log(Level.FINE, e.getMessage(), e);
      env.error(e);
      
      return null;
    }
  }
}
