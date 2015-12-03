/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.transaction.xalog;

import com.caucho.transaction.XidImpl;
import com.caucho.vfs.Path;

import java.io.IOException;

/**
 * Implements a single log stream.  Each log stream has two associated
 * files in order to switch at the end of the file.
 */
abstract public class AbstractXALogManager
{
  /**
   * Sets a log path.
   */
  abstract public void setPath(Path path)
    throws java.io.IOException;

  /**
   * Initialize the log manager.
   */
  abstract public void init();
  
  /**
   * Starts the log manager.
   */
  abstract public void start()
    throws IOException;

  /**
   * True if the xid is an already-committed xid
   */
  abstract public boolean hasCommittedXid(XidImpl xid);

  /**
   * Returns a stream for a new transaction.
   */
  abstract public AbstractXALogStream getStream();

  /**
   * Returns a stream for a new transaction.
   */
  abstract public void flush();

  /**
   * Closes the log manager.
   */
  abstract public void close();

}
