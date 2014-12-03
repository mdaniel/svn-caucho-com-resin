/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.admin;

import io.baratine.core.Startup;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.server.admin.SnapshotService;
import com.caucho.server.container.ServerBase;

/**
 * Convenience collection of the standard administration services.
 */
@Startup
@Singleton
public class ProAdminServices extends AdminServices
{
  @PostConstruct
  public void initImpl()
  {
    super.initImpl();

    ServerBase resin = ServerBase.getCurrent();
   
    if (resin != null) {
      StatService statService = new StatService();
      statService.init();
      
      LogService logService = new LogService();
      logService.init();
    
      JmxService jmxService = new JmxService();
      jmxService.init();
 
      XaLogService xaLogService = new XaLogService();
      xaLogService.init();

      SnapshotService snapshotService = new SnapshotService();
      snapshotService.init();
    }
  }
}
