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

package com.caucho.bam.actor;

import com.caucho.bam.BamError;
import com.caucho.bam.BamException;


/**
 * Exception caught by the skeleton invocation
 */
@SuppressWarnings("serial")
public class SkeletonInvocationException extends BamException {
  public SkeletonInvocationException()
  {
  }

  public SkeletonInvocationException(String msg)
  {
    super(msg);
  }

  public SkeletonInvocationException(Throwable e)
  {
    super(e);
  }

  public SkeletonInvocationException(String msg, Throwable e)
  {
    super(msg, e);
  }

  public static RuntimeException createRuntimeException(Throwable e)
  {
    if (e instanceof RuntimeException)
      return (RuntimeException) e;
    else
      return new SkeletonInvocationException(e);
  }

  public BamError createActorError()
  {
    Throwable cause = getCause();

    if (cause instanceof BamException)
      return ((BamException) cause).createActorError();
    else if (cause != null) {
      return new BamError(BamError.TYPE_CANCEL,
                            BamError.INTERNAL_SERVER_ERROR,
                            cause.toString());
    }
    else {
      return new BamError(BamError.TYPE_CANCEL,
                            BamError.INTERNAL_SERVER_ERROR,
                            toString());
    }
  }
}
