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
 * Represents a messaing exception
 */
public class SendFailedException extends MessagingException {
  private Address []validSent;
  private Address []validUnsent;
  private Address []invalid;
  
  /**
   * Creates an exception.
   */
  public SendFailedException()
  {
  }
  
  /**
   * Creates an exception.
   */
  public SendFailedException(String msg)
  {
    super(msg);
  }
  
  /**
   * Creates an exception.
   */
  public SendFailedException(String msg, Exception e)
  {
    super(msg, e);
  }
  
  /**
   * Creates an exception.
   */
  public SendFailedException(String msg, Exception e,
			     Address []sent, Address []unsent,
			     Address []invalid)
  {
    super(msg, e);

    this.validSent = sent;
    this.validUnsent = unsent;
    this.invalid = invalid;
  }

  /**
   * Returns the invalid addresses.
   */
  public Address []getInvalidAddresses()
  {
    return this.invalid;
  }

  /**
   * Returns the valid sent addresses.
   */
  public Address []getValidSentAddresses()
  {
    return this.validSent;
  }

  /**
   * Returns the valid unsent addresses.
   */
  public Address []getValidUnsentAddresses()
  {
    return this.validUnsent;
  }
}
