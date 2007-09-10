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

import com.caucho.util.*;

class Node {
  static final int RC_END = 0;
  static final int RC_NULL = 1;
  static final int RC_STRING = 2;
  static final int RC_SET = 3;
  static final int RC_NSET = 4;
  static final int RC_BEG_GROUP = 5;
  static final int RC_END_GROUP = 6;
  
  static final int RC_GROUP_REF = 7;
  static final int RC_LOOP = 8;
  static final int RC_LOOP_INIT = 9;
  static final int RC_LOOP_SHORT = 10;
  static final int RC_LOOP_UNIQUE = 11;
  static final int RC_LOOP_SHORT_UNIQUE = 12;
  static final int RC_LOOP_LONG = 13;
  
  static final int RC_OR = 64;
  static final int RC_OR_UNIQUE = 65;
  static final int RC_POS_LOOKAHEAD = 66;
  static final int RC_NEG_LOOKAHEAD = 67;
  static final int RC_POS_LOOKBEHIND = 68;
  static final int RC_NEG_LOOKBEHIND = 69;
  static final int RC_LOOKBEHIND_OR = 70;
  
  static final int RC_WORD = 73;
  static final int RC_NWORD = 74;
  static final int RC_BLINE = 75;
  static final int RC_ELINE = 76;
  static final int RC_BSTRING = 77;
  static final int RC_ESTRING = 78;
  static final int RC_ENSTRING = 79;
  static final int RC_GSTRING = 80;
  
  // conditionals
  static final int RC_COND = 81;
  
  // ignore case
  static final int RC_STRING_I = 128;
  static final int RC_SET_I = 129;
  static final int RC_NSET_I = 130;
  static final int RC_GROUP_REF_I = 131;

  static final int RC_LEXEME = 256;
  
  // unicode properties
  static final int RC_UNICODE = 512;
  static final int RC_NUNICODE = 513;
  
  // unicode properties sets
  static final int RC_C = 1024;
  static final int RC_L = 1025;
  static final int RC_M = 1026;
  static final int RC_N = 1027;
  static final int RC_P = 1028;
  static final int RC_S = 1029;
  static final int RC_Z = 1030;
  
  // negated unicode properties sets
  static final int RC_NC = 1031;
  static final int RC_NL = 1032;
  static final int RC_NM = 1033;
  static final int RC_NN = 1034;
  static final int RC_NP = 1035;
  static final int RC_NS = 1036;
  static final int RC_NZ = 1037;
  
  // POSIX character classes
  static final int RC_CHAR_CLASS = 2048;
  static final int RC_ALNUM = 1;
  static final int RC_ALPHA = 2;
  static final int RC_BLANK = 3;
  static final int RC_CNTRL = 4;
  static final int RC_DIGIT = 5;
  static final int RC_GRAPH = 6;
  static final int RC_LOWER = 7;
  static final int RC_PRINT = 8;
  static final int RC_PUNCT = 9;
  static final int RC_SPACE = 10;
  static final int RC_UPPER = 11;
  static final int RC_XDIGIT = 12;
  
  /*
  static final int RC_C = 512;
  static final int RC_CC = 513;
  static final int RC_CF = 514;
  static final int RC_CN = 515;
  static final int RC_CO = 516;
  static final int RC_CS = 517;
  static final int RC_L = 518;
  static final int RC_LL = 519;
  static final int RC_LM = 520;
  static final int RC_LO = 521;
  static final int RC_LT = 522;
  static final int RC_LU = 523;
  static final int RC_M = 524;
  static final int RC_MC = 525;
  static final int RC_ME = 526;
  static final int RC_MN = 527;
  static final int RC_N = 528;
  static final int RC_ND = 529;
  static final int RC_NL = 530;
  static final int RC_NO = 531;
  static final int RC_P = 532;
  static final int RC_PC = 533;
  static final int RC_PD = 534;
  static final int RC_PE = 535;
  static final int RC_PF = 536;
  static final int RC_PI = 537;
  static final int RC_PO = 538;
  static final int RC_PS = 539;
  static final int RC_S = 540;
  static final int RC_SC = 541;
  static final int RC_SK = 542;
  static final int RC_SM = 543;
  static final int RC_SO = 544;
  static final int RC_Z = 545;
  static final int RC_ZL = 546;
  static final int RC_ZP = 547;
  static final int RC_ZS = 548;
  */
  
  static Node END = new Node(RC_END);

  Node _rest;
  int _code;
  
  CharBuffer _string;
  RegexpSet _set;
  int _index;
  int _min;
  int _max;
  Node _branch;
  
  //for conditionals
  Node _condition;
  Node _nBranch;

  //for lookbehind
  int _length;
  
  boolean _mark;
  boolean _printMark;

  byte _unicodeCategory;
  
  public static String code(int code)
  {
    switch (code) {
      case RC_END: return "RC_END";
      case RC_NULL: return "RC_NULL";
      case RC_STRING: return "RC_STRING";
      case RC_SET: return "RC_SET";
      case RC_NSET: return "RC_NSET";
      case RC_BEG_GROUP: return "RC_BEG_GROUP";
      case RC_END_GROUP: return "RC_END_GROUP";
      case RC_GROUP_REF: return "RC_GROUP_REF";
      case RC_LOOP: return "RC_LOOP";
      case RC_LOOP_INIT: return "RC_LOOP_INIT";
      case RC_LOOP_SHORT: return "RC_LOOP_SHORT";
      case RC_LOOP_UNIQUE: return "RC_LOOP_UNIQUE";
      case RC_LOOP_SHORT_UNIQUE: return "RC_LOOP_SHORT_UNIQUE";
      case RC_LOOP_LONG: return "RC_LOOP_LONG";
      case RC_OR: return "RC_OR";
      case RC_OR_UNIQUE: return "RC_OR_UNIQUE";
      case RC_POS_LOOKAHEAD: return "RC_POS_PEEK";
      case RC_NEG_LOOKAHEAD: return "RC_NEG_PEEK";
      case RC_WORD: return "RC_WORD";
      case RC_NWORD: return "RC_NWORD";
      case RC_BLINE: return "RC_BLINE";
      case RC_ELINE: return "RC_ELINE";
      case RC_BSTRING: return "RC_BSTRING";
      case RC_ESTRING: return "RC_ESTRING";
      case RC_ENSTRING: return "RC_ENSTRING";
      case RC_GSTRING: return "RC_GSTRING";
      case RC_COND: return "RC_COND";
      case RC_POS_LOOKBEHIND: return "RC_POS_LOOKBEHIND";
      case RC_NEG_LOOKBEHIND: return "RC_NEG_LOOKBEHIND";
      case RC_LOOKBEHIND_OR: return "RC_LOOKBEHIND_OR";
      case RC_STRING_I: return "RC_STRING_I";
      case RC_SET_I: return "RC_SET_I";
      case RC_NSET_I: return "RC_NSET_I";
      case RC_GROUP_REF_I: return "RC_GROUP_REF_I";
      case RC_LEXEME: return "RC_LEXEME";
      default: return "unknown(" + code + ")";
    }
  }
  
  /**
   * Creates a node with a code
   */
  Node(int code)
  {
    _rest = END;
    _code = code;
  }

  /**
   * Creates a node with a group index
   */
  Node(int code, int index)
  {
    this(code);

    _index = index;
  }

  /**
   * Creates a node with a group index
   */
  Node(int code, Node branch)
  {
    this(code);

    _branch = branch;
  }

  /**
   * Creates a node with a group index
   */
  Node(int code, int index, int min, int max)
  {
    this(code);

    _index = index;
    _min = min;
    _max = max;
  }

  /**
   * Creates a node with a group index
   */
  Node(int code, RegexpSet set)
  {
    this(code);

    _set = set;
    _length = 1;
  }

  /**
   * Creates a node with a string
   */
  Node(CharBuffer buf)
  {
    this(RC_STRING);

    _string = buf;
  }

  /**
   * Replaces the tail of a node.
   */
  static Node replaceTail(Node node, Node tail)
  {
    if (node == null || node._code == RC_END || node == tail)
      return tail;

    if (node._code == RC_OR)
      node._branch = replaceTail(node._branch, tail);

    node._rest = replaceTail(node._rest, tail);

    return node;
  }

  /**
   * Connects lastBegin to the tail, returning the head;
   */
  static Node concat(Node head, Node tail)
  {
    if (head == null || head._code == RC_END)
      return tail;

    Node node = head;
    while (node._rest != null && node._rest._code != RC_END)
      node = node._rest;

    node._rest = tail;

    return head;
  }

  public Object clone()
  {
    Node node = new Node(_code);
    node._rest = _rest;
    node._string = _string;
    node._set = _set;
    node._index = _index;
    node._min = _min;
    node._max = _max;
    node._branch = _branch;
    
    node._length = _length;
    node._unicodeCategory = _unicodeCategory;

    return node;
  }

  public String toString()
  {
    if (_printMark)
      return "...";
    
    _printMark = true;
    try {
    switch (_code) {
    case RC_END:
      return "";
      
    case RC_STRING:
      return _string.toString() + (_rest == null ? "" : _rest.toString());
      
    case RC_OR:
      return "(?:" + _branch + "|" + _rest + ")";
      
    case RC_OR_UNIQUE:
      return "(?:" + _branch + "|!" + _rest + ")";
      
    case RC_ESTRING:
      return "\\Z" + (_rest == null ? "" : _rest.toString());
      
    case RC_LOOP_INIT:
      return _rest.toString();
      
    case RC_LOOP:
      return ("(?:" + _branch + "){" + _min + "," + _max + "}" +
              (_rest == null ? "" : _rest.toString()));
      
    case RC_LOOP_UNIQUE:
      return ("(?:" + _branch + ")!{" + _min + "," + _max + "}" +
              (_rest == null ? "" : _rest.toString()));
      
    case RC_BEG_GROUP:
      return "(" + (_rest == null ? "" : _rest.toString());
      
    case RC_END_GROUP:
      return ")" + (_rest == null ? "" : _rest.toString());
      
    case RC_SET:
      return "[" + _set + "]" + (_rest == null ? "" : _rest.toString());
      
    case RC_NSET:
      return "[^" + _set + "]" + (_rest == null ? "" : _rest.toString());
      
    default:
      return "" + _code + " " + super.toString();
    }
    } finally {
      _printMark = false;
    }
  }

  char charAt(int index)
  {
    return _string.charAt(index);
  }
}
