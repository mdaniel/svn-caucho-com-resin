/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.admin;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.config.ResinService;
import com.caucho.env.system.RootDirectorySystem;
import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.transaction.xalog.AbstractXALogManager;
import com.caucho.transaction.xalog.XALogManager;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * Remote administration service for log
 */
@ResinService
public class XaLogService
{
  private static final Logger log
    = Logger.getLogger(XaLogService.class.getName());
  private static final L10N L = new L10N(XaLogService.class);

  private String _path;
  private AbstractXALogManager _xaLog;
  
  public XaLogService()
  {
  }

  public void setPath(Path path)
  {
    // backwards compat
  }
  
  /*
    return createTransactionManager().createTransactionLog();
  */

  @PostConstruct
  public void init()
  {
    RootDirectorySystem rootService = RootDirectorySystem.getCurrent();
    
    if (rootService == null)
      throw new ConfigException(L.l("XaLogService requires an active Resin server."));

    if (_path == null) {
      _path = "xa.log";
    }

    _xaLog = new XALogManager();

    try {
      _xaLog.setPath(rootService.getDataDirectory().lookup(_path));

      TransactionManagerImpl tm = TransactionManagerImpl.getLocal();

      tm.setXALogManager(_xaLog);

      _xaLog.start();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }


  public void destroy()
  {
    AbstractXALogManager xaLog = _xaLog;
    _xaLog = null;

    if (xaLog != null) {
      try {
        xaLog.close();
      }
      catch (Exception ex) {
        log.log(Level.INFO, ex.toString(), ex);
      }
    }

  }
}
