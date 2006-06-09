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
 * The adapter which receives connection events. The methods in this
 * class are empty; this class is provided as a convenience for easily
 * creating listeners by extending this class and overriding only the
 * methods of interest.
 */
public abstract class ConnectionAdapter implements ConnectionListener {

  public ConnectionAdapter()
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Description copied from interface:
   * Invoked when a Store/Folder/Transport is closed.
   */
  public void closed(ConnectionEvent e)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Description copied from interface: Invoked when a Store is
   * disconnected. Note that a folder cannot be disconnected, so a
   * folder will not fire this event
   */
  public void disconnected(ConnectionEvent e)
  {
    throw new UnsupportedOperationException("not implemented");
  }

  /**
   * Description copied from interface:
   * Invoked when a Store/Folder/Transport is opened.
   */
  public void opened(ConnectionEvent e)
  {
    throw new UnsupportedOperationException("not implemented");
  }

}
