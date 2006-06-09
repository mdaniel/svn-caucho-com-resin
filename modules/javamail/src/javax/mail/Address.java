/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package javax.mail;

/**
 * This abstract class models the addresses in a message. Subclasses
 * provide specific implementations. Subclasses will typically be
 * serializable so that (for example) the use of Address objects in
 * search terms can be serialized along with the search terms.  See
 * Also:Serialized Form
 */
public abstract class Address implements java.io.Serializable {

  /**
   * The equality operator. Subclasses should provide an
   * implementation of this method that supports value equality (do
   * the two Address objects represent the same destination?), not
   * object reference equality. A subclass must also provide a
   * corresponding implementation of the hashCode method that
   * preserves the general contract of equals and hashCode - objects
   * that compare as equal must have the same hashCode.
   */
  public abstract boolean equals(java.lang.Object address);

  /**
   * Returns the address type.
   */
  public abstract String getType();

  /**
   * Return a String representation of this address object.
   */
  public abstract String toString();

}
