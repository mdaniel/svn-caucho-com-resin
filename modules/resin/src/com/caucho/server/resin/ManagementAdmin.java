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
import com.caucho.boot.LogLevelCommand;
import com.caucho.boot.WatchdogStatusQuery;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.cloud.network.NetworkClusterSystem;
import com.caucho.cloud.topology.CloudServer;
import com.caucho.config.types.Period;
import com.caucho.jmx.MXName;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ManagementMXBean;
import com.caucho.quercus.lib.reflection.ReflectionException;
import com.caucho.server.admin.ControllerRestartQuery;
import com.caucho.server.admin.ControllerStartQuery;
import com.caucho.server.admin.ControllerStopQuery;
import com.caucho.server.admin.DeployActor;
import com.caucho.server.admin.HmuxClientFactory;
import com.caucho.server.admin.JmxCallQuery;
import com.caucho.server.admin.JmxDumpQuery;
import com.caucho.server.admin.JmxListQuery;
import com.caucho.server.admin.JmxSetQuery;
import com.caucho.server.admin.LogLevelQuery;
import com.caucho.server.admin.ManagementQueryResult;
import com.caucho.server.admin.PdfReportQuery;
import com.caucho.server.admin.ThreadDumpQuery;
import com.caucho.util.L10N;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
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
  public ManagementQueryResult listJmx(String serverId,
                                       String pattern,
                                       boolean isPrintAttributes,
                                       boolean isPrintValues,
                                       boolean isPrintOperations,
                                       boolean isPrintAllBeans,
                                       boolean isPrintPlatformBeans)
  {
    JmxListQuery query = new JmxListQuery(pattern,
                                          isPrintAttributes,
                                          isPrintValues,
                                          isPrintOperations,
                                          isPrintAllBeans,
                                          isPrintPlatformBeans);

    ManagementQueryResult result = query(serverId, query);

    return result;
  }

  @Override
  public ManagementQueryResult logLevel(String serverId,
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

    LogLevelQuery query = new LogLevelQuery(loggers, level, period);

    return query(serverId, query);
  }

  @Override
  public ManagementQueryResult dumpThreads(String serverId)
  {

    ThreadDumpQuery query = new ThreadDumpQuery();

    return query(serverId, query);
  }

  @Override
  public ManagementQueryResult pdfReport(String serverId,
                                         String path,
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

    PdfReportQuery query = new PdfReportQuery(path,
                                              report,
                                              period,
                                              logDirectory,
                                              profileTime,
                                              samplePeriod,
                                              isSnapshot,
                                              isWatchdog,
                                              isLoadPdf);

    return query(serverId, query);
  }

  @Override
  public ManagementQueryResult setJmx(String serverId,
                                      String pattern,
                                      String attribute,
                                      String value)
  {
    JmxSetQuery query = new JmxSetQuery(pattern, attribute, value);

    return query(serverId, query);
  }

  @Override
  public ManagementQueryResult callJmx(String serverId,
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

    JmxCallQuery query = new JmxCallQuery(pattern,
                                          operation,
                                          operationIndex,
                                          params);

    return query(serverId, query);
  }

  @Override
  public ManagementQueryResult startWebApp(String serverId,
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

    ControllerStartQuery query = new ControllerStartQuery(tag);

    ManagementQueryResult result = queryDeployActor(serverId, query);

    return result;
  }

  @Override
  public ManagementQueryResult stopWebApp(String serverId,
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

    ControllerStopQuery query = new ControllerStopQuery(tag);

    ManagementQueryResult result = queryDeployActor(serverId, query);

    return result;
  }

  @Override
  public ManagementQueryResult restartWebApp(String serverId,
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

    ControllerRestartQuery query = new ControllerRestartQuery(tag);

    ManagementQueryResult result = queryDeployActor(serverId, query);

    return result;
  }

  @Override
  public ManagementQueryResult dumpJmx(String serverId)
  {
    JmxDumpQuery query = new JmxDumpQuery();

    return query(serverId, query);
  }

  @Override
  public ManagementQueryResult getStatus(String serverId)
  {
    WatchdogStatusQuery query = new WatchdogStatusQuery();

    return query(serverId, query);
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

  private ManagementQueryResult query(String serverId, Serializable query)
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

    return (ManagementQueryResult) sender.query("manager@resin.caucho", query);
  }

  private ManagementQueryResult queryDeployActor(String serverId,
                                                 Serializable query)
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

    return (ManagementQueryResult) sender.query(DeployActor.address, query);
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
