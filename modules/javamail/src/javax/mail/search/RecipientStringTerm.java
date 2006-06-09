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
 * This class implements string comparisons for the Recipient Address
 * headers.  Note that this class differs from the RecipientTerm class
 * in that this class does comparisons on address strings rather than
 * Address objects. The string comparisons are case-insensitive.
 * Since: JavaMail 1.1 See Also:Serialized Form
 */
public final class RecipientStringTerm extends AddressStringTerm {

  /**
   * Constructor.
   * type - the recipient typepattern - the address pattern to be compared.
   */
  public RecipientStringTerm(Message.RecipientType type, String pattern)
  {
    super(null); // XXX: remove this
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
   * Return the type of recipient to match with.
   */
  public Message.RecipientType getRecipientType()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Compute a hashCode for this object.
   */
  public int hashCode()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Check whether the address specified in the constructor is a
   * substring of the recipient address of this Message.
   */
  public boolean match(Message msg)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
