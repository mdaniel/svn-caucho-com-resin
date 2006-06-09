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

/**
 * This class holds MIME parameters (attribute-value pairs). The
 * mail.mime.encodeparameters and mail.mime.decodeparameters System
 * properties control whether encoded parameters, as specified by RFC
 * 2231, are supported. By default, such encoded parameters are not
 * supported.
 *
 * Also, in the current implementation, setting the System property
 * mail.mime.decodeparameters.strict to "true" will cause a
 * ParseException to be thrown for errors detected while decoding
 * encoded parameters. By default, if any decoding errors occur, the
 * original (undecoded) string is used.
 */
public class ParameterList {

  /**
   * No-arg Constructor.
   */
  public ParameterList()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Constructor that takes a parameter-list string. The String is
   * parsed and the parameters are collected and stored internally. A
   * ParseException is thrown if the parse fails. Note that an empty
   * parameter-list string is valid and will be parsed into an empty
   * ParameterList.  s - the parameter-list string.  - if the parse
   * fails.
   */
  public ParameterList(String s) throws ParseException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Returns the value of the specified parameter. Note that parameter
   * names are case-insensitive.
   */
  public String get(String name)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return an enumeration of the names of all parameters in this list.
   */
  public Enumeration getNames()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Removes the specified parameter from this ParameterList. This
   * method does nothing if the parameter is not present.
   */
  public void remove(String name)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set a parameter. If this parameter already exists, it is replaced
   * by this new value.
   */
  public void set(String name, String value)
  {
    throw new UnsupportedOperationException("not implemented");
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
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Convert this ParameterList into a MIME String. If this is an
   * empty list, an empty string is returned.
   */
  public String toString()
  {
    throw new UnsupportedOperationException("not implemented");
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
