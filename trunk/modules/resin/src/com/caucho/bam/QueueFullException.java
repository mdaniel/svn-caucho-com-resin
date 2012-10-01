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

package com.caucho.bam;


/**
 * HMPP wrapper
 */
@SuppressWarnings("serial")
public class QueueFullException
  extends RemoteConnectionFailedException
{
  public QueueFullException()
  {
  }

  public QueueFullException(String msg)
  {
    super(msg);
  }

  public QueueFullException(Throwable e)
  {
    super(e.toString(), e);
  }

  public QueueFullException(String msg, Throwable e)
  {
    super(msg, e);
  }

  public QueueFullException(String msg, BamError error)
  {
    super(msg, error);
  }

  public QueueFullException(String msg, ErrorPacketException e)
  {
    super(msg, e);
  }

  public QueueFullException(BamError error)
  {
    super(error);
  }

  @Override
  public BamError createActorError()
  {
    BamError error = getActorError();

    if (error != null)
      return error;

    return new BamError(BamError.TYPE_CANCEL,
                        BamError.REMOTE_CONNECTION_FAILED,
                        getMessage());
  }
}
