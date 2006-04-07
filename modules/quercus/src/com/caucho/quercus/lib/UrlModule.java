/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.quercus.lib;

import com.caucho.util.L10N;
import com.caucho.util.Base64;
import com.caucho.util.CharBuffer;

import com.caucho.quercus.module.AbstractQuercusModule;

import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.env.LongValue;
import com.caucho.quercus.env.StringValueImpl;
import com.caucho.quercus.env.ArrayValueImpl;

/**
 * PHP URL
 */
public class UrlModule extends AbstractQuercusModule {
  private static final L10N L = new L10N(UrlModule.class);

  /**
   * Encodes base64
   */
  public static String base64_encode(String str)
  {
    CharBuffer cb = new CharBuffer();

    byte []bytes = str.getBytes();

    Base64.encode(cb, bytes, 0, bytes.length);

    return cb.toString();
  }

  /**
   * Decodes base64
   */
  public static String base64_decode(String str)
  {
    return Base64.decode(str);
  }

  /**
   * Creates a http string.
   */
  public String http_build_query(Value value,
				 @Optional String prefix)
  {
    StringBuilder sb = new StringBuilder();

    int index = 0;
    if (value instanceof ArrayValue) {
      ArrayValue array = (ArrayValue) value;

      for (Map.Entry<Value,Value> entry : array.entrySet()) {
	Value keyValue = entry.getKey();
	Value v = entry.getValue();

	String key;

	if (keyValue.isLong())
	  key = prefix + keyValue;
	else
	  key = keyValue.toString();

	if (v instanceof ArrayValue)
	  http_build_query(sb, key, (ArrayValue) v);
	else {
	  if (sb.length() > 0)
	    sb.append('&');

	  sb.append(key);
	  sb.append('=');
	  urlencode(sb, v.toString());
	}
      }
    }

    return sb.toString();
  }

  /**
   * Creates a http string.
   */
  private void http_build_query(StringBuilder sb,
				String prefix,
				ArrayValue array)
  {
    for (Map.Entry<Value,Value> entry : array.entrySet()) {
      Value keyValue = entry.getKey();
      Value v = entry.getValue();

      String key = prefix + '[' + keyValue + ']';

      if (v instanceof ArrayValue)
	http_build_query(sb, key, (ArrayValue) v);
      else {
	if (sb.length() > 0)
	  sb.append('&');

	sb.append(key);
	sb.append('=');
	urlencode(sb, v.toString());
      }
    }
  }

  /**
   * Gets the magic quotes value.
   */
  public static String urlencode(String str)
  {
    StringBuilder sb = new StringBuilder();

    urlencode(sb, str);

    return sb.toString();
  }

  /**
   * Gets the magic quotes value.
   */
  private static void urlencode(StringBuilder sb, String str)
  {
    int len = str.length();

    for (int i = 0; i < len; i++) {
      char ch = str.charAt(i);

      if ('a' <= ch && ch <= 'z')
	sb.append(ch);
      else if ('A' <= ch && ch <= 'Z')
	sb.append(ch);
      else if ('0' <= ch && ch <= '0')
	sb.append(ch);
      else if (ch == '-' || ch == '_' || ch == '.')
	sb.append(ch);
      else if (ch == ' ')
	sb.append('+');
      else {
	sb.append('%');
	sb.append(toHexDigit(ch / 16));
	sb.append(toHexDigit(ch));
      }
    }
  }

  /**
   * Returns the decoded string.
   */
  public static String urldecode(String s)
  {
    int len = s.length();
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      if (ch == '%' && i + 2 < len) {
        int d1 = s.charAt(i + 1);
        int d2 = s.charAt(i + 2);

        int v = 0;

        if ('0' <= d1 && d1 <= '9')
          v = 16 * (d1 - '0');
        else if ('a' <= d1 && d1 <= 'f')
          v = 16 * (d1 - 'a' + 10);
        else if ('A' <= d1 && d1 <= 'F')
          v = 16 * (d1 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        if ('0' <= d2 && d2 <= '9')
          v += (d2 - '0');
        else if ('a' <= d2 && d2 <= 'f')
          v += (d2 - 'a' + 10);
        else if ('A' <= d2 && d2 <= 'F')
          v += (d2 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        i += 2;
        sb.append((char) v);
      }
      else if (ch == '+')
	sb.append(' ');
      else
        sb.append(ch);
    }

    return sb.toString();
  }

  /**
   * Returns the decoded string.
   */
  public static String rawurldecode(String s)
  {
    int len = s.length();
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < len; i++) {
      char ch = s.charAt(i);

      if (ch == '%' && i + 2 < len) {
        int d1 = s.charAt(i + 1);
        int d2 = s.charAt(i + 2);

        int v = 0;

        if ('0' <= d1 && d1 <= '9')
          v = 16 * (d1 - '0');
        else if ('a' <= d1 && d1 <= 'f')
          v = 16 * (d1 - 'a' + 10);
        else if ('A' <= d1 && d1 <= 'F')
          v = 16 * (d1 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        if ('0' <= d2 && d2 <= '9')
          v += (d2 - '0');
        else if ('a' <= d2 && d2 <= 'f')
          v += (d2 - 'a' + 10);
        else if ('A' <= d2 && d2 <= 'F')
          v += (d2 - 'A' + 10);
        else {
          sb.append('%');
          continue;
        }

        i += 2;
        sb.append((char) v);
      }
      else
        sb.append(ch);
    }

    return sb.toString();
  }

  /**
   * Encodes the url
   */
  public static String rawurlencode(String str)
  {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);

      if ('a' <= ch && ch <= 'z' ||
          'A' <= ch && ch <= 'Z' ||
          '0' <= ch && ch <= '9' ||
          ch == '-' || ch == '_' || ch == '.' || ch == ' ') {
        sb.append(ch);
      }
      else {
        sb.append('%');
        sb.append(toHexDigit(ch >> 4));
        sb.append(toHexDigit(ch));
      }
    }

    return sb.toString();
  }

  enum ParseUrlState {
    INIT, USER, PASS, HOST, PORT, PATH, QUERY, FRAGMENT
  };

  /**
   * Parses the URL into an array.
   */
  public static Value parse_url(Env env, String str)
  {
    int i = 0;
    int length = str.length();

    CharBuffer sb = new CharBuffer();

    ArrayValueImpl value = new ArrayValueImpl();

    ParseUrlState state = ParseUrlState.INIT;

    String user = null;

    for (; i < length; i++) {
      char ch = str.charAt(i);

      switch (ch) {
      case ':':
	if (state == ParseUrlState.INIT) {
	  value.put("scheme", sb.toString());
	  sb.clear();

	  if (length <= i + 1 || str.charAt(i + 1) != '/') {
	    state = ParseUrlState.PATH;
	  }
	  else if (length <= i + 2 || str.charAt(i + 2) != '/') {
	    state = ParseUrlState.PATH;
	  }
	  else if (length <= i + 3 || str.charAt(i + 3) != '/') {
	    i += 2;
	    state = ParseUrlState.USER;
	  }
	  else {
	    // file:///foo

	    i += 2;
	    state = ParseUrlState.PATH;
	  }
	}
	else if (state == ParseUrlState.USER) {
	  user = sb.toString();
	  sb.clear();
	  state = ParseUrlState.PASS;
	}
	else if (state == ParseUrlState.HOST) {
	  value.put("host", sb.toString());
	  sb.clear();
	  state = ParseUrlState.PORT;
	}
	else
	  sb.append(ch);
	break;

      case '@':
	if (state == ParseUrlState.USER) {
	  value.put("user", sb.toString());
	  sb.clear();
	  state = ParseUrlState.HOST;
	}
	else if (state == ParseUrlState.PASS) {
	  value.put("user", user);
	  value.put("pass", sb.toString());
	  sb.clear();
	  state = ParseUrlState.HOST;
	}
	else
	  sb.append(ch);
	break;

      case '/':
	if (state == ParseUrlState.USER || state == ParseUrlState.HOST) {
	  value.put("host", sb.toString());
	  sb.clear();
	  state = ParseUrlState.PATH;
	  sb.append(ch);
	}
	else if (state == ParseUrlState.PASS) {
	  value.put("host", user);
	  value.put(new StringValueImpl("port"),
		    new LongValue(new StringValueImpl(sb.toString()).toLong()));
	  sb.clear();
	  state = ParseUrlState.PATH;
	  sb.append(ch);
	}
	else if (state == ParseUrlState.PORT) {
	  value.put(new StringValueImpl("port"),
		    new LongValue(new StringValueImpl(sb.toString()).toLong()));
	  sb.clear();
	  state = ParseUrlState.PATH;
	  sb.append(ch);
	}
	else
	  sb.append(ch);
	break;

      case '?':
	if (state == ParseUrlState.USER || state == ParseUrlState.HOST) {
	  value.put("host", sb.toString());
	  sb.clear();
	  state = ParseUrlState.QUERY;
	}
	else if (state == ParseUrlState.PASS) {
	  value.put("host", user);
	  value.put(new StringValueImpl("port"),
		    new LongValue(new StringValueImpl(sb.toString()).toLong()));
	  sb.clear();
	  state = ParseUrlState.QUERY;
	}
	else if (state == ParseUrlState.PORT) {
	  value.put(new StringValueImpl("port"),
		    new LongValue(new StringValueImpl(sb.toString()).toLong()));
	  sb.clear();
	  state = ParseUrlState.QUERY;
	}
	else if (state == ParseUrlState.PATH) {
	  if (sb.length() > 0)
	    value.put("path", sb.toString());
	  sb.clear();
	  state = ParseUrlState.QUERY;
	}
	else
	  sb.append(ch);
	break;

      case '#':
	if (state == ParseUrlState.USER || state == ParseUrlState.HOST) {
	  value.put("host", sb.toString());
	  sb.clear();
	  state = ParseUrlState.FRAGMENT;
	}
	else if (state == ParseUrlState.PASS) {
	  value.put("host", user);
	  value.put(new StringValueImpl("port"),
		    new LongValue(new StringValueImpl(sb.toString()).toLong()));
	  sb.clear();
	  state = ParseUrlState.FRAGMENT;
	}
	else if (state == ParseUrlState.PORT) {
	  value.put(new StringValueImpl("port"),
		    new LongValue(new StringValueImpl(sb.toString()).toLong()));
	  sb.clear();
	  state = ParseUrlState.FRAGMENT;
	}
	else if (state == ParseUrlState.PATH) {
	  if (sb.length() > 0)
	    value.put("path", sb.toString());
	  sb.clear();
	  state = ParseUrlState.FRAGMENT;
	}
	else if (state == ParseUrlState.QUERY) {
	  if (sb.length() > 0)
	    value.put("query", sb.toString());
	  sb.clear();
	  state = ParseUrlState.FRAGMENT;
	}
	else
	  sb.append(ch);
	break;

      default:
	sb.append((char) ch);
	break;
      }
    }

    if (sb.length() == 0) {
    }
    else if (state == ParseUrlState.USER ||
	     state == ParseUrlState.HOST)
      value.put("host", sb.toString());
    else if (state == ParseUrlState.PASS) {
      value.put("host", user);
      value.put(new StringValueImpl("port"),
		new LongValue(new StringValueImpl(sb.toString()).toLong()));
    }
    else if (state == ParseUrlState.PORT) {
      value.put(new StringValueImpl("port"),
		new LongValue(new StringValueImpl(sb.toString()).toLong()));
    }
    else if (state == ParseUrlState.QUERY)
      value.put("query", sb.toString());
    else if (state == ParseUrlState.FRAGMENT)
      value.put("fragment", sb.toString());
    else
      value.put("path", sb.toString());

    return value;
  }

  private static char toHexDigit(int d)
  {
    d = d & 0xf;

    if (d < 10)
      return (char) ('0' + d);
    else
      return (char) ('a' + d - 10);
  }
}

