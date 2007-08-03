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
