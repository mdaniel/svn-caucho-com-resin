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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.amp.AmpSystem;
import com.caucho.amp.ServiceManagerAmp;
import com.caucho.amp.remote.ClientAmp;
import com.caucho.amp.remote.ClientAmpLocal;
import com.caucho.bartender.ServerBartender;
import com.caucho.cli.boot.LogLevelCommand;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.health.action.JmxSetQueryReply;
import com.caucho.jmx.AdminStream;
import com.caucho.jmx.Description;
import com.caucho.jmx.MXAction;
import com.caucho.jmx.MXContentType;
import com.caucho.jmx.MXParam;
import com.caucho.jmx.server.ManagedObjectBase;
import com.caucho.management.server.ManagementMXBean;
import com.caucho.server.admin.AddUserQueryReply;
import com.caucho.server.admin.JmxCallQueryReply;
import com.caucho.server.admin.JsonQueryReply;
import com.caucho.server.admin.ListJmxQueryReply;
import com.caucho.server.admin.ListUsersQueryReply;
import com.caucho.server.admin.ManagerProxyApi;
import com.caucho.server.admin.ManagerServiceApi;
import com.caucho.server.admin.PdfReportQueryReply;
import com.caucho.server.admin.RemoveUserQueryReply;
import com.caucho.server.admin.StatServiceValuesQueryReply;
import com.caucho.server.admin.StringQueryReply;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.server.container.ServerBase;
import com.caucho.server.deploy.DeployControllerState;
import com.caucho.server.deploy.DeployTagResult;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.Vfs;
import javax.management.ReflectionException;

public class ManagementAdmin extends ManagedObjectBase
  implements ManagementMXBean
{
  private static final L10N L = new L10N(ManagementAdmin.class);
  private static Logger log = Logger.getLogger(ManagementAdmin.class.getName());

  private final ServerBase _server;
  
  //private final ConcurrentHashMap<String,ActorSender> _senderCache
//    = new ConcurrentHashMap<String,ActorSender>();

  /**
   * Creates the admin object and registers with JMX.
   */
  public ManagementAdmin(ServerBase resin)
  {
    _server = resin;

    registerSelf();
  }

  @Override
  public String getName()
  {
    return null;
  }

  @Override
  public StringQueryReply addLicense(String serverId,
                                      boolean isOverwrite,
                                      String to,
                                      boolean isRestart,
                                      AdminStream in)
    throws ReflectionException
  {
    String licenseContent = null;

    StringBuilder cb = new StringBuilder();
    
    try (ReadStream is = Vfs.openRead(in.openRead())) {
      int ch;
      while ((ch = is.read()) >= 0)
        cb.append((char) ch);

      licenseContent = cb.toString();
    } catch (IOException e) {
      throw new ConfigException(L.l(
        "Failed to read license from request input stream: {0}",
        e.toString()), e);
    }

    if (licenseContent == null || licenseContent.isEmpty()) {
      throw new ConfigException(L.l(
        "Failed to read license from request input stream: empty"));
    }

    ManagerServiceApi client = getManagerClient(serverId);

    return client.addLicense(licenseContent, to, isOverwrite, isRestart);
  }

  @Override public Date []listRestarts(@MXParam("server") String serverId,
                                       @MXParam("period") String periodStr)
    throws ReflectionException
  {
    final long period = Period.toPeriod(periodStr);

    ManagerServiceApi client = getManagerClient(serverId);

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
    ManagerServiceApi managerClient = getManagerClient(serverId);

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

    ManagerServiceApi managerClient = getManagerClient(serverId);

    return managerClient.setLogLevel(loggers, level, period);
  }

  @Override
  public JsonQueryReply doThreadDump(String serverId)
  {
    ManagerServiceApi managerClient = getManagerClient(serverId);

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

    ManagerServiceApi managerClient = getManagerClient(serverId);

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
  public StatServiceValuesQueryReply getStats(String serverId,
                                               String metersStr,
                                               String periodStr)
  throws ReflectionException
  {
    Date to = new Date(CurrentTime.getCurrentTime());

    ManagerServiceApi managerClient = getManagerClient(serverId);

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
    ManagerServiceApi managerClient = getManagerClient(serverId);

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

    ManagerServiceApi managerClient = getManagerClient(serverId);

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
  public DeployTagResult []deployList(String serverId, String pattern)
    throws ReflectionException
  {
    WebAppDeployClient deployClient = getWebappDeployClient(serverId);

    DeployTagResult []result = deployClient.queryTags(pattern);

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
    ManagerServiceApi managerClient = getManagerClient(serverId);

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

    ManagerServiceApi managerClient = getManagerClient(serverId);

    return managerClient.addUser(user, password.toCharArray(), roles);
  }

  @Override
  public ListUsersQueryReply listUsers(String serverId)
    throws ReflectionException
  {
    ManagerServiceApi managerClient = getManagerClient(serverId);

    return managerClient.listUsers();
  }

  @Override
  public RemoveUserQueryReply removeUser(String serverId,
                                          String user)
  throws ReflectionException
  {
    ManagerServiceApi managerClient = getManagerClient(serverId);

    return managerClient.removeUser(user);
  }

  @Override
  public StringQueryReply getStatus(String serverId)
  {
    ManagerServiceApi managerClient = getManagerClient(serverId);
    
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

  private ServerBartender getServer(String server)
  {
    /* XXX:
    CloudServer cloudServer;

    if (server == null)
      cloudServer = NetworkClusterSystem.getCurrent().getSelfServer();
    else {
      ServerBartender selfServer = NetworkClusterSystem.getCurrent().getSelfServer(); 
      cloudServer = selfServer.getPod().findServerByDisplayId(server);
    }

    return cloudServer;
    */
    
    return null;
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
    ServerBartender server = getServer(serverId);

    if (server == null)
      throw ConfigException.create(new IllegalArgumentException(L.l("unknown server '{0}'", serverId)));

    ManagerProxyApi proxy = null;//server.getData(ManagerProxyApi.class);

    if (proxy == null) {
      /*
      proxy = getManagerClient(serverId);
      
      ManagerActorApi presentProxy = server.putDataIfAbsent(proxy);

      proxy = server.putDataIfAbsent(proxy);
      */
    }

    return proxy;
  }

  private ManagerServiceApi getManagerClient(String serverId)
  {
    ServerBartender server = getServer(serverId);

  //private WebAppDeployClient getWebappDeployClient(String serverId)
  //{
    //final ActorSender sender = getSender(serverId);

    ServiceManagerAmp rampManager = AmpSystem.getCurrentManager();
    
    return rampManager.lookup("/manager").as(ManagerServiceApi.class);
  }

  private WebAppDeployClient getWebappDeployClient(String serverId)
  {
    ServerBartender server = getServer(serverId);

    if (server == null) {
      throw ConfigException.create(new IllegalArgumentException(L.l("unknown server '{0}'", serverId)));
    }
    
    // RampManager rampManager = AmpSystem.getCurrentManager();
    
    String url = "champ://" + server.getId() + "/resin/deploy";
    
    // return new WebAppDeployClient(url, null, null);
    
    // throw new UnsupportedOperationException(getClass().getName());
    
    ClientAmp rampClient = new ClientAmpLocal(AmpSystem.getCurrentManager());
    
    return new WebAppDeployClient(rampClient);
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

  /* (non-Javadoc)
   * @see com.caucho.management.server.ManagementMXBean#webappDeploy(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.caucho.jmx.AdminStream)
   */
  @Override
  @Description("deploys an application")
  @MXAction(value = "web-app-deploy", method = "POST")
  @MXContentType
  public String webappDeploy(@MXParam("server") String serverId,
                             @MXParam(value = "context", required = true) String context,
                             @MXParam(value = "host", defaultValue = "default") String host,
                             @MXParam("stage") String stage,
                             @MXParam("version") String version,
                             @MXParam("message") String message, AdminStream is)
      throws javax.management.ReflectionException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.management.server.ManagementMXBean#deployCopy(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @Description("copies a deployment to a new tag name")
  @MXAction(value = "deploy-copy", method = "POST")
  @MXContentType
  public String deployCopy(@MXParam("server") String serverId,
                           @MXParam("source-context") String sourceContext,
                           @MXParam(value = "source-host", defaultValue = "default") String sourceHost,
                           @MXParam("source-stage") String sourceStage,
                           @MXParam("source-version") String sourceVersion,
                           @MXParam("target-context") String targetContext,
                           @MXParam(value = "target-host", defaultValue = "default") String targetHost,
                           @MXParam("target-stage") String targetStage,
                           @MXParam("target-version") String targetVersion,
                           @MXParam("message") String message)
      throws javax.management.ReflectionException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.management.server.ManagementMXBean#undeploy(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @Description("undeploys an application")
  @MXAction(value = "web-app-undeploy", method = "POST")
  @MXContentType
  public String undeploy(@MXParam("server") String serverId,
                         @MXParam(value = "context", required = true) String context,
                         @MXParam(value = "host", defaultValue = "default") String host,
                         @MXParam("stage") String stage,
                         @MXParam("version") String version,
                         @MXParam("message") String message)
      throws javax.management.ReflectionException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see com.caucho.management.server.ManagementMXBean#configUndeploy(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  @Description("undeploy configuration")
  @MXAction(value = "config-undeploy", method = "POST")
  @MXContentType
  public String configUndeploy(@MXParam("server") String serverId,
                               @MXParam("stage") String stage,
                               @MXParam("version") String version,
                               @MXParam("message") String message)
      throws javax.management.ReflectionException
  {
    // TODO Auto-generated method stub
    return null;
  }
}
