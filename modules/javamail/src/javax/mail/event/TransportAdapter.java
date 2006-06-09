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

package javax.mail.event;
import javax.mail.*;

/**
 * The adapter which receives Transport events. The methods in this
 * class are empty; this class is provided as a convenience for easily
 * creating listeners by extending this class and overriding only the
 * methods of interest.
 */
public abstract class TransportAdapter implements TransportListener {

  public TransportAdapter()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Description copied from interface:
   * Invoked when a Message is succesfully delivered.
   */
  public void messageDelivered(TransportEvent e)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Description copied from interface:
   * Invoked when a Message is not delivered.
   */
  public void messageNotDelivered(TransportEvent e)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Description copied from interface:
   * Invoked when a Message is partially delivered.
   */
  public void messagePartiallyDelivered(TransportEvent e)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
