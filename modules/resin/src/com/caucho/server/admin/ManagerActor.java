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
 * @author Alex Rojkov
 */

package com.caucho.server.admin;

import com.caucho.admin.action.*;
import com.caucho.bam.Query;
import com.caucho.bam.actor.SimpleActor;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.cloud.network.NetworkClusterSystem;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.ConfigException;
import com.caucho.env.service.ResinSystem;
import com.caucho.security.AdminAuthenticator;
import com.caucho.security.PasswordUser;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.resin.Resin;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.Vfs;

import javax.annotation.PostConstruct;
import javax.management.JMException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManagerActor extends SimpleActor
{
  private static final Logger log
    = Logger.getLogger(ManagerActor.class.getName());

  private static final L10N L = new L10N(ManagerActor.class);

  private ServletService _server;
  private Path _hprofDir;

  private AtomicBoolean _isInit = new AtomicBoolean();

  private AdminAuthenticator _adminAuthenticator;

  public ManagerActor()
  {
    super("manager@resin.caucho", BamSystem.getCurrentBroker());
  }

  @PostConstruct
  public void init()
  {
    if (_isInit.getAndSet(true))
      return;

    _server = ServletService.getCurrent();
    if (_server == null)
      throw new ConfigException(L.l(
        "resin:ManagerService requires an active Server.\n  {0}",
        Thread.currentThread().getContextClassLoader()));

    _adminAuthenticator = _server.getAdminAuthenticator();

    String address = getActor().getAddress();
    
    setBroker(getBroker());
    MultiworkerMailbox mailbox
      = new MultiworkerMailbox(address,
                               getActor(), getBroker(), 2);

    getBroker().addMailbox(address, mailbox);
  }

  public Path getHprofDir()
  {
    return _hprofDir;
  }

  public void setHprofDir(String hprofDir)
  {
    if (hprofDir.isEmpty())
      throw new ConfigException("hprof-dir can not be set to an emtpy string");

    Path path = Vfs.lookup(hprofDir);

    _hprofDir = path;
  }

  @Query
  public AddUserQueryReply addUser(long id,
                                    String to,
                                    String from,
                                    AddUserQuery query)
  {
    PasswordUser user = new AddUserAction(_adminAuthenticator,
                                          query.getUser(),
                                          query.getPassword(),
                                          query.getRoles()).execute();

    AddUserQueryReply result
      = new AddUserQueryReply(new UserQueryReply.User(user.getPrincipal()
                                                            .getName(),
                                                        user.getRoles()));

    getBroker().queryResult(id, from, to, result);

    return result;
  }

  @Query
  public ListUsersQueryReply listUsers(long id,
                                        String to,
                                        String from,
                                        ListUsersQuery query)
  {
    Hashtable<String,com.caucho.security.PasswordUser> userMap
      = new ListUsersAction(_adminAuthenticator).execute();

    List<UserQueryReply.User> userList = new ArrayList<UserQueryReply.User>();

    for (Map.Entry<String,PasswordUser> userEntry : userMap.entrySet()) {
      com.caucho.security.PasswordUser passwordUser = userEntry.getValue();
      UserQueryReply.User user = new UserQueryReply.User(userEntry.getKey(),
                                                           passwordUser.getRoles());
      userList.add(user);
    }

    UserQueryReply.User[] users
      = userList.toArray(new UserQueryReply.User[userList.size()]);

    ListUsersQueryReply result = new ListUsersQueryReply(users);

    getBroker().queryResult(id, from, to, result);

    return result;
  }

  @Query
  public RemoveUserQueryReply removeUser(long id,
                                          String to,
                                          String from,
                                          RemoveUserQuery query)
  {
    PasswordUser user = new RemoveUserAction(_adminAuthenticator,
                                             query.getUser()).execute();

    RemoveUserQueryReply result
      = new RemoveUserQueryReply(new UserQueryReply.User(user.getPrincipal()
                                                               .getName(),
                                                           user.getRoles()));

    getBroker().queryResult(id, from, to, result);

    return result;
  }

  @Query
  public StringQueryReply doThreadDump(long id,
                                       String to,
                                       String from,
                                       ThreadDumpQuery query)
  {
    StringQueryReply reply;
    
    if (query.isJson()) {
      reply = new JsonQueryReply(new ThreadDumpAction().executeJson());
    } else {
      reply = new StringQueryReply(new ThreadDumpAction().execute(32, false));
    }

    getBroker().queryResult(id, from, to, reply);

    return reply;
  }

  @Query
  public StringQueryReply doHeapDump(long id,
                                      String to,
                                      String from,
                                      HeapDumpQuery query)
  {
    try {
      String dump = new HeapDumpAction().execute(query.isRaw(),
                                                 _server.getServerId(),
                                                 _hprofDir);

      StringQueryReply result = new StringQueryReply(dump);

      getBroker().queryResult(id, from, to, result);

      return result;
    } catch (JMException e) {
      e.printStackTrace();

      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Query
  public ListJmxQueryReply listJmx(long id,
                                    String to,
                                    String from,
                                    JmxListQuery query)
  {
    try {
      ListJmxQueryReply result
        = new ListJmxAction().execute(query.getPattern(),
                                      query.isPrintAttributes(),
                                      query.isPrintValues(),
                                      query.isPrintOperations(),
                                      query.isAllBeans(),
                                      query.isPlatform());

      getBroker().queryResult(id, from, to, result);

      return result;
    } catch (JMException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Query
  public JsonQueryReply doJmxDump(long id,
                                     String to,
                                     String from,
                                     JmxDumpQuery query)
  {
    try {
      String jmxDump = new JmxDumpAction().execute();

      JsonQueryReply result = new JsonQueryReply(jmxDump);
      getBroker().queryResult(id, from, to, result);

      return result;
    } catch (JMException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }  

  @Query
  public JmxSetQueryReply setJmx(long id,
                                  String to,
                                  String from,
                                  JmxSetQuery query)
  {
    try {
      JmxSetQueryReply result = new SetJmxAction().execute(query.getPattern(),
                                                            query.getAttribute(),
                                                            query.getValue());

      getBroker().queryResult(id, from, to, result);

      return result;
    } catch (JMException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Query
  public JmxCallQueryReply callJmx(long id,
                                   String to,
                                   String from,
                                   JmxCallQuery query)
  {
    try {
      JmxCallQueryReply reply
        = new CallJmxAction().execute(query.getPattern(),
                                      query.getOperation(),
                                      query.getOperationIndex(),
                                      query.getParams());
     

      getBroker().queryResult(id, from, to, reply);

      return reply;
    } catch (JMException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Query
  public StringQueryReply setLogLevel(long id,
                                       String to,
                                       String from,
                                       LogLevelQuery query)
  {
    String message = new SetLogLevelAction().execute(query.getLoggers(),
                                                     query.getLevel(),
                                                     query.getPeriod());

    StringQueryReply result = new StringQueryReply(message);

    getBroker().queryResult(id, from, to, result);

    return result;
  }

  @Query
  Serializable getStats(long id, String to, String from, StatsQuery query)
  {
    GetStatsAction action = new GetStatsAction();

    StatServiceValuesQueryReply result = action.execute(query.getMeters(),
                                                         query.getFrom(),
                                                         query.getTo());

    getBroker().queryResult(id, from, to, result);

    return result;
  }

  @Query
  public PdfReportQueryReply pdfReport(long id,
                                        String to,
                                        String from,
                                        PdfReportQuery query)
  {
    PdfReportAction action = new PdfReportAction();
    
    if (query.getPath() != null)
      action.setPath(query.getPath());
    
    if (query.getPeriod() > 0)
      action.setPeriod(query.getPeriod());

    action.setSnapshot(query.isSnapshot());
    action.setWatchdog(query.isWatchdog());

    if (query.getProfileTime() > 0)
      action.setProfileTime(query.getProfileTime());

    if (query.getSamplePeriod() > 0)
      action.setProfileTick(query.getSamplePeriod());

    if (query.getServerId() != null)
      action.setServerId(query.getServerId());

    if (query.getReport() != null)
      action.setReport(query.getReport());
    
    if (query.getLogDirectory() != null)
      action.setLogDirectory(query.getLogDirectory());

    action.setReturnPdf(query.isReturnPdf());

    try {
      action.init();

      PdfReportAction.PdfReportActionResult actionResult
        = action.execute();

      StreamSource pdfSource = null;

      if (query.isReturnPdf())
        pdfSource = new StreamSource(actionResult.getPdfOutputStream());

      PdfReportQueryReply result
        = new PdfReportQueryReply(actionResult.getMessage(),
                                   actionResult.getFileName(),
                                   pdfSource);

      getBroker().queryResult(id, from, to, result);

      return result;
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw e;
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw new RuntimeException(e);
    }
  }

  @Query
  public StringQueryReply profile(long id,
                                   String to,
                                   String from,
                                   ProfileQuery query)
  {
    String profile = new ProfileAction().execute(query.getActiveTime(),
                                                 query.getPeriod(),
                                                 query.getDepth());

    StringQueryReply result = new StringQueryReply(profile);

    getBroker().queryResult(id, from, to, result);

    return result;
  }

  @Query
  public Date []listRestarts(long id,
                                        String to,
                                        String from,
                                        ListRestartsQuery query)
  {
    final long now = CurrentTime.getCurrentTime();

    NetworkClusterSystem clusterService = NetworkClusterSystem.getCurrent();

    CloudServer cloudServer = clusterService.getSelfServer();

    int index = cloudServer.getIndex();

    StatSystem statSystem = ResinSystem.getCurrentService(StatSystem.class);

    if (statSystem == null)
      throw new IllegalStateException("StatSystem is not active");

    long []restartTimes
      = statSystem.getStartTimes(index, now - query.getTimeBackSpan(), now);

    List<Date> restartsList = new ArrayList<Date>();

    for (long restartTime : restartTimes) {
      restartsList.add(new Date(restartTime));
    }
    
    Date []restarts = new Date[restartsList.size()];
    restartsList.toArray(restarts);
    
    getBroker().queryResult(id, from, to, restarts);

    return restarts;
  }
  
  @Query
  public StringQueryReply addLicense(long id,
                                      String to,
                                      String from,
                                      LicenseAddQuery query)
  {
    String message = new AddLicenseAction().execute(query.getLicenseContent(),
                                                    query.getFileName(),
                                                    query.isOverwrite(),
                                                    query.isRestart());

    StringQueryReply result = new StringQueryReply(message);

    getBroker().queryResult(id, from, to, result);

    return result;
  }
  
  @Query
  public StringQueryReply status(long id,
                                 String to,
                                 String from,
                                 ServerStatusQuery query)
  {
    Resin resin = Resin.getCurrent();

    CloudServer cloudServer = resin.getSelfServer();

    String status = L.l("Server {0} : {1}",
                        cloudServer,
                        cloudServer.getState());

    final StringQueryReply reply = new StringQueryReply(status);

    getBroker().queryResult(id, from, to, reply);

    return reply;
  }
  
  @Query
  public StringQueryReply scoreboard(long id,
                                           String to,
                                           String from,
                                           ScoreboardQuery query)
  {
    String message = new ScoreboardAction().excute(query.getType(),
                                                   query.isGreedy());

    StringQueryReply result = new StringQueryReply(message);

    getBroker().queryResult(id, from, to, result);

    return result;
  }
}
