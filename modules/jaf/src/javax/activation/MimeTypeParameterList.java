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

package javax.activation;

import java.util.HashMap;
import java.util.Enumeration;
import java.util.Collections;

/**
 * Parameter list of a mime type.
 */
public class MimeTypeParameterList {
  private HashMap _map = new HashMap();
  
  public MimeTypeParameterList()
  {
  }

  /**
   * Constructs a new MimeTypeParameterList with the passed in data.
   */
  public MimeTypeParameterList(String parameterList)
    throws MimeTypeParseException
  {
    parse(parameterList);
  }

  /**
   * Parses the parameter list.
   */
  protected void parse(String parameterList)
    throws MimeTypeParseException
  {
    // can't use split() here because of quoting
    String s = parameterList.trim();
    if (s.charAt(0)!=';')
      throw new MimeTypeParseException("argument must begin with ';'");
    s = s.substring(1).trim();

    while(s.length()>0) {
      int equals = s.indexOf('=');

      if (equals==-1)
	throw new MimeTypeParseException("parameter does not contain '=': "+s);

      String key = s.substring(0, equals);
      s = s.substring(equals+1);

      String val = null;

      if (s.charAt(0)=='\"') {
	int otherquote = s.indexOf('\"', 1);

	if (otherquote==-1)
	  throw new MimeTypeParseException("missing close-quote: "+s);
	val = s.substring(1, otherquote);
	s = s.substring(otherquote+1);
      }
      else {
	int semi = s.indexOf(';');
	if (semi==-1)
	  semi = s.length();
	val = s.substring(0, semi);
	s = s.substring(semi);
      }

      if (s.length() > 0 && s.charAt(0)==';')
	s = s.substring(1);

      _map.put(key, val);
    }
  }

  /**
   * Returns the value with the given name.
   */
  public int size()
  {
    return _map.size();
  }

  /**
   * Returns true if the map is empty.
   */
  public boolean isEmpty()
  {
    return _map.isEmpty();
  }

  /**
   * Returns the value for the given name.
   */
  public String get(String name)
  {
    return (String) _map.get(name);
  }

  /**
   * Sets the value.
   */
  public void set(String name, String value)
  {
    _map.put(name, value);
  }

  /**
   * Removes a value
   */
  public void remove(String name)
  {
    _map.remove(name);
  }

  /**
   * Returns an enumeration of the names.
   */
  public Enumeration getNames()
  {
    return Collections.enumeration(_map.keySet());
  }

  public String toString()
  {
    if (size()==0)
      return "";

    StringBuffer sb = new StringBuffer();
    sb.append("; ");

    for(Enumeration e = getNames(); e.hasMoreElements();) {

      String key = (String)e.nextElement();
      sb.append(key);
      sb.append("=");
      String val = get(key);

      if (val.indexOf(' ')==-1 && val.indexOf(';')==-1) {
	sb.append(val);
      }
      else {
	sb.append("\"");
	sb.append(val);
	sb.append("\"");
      }

      if (e.hasMoreElements())
	sb.append("; ");

    }
    return sb.toString();
  }

}
