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

import com.caucho.bam.actor.ActorSender;
import com.caucho.bam.actor.LocalActorSender;
import com.caucho.bam.actor.RemoteActorSender;
import com.caucho.boot.LogLevelCommand;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.cloud.network.NetworkClusterSystem;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.types.Period;
import com.caucho.env.repository.CommitBuilder;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ManagementMXBean;
import com.caucho.quercus.lib.reflection.ReflectionException;
import com.caucho.server.admin.AddUserQueryResult;
import com.caucho.server.admin.ControllerStateActionQueryResult;
import com.caucho.server.admin.DeployClient;
import com.caucho.server.admin.HmuxClientFactory;
import com.caucho.server.admin.ListUsersQueryResult;
import com.caucho.server.admin.ManagerClient;
import com.caucho.server.admin.PdfReportQueryResult;
import com.caucho.server.admin.RemoveUserQueryResult;
import com.caucho.server.admin.StringQueryResult;
import com.caucho.server.admin.TagResult;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.util.L10N;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManagementAdmin extends AbstractManagedObject
  implements ManagementMXBean
{
  private static final L10N L = new L10N(ManagementAdmin.class);
  private static Logger log = Logger.getLogger(ManagementAdmin.class.getName());

  private final Resin _resin;

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

  /**
   * Test interface
   */
  @Override
  public String hello()
  {
    return "hello, world";
  }

  @Override
  public StringQueryResult listJmx(String serverId,
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
  public StringQueryResult setLogLevel(String serverId,
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
  public StringQueryResult doThreadDump(String serverId)
  {
    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.doThreadDump();
  }

  @Override
  public PdfReportQueryResult pdfReport(String serverId,
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
                                   period,
                                   logDirectory,
                                   profileTime,
                                   samplePeriod,
                                   isSnapshot,
                                   isWatchdog,
                                   isLoadPdf);
  }

  @Override
  public StringQueryResult setJmx(String serverId,
                                  String pattern,
                                  String attribute,
                                  String value)
  {
    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.setJmx(pattern, attribute, value);
  }

  @Override
  public StringQueryResult callJmx(String serverId,
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

  @Override
  public ControllerStateActionQueryResult startWebApp(String serverId,
                                                      String tag,
                                                      String name,
                                                      String stage,
                                                      String host,
                                                      String version)
    throws ReflectionException
  {
    if (tag != null && name != null)
      throw new IllegalArgumentException(L.l(
        "can't specify name '{0}' with tag {1}",
        name,
        tag));

    if (tag == null)
      tag = makeTag(name, stage, host, version);

    WebAppDeployClient deployClient = getWebappDeployClient(serverId);

    ControllerStateActionQueryResult result = deployClient.start(tag);

    return result;
  }

  @Override
  public ControllerStateActionQueryResult stopWebApp(String serverId,
                                                     String tag,
                                                     String name,
                                                     String stage,
                                                     String host,
                                                     String version)
    throws ReflectionException
  {
    if (tag != null && name != null)
      throw new IllegalArgumentException(L.l(
        "can't specify name '{0}' with tag {1}",
        name,
        tag));

    if (tag == null)
      tag = makeTag(name, stage, host, version);

    WebAppDeployClient deployClient = getWebappDeployClient(serverId);

    ControllerStateActionQueryResult result = deployClient.stop(tag);

    return result;
  }

  @Override
  public ControllerStateActionQueryResult restartWebApp(String serverId,
                                                        String tag,
                                                        String name,
                                                        String stage,
                                                        String host,
                                                        String version)
    throws ReflectionException
  {
    if (tag != null && name != null)
      throw new IllegalArgumentException(L.l(
        "can't specify name '{0}' with tag {1}",
        name,
        tag));

    if (tag == null)
      tag = makeTag(name, stage, host, version);

    WebAppDeployClient deployClient = getWebappDeployClient(serverId);

    ControllerStateActionQueryResult result = deployClient.restart(tag);

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
  public TagResult []deployList(String serverId, String pattern)
    throws ReflectionException
  {
    WebAppDeployClient deployClient = getWebappDeployClient(serverId);

    TagResult []result = deployClient.queryTags(pattern);

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
  public StringQueryResult doJmxDump(String serverId)
  {
    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.doJmxDump();
  }

  @Override
  public AddUserQueryResult addUser(String serverId,
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
  public ListUsersQueryResult listUsers(String serverId)
    throws ReflectionException
  {
    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.listUsers();
  }

  @Override
  public RemoveUserQueryResult removeUser(String serverId,
                                          String user)
  throws ReflectionException
  {
    ManagerClient managerClient = getManagerClient(serverId);

    return managerClient.removeUser(user);
  }

  @Override
  public StringQueryResult getStatus(String serverId)
  {
    return null;
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
    else cloudServer = NetworkClusterSystem.getCurrent()
                                           .getSelfServer()
                                           .getPod()
                                           .findServer(
                                             server);

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

  private ManagerClient getManagerClient(String serverId) {
    final ActorSender sender;

    CloudServer server = getServer(serverId);

    if (server.isSelf()) {
      sender = new LocalActorSender(BamSystem.getCurrentBroker(), "");
    }
    else {
      String authKey = Resin.getCurrent().getResinSystemAuthKey();

      HmuxClientFactory hmuxFactory
        = new HmuxClientFactory(server.getAddress(),
                                server.getPort(),
                                "",
                                authKey);

      sender = hmuxFactory.create();
    }

    return new ManagerClient(sender);
  }

  private WebAppDeployClient getWebappDeployClient(String serverId)
  {
    final ActorSender sender;

    CloudServer server = getServer(serverId);

    if (server.isSelf()) {
      sender = new LocalActorSender(BamSystem.getCurrentBroker(), "");
    }
    else {
      String authKey = Resin.getCurrent().getResinSystemAuthKey();

      HmuxClientFactory hmuxFactory
        = new HmuxClientFactory(server.getAddress(),
                                server.getPort(),
                                "",
                                authKey);

      sender = hmuxFactory.create();
    }

    String url;
    if (sender instanceof RemoteActorSender)
      url = ((RemoteActorSender) sender).getUrl();
    else
      url = sender.getAddress();

    return new WebAppDeployClient(url, sender);
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
