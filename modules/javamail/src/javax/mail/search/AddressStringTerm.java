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

package javax.mail.search;
import javax.mail.*;

/**
 * This abstract class implements string comparisons for Message
 * addresses.  Note that this class differs from the AddressTerm class
 * in that this class does comparisons on address strings rather than
 * Address objects.  Since: JavaMail 1.1 See Also:Serialized Form
 */
public abstract class AddressStringTerm extends StringTerm {

  /**
   * Constructor.
   * pattern - the address pattern to be compared.
   */
  protected AddressStringTerm(String pattern)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Equality comparison.
   */
  public boolean equals(Object obj)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Check whether the address pattern specified in the constructor is
   * a substring of the string representation of the given Address
   * object.  Note that if the string representation of the given
   * Address object contains charset or transfer encodings, the
   * encodings must be accounted for, during the match process.
   */
  protected boolean match(Address a)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
