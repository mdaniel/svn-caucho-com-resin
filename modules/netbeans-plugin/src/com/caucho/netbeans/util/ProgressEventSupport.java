/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.netbeans.util;

import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import java.util.Vector;

/**
 * Progress event support
 */
public final class ProgressEventSupport
{

  private final Object eventSource;
  private final Vector<ProgressListener> listeners
    = new Vector<ProgressListener>();
  private DeploymentStatus status;
  private TargetModuleID targetModuleID;


  public ProgressEventSupport(Object eventSource)
  {
    if (eventSource == null) {
      throw new NullPointerException();
    }
    this.eventSource = eventSource;
  }

  /**
   * Add a ProgressListener to the listener list.
   */
  public synchronized void addProgressListener(ProgressListener progressListener)
  {
    listeners.add(progressListener);
  }

  public synchronized void removeProgressListener(ProgressListener progressListener)
  {
    listeners.remove(progressListener);
  }

  public void fireProgressEvent(TargetModuleID targetModuleID,
                                DeploymentStatus status)
  {
    Vector<ProgressListener> listenersCopy = null;
    synchronized (this) {
      this.status = status;
      this.targetModuleID = targetModuleID;
      if (listeners != null) {
        listenersCopy = (Vector<ProgressListener>) listeners.clone();
      }
    }
    if (listenersCopy != null) {
      ProgressEvent evt = new ProgressEvent(eventSource,
                                            targetModuleID,
                                            status);
      for (ProgressListener progressListener : listenersCopy) {
        progressListener.handleProgressEvent(evt);
      }
    }
  }

  public synchronized DeploymentStatus getDeploymentStatus()
  {
    return status;
  }
}
