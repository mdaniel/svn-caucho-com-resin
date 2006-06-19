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
 * This class implements Message Address comparisons.
 */
public abstract class AddressTerm extends SearchTerm {

  /**
   * The address.
   */
  protected Address address;

  protected AddressTerm(Address address)
  {
    this.address = address;
  }

  /**
   * Equality comparison.
   */
  public boolean equals(Object obj)
  {
    if (! (obj instanceof AddressTerm))
      return false;

    AddressTerm addressTerm = (AddressTerm)obj;

    return addressTerm.address.equals(address);
  }

  /**
   * Return the address to match with.
   */
  public Address getAddress()
  {
    return address;
  }

  /**
   * Compute a hashCode for this object.
   */
  public int hashCode()
  {
    return address.hashCode();
  }

  /**
   * Match against the argument Address.
   */
  protected boolean match(Address a)
  {
    return address.equals(a);
  }

}
