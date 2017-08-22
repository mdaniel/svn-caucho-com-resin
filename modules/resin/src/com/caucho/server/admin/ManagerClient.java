/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.bam.RemoteListenerUnavailableException;
import com.caucho.bam.ServiceUnavailableException;
import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.actor.RemoteActorSender;
import com.caucho.bam.broker.ManagedBroker;
import com.caucho.bam.manager.BamManager;
import com.caucho.bam.manager.SimpleBamManager;
import com.caucho.bam.proxy.BamProxyFactory;
import com.caucho.hmtp.HmtpClient;
import com.caucho.server.cluster.ServletService;
import com.caucho.util.L10N;

import java.io.Serializable;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deploy Client API
 */
public class ManagerClient
{
  private static final Logger log
    = Logger.getLogger(ManagerClient.class.getName());
  private static final L10N L = new L10N(ManagerClient.class);

  private static final long MANAGER_TIMEOUT = 600 * 1000L;
  
  private BamManager _bamManager;

  private String _url;

  private ActorSender _bamClient;
  private String _managerAddress;
  
  private ManagerProxyApi _managerProxy;

  public ManagerClient()
  {
    ServletService server = ServletService.getCurrent();

    if (server == null)
      throw new IllegalStateException(L.l("ManagerClient was not called in a Resin context. For external clients, use the ManagerClient constructor with host,port arguments."));

    _bamManager = server.getBamManager();
    _bamClient = server.createAdminClient(getClass().getSimpleName());

    _managerAddress = "manager@resin.caucho";
    
    initImpl();
  }

  public ManagerClient(String serverId)
  {
    ServletService server = ServletService.getCurrent();

    if (server == null)
      throw new IllegalStateException(L.l("ManagerClient was not called in a Resin context. For external clients, use the ManagerClient constructor with host,port arguments."));

    _bamManager = server.getBamManager();
    _bamClient = server.createAdminClient(getClass().getSimpleName());

    _managerAddress = "manager@" + serverId + ".resin.caucho";
    
    initImpl();
  }
  
  public ManagerClient(ActorSender bamClient)
  {
    this(new SimpleBamManager(bamClient.getBroker()), bamClient);
  }
  
  public ManagerClient(BamManager bamManager,
                       ActorSender bamClient)
  {
    _bamManager = bamManager;
    _bamClient = bamClient;

    _managerAddress = "manager@resin.caucho";
    // _managerAddress = bamClient.getAddress();
    
    initImpl();
  }

  public ManagerClient(String host, int serverPort, int httpPort,
                       String userName, String password)
  {
    RuntimeException exn = null;
    
    try {
      if (serverPort > 0) {
        HmuxClientFactory clientFactory
          = new HmuxClientFactory(host, serverPort, userName, password);
        
        RemoteActorSender remoteClient = clientFactory.create();
        
        _bamManager = new SimpleBamManager(remoteClient.getBroker());
        _bamClient = remoteClient;
      }
    
      _managerAddress = "manager@resin.caucho";
      
      if (_bamClient != null)
        return;
    } catch (RemoteConnectionFailedException e) {
      exn = new RemoteConnectionFailedException(L.l("Connection to '{0}:{1}' failed for remote admin. Check the server and make sure <resin:RemoteAdminService> is enabled in the resin.xml.\n  {2}",
                                                    host, serverPort,
                                                    e.getMessage()),
                                                    e);
      
      if (httpPort == 0)
        throw exn;
    } catch (RemoteListenerUnavailableException e) {
      exn = new RemoteListenerUnavailableException(L.l("HMTP request to '{0}:{1}' failed for remote admin, because remote administration was not enabled. Check that <resin:RemoteAdminService> is enabled in the resin.xml.\n  {2}",
                                                       host, serverPort,
                                                       e.getMessage()),
                                                       e);
      
      if (httpPort == 0)
        throw exn;
    }
    
    String url = "http://" + host + ":" + httpPort + "/hmtp";
    
    _url = url;
    
    HmtpClient client = new HmtpClient(url);
    try {
      client.setVirtualHost("admin.resin");

      client.connect(userName, password);

      _bamManager = new SimpleBamManager(client.getBroker());
      _bamClient = client;
    
      _managerAddress = "manager@resin.caucho";
    } catch (RemoteConnectionFailedException e) {
      throw new RemoteConnectionFailedException(L.l("Connection to '{0}' failed for remote admin. Check the server and make sure <resin:RemoteAdminService> is enabled in the resin.xml.\n  {1}",
                                                    url, e.getMessage()),
                                                e);
    } catch (RemoteListenerUnavailableException e) {
      throw new RemoteListenerUnavailableException(L.l("Remote admin request to '{0}' failed because remote administration has not been abled. Check the server and make sure <resin:RemoteAdminService> is enabled in the resin.xml.\n  {1}",
                                                    url, e.getMessage()),
                                                e);
    }
    
    initImpl();
  }
  
  private void initImpl()
  {
    _managerProxy = createAgentProxy(ManagerProxyApi.class,
                                     "manager-proxy@resin.caucho");
  }
  
  public String getUrl()
  {
    return _url;
  }

  public ActorSender getSender()
  {
    return _bamClient;
  }
  
  public <T> T createAgentProxy(Class<T> api, String address)
  {
    return _bamManager.createProxy(api,
                                   _bamManager.createActorRef(address),
                                   getSender());
  }
  
  private ManagerProxyApi getManagerProxy()
  {
    return _managerProxy;
  }

  public AddUserQueryReply addUser(String user,
                                    char []password,
                                    String []roles)
  {
    AddUserQuery query = new AddUserQuery(user, password, roles);

    return (AddUserQueryReply) query(query);
  }

  public RemoveUserQueryReply removeUser(String user)
  {
    RemoveUserQuery query = new RemoveUserQuery(user);

    return (RemoveUserQueryReply) query(query);
  }

  public ListUsersQueryReply listUsers()
  {
    ListUsersQuery query = new ListUsersQuery();

    return (ListUsersQueryReply) query(query);
  }

  public StringQueryReply doThreadDump()
  {
    ThreadDumpQuery query = new ThreadDumpQuery();

    return (StringQueryReply) query(query);
  }

  public JsonQueryReply doJsonThreadDump()
  {
    ThreadDumpQuery query = new ThreadDumpQuery(true);

    return (JsonQueryReply) query(query);
  }

  public StringQueryReply doHeapDump(boolean raw)
  {
    HeapDumpQuery query = new HeapDumpQuery(raw);

    return (StringQueryReply) query(query);
  }

  public JsonQueryReply doJmxDump()
  {
    JmxDumpQuery query = new JmxDumpQuery();

    return (JsonQueryReply) query(query);
  }

  public StringQueryReply setLogLevel(String []loggers,
                                           Level logLevel,
                                           long period)
  {
    LogLevelQuery query = new LogLevelQuery(loggers, logLevel, period);

    return (StringQueryReply) query(query);
  }

  public ListJmxQueryReply listJmx(String pattern,
                                    boolean isPrintAttributes,
                                    boolean isPrintValues,
                                    boolean isPrintOperations,
                                    boolean isAll,
                                    boolean isPlatform)
  {
    JmxListQuery query = new JmxListQuery(pattern,
                                          isPrintAttributes,
                                          isPrintValues,
                                          isPrintOperations,
                                          isAll,
                                          isPlatform);

    return (ListJmxQueryReply) query(query);
  }

  public JmxSetQueryReply setJmx(String pattern,
                                      String attribute,
                                      String value)
  {
    JmxSetQuery query = new JmxSetQuery(pattern, attribute, value);

    return (JmxSetQueryReply) query(query);
  }

  public JmxCallQueryReply callJmx(String pattern,
                                   String operation,
                                   int opIndex,
                                   String []trailingArgs)
  {
    JmxCallQuery query = new JmxCallQuery(pattern,
                                          operation,
                                          opIndex,
                                          trailingArgs);

    return (JmxCallQueryReply) query(query);
  }

  public PdfReportQueryReply pdfReport(String path,
                                        String report,
                                        String serverId,
                                        long period,
                                        String logDirectory,
                                        long profileTime,
                                        long samplePeriod,
                                        boolean isSnapshot,
                                        boolean isWatchdog,
                                        boolean isReportReturned)
  {
    PdfReportQuery query = new PdfReportQuery(path,
                                              report,
                                              serverId,
                                              period,
                                              logDirectory,
                                              profileTime,
                                              samplePeriod,
                                              isSnapshot,
                                              isWatchdog,
                                              isReportReturned);

    long timeout;

    if (profileTime > 0)
      timeout = profileTime + 60000L;
    else
      timeout = 60000L;

    return (PdfReportQueryReply) query(query, timeout);
  } 

  public StringQueryReply profile(long activeTime, long period, int depth)
  {
    ProfileQuery query = new ProfileQuery(activeTime, period, depth);

    return (StringQueryReply) query(query);
  }

  public Date []listRestarts(long period)
  {
    ListRestartsQuery query = new ListRestartsQuery(period);

    return (Date[]) query(query);
  }

  public StringQueryReply addLicense(String licenseContent,
                                      String fileName,
                                      boolean overwrite,
                                      boolean restart)
  {
    LicenseAddQuery query = new LicenseAddQuery(licenseContent, 
                                                fileName,
                                                overwrite, 
                                                restart);
    return (StringQueryReply) query(query);
  }

  public StatServiceValuesQueryReply getStats(String []meters, Date from, Date to)
  {
    StatsQuery query = new StatsQuery(meters, from, to);

    return (StatServiceValuesQueryReply) query(query);
  }

  public StringQueryReply status()
  {
    ServerStatusQuery status = new ServerStatusQuery();

    return (StringQueryReply) query(status);
  }

  public StringQueryReply statusWebApp()
  {
    ServerStatusWebAppQuery status = new ServerStatusWebAppQuery();

    return (StringQueryReply) query(status);
  }

  public StringQueryReply scoreboard(String type, boolean greedy)
  {
    ScoreboardQuery query = new ScoreboardQuery(type, greedy);

    return (StringQueryReply) query(query);
  }

  //
  // enable/disable
  //
  
  public String enable(String serverId)
  {
    ManagerProxyApi manager = getManagerProxy();
    
    return manager.enable();
  }
  
  public String disable(String serverId)
  {
    ManagerProxyApi manager = getManagerProxy();
    
    return manager.disable();
  }

  protected Serializable query(Serializable query)
  {
    try {
      return _bamClient.query(_managerAddress, query);
    } catch (ServiceUnavailableException e) {
      throw new ServiceUnavailableException("Manager service is not available, possibly because the resin.xml is missing a <resin:ManagerService> tag\n  " + e.getMessage(),
                                            e);
    }
  }

  protected Serializable query(Serializable query, long timeout)
  {
    try {
      return _bamClient.query(_managerAddress,
                              query,
                              timeout);
    } catch (ServiceUnavailableException e) {
      throw new ServiceUnavailableException("Manager service is not available, possibly because the resin.xml is missing a <resin:ManagerService> tag\n  " + e.getMessage(),
                                            e);
    }
  }

  public void close()
  {
    _bamClient.close();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _bamClient + "]";
  }
}

