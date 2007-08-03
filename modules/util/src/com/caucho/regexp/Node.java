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

package com.caucho.regexp;

import java.util.*;
import com.caucho.util.*;

class Node {
  static final int RC_END = 0;
  static final int RC_NULL = RC_END + 1;
  static final int RC_STRING = RC_NULL + 1;
  static final int RC_SET = RC_STRING + 1;
  static final int RC_NSET = RC_SET + 1;
  static final int RC_BEG_GROUP = RC_NSET + 1;
  static final int RC_END_GROUP = RC_BEG_GROUP + 1;
  static final int RC_GROUP_REF = RC_END_GROUP + 1;
  static final int RC_LOOP = RC_GROUP_REF + 1;
  static final int RC_LOOP_INIT = RC_LOOP + 1;
  static final int RC_LOOP_SHORT = RC_LOOP_INIT + 1;
  static final int RC_LOOP_UNIQUE = RC_LOOP_SHORT + 1;
  static final int RC_LOOP_SHORT_UNIQUE = RC_LOOP_UNIQUE + 1;
  static final int RC_OR = RC_LOOP_SHORT_UNIQUE + 1;
  static final int RC_OR_UNIQUE = RC_OR + 1;
  static final int RC_POS_PEEK = RC_OR_UNIQUE + 1;
  static final int RC_NEG_PEEK = RC_POS_PEEK + 1;
  static final int RC_WORD = RC_NEG_PEEK + 1;
  static final int RC_NWORD = RC_WORD + 1;
  static final int RC_BLINE = RC_NWORD + 1;
  static final int RC_ELINE = RC_BLINE + 1;
  static final int RC_BSTRING = RC_ELINE + 1;
  static final int RC_ESTRING = RC_BSTRING + 1;
  static final int RC_GSTRING = RC_ESTRING + 1;

  // ignore case
  static final int RC_STRING_I = RC_GSTRING + 1;
  static final int RC_SET_I = RC_STRING_I + 1;
  static final int RC_NSET_I = RC_SET_I + 1;
  static final int RC_GROUP_REF_I = RC_NSET_I + 1;

  static final int RC_LEXEME = RC_GROUP_REF_I + 1;

  static Node END = new Node(RC_END);

  Node rest;
  int code;
  
  CharBuffer string;
  RegexpSet set;
  int index;
  int min;
  int max;
  Node branch;

  boolean mark;
  boolean printMark;

  /**
   * Creates a node with a code
   */
  Node(int code)
  {
    this.rest = END;
    this.code = code;
  }

  /**
   * Creates a node with a group index
   */
  Node(int code, int index)
  {
    this(code);

    this.index = index;
  }

  /**
   * Creates a node with a group index
   */
  Node(int code, Node branch)
  {
    this(code);

    this.branch = branch;
  }

  /**
   * Creates a node with a group index
   */
  Node(int code, int index, int min, int max)
  {
    this(code);

    this.index = index;
    this.min = min;
    this.max = max;
  }

  /**
   * Creates a node with a group index
   */
  Node(int code, RegexpSet set)
  {
    this(code);

    this.set = set;
  }

  /**
   * Creates a node with a string
   */
  Node(CharBuffer buf)
  {
    this(RC_STRING);

    this.string = buf;
  }

  /**
   * Replaces the tail of a node.
   */
  static Node replaceTail(Node node, Node tail)
  {
    if (node == null || node.code == RC_END || node == tail)
      return tail;

    if (node.code == RC_OR)
      node.branch = replaceTail(node.branch, tail);

    node.rest = replaceTail(node.rest, tail);

    return node;
  }

  /**
   * Connects lastBegin to the tail, returning the head;
   */
  static Node concat(Node head, Node tail)
  {

    if (head == null || head.code == RC_END)
      return tail;

    Node node = head;
    while (node.rest != null && node.rest.code != RC_END)
      node = node.rest;

    node.rest = tail;

    return head;
  }

  public Object clone()
  {
    Node node = new Node(code);
    node.rest = rest;
    node.string = string;
    node.set = set;
    node.index = index;
    node.min = min;
    node.max = max;
    node.branch = branch;

    return node;
  }

  public String toString()
  {
    if (printMark)
      return "...";
    
    printMark = true;
    try {
    switch (code) {
    case RC_END:
      return "";
      
    case RC_STRING:
      return string.toString() + (rest == null ? "" : rest.toString());
      
    case RC_OR:
      return "(?:" + branch + "|" + rest + ")";
      
    case RC_OR_UNIQUE:
      return "(?:" + branch + "|!" + rest + ")";
      
    case RC_ESTRING:
      return "\\Z" + (rest == null ? "" : rest.toString());
      
    case RC_LOOP_INIT:
      return rest.toString();
      
    case RC_LOOP:
      return ("(?:" + branch + "){" + min + "," + max + "}" +
              (rest == null ? "" : rest.toString()));
      
    case RC_LOOP_UNIQUE:
      return ("(?:" + branch + ")!{" + min + "," + max + "}" +
              (rest == null ? "" : rest.toString()));
      
    case RC_BEG_GROUP:
      return "(" + (rest == null ? "" : rest.toString());
      
    case RC_END_GROUP:
      return ")" + (rest == null ? "" : rest.toString());
      
    case RC_SET:
      return "[" + set + "]" + (rest == null ? "" : rest.toString());
      
    case RC_NSET:
      return "[^" + set + "]" + (rest == null ? "" : rest.toString());
      
    default:
      return "" + code + " " + super.toString();
    }
    } finally {
      printMark = false;
    }
  }

  char charAt(int index)
  {
    return string.charAt(index);
  }
}
