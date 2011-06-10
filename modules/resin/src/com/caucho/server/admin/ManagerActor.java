/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
 * @author Alex Rojkov
 */

package com.caucho.server.admin;

import com.caucho.admin.action.*;
import com.caucho.bam.Query;
import com.caucho.bam.actor.SimpleActor;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.config.ConfigException;
import com.caucho.server.cluster.Server;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;

public class ManagerActor extends SimpleActor
{
  private static final Logger log
    = Logger.getLogger(ManagerActor.class.getName());

  private static final L10N L = new L10N(ManagerActor.class);

  private Server _server;
  private File _hprofDir;

  private AtomicBoolean _isInit = new AtomicBoolean();

  public ManagerActor()
  {
    super("manager@resin.caucho", BamSystem.getCurrentBroker());
  }

  @PostConstruct
  public void init()
  {
    if (_isInit.getAndSet(true))
      return;

    _server = Server.getCurrent();
    if (_server == null)
      throw new ConfigException(L.l(
        "resin:ManagerService requires an active Server.\n  {0}",
        Thread.currentThread().getContextClassLoader()));

    setBroker(getBroker());
    MultiworkerMailbox mailbox
      = new MultiworkerMailbox(getActor().getAddress(),
                               getActor(), getBroker(), 2);

    getBroker().addMailbox(mailbox);
  }

  public File getHprofDir()
  {
    return _hprofDir;
  }

  public void setHprofDir(String hprofDir)
  {
    if (hprofDir.isEmpty())
      throw new ConfigException("hprof-dir can not be set to an emtpy string");

    File file = new File(hprofDir);

    if (!file.isAbsolute())
      throw new ConfigException("hprof-dir must be an absolute path");

    _hprofDir = file;
  }

  @Query
  public String doThreadDump(long id,
                             String to,
                             String from,
                             ThreadDumpQuery query)
  {
    String result = null;
    
    try {
      result = new ThreadDumpAction().execute();
    } catch (ConfigException e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.getMessage();
    } catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.toString();
    }
    
    getBroker().queryResult(id, from, to, result);
    return result;
  }

  @Query
  public String doHeapDump(long id, String to, String from, HeapDumpQuery query)
  {
    String result = null;
    
    try {
      result = new HeapDumpAction().execute(query.isRaw(), 
                                            _server.getServerId(), 
                                            _hprofDir);
    } catch (ConfigException e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.getMessage();
    } catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.toString();
    }
    
    getBroker().queryResult(id, from, to, result);
    return result;
  }

  @Query
  public String listJmx(long id, String to, String from, JmxListQuery query)
  {
    String result = null;
    
    try {
      result = new ListJmxAction().execute(query.getPattern(),
                                           query.isPrintAttributes(),
                                           query.isPrintValues(),
                                           query.isPrintOperations(),
                                           query.isAllBeans(),
                                           query.isPlatform());
    } catch (ConfigException e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.getMessage();
    } catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.toString();
    }
    
    getBroker().queryResult(id, from, to, result);
    return result;  
  }

  @Query
  public String setJmx(long id, String to, String from, JmxSetQuery query)
  {
    String result = null;
    
    try {
      result = new SetJmxAction().execute(query.getPattern(),
                                          query.getAttribute(),
                                          query.getValue());
    } catch (ConfigException e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.getMessage();
    } catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.toString();
    }
    
    getBroker().queryResult(id, from, to, result);
    return result;
  }

  @Query
  public String callJmx(long id, String to, String from, JmxCallQuery query)
  {
    String result = null;
    
    try {
      result = new CallJmxAction().execute(query.getPattern(),
                                           query.getOperation(),
                                           query.getOperationIndex(),
                                           query.getParams());
    } catch (ConfigException e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.getMessage();
    } catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.toString();
    }
      
    getBroker().queryResult(id, from, to, result);
    return result;
  }

  @Query
  public String setLogLevel(long id, 
                            String to, 
                            String from, 
                            LogLevelQuery query)
  {
    String result = null;
    
    try {
      result = new SetLogLevelAction().execute(query.getLoggers(),
                                               query.getLevel(),
                                               query.getPeriod());
    } catch (ConfigException e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.getMessage();
    } catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.toString();
    }

    getBroker().queryResult(id, from, to, result);
    return result;
  }

  @Query
  public String profile(long id, String to, String from, ProfileQuery query)
  {
    String result = null;
    
    try {
      result = new ProfileAction().execute(query.getActiveTime(), 
                                           query.getPeriod(), 
                                           query.getDepth());
    } catch (ConfigException e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.getMessage();
    } catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);
      result = e.toString();
    }
    
    getBroker().queryResult(id, from, to, result);
    return result;
  }
}
