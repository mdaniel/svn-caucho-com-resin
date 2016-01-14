/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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

package com.caucho.v5.deploy;

import io.baratine.files.Watch;
import io.baratine.service.Cancel;
import io.baratine.service.Result;

import java.util.logging.Logger;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.lifecycle.LifecycleListener;
import com.caucho.v5.lifecycle.LifecycleState;
import com.caucho.v5.vfs.PathImpl;


/**
 * DeployController controls the lifecycle of the DeployInstance.
 */
public interface DeployControllerService<I extends DeployInstance>
{
  I getDeployInstance();

  I getActiveDeployInstance();

  boolean logModified(Logger log);

  LifecycleState getState();
  
  boolean startOnInit();
  void startOnInit(Result<Boolean> result);

  boolean start();
  void start(Result<Boolean> result);

  boolean stop(ShutdownModeAmp mode);
  void stop(ShutdownModeAmp mode, Result<Boolean> result);

  void destroy(Result<Boolean> result);
  
  boolean restart();
  void restart(Result<Boolean> result);

  boolean update();
  void update(Result<Boolean> result);

  I request();
  I requestSafe();
  void requestSafe(Result<I> result);
  
  I subrequest();
  void subrequestSafe(Result<I> result);

  boolean alarm();
  void alarm(Result<Boolean> result);

  void addWatch(PathImpl path, Watch watch, Result<Cancel> result);
  // void removeWatch(Path path);

  // void updateConfig(Object config);
  
  void setStrategy(DeployControllerStrategy strategy);
  
  DeployController<I> getController();
  void setController(DeployController<I> controller);

  Throwable getConfigException();

  void addLifecycleListener(LifecycleListener listener);

  boolean isModified();
}
