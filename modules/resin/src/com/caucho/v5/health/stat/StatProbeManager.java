/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import java.util.ArrayList;

import com.caucho.v5.health.meter.MeterBase;
import com.caucho.v5.health.meter.MeterService;
import com.caucho.v5.loader.EnvLoader;

// TODO: service cleanup
public class StatProbeManager extends MeterService { // implements Closeable {
  private final Object _lock = new Object();

  private StatServiceLocal _statService;
  private ArrayList<MeterBase> 
    _pendingProbes = new ArrayList<MeterBase>();

  public StatProbeManager()
  {
    MeterService oldService = getCurrent();

    // server/27o2
    if (oldService instanceof StatProbeManager) {
      _pendingProbes.addAll(((StatProbeManager) oldService)._pendingProbes);
    }
    
    setManager(this);

    EnvLoader.addCloseListener(this);
  }

  public void setService(StatServiceLocal statService)
  {
    synchronized (_lock) {
      if (_statService == null) {
        _statService = statService;

        for (MeterBase probe : _pendingProbes) {
          _statService.addMeter(probe);
        }

        _pendingProbes.clear();
      }
    }
  }

  @Override
  protected void registerMeter(MeterBase probe)
  {
    synchronized (_lock) {
      if (_statService != null)
        _statService.addMeter(probe);
      else
        _pendingProbes.add(probe);
    }
  }

  public void close()
  {
    setManager(null);
  }
}
