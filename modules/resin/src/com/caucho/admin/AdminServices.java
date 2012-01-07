/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.admin;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Startup;
import javax.inject.Singleton;

import com.caucho.server.admin.DeployService;
import com.caucho.server.admin.ManagerService;

/**
 * Convenience collection of the standard administration services.
 */
@Startup
@Singleton
public class AdminServices
{
  private static final Logger log
    = Logger.getLogger(AdminServices.class.getName());
  
  @PostConstruct
  public void init()
  {
    AdminServices services = this;
    
    try {
      Class<?> proCl = Class.forName("com.caucho.admin.ProAdminServices");
      
      services = (AdminServices) proCl.newInstance();
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    services.initImpl();
  }
  
  protected void initImpl()
  {
    DeployService deployService = new DeployService();
    deployService.init();

    ManagerService managerService = new ManagerService();
    managerService.init();
  }
}
