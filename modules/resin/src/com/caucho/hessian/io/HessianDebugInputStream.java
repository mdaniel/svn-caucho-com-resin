/*
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package com.caucho.hessian.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Debugging input stream for Hessian requests.
 */
public class HessianDebugInputStream extends InputStream {
  private InputStream _is;
  private PrintWriter _dbg;

  private State _state;
  private ArrayList<State> _stateStack = new ArrayList<State>();

  private int _refId;
  
  /**
   * Creates an uninitialized Hessian input stream.
   */
  public HessianDebugInputStream(InputStream is, PrintWriter dbg)
  {
    _is = is;
    _dbg = dbg;

    _state = new InitialState();
  }

  /**
   * Reads a character.
   */
  public int read()
    throws IOException
  {
    int ch;

    InputStream is = _is;

    if (is == null)
      return -1;
    else
      ch = is.read();

    _state = _state.next(ch);

    return ch;
  }

  /**
   * closes the stream.
   */
  public void close()
    throws IOException
  {
    InputStream is = _is;
    _is = null;
    _dbg = null;

    if (is != null)
      is.close();
  }

  void println(String string)
  {
    println(0, string);
  }

  void println(int depth, String string)
  {
    for (int i = 0; i < _stateStack.size() + depth - 1; i++)
      _dbg.print("  ");

    _dbg.println(string);
    _dbg.flush();
  }

  void pushStack(State state)
  {
    _stateStack.add(state);
  }

  State popStack()
  {
    return _stateStack.remove(_stateStack.size() - 1);
  }

  abstract class State {
    abstract State next(int ch);
    
    protected State nextObject(int ch)
    {
      switch (ch) {
      case -1:
	return this;
	
      case 'N':
	println(1, "N: null");
	return this;
	
      case 'T':
	println(1, "T: true");
	return this;
	
      case 'F':
	println(1, "F: false");
	return this;

      case 'I':
	pushStack(this);
	return new IntegerState("I");

      case 'R':
	pushStack(this);
	return new IntegerState("Ref");

      case 'r':
	pushStack(this);
	return new RemoteState();

      case 'L':
	pushStack(this);
	return new LongState();

      case 'd':
	pushStack(this);
	return new DateState();

      case 'D':
	pushStack(this);
	return new DoubleState();

      case 'S': case 'X':
	pushStack(this);
	return new StringState('S', true);

      case 's': case 'x':
	pushStack(this);
	return new StringState('S', false);

      case 'B':
	pushStack(this);
	return new ByteState(true);

      case 'b':
	pushStack(this);
	return new ByteState(false);

      case 'M':
	println(1, "M: map/object #" + _refId++);
	pushStack(this);
	return new MapState();

      case 'V':
	println(1, "V: list #" + _refId++);
	pushStack(this);
	return new ListState();

      case ' ': case '\n': case '\r': case '\t':
	return this;
	
      default:
	println(String.valueOf((char) ch) + ": unexpected character");
	return this;
      }
    }
  }
  
  class InitialState extends State {
    State next(int ch)
    {
      if (ch == 'r') {
	pushStack(this);
	return new ReplyState();
      }
      else if (ch == 'c') {
	pushStack(this);
	return new CallState();
      }
      else
	return nextObject(ch);
    }
  }
  
  class ObjectState extends State {
    State next(int ch)
    {
      return nextObject(ch);
    }
  }
  
  class IntegerState extends State {
    String _typeCode;
    
    int _length;
    int _value;

    IntegerState(String typeCode)
    {
      _typeCode = typeCode;
    }
    
    State next(int ch)
    {
      _value = 256 * _value + (ch & 0xff);

      if (++_length == 4) {
	println(_typeCode + ": " + _value);
	return popStack();
      }
      else
	return this;
    }
  }
  
  class LongState extends State {
    int _length;
    long _value;
    
    State next(int ch)
    {
      _value = 256 * _value + (ch & 0xff);

      if (++_length == 8) {
	println("L: " + _value);
	return popStack();
      }
      else
	return this;
    }
  }
  
  class DateState extends State {
    int _length;
    long _value;
    
    State next(int ch)
    {
      _value = 256 * _value + (ch & 0xff);

      if (++_length == 8) {
	println("d: " + new java.util.Date(_value));
	return popStack();
      }
      else
	return this;
    }
  }
  
  class DoubleState extends State {
    int _length;
    long _value;
    
    State next(int ch)
    {
      _value = 256 * _value + (ch & 0xff);

      if (++_length == 8) {
	println("D: " + Double.longBitsToDouble(_value));
	return popStack();
      }
      else
	return this;
    }
  }
  
  class StringState extends State {
    private static final int TOP = 0;
    private static final int UTF_2_1 = 1;
    private static final int UTF_3_1 = 2;
    private static final int UTF_3_2 = 3;

    char _typeCode;
    
    StringBuilder _value = new StringBuilder();
    int _lengthIndex;
    int _length;
    boolean _isLastChunk;
    
    int _utfState;
    char _ch;

    StringState(char typeCode, boolean isLastChunk)
    {
      _typeCode = typeCode;
      _isLastChunk = isLastChunk;
    }
    
    State next(int ch)
    {
      if (_lengthIndex < 2) {
	_length = 256 * _length + (ch & 0xff);
	
	if (++_lengthIndex == 2 && _length == 0 && _isLastChunk) {
	  println(_typeCode + ": " + _value);
	  return popStack();
	}
	else
	  return this;
      }
      else if (_length == 0) {
	if (ch == 's' || ch == 'x') {
	  _isLastChunk = false;
	  _lengthIndex = 0;
	  return this;
	}
	else if (ch == 'S' || ch == 'X') {
	  _isLastChunk = true;
	  _lengthIndex = 0;
	  return this;
	}
	else {
	  println(String.valueOf((char) ch) + ": unexpected character");
	  return popStack();
	}
      }

      switch (_utfState) {
      case TOP:
	if (ch < 0x80) {
	  _length--;

	  _value.append((char) ch);
	}
	else if (ch < 0xe0) {
	  _ch = (char) ((ch & 0x1f) << 6);
	  _utfState = UTF_2_1;
	}
	else {
	  _ch = (char) ((ch & 0xf) << 12);
	  _utfState = UTF_3_1;
	}
	break;

      case UTF_2_1:
      case UTF_3_2:
	_ch += ch & 0x3f;
	_value.append(_ch);
	_length--;
	_utfState = TOP;
	break;

      case UTF_3_1:
	_ch += (char) ((ch & 0x3f) << 6);
	_utfState = UTF_3_2;
	break;
      }

      if (_length == 0) {
	println(_typeCode + ": " + _value);
	return popStack();
      }
      else
	return this;
    }
  }
  
  class ByteState extends State {
    int _lengthIndex;
    int _length;
    boolean _isLastChunk;

    ByteState(boolean isLastChunk)
    {
      _isLastChunk = isLastChunk;
    }
    
    State next(int ch)
    {
      if (_lengthIndex < 2) {
	_length = 256 * _length + (ch & 0xff);
	
	if (++_lengthIndex == 2) {
	  if (_isLastChunk)
	    println("B: " + _length);
	  else
	    println("b: " + _length);
	}

	if (_lengthIndex == 2 && _length == 0 && _isLastChunk) {
	  return popStack();
	}
	else
	  return this;
      }
      else if (_length == 0) {
	if (ch == 'b') {
	  _isLastChunk = false;
	  _lengthIndex = 0;
	  return this;
	}
	else if (ch == 'B') {
	  _isLastChunk = true;
	  _lengthIndex = 0;
	  return this;
	}
	else {
	  println(String.valueOf((char) ch) + ": unexpected character");
	  return popStack();
	}
      }

      _length--;

      if (_length == 0) {
	return popStack();
      }
      else
	return this;
    }
  }
  
  class MapState extends State {
    private static final int TYPE = 0;
    private static final int KEY = 1;
    private static final int VALUE = 2;

    private int _state;
    
    State next(int ch)
    {
      switch (_state) {
      case TYPE:
	if (ch == 't') {
	  _state = KEY;
	  pushStack(this);
	  return new StringState('t', true);
	}
	else if (ch == 'z') {
	  return popStack();
	}
	else {
	  _state = VALUE;
	  return nextObject(ch);
	}
	
      case KEY:
	if (ch == 'z') {
	  return popStack();
	}
	else {
	  _state = VALUE;
	  return nextObject(ch);
	}
	
      case VALUE:
	_state = KEY;
	return nextObject(ch);

      default:
	throw new IllegalStateException();
      }
    }
  }
  
  class ListState extends State {
    private static final int TYPE = 0;
    private static final int LENGTH = 1;
    private static final int VALUE = 2;

    private int _state;
    
    State next(int ch)
    {
      switch (_state) {
      case TYPE:
	if (ch == 'z') {
	  return popStack();
	}
	else if (ch == 't') {
	  _state = LENGTH;
	  pushStack(this);
	  return new StringState('t', true);
	}
	else if (ch == 'l') {
	  _state = VALUE;
	  pushStack(this);
	  return new IntegerState("l");
	}
	else {
	  _state = VALUE;
	  return nextObject(ch);
	}
	
      case LENGTH:
	if (ch == 'z') {
	  return popStack();
	}
	else if (ch == 'l') {
	  _state = VALUE;
	  pushStack(this);
	  return new IntegerState("len");
	}
	else {
	  _state = VALUE;
	  return nextObject(ch);
	}
	
      case VALUE:
	if (ch == 'z') {
	  return popStack();
	}
	else {
	  return nextObject(ch);
	}

      default:
	throw new IllegalStateException();
      }
    }
  }
  
  class CallState extends State {
    private static final int MAJOR = 0;
    private static final int MINOR = 1;
    private static final int HEADER = 2;
    private static final int VALUE = 3;
    private static final int ARG = 4;

    private int _state;
    private int _major;
    private int _minor;
    
    State next(int ch)
    {
      switch (_state) {
      case MAJOR:
	_major = ch;
	_state = MINOR;
	return this;
	
      case MINOR:
	_minor = ch;
	_state = HEADER;
	println(-1, "call " + _major + "." + _minor);
	return this;
	
      case HEADER:
	if (ch == 'H') {
	  _state = VALUE;
	  pushStack(this);
	  return new StringState('H', true);
	}
 	else if (ch == 'm') {
	  _state = ARG;
	  pushStack(this);
	  return new StringState('m', true);
	}
	else {
	  println((char) ch + ": unexpected char");
	  return popStack();
	}
	
      case VALUE:
	_state = HEADER;
	return nextObject(ch);
	
      case ARG:
	if (ch == 'z')
	  return popStack();
	else
	  return nextObject(ch);

      default:
	throw new IllegalStateException();
      }
    }
  }
  
  class ReplyState extends State {
    private static final int MAJOR = 0;
    private static final int MINOR = 1;
    private static final int HEADER = 2;
    private static final int VALUE = 3;
    private static final int END = 4;

    private int _state;
    private int _major;
    private int _minor;
    
    State next(int ch)
    {
      switch (_state) {
      case MAJOR:
	if (ch == 't' || ch == 'S')
	  return new RemoteState().next(ch);
	
	_major = ch;
	_state = MINOR;
	return this;
	
      case MINOR:
	_minor = ch;
	_state = HEADER;
	println(-1, "reply " + _major + "." + _minor);
	return this;
	
      case HEADER:
	if (ch == 'H') {
	  _state = VALUE;
	  pushStack(this);
	  return new StringState('H', true);
	}
	else if (ch == 'f') {
	  println("f: fault");
	  _state = END;
	  return new MapState();
	}
 	else {
	  _state = END;
	  return nextObject(ch);
	}
	
      case VALUE:
	_state = HEADER;
	return nextObject(ch);
	
      case END:
	return popStack().next(ch);

      default:
	throw new IllegalStateException();
      }
    }
  }
  
  class RemoteState extends State {
    private static final int TYPE = 0;
    private static final int VALUE = 1;
    private static final int END = 2;

    private int _state;
    private int _major;
    private int _minor;
    
    State next(int ch)
    {
      switch (_state) {
      case TYPE:
	println(-1, "remote");
	if (ch == 't') {
	  _state = VALUE;
	  pushStack(this);
	  return new StringState('t', false);
	}
	else {
	  _state = END;
	  return nextObject(ch);
	}

      case VALUE:
	_state = END;
	return nextObject(ch);

      case END:
	return popStack().next(ch);

      default:
	throw new IllegalStateException();
      }
    }
  }
}
