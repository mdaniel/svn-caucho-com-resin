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
 * @author Sam
 */

package com.caucho.server.resin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.actor.LocalActorSender;
import com.caucho.bam.actor.RemoteActorSender;
import com.caucho.boot.LogLevelCommand;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.cloud.network.NetworkClusterSystem;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.env.repository.CommitBuilder;
import com.caucho.env.service.ResinSystem;
import com.caucho.jmx.MXParam;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ManagementMXBean;
import com.caucho.quercus.lib.reflection.ReflectionException;
import com.caucho.server.admin.AddUserQueryReply;
import com.caucho.server.admin.HmuxClientFactory;
import com.caucho.server.admin.JmxCallQueryReply;
import com.caucho.server.admin.JmxSetQueryReply;
import com.caucho.server.admin.JsonQueryReply;
import com.caucho.server.admin.ListJmxQueryReply;
import com.caucho.server.admin.ListUsersQueryReply;
import com.caucho.server.admin.ManagerClient;
import com.caucho.server.admin.ManagerProxyApi;
import com.caucho.server.admin.PdfReportQueryReply;
import com.caucho.server.admin.RemoveUserQueryReply;
import com.caucho.server.admin.StatServiceValuesQueryReply;
import com.caucho.server.admin.StringQueryReply;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.server.deploy.DeployClient;
import com.caucho.server.deploy.DeployControllerState;
import com.caucho.server.deploy.DeployTagResult;
import com.caucho.util.CharBuffer;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;

public class ManagementAdmin extends AbstractManagedObject
  implements ManagementMXBean
{
  private static final L10N L = new L10N(ManagementAdmin.class);
  private static Logger log = Logger.getLogger(ManagementAdmin.class.getName());

  private final Resin _resin;
  
  private final ConcurrentHashMap<String,ActorSender> _senderCache
    = new ConcurrentHashMap<String,ActorSender>();

  /**
   * Creates the admin object and registers with JMX.
   */
  public ManagementAdmin(Resin resin)
  {
    _resin = resin;

    registerSelf();
  }

  @Override
  public String getName()
  {
    return null;
  }

  @Override
  public String configDeploy(String serverId,
                             String stage,
                             String version,
                             String message,
                             InputStream is) throws ReflectionException
  {
    CommitBuilder commit = new CommitBuilder();

    if (stage != null)
      commit.stage(stage);

    commit.type("config");
    commit.tagKey("resin");


    if (message == null)
      message = "deploy config via REST";

    commit.message(message);

    if (version != null)
      DeployClient.fillInVersion(commit, version);

    DeployClient client = getWebappDeployClient(serverId);

    client.commitArchive(commit, is);

    return "Deployed config " + commit.getId() + " to " + client.getUrl();
  }

  @Override
  public InputStream configCat(String serverId,
                                String name,
                                String stage,
                                String version)
    throws ReflectionException
  {
    CommitBuilder commit = new CommitBuilder();
    if (stage != null)
      commit.stage(stage);

    commit.type("config");
    commit.tagKey("resin");

    try {
      TempOutputStream out = new TempOutputStream();

      DeployClient client = getWebappDeployClient(serverId);

      client.getFile(commit.getId(), name, out);

      out.flush();

      return out.getInputStream();
    } catch (IOException e) {
      throw ConfigException.create(e);
    }
  }

  @Override
  public String []configLs(String serverId,
                         String name,
                         String stage,
                         String version)
    throws ReflectionException
  {

    CommitBuilder commit = new CommitBuilder();
    if (stage != null)
      commit.stage(stage);

    commit.type("config");
    commit.tagKey("resin");

    try {
      DeployClient client = getWebappDeployClient(serverId);

      String []files = client.listFiles(commit.getId(), name);

      return files;
    } catch (IOException e) {
      throw ConfigException.create(e);
    }
  }

  @Override
  public String configUndeploy(String serverId,
                               String stage,
                               String version,
                               String message)
    throws ReflectionException
  {
    CommitBuilder commit = new CommitBuilder();
    if (stage != null)
      commit.stage(stage);

    commit.type("config");
    commit.tagKey("resin");

    if (message == null)
      message = "undeploy config via REST";

    commit.message(message);

    if (version != null)
      DeployClient.fillInVersion(commit, version);

    DeployClient client = getWebappDeployClient(serverId);

    client.undeploy(commit);

    return "Undeployed " + commit.getId() + " from " + client.getUrl();
  }

  @Override
  public StringQueryReply addLicense(String serverId,
                                      boolean isOverwrite,
                                      String to,
                                      boolean isRestart,
                                      InputStream in) throws ReflectionException
  {
    String licenseContent = null;

    ReadStream is = Vfs.openRead(in);
    CharBuffer cb = new CharBuffer();
    try {
      int ch;
      while ((ch = is.read()) >= 0)
        cb.append((char) ch);

      licenseContent = cb.toString();
    } catch (IOException e) {
      throw new ConfigException(L.l(
        "Failed to read license from request input stream: {0}",
        e.toString()), e);
    } finally {
      if (cb != null)
        cb.close();
      if (is != null)
        is.close();
    }

    if (licenseContent == null || licenseContent.isEmpty()) {
      throw new ConfigException(L.l(
        "Failed to read license from request input stream: empty"));
    }

    ManagerClient client = getManagerClient(serverId);

    return client.addLicense(licenseContent, to, isOverwrite, isRestart);
  }

  @Override public Date []listRestarts(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "period")
    String periodStr)
    throws ReflectionException
  {
    final long period = Period.toPeriod(periodStr);

    ManagerClient client = getManagerClient(serverId);

    Date []result = client.listRestarts(period);

    return result;
  }

  @Override
  public ListJmxQueryReply listJmx(String serverId,
                                   String pattern,
                                   boolean isPrintAttributes,
                                   boolean isPrintValues,
                                   boolean isPrintOperations,
                                   boolean isPrintAllBeans,
                                   boolean isPrintPlatformBeans)
  {
    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.listJmx(pattern,
                                 isPrintAttributes,
                                 isPrintValues,
                                 isPrintOperations,
                                 isPrintAllBeans,
                                 isPrintPlatformBeans);

  }

  @Override
  public StringQueryReply setLogLevel(String serverId,
                                       String loggersValue,
                                       String levelValue,
                                       String activeTime)
  {
    String[] loggers = null;

    loggers = parseValues(loggersValue);

    if (loggers.length == 0)
      loggers = new String[]{"", "com.caucho"};

    long period = 0;

    if (activeTime != null)
      period = Period.toPeriod(activeTime);

    Level level = LogLevelCommand.getLevel("-" + levelValue);

    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.setLogLevel(loggers, level, period);
  }

  @Override
  public JsonQueryReply doThreadDump(String serverId)
  {
    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.doJsonThreadDump();
  }

  @Override
  public PdfReportQueryReply pdfReport(String serverId,
                                        String report,
                                        String periodStr,
                                        String logDirectory,
                                        String profileTimeStr,
                                        String samplePeriodStr,
                                        boolean isSnapshot,
                                        boolean isWatchdog,
                                        boolean isLoadPdf)
  {
    long period = -1;

    if (periodStr != null)
      period = Period.toPeriod(periodStr);

    long profileTime = -1;

    if (profileTimeStr != null)
      profileTime = Period.toPeriod(profileTimeStr);

    long samplePeriod = -1;

    if (samplePeriodStr != null)
      samplePeriod = Period.toPeriod(samplePeriodStr, 1);

    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.pdfReport(null,
                                   report,
                                   serverId,
                                   period,
                                   logDirectory,
                                   profileTime,
                                   samplePeriod,
                                   isSnapshot,
                                   isWatchdog,
                                   isLoadPdf);
  }

  @Override
  public StatServiceValuesQueryReply getStats(String serverId,
                                               String metersStr,
                                               String periodStr)
  throws ReflectionException
  {
    Date to = new Date(CurrentTime.getCurrentTime());

    ManagerClient managerClient = getManagerClient(serverId);

    long period = Period.toPeriod(periodStr);

    Date from = new Date(to.getTime() - period);

    String []meters = metersStr.split(",");

    return managerClient.getStats(meters, from, to);
  }

  @Override
  public JmxSetQueryReply setJmx(String serverId,
                                  String pattern,
                                  String attribute,
                                  String value)
  {
    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.setJmx(pattern, attribute, value);
  }

  @Override
  public JmxCallQueryReply callJmx(String serverId,
                                   String pattern,
                                   String operation,
                                   String operationIdx,
                                   String values)
  {
    String[] params;
    params = parseValues(values);
    //
    int operationIndex = -1;
    if (operationIdx != null)
      operationIndex = Integer.parseInt(operationIdx);

    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.callJmx(pattern, operation, operationIndex, params);
  }

  //
  // deploy
  //

  @Override
  public DeployControllerState startWebApp(String serverId,
                                                      String tag,
                                                      String context,
                                                      String stage,
                                                      String host,
                                                      String version)
    throws ReflectionException
  {
    if (tag != null && context != null)
      throw new IllegalArgumentException(L.l(
        "can't specify context '{0}' with tag {1}",
        context,
        tag));

    if (tag == null)
      tag = makeTag(context, stage, host, version);

    WebAppDeployClient deployClient = getWebappDeployClient(serverId);

    DeployControllerState result = deployClient.start(tag);

    return result;
  }

  @Override
  public DeployControllerState stopWebApp(String serverId,
                                                     String tag,
                                                     String context,
                                                     String stage,
                                                     String host,
                                                     String version)
    throws ReflectionException
  {
    if (tag != null && context != null)
      throw new IllegalArgumentException(L.l(
        "can't specify context '{0}' with tag {1}",
        context,
        tag));

    if (tag == null)
      tag = makeTag(context, stage, host, version);

    WebAppDeployClient deployClient = getWebappDeployClient(serverId);

    DeployControllerState result = deployClient.stop(tag);

    return result;
  }

  @Override
  public DeployControllerState restartWebApp(String serverId,
                                                        String tag,
                                                        String context,
                                                        String stage,
                                                        String host,
                                                        String version)
    throws ReflectionException
  {
    if (tag != null && context != null)
      throw new IllegalArgumentException(L.l(
        "can't specify context '{0}' with tag {1}",
        context,
        tag));

    if (tag == null)
      tag = makeTag(context, stage, host, version);

    WebAppDeployClient deployClient = getWebappDeployClient(serverId);

    DeployControllerState result = deployClient.restart(tag);

    return result;
  }

  @Override
  public String webappDeploy(String serverId,
                             String context,
                             String host,
                             String stage,
                             String version,
                             String message,
                             InputStream is)
    throws ReflectionException
  {
    WebAppDeployClient deployClient = getWebappDeployClient(serverId);

    CommitBuilder commit = new CommitBuilder();

    commit.type("webapp");

    if (stage != null)
      commit.stage(stage);

    commit.tagKey(host + "/" + context);

    if (version != null)
      DeployClient.fillInVersion(commit, version);

    if (message == null)
      message = "deploy " + context + " via REST interface";

    commit.message(message);

    commit.attribute("user", System.getProperty("user.name"));

    deployClient.commitArchive(commit, is);

    String result = "Deployed "
                    + commit.getId()
                    + " to "
                    + deployClient.getUrl();

    return result;
  }

  @Override
  public String deployCopy(String serverId,
                           String sourceContext,
                           String sourceHost,
                           String sourceStage,
                           String sourceVersion,
                           String targetContext,
                           String targetHost,
                           String targetStage,
                           String targetVersion,
                           String message) throws ReflectionException
  {

    if (sourceContext == null)
      throw new IllegalArgumentException(L.l("missing source parameter"));

    if (sourceHost == null)
      sourceHost = "default";

    CommitBuilder source = new CommitBuilder();
    source.type("webapp");

    if (sourceStage != null)
      source.stage(sourceStage);

    source.tagKey(sourceHost + "/" + sourceContext);


    if (targetContext == null)
      throw new IllegalArgumentException(L.l("missing target parameter"));

    if (targetHost == null)
      targetHost = "default";

    CommitBuilder target = new CommitBuilder();
    target.type("webapp");

    if (targetStage != null)
      target.stage(targetStage);

    target.tagKey(targetHost + "/" + targetContext);

    if (sourceVersion != null)
      DeployClient.fillInVersion(source, sourceVersion);

    if (targetVersion != null)
      DeployClient.fillInVersion(source, sourceVersion);

    if (message == null)
      message = L.l("copy '{0}' to '{1}'", source.getTagKey(), target.getTagKey());

    target.message(message);

    WebAppDeployClient deployClient = getWebappDeployClient(serverId);

    deployClient.copyTag(target, source);

    String result = L.l("copied {0} to {1}", source.getId(), target.getId());

    return result;
  }

  @Override
  public DeployTagResult []deployList(String serverId, String pattern)
    throws ReflectionException
  {
    WebAppDeployClient deployClient = getWebappDeployClient(serverId);

    DeployTagResult []result = deployClient.queryTags(pattern);

    return result;
  }

  @Override
  public String undeploy(String serverId,
                         String context,
                         String host,
                         String stage,
                         String version,
                         String message)
  throws ReflectionException
  {
    if (context == null) {
      throw new IllegalArgumentException(L.l("missing context parameter"));
    }

    CommitBuilder commit = new CommitBuilder();
    commit.type("webapp");

    if (stage != null)
      commit.stage(stage);

    commit.tagKey(host + "/" + context);

    if (message == null)
      message = "undeploy " + context + " via REST interface";

    commit.message(message);

    if (version != null)
      DeployClient.fillInVersion(commit, version);

    WebAppDeployClient deployClient = getWebappDeployClient(serverId);

    deployClient.removeTag(commit);

    String result = L.l("Undeployed {0} from {1}",
                        context,
                        deployClient.getUrl());

    return result;
  }

  @Override
  public String enable(String serverId)
  {
    ManagerProxyApi proxy = getManagerProxy(serverId);

    return proxy.enable();
  }

  @Override
  public String disable(String serverId)
  {
    ManagerProxyApi proxy = getManagerProxy(serverId);

    return proxy.disable();
  }

  @Override
  public String disableSoft(String serverId)
    throws javax.management.ReflectionException
  {
    ManagerProxyApi proxy = getManagerProxy(serverId);

    return proxy.disableSoft();
  }

  //
  // jmx dump
  //

  @Override
  public JsonQueryReply doJmxDump(String serverId)
  {
    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.doJmxDump();
  }

  //
  // user admin
  //

  @Override
  public AddUserQueryReply addUser(String serverId,
                                    String user,
                                    String password,
                                    String rolesStr)
  throws ReflectionException
  {
    String[] roles;
    if (rolesStr != null)
      roles = rolesStr.split("(,|;)");
    else
      roles = new String[]{};

    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.addUser(user, password.toCharArray(), roles);
  }

  @Override
  public ListUsersQueryReply listUsers(String serverId)
    throws ReflectionException
  {
    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.listUsers();
  }

  @Override
  public RemoveUserQueryReply removeUser(String serverId,
                                          String user)
  throws ReflectionException
  {
    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.removeUser(user);
  }

  @Override
  public StringQueryReply getStatus(String serverId)
  {
    ManagerClient managerClient = getManagerClient(serverId);
    
    return managerClient.status();
  }

  private String makeTag(String name,
                         String stage,
                         String host,
                         String version)
  {
    String tag = stage + "/webapp/" + host;

    if (name.startsWith("/"))
      tag = tag + name;
    else
      tag = tag + '/' + name;

    if (version != null)
      tag = tag + '-' + version;

    return tag;
  }

  private CloudServer getServer(String server)
  {
    CloudServer cloudServer;

    if (server == null)
      cloudServer = NetworkClusterSystem.getCurrent().getSelfServer();
    else {
      CloudServer selfServer = NetworkClusterSystem.getCurrent().getSelfServer(); 
      cloudServer = selfServer.getPod().findServerByDisplayId(server);
    }

    return cloudServer;
  }

  private String[] parseValues(String values)
  {
    List<String> params = new ArrayList<String>();
    StringBuilder builder = null;

    if (values != null) {
      char[] chars = values.toCharArray();

      for (int i = 0; i < chars.length; i++) {
        char c = chars[i];

        if (c == ' ') {

          if (builder != null) {
            params.add(builder.toString());

            builder = null;
          }

          continue;
        }

        if ('\'' == c || '"' == c) {
          char quote = c;

          i++;

          final int start = i;

          while (i < chars.length) {
            if (chars[i] == quote && chars[i - 1] != '\\')
              break;

            i++;
          }

          final int end = i;

          if (i == chars.length || chars[end] != quote)
            throw new IllegalArgumentException(L.l(
              "`{0}' expected at {1} in '{2}'",
              quote,
              end,
              values));

          builder = new StringBuilder();
          for (int j = start; j < end; j++) {
            if (chars[j] == '\\' && chars[j + 1] == '\'') {
              builder.append('\'');
              j++;
            }
            else if (chars[j] == '\\' && chars[j + 1] == '"') {
              builder.append('"');
              j++;
            }
            else {
              builder.append(chars[j]);
            }
          }
          params.add(builder.toString());

          builder = null;
        }
        else {
          if (builder == null)
            builder = new StringBuilder();

          builder.append(c);
        }
      }

      if (builder != null)
        params.add(builder.toString());
    }

    return params.toArray(new String[params.size()]);
  }

  private ManagerProxyApi getManagerProxy(String serverId)
  {
    CloudServer server = getServer(serverId);

    if (server == null)
      throw ConfigException.create(new IllegalArgumentException(L.l("unknown server '{0}'", serverId)));

    ManagerProxyApi proxy = server.getData(ManagerProxyApi.class);

    if (proxy == null) {
      ManagerClient client = getManagerClient(serverId);

      proxy = client.createAgentProxy(ManagerProxyApi.class,
                                      "manager-proxy@resin.caucho");
      
      ManagerProxyApi presentProxy = server.putDataIfAbsent(proxy);

/*      proxy = server.putDataIfAbsent(proxy);*/
    }

    return proxy;
  }

  private ManagerClient getManagerClient(String serverId)
  {
    final ActorSender sender = getSender(serverId);

    return new ManagerClient(sender);
  }

  private WebAppDeployClient getWebappDeployClient(String serverId)
  {
    final ActorSender sender = getSender(serverId);

    String url;
    if (sender instanceof RemoteActorSender)
      url = ((RemoteActorSender) sender).getUrl();
    else
      url = sender.getAddress();

    return new WebAppDeployClient(url, sender);
  }

  
  private ActorSender getSender(String serverId)
  {
    if (serverId == null) {
      serverId = ResinSystem.getCurrentId();
    }
    
    ActorSender sender = _senderCache.get(serverId);
  
    if (sender == null) {
      sender = getSenderImpl(serverId);
      
      _senderCache.putIfAbsent(serverId, sender);
      
      sender = _senderCache.get(serverId);
    }
    
    return sender;
  }
  
  private ActorSender getSenderImpl(String serverId)
  {
    final ActorSender sender;

    CloudServer server = getServer(serverId);

    if (server != null && server.isSelf()
        || server == null && serverId.equals(ResinSystem.getCurrentId())) {
      sender = new LocalActorSender(BamSystem.getCurrentBroker(), "");
    }
    else if (server == null) {
      throw ConfigException.create(new IllegalArgumentException(L.l("unknown server '{0}'", serverId)));
    }
    else {
      String authKey = Resin.getCurrent().getClusterSystemKey();

      HmuxClientFactory hmuxFactory
        = new HmuxClientFactory(server.getAddress(),
                                server.getPort(),
                                "",
                                authKey);

      sender = hmuxFactory.create();
    }

    return sender;
  }
  
  public InputStream test(String value, InputStream is)
    throws IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    out.write(value.getBytes());
    out.write('\n');

    int ch;

    while ((ch = is.read()) >= 0) {
      out.write(ch);
    }

    return new ByteArrayInputStream(out.toByteArray());
  }
}
