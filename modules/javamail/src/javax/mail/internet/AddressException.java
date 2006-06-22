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
 * The exception thrown when a wrongly formatted address is encountered.
 */
public class AddressException extends ParseException {

  /**
   * The index in the string where the error occurred, or -1 if not known.
   */
  protected int pos;

  /**
   * The string being parsed.
   */
  protected String ref;

  /**
   * Constructs an AddressException with no detail message.
   */
  public AddressException()
  {
    this(null);
  }

  /**
   * Constructs an AddressException with the specified detail message.
   */
  public AddressException(String s)
  {
    this(s, null);
  }

  /**
   * Constructs an AddressException with the specified detail message
   * and reference info.
   */
  public AddressException(String s, String ref)
  {
    this(s, ref, -1);
  }

  /**
   * Constructs an AddressException with the specified detail message
   * and reference info.  s - the detail message
   */
  public AddressException(String s, String ref, int pos)
  {
    super(s);
    this.ref = ref;
    this.pos = pos;
  }

  /**
   * Get the position with the reference string where the error was
   * detected (-1 if not relevant).
   */
  public int getPos()
  {
    return pos;
  }

  /**
   * Get the string that was being parsed when the error was detected
   * (null if not relevant).
   */
  public String getRef()
  {
    return ref;
  }

  public String toString()
  {
    return super.toString();
  }

}
