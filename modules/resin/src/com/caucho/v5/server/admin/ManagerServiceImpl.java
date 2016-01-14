/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.server.admin;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.management.JMException;

import com.caucho.v5.admin.action.CallJmxAction;
import com.caucho.v5.admin.action.GetStatsAction;
import com.caucho.v5.admin.action.HeapDumpAction;
import com.caucho.v5.admin.action.JmxDumpAction;
import com.caucho.v5.admin.action.ListJmxAction;
import com.caucho.v5.admin.action.ProfileAction;
import com.caucho.v5.admin.action.ScoreboardAction;
import com.caucho.v5.admin.action.SetJmxAction;
import com.caucho.v5.admin.action.SetLogLevelAction;
import com.caucho.v5.admin.action.StoreDumpAction;
import com.caucho.v5.admin.action.StoreRestoreAction;
import com.caucho.v5.admin.action.ThreadDumpAction;
import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.baratine.Remote;
import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.health.action.JmxSetQueryReply;
import com.caucho.v5.health.stat.StatSystem;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.security.AuthenticatorRole;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.StreamSource;
import com.caucho.v5.vfs.VfsOld;

@Remote
public class ManagerServiceImpl
{
  private static final Logger log
    = Logger.getLogger(ManagerServiceImpl.class.getName());

  private static final L10N L = new L10N(ManagerServiceImpl.class);

  private HttpContainerServlet _server;
  private PathImpl _hprofDir;

  private AtomicBoolean _isInit = new AtomicBoolean();

  private AuthenticatorRole _adminAuthenticator;

  public ManagerServiceImpl()
  {
    ServiceManagerAmp manager = AmpSystem.getCurrentManager();
    
    manager.newService(this).address("/manager").ref();
  }

  @PostConstruct
  public void init()
  {
    if (_isInit.getAndSet(true))
      return;

    _server = HttpContainerServlet.current();
    if (_server == null)
      throw new ConfigException(L.l(
        "resin:ManagerService requires an active Server.\n  {0}",
        Thread.currentThread().getContextClassLoader()));

    _adminAuthenticator = _server.getAdminAuthenticator();
  }

  public PathImpl getHprofDir()
  {
    return _hprofDir;
  }

  public void setHprofDir(String hprofDir)
  {
    if (hprofDir.isEmpty())
      throw new ConfigException("hprof-dir can not be set to an emtpy string");

    PathImpl path = VfsOld.lookup(hprofDir);

    _hprofDir = path;
  }

  /*
  public AddUserQueryReply addUser(long id,
                                    String to,
                                    String from,
                                    AddUserQuery query)
  {
    PasswordUser2 user = new AddUserAction(_adminAuthenticator,
                                          query.getUser(),
                                          query.getPassword(),
                                          query.getRoles()).execute();

    AddUserQueryReply result
      = new AddUserQueryReply(new UserQueryReply.User(user.getPrincipal()
                                                            .getName(),
                                                        user.getRoles()));

    return result;
  }
  */

  /*
  public ListUsersQueryReply listUsers(long id,
                                        String to,
                                        String from,
                                        ListUsersQuery query)
  {
    Hashtable<String,com.caucho.http.security.PasswordUser2> userMap
      = new ListUsersAction(_adminAuthenticator).execute();

    List<UserQueryReply.User> userList = new ArrayList<UserQueryReply.User>();

    for (Map.Entry<String,PasswordUser2> userEntry : userMap.entrySet()) {
      com.caucho.http.security.PasswordUser2 passwordUser = userEntry.getValue();
      UserQueryReply.User user = new UserQueryReply.User(userEntry.getKey(),
                                                           passwordUser.getRoles());
      userList.add(user);
    }

    UserQueryReply.User[] users
      = userList.toArray(new UserQueryReply.User[userList.size()]);

    ListUsersQueryReply result = new ListUsersQueryReply(users);

    return result;
  }

  public RemoveUserQueryReply removeUser(long id,
                                          String to,
                                          String from,
                                          RemoveUserQuery query)
  {
    PasswordUser2 user = new RemoveUserAction(_adminAuthenticator,
                                             query.getUser()).execute();

    RemoveUserQueryReply result
      = new RemoveUserQueryReply(new UserQueryReply.User(user.getPrincipal()
                                                               .getName(),
                                                           user.getRoles()));

    return result;
  }
  */
  
  public JsonQueryReply doJsonThreadDump()
  {
    return null;
  }
  
  public StatServiceValuesQueryReply getStats(String[] meters, 
                                              Date from, 
                                              Date to)
  {
    return null;
  }

  public String doThreadDump(boolean isJson)
  {
    if (isJson) {
      return new ThreadDumpAction().executeJson();
    } else {
      return new ThreadDumpAction().execute(32, false);
    }
  }

  public String doHeapDump(boolean isRaw)
  {
    try {
      String dump = new HeapDumpAction().execute(isRaw,
                                                 _server.getServerId(),
                                                 _hprofDir);
      
      return dump;
    } catch (JMException e) {
      e.printStackTrace();

      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String doStoreDump(String name)
  {
    return new StoreDumpAction().execute(name);
  }

  public boolean doStoreRestore(String name, StreamSource value)
  {
    new StoreRestoreAction().execute(name, value);
    
    return true;
  }
  
  public ListJmxQueryReply listJmx(String pattern,
                                   boolean isPrintAttributes,
                                   boolean isPrintValues,
                                   boolean isPrintOperations,
                                   boolean isPrintAllBeans,
                                   boolean isPrintPlatformBeans)
  {
    try {
      ListJmxQueryReply result
        = new ListJmxAction().execute(pattern,
                                      isPrintAttributes,
                                      isPrintValues,
                                      isPrintOperations,
                                      isPrintAllBeans,
                                      isPrintPlatformBeans);

      return result;
    } catch (JMException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  public JsonQueryReply doJmxDump()
  {
    try {
      String jmxDump = new JmxDumpAction().execute();

      JsonQueryReply result = new JsonQueryReply(jmxDump);

      return result;
    } catch (JMException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }  

  public JmxSetQueryReply setJmx(String pattern,
                                 String attribute,
                                 String value)
  {
    try {
      JmxSetQueryReply result = new SetJmxAction().execute(pattern,
                                                           attribute,
                                                           value);

      return result;
    } catch (JMException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
  
  public JmxCallQueryReply callJmx(String pattern,
                                   String operation,
                                   int operationIndex,
                                   String []params)
  {
    try {
      JmxCallQueryReply reply
        = new CallJmxAction().execute(pattern,
                                      operation,
                                      operationIndex,
                                      params);

      return reply;
    } catch (JMException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public StringQueryReply setLogLevel(String[] loggers, 
                                      Level level, 
                                      long period)
  {
    String message = new SetLogLevelAction().execute(loggers,
                                                     level,
                                                     period);

    StringQueryReply result = new StringQueryReply(message);

    return result;
  }

  Serializable getStats(long id, String to, String from, StatsQuery query)
  {
    GetStatsAction action = new GetStatsAction();

    StatServiceValuesQueryReply result = action.execute(query.getMeters(),
                                                         query.getFrom(),
                                                         query.getTo());

    return result;
  }

  /*
  public PdfReportQueryReply pdfReport(String path,
                                       String report, 
                                       long period, 
                                       String logDirectory, 
                                       long profileTime, 
                                       long samplePeriod, 
                                       boolean isSnapshot, 
                                       boolean isWatchdog, 
                                       boolean isLoadPdf)
  {
    PdfReportAction action = new PdfReportAction();
    
    if (path != null) {
      action.setPath(path);
    }
    
    if (period > 0) {
      action.setPeriod(period);
    }

    action.setSnapshot(isSnapshot);
    action.setWatchdog(isWatchdog);

    if (profileTime > 0) {
      action.setProfileTime(profileTime);
    }

    if (samplePeriod > 0) {
      action.setProfileTick(samplePeriod);
    }

    if (report != null) {
      action.setReport(report);
    }
    
    if (logDirectory != null) {
      action.setLogDirectory(logDirectory);
    }

    action.setReturnPdf(isLoadPdf);

    try {
      action.init();

      PdfReportAction.PdfReportActionResult actionResult
        = action.execute();

      StreamSource pdfSource = null;

      if (isLoadPdf) {
        pdfSource = new StreamSource(actionResult.getPdfOutputStream());
      }

      PdfReportQueryReply result
        = new PdfReportQueryReply(actionResult.getMessage(),
                                  actionResult.getFileName(),
                                  pdfSource);

      return result;
    } catch (RuntimeException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw e;
    } catch (IOException e) {
      log.log(Level.WARNING, e.getMessage(), e);

      throw new RuntimeException(e);
    }
  }
  */

  public StringQueryReply profile(long id,
                                   String to,
                                   String from,
                                   ProfileQuery query)
  {
    String profile = new ProfileAction().execute(query.getActiveTime(),
                                                 query.getPeriod(),
                                                 query.getDepth());

    StringQueryReply result = new StringQueryReply(profile);

    return result;
  }

  public Date []listRestarts(long id,
                                        String to,
                                        String from,
                                        ListRestartsQuery query)
  {
    final long now = CurrentTime.getCurrentTime();

    ServerBartender cloudServer = BartenderSystem.getCurrentSelfServer();

    int index = cloudServer.getServerIndex();

    StatSystem statSystem = SystemManager.getCurrentSystem(StatSystem.class);

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
    
    return restarts;
  }
  
  public StringQueryReply status(long id,
                                 String to,
                                 String from,
                                 ServerStatusQuery query)
  {
    ServerBartender server = BartenderSystem.getCurrentSelfServer();

    String status = L.l("Server {0} : {1}",
                        server,
                        server.getState());

    final StringQueryReply reply = new StringQueryReply(status);

    return reply;
  }
  
  public StringQueryReply scoreboard(long id,
                                           String to,
                                           String from,
                                           ScoreboardQuery query)
  {
    String message = new ScoreboardAction().excute(query.getType(),
                                                   query.isGreedy());

    StringQueryReply result = new StringQueryReply(message);

    return result;
  }
  
  public String enable(String serverId)
  {
    return null;
  }
  
  public String disable(String serverId)
  {
    return null;
  }
}
