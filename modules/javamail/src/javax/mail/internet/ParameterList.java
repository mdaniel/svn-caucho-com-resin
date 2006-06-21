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

package javax.mail.internet;
import javax.mail.*;
import java.util.*;
import javax.activation.MimeTypeParameterList;
import javax.activation.MimeTypeParseException;

/**
 * This class holds MIME parameters (attribute-value pairs).
 */
public class ParameterList {

  // Hashtable used in order to support Enumeration API
  private Hashtable _hash = new Hashtable();

  /**
   * No-arg Constructor.
   */
  public ParameterList()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Constructor that takes a parameter-list string.
   */
  public ParameterList(String s) throws ParseException
  {
    try {
      if (s.length() > 0)
	addParameterList(new MimeTypeParameterList(s));
    }
    catch (MimeTypeParseException e) {
      throw new ParseException(e.getMessage());
    }
  }

  ParameterList(MimeTypeParameterList mimeTypeParameterList)
    throws ParseException
  {
    addParameterList(mimeTypeParameterList);
  }

  private void addParameterList(MimeTypeParameterList mimeTypeParameterList)
    throws ParseException {

    for(Enumeration enu = mimeTypeParameterList.getNames();
	enu.hasMoreElements();) {
      
      String key = (String)enu.nextElement();
      String val = mimeTypeParameterList.get(key);
      
      set(key, val);
    }
  }

  /**
   * Returns the value of the specified parameter. Note that parameter
   * names are case-insensitive.
   */
  public String get(String name)
  {
    return (String)_hash.get(name.toLowerCase());    
  }

  /**
   * Return an enumeration of the names of all parameters in this list.
   */
  public Enumeration getNames()
  {
    return _hash.keys();
  }

  /**
   * Removes the specified parameter from this ParameterList.
   */
  public void remove(String name)
  {
    _hash.remove(name);
  }

  /**
   * Set a parameter.
   */
  public void set(String name, String value)
  {
    _hash.put(name, value);
  }

  /**
   * Set a parameter. If this parameter already exists, it is replaced
   * by this new value. If the mail.mime.encodeparameters System
   * property is true, and the parameter value is non-ASCII, it will
   * be encoded with the specified charset, as specified by RFC 2231.
   */
  public void set(String name, String value, String charset)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the number of parameters in this list.
   */
  public int size()
  {
    return _hash.size();
  }

  /**
   * Convert this ParameterList into a MIME String. If this is an
   * empty list, an empty string is returned.
   */
  public String toString()
  {
    return "FIXME XXX";
  }

  /**
   * Convert this ParameterList into a MIME String. If this is an
   * empty list, an empty string is returned. The 'used' parameter
   * specifies the number of character positions already taken up in
   * the field into which the resulting parameter list is to be
   * inserted. It's used to determine where to fold the resulting
   * parameter list.
   */
  public String toString(int used)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
