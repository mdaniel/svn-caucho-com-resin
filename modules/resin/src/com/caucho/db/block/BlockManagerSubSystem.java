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

package com.caucho.db.block;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.*;

import com.caucho.env.service.*;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.warning.WarningService;
import com.caucho.lifecycle.*;
import com.caucho.util.*;
import com.caucho.vfs.TempBuffer;

/**
 * The ShutdownSystem manages the Resin shutdown and includes a timeout
 * thread. If the timeout takes longer than shutdown-wait-max, the ShutdownSystem
 * will force a JVM exit.
 */
public class BlockManagerSubSystem extends AbstractResinSubSystem
{
  private static final Logger log = 
    Logger.getLogger(BlockManagerSubSystem.class.getName());
  private static final L10N L = new L10N(BlockManagerSubSystem.class);

  public static final int START_PRIORITY = 100;
  public static final int STOP_PRIORITY = START_PRIORITY_DATABASE_SYSTEM;
  
  public static final long BLOCK_FLUSH_PERIOD = 5 * 60 * 1000L;

  private static final AtomicReference<BlockManagerSubSystem> _activeService
    = new AtomicReference<BlockManagerSubSystem>();
  
  private Alarm _blockFlushAlarm;
  private Lifecycle _lifecycle = new Lifecycle();
  
  private BlockManagerSubSystem()
  {
  }

  public static BlockManagerSubSystem createAndAddService()
  {
    ResinSystem system = preCreate(BlockManagerSubSystem.class);
      
    BlockManagerSubSystem service = new BlockManagerSubSystem();
    system.addService(BlockManagerSubSystem.class, service);
    
    return service;
  }

  public static BlockManagerSubSystem getCurrent()
  {
    return ResinSystem.getCurrentService(BlockManagerSubSystem.class);
  }
  
  //
  // Service API
  //
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }

  /**
   * Starts the server.
   */
  @Override
  public void start()
  {
    _lifecycle.toActive();
    
    _blockFlushAlarm = new Alarm(new BlockFlushHandler());
    
    _blockFlushAlarm.queue(0);
  }
  
  /**
   * Stops the server.
   */
  @Override
  public void stop()
  {
    _lifecycle.toStop();
    
    Alarm alarm = _blockFlushAlarm;
    _blockFlushAlarm = null;
    
    if (alarm != null) {
      alarm.dequeue();
    }
    
    BlockManager blockManager = BlockManager.getBlockManager();

    if (blockManager != null) {
      blockManager.destroy();
    }
  }
  
  class BlockFlushHandler implements AlarmListener {
    @Override
    public void handleAlarm(Alarm alarm)
    {
      try {
        BlockManager manager = BlockManager.getBlockManager();

        if (manager != null)
          manager.flush();
      } finally {
        if (_lifecycle.isActive()) {
          alarm.queue(BLOCK_FLUSH_PERIOD);
        }
      }
    }
    
  }
}
