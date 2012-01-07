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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.bam.mailbox;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.packet.Packet;
import com.caucho.env.thread.TaskWorker;

/**
 * Queue of hmtp packets
 */
public class MailboxWorker extends TaskWorker
{
  private static final Logger log
    = Logger.getLogger(MailboxWorker.class.getName());

  private final MultiworkerMailbox _queue;
  private final String _toString;
  
  private volatile boolean _isRunning;

  public MailboxWorker(MultiworkerMailbox queue)
  {
    _queue = queue;
    
    setWorkerIdleTimeout(5000);
    
    _toString = getClass().getSimpleName() + "[" + _queue.getAddress() + "]";
  }
  
  boolean isRunning()
  {
    return _isRunning;
  }

  @Override
  public long runTask()
  {
    _isRunning = true;

    try {
      Packet packet;
    
      while ((packet = _queue.dequeue()) != null) {
        if (log.isLoggable(Level.FINEST))
          log.finest(this + " dequeue " + packet);

        try {
          _queue.dispatch(packet);
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    
      return -1;
    } finally {
      _isRunning = false;
    }
  }
  
  @Override
  public String toString()
  {
    return _toString;
  }
}
