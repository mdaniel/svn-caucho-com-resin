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

/**
 * This class represents a MIME ContentType value. It provides methods
 * to parse a ContentType string into individual components and to
 * generate a MIME style ContentType string.
 */
public class ContentType {

  /**
   * No-arg Constructor.
   */
  public ContentType()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Constructor that takes a Content-Type string. The String is
   * parsed into its constituents: primaryType, subType and
   * parameters. A ParseException is thrown if the parse fails.  s -
   * the Content-Type string.  - if the parse fails.
   */
  public ContentType(String s) throws ParseException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Constructor.
   * primaryType - primary typesubType - subTypelist - ParameterList
   */
  public ContentType(String primaryType, String subType, ParameterList list)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the MIME type string, without the parameters. The returned
   * value is basically the concatenation of the primaryType, the '/'
   * character and the secondaryType.
   */
  public String getBaseType()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the specified parameter value. Returns null if this
   * parameter is absent.
   */
  public String getParameter(String name)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return a ParameterList object that holds all the available
   * parameters. Returns null if no parameters are available.
   */
  public ParameterList getParameterList()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the primary type.
   */
  public String getPrimaryType()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the subType.
   */
  public String getSubType()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Match with the specified ContentType object. This method compares
   * only the primaryType and subType . The parameters of both
   * operands are ignored.
   *
   * For example, this method will return true when comparing the
   * ContentTypes for "text/plain" and "text/plain;
   * charset=foobar". If the subType of either operand is the special
   * character '*', then the subtype is ignored during the match. For
   * example, this method will return true when comparing the
   * ContentTypes for "text/plain" and "text/*"
   */
  public boolean match(ContentType cType)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Match with the specified content-type string. This method
   * compares only the primaryType and subType . The parameters of
   * both operands are ignored.
   *
   * For example, this method will return true when comparing the
   * ContentType for "text/plain" with "text/plain;
   * charset=foobar". If the subType of either operand is the special
   * character '*', then the subtype is ignored during the match. For
   * example, this method will return true when comparing the
   * ContentType for "text/plain" with "text/*"
   */
  public boolean match(String s)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the specified parameter. If this parameter already exists, it
   * is replaced by this new value.
   */
  public void setParameter(String name, String value)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set a new ParameterList.
   */
  public void setParameterList(ParameterList list)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the primary type. Overrides existing primary type.
   */
  public void setPrimaryType(String primaryType)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Set the subType. Replaces the existing subType.
   */
  public void setSubType(String subType)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Retrieve a RFC2045 style string representation of this
   * Content-Type. Returns null if the conversion failed.
   */
  public String toString()
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
