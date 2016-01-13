/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.admin;

import io.baratine.service.Startup;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.v5.server.admin.SnapshotService;
import com.caucho.v5.server.container.ServerBaseOld;

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

    ServerBaseOld resin = ServerBaseOld.current();
   
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
