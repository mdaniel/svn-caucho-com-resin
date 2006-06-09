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
 * This class represents a MIME ContentDisposition value. It provides
 * methods to parse a ContentDisposition string into individual
 * components and to generate a MIME style ContentDisposition string.
 */
public class ContentDisposition {

  /**
   * No-arg Constructor.
   */
  public ContentDisposition()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Constructor that takes a ContentDisposition string. The String is
   * parsed into its constituents: dispostion and parameters. A
   * ParseException is thrown if the parse fails.  s - the
   * ContentDisposition string.  - if the parse fails.  JavaMail 1.2
   */
  public ContentDisposition(String s) throws ParseException
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Constructor.  disposition - dispositionlist - ParameterList
   * JavaMail 1.2
   */
  public ContentDisposition(String disposition, ParameterList list)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Return the disposition value.
   */
  public String getDisposition()
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
   * Set the disposition. Replaces the existing disposition.
   */
  public void setDisposition(String disposition)
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
   * Retrieve a RFC2045 style string representation of this
   * ContentDisposition. Returns null if the conversion failed.
   */
  public String toString()
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
