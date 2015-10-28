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
 * @author Scott Ferguson
 */

package com.caucho.management.server;

import java.util.Date;

import javax.management.ReflectionException;

import com.caucho.server.admin.AddUserQueryReply;
import com.caucho.server.admin.JmxCallQueryReply;
import com.caucho.server.admin.JsonQueryReply;
import com.caucho.server.admin.ListJmxQueryReply;
import com.caucho.server.admin.ListUsersQueryReply;
import com.caucho.server.admin.PdfReportQueryReply;
import com.caucho.server.admin.RemoveUserQueryReply;
import com.caucho.server.admin.StatServiceValuesQueryReply;
import com.caucho.server.admin.StringQueryReply;
import com.caucho.v5.health.action.JmxSetQueryReply;
import com.caucho.v5.jmx.AdminStream;
import com.caucho.v5.jmx.Description;
import com.caucho.v5.jmx.MXAction;
import com.caucho.v5.jmx.MXContentType;
import com.caucho.v5.jmx.MXParam;
import com.caucho.v5.jmx.server.ManagedObjectMXBean;

/**
 * Management facade for Resin, used for REST.
 * <p/>
 * <pre>
 * resin:type=Management
 * </pre>
 */
@Description("Management Facade for Resin")
public interface ManagementMXBean extends ManagedObjectMXBean
{
  @Description("undeploy configuration")
  @MXAction(value = "config-undeploy", method = "POST")
  @MXContentType
  public String configUndeploy(@MXParam("server") String serverId,
                               @MXParam("stage") String stage,
                               @MXParam("version") String version,
                               @MXParam("message") String message)
    throws ReflectionException;

  @Description("Produces a complete dump of JMX objects and values")
  @MXAction("jmx-dump")
  public JsonQueryReply doJmxDump(@MXParam("server") String value)
    throws ReflectionException;

  @Description("lists the JMX MBeans in a Resin server (Resin Pro)")
  @MXAction("jmx-list")
  @MXContentType
  public ListJmxQueryReply listJmx(@MXParam("server") String serverId,
                                   @MXParam("pattern") String pattern,
                                   @MXParam("print-attributes")
                                   boolean isPrintAttributes,
                                   @MXParam("print-values")
                                   boolean isPrintValues,
                                   @MXParam("print-operations")
                                   boolean isPrintOperations,
                                   @MXParam("print-all-beans")
                                   boolean isPrintAllBeans,
                                   @MXParam("print-platform-beans")
                                   boolean isPrintPlatformBeans)
    throws ReflectionException;

  @Description("sets the java.util.logging level for debugging (Resin Pro)")
  @MXAction(value = "log-level", method = "POST")
  @MXContentType

  public StringQueryReply setLogLevel(
    @MXParam(value = "server") String serverId,
    @MXParam(value = "loggers")
    String loggersValue,
    @MXParam(value = "level",
             required = true)
    String levelValue,
    @MXParam(value = "active-time")
    String activeTime)
    throws ReflectionException;

  @Description("creates a PDF report of a Resin server (Resin Pro)")
  @MXAction(value = "pdf-report", method = "GET")
  @MXContentType
  public PdfReportQueryReply pdfReport(
                                       @MXParam("server") String serverId,
                                       @MXParam("report") String report,
                                       @MXParam("period")
                                       String periodStr,
                                       @MXParam("log-directory")
                                       String logDirectory,
                                       @MXParam("profile-time")
                                       String profileTimeStr,
                                       @MXParam("sample-period")
                                       String samplePeriodStr,
                                       @MXParam(value = "snapshot",
                                                defaultValue = "true")
                                       boolean isSnapshot,
                                       @MXParam("watchdog")
                                       boolean isWatchdog,
                                       @MXParam("load-pdf")
                                       boolean isLoadPdf)
    throws ReflectionException;

  @Description("outputs stats collected by a named meter")
  @MXAction(value = "stats", method = "GET")
  @MXContentType
  public StatServiceValuesQueryReply getStats(
    @MXParam(value = "server") String serverId,
    @MXParam(value = "meters", required = true) String metersStr,
    @MXParam(value = "period", defaultValue = "7D")
    String periodStr)
  throws ReflectionException;

  @Description("sets JMX Mbean's attribute")
  @MXAction(value = "jmx-set", method = "POST")
  @MXContentType
  public JmxSetQueryReply setJmx(@MXParam(value = "server") String serverId,
                                  @MXParam(value = "pattern") String pattern,
                                  @MXParam(value = "attribute")
                                  String attribute,
                                  @MXParam(value = "value") String value)
  throws ReflectionException;

  @Description("displays a JVM thread dump summary")
  @MXAction("thread-dump")
  @MXContentType
  public JsonQueryReply doThreadDump(
    @MXParam(value = "server") String serverId)
    throws ReflectionException;

  @Description("adds a Resin-Professional license to an installation")
  @MXAction(value = "license-add", method = "POST")
  @MXContentType
  public StringQueryReply addLicense(
    @MXParam(value = "server") String serverId,
    @MXParam(value = "overwrite") boolean isOverwrite,
    @MXParam(value = "to", required = true) String to,
    @MXParam(value = "restart") boolean isRestart,
    AdminStream in)
    throws ReflectionException;

  @Description("lists the most recent Resin server restart times")
  @MXAction(value = "list-restarts", method = "GET")
  @MXContentType
  public Date[]listRestarts(
    @MXParam(value = "server") String serverId,
    @MXParam(value = "period", defaultValue = "7D") String periodStr)
    throws ReflectionException;

  @Description("calls a method on a JMX MBean")
  @MXAction(value = "jmx-call", method = "POST")
  @MXContentType
  public JmxCallQueryReply callJmx(
    @MXParam(value = "server") String serverId,
    @MXParam(value = "pattern") String pattern,
    @MXParam(value = "operation") String operation,
    @MXParam(value = "operation-index")
    String operationIdx,
    @MXParam(value = "values") String values)
    throws ReflectionException;

  /*
  @Description("starts a deployed application")
  @MXAction(value = "web-app-start", method = "POST")
  @MXContentType
  public DeployControllerState startWebApp(
    @MXParam(value = "server") String serverId,
    @MXParam(value = "tag") String tag,
    @MXParam(value = "context") String context,
    @MXParam(value = "stage",
             defaultValue = "production")
    String stage,
    @MXParam(value = "host",
             defaultValue = "default")
    String host,
    @MXParam(value = "version") String version)
    throws ReflectionException;

  @Description("stops a deployed application")
  @MXAction(value = "web-app-stop", method = "POST")
  @MXContentType
  public DeployControllerState stopWebApp(
    @MXParam(value = "server") String serverId,
    @MXParam(value = "tag") String tag,
    @MXParam(value = "context") String context,
    @MXParam(value = "stage",
             defaultValue = "production")
    String stage,
    @MXParam(value = "host",
             defaultValue = "default")
    String host,
    @MXParam(value = "version") String version)
    throws ReflectionException;

  @Description("restarts a deployed application")
  @MXAction(value = "web-app-restart", method = "POST")
  @MXContentType
  public DeployControllerState restartWebApp(
    @MXParam(value = "server") String serverId,
    @MXParam(value = "tag") String tag,
    @MXParam(value = "context") String context,
    @MXParam(value = "stage",
             defaultValue = "production")
    String stage,
    @MXParam(value = "host", defaultValue = "default") String host,
    @MXParam(value = "version") String version)
    throws ReflectionException;
    */

  @Description("deploys an application")
  @MXAction(value = "web-app-deploy", method = "POST")
  @MXContentType
  public String webappDeploy(@MXParam(value = "server") String serverId,
                             @MXParam(value = "context", required = true)
                             String context,
                             @MXParam(value = "host", defaultValue = "default")
                             String host,
                             @MXParam(value = "stage") String stage,
                             @MXParam(value = "version") String version,
                             @MXParam(value = "message") String message,
                             AdminStream is)
    throws ReflectionException;

  @Description("copies a deployment to a new tag name")
  @MXAction(value = "deploy-copy", method = "POST")
  @MXContentType
  public String deployCopy(@MXParam(value = "server") String serverId,
                           @MXParam(value = "source-context")
                           String sourceContext,
                           @MXParam(value = "source-host",
                                    defaultValue = "default") String sourceHost,
                           @MXParam(value = "source-stage") String sourceStage,
                           @MXParam(value = "source-version")
                           String sourceVersion,
                           @MXParam(value = "target-context")
                           String targetContext,
                           @MXParam(value = "target-host",
                                    defaultValue = "default") String targetHost,
                           @MXParam(value = "target-stage") String targetStage,
                           @MXParam(value = "target-version")
                           String targetVersion,
                           @MXParam(value = "message") String message)
    throws ReflectionException;

  /*
  @Description("lists deployed applications")
  @MXAction("deploy-list")
  @MXContentType
  public DeployTagResult[] deployList(@MXParam(value = "server") String serverId,
                                @MXParam(value = "pattern", defaultValue = ".*")
                                String pattern)
    throws ReflectionException;
    */

  @Description("undeploys an application")
  @MXAction(value = "web-app-undeploy", method = "POST")
  @MXContentType
  public String undeploy(@MXParam(value = "server") String serverId,
                         @MXParam(value = "context", required = true) String context,
                         @MXParam(value = "host", defaultValue = "default")
                         String host,
                         @MXParam(value = "stage") String stage,
                         @MXParam(value = "version") String version,
                         @MXParam(value = "message") String message)
    throws ReflectionException;

  @Description("adds an administration user and password")
  //@MXAction(value = "user-add", method = "POST")
  @MXContentType
  public AddUserQueryReply addUser(@MXParam(value = "server") String serverId,
                                    @MXParam(value = "user", required = true)
                                    String user,
                                    @MXParam(value = "password") String password,
                                    @MXParam(value = "roles") String rolesStr)
    throws ReflectionException;

  @Description("lists the administration user")
  //@MXAction(value = "user-list", method = "GET")
  @MXContentType
  public ListUsersQueryReply listUsers(
    @MXParam(value = "server") String serverId)
    throws ReflectionException;

  @Description("removes an administration user")
  //@MXAction(value = "user-remove", method = "POST")
  @MXContentType
  public RemoveUserQueryReply removeUser(
    @MXParam(value = "server") String serverId,
    @MXParam(value = "user") String user)
    throws ReflectionException;

  @Description("Prints status of a server")
  @MXAction("status")
  @MXContentType
  public StringQueryReply getStatus(@MXParam(value = "server") String value)
    throws ReflectionException;

  @Description("Enables a server")
  @MXAction(value = "enable", method = "POST")
  public String enable(@MXParam(value = "server") String serverId)
    throws ReflectionException;

  @Description("Disables a server")
  @MXAction(value = "disable", method = "POST")
  public String disable(@MXParam(value = "server") String serverId)
    throws ReflectionException;

  @Description("Disables a server from accepting any new sessions")
  @MXAction(value = "disable-soft", method = "POST")
  public String disableSoft(@MXParam(value = "server") String serverId)
    throws ReflectionException;
}
