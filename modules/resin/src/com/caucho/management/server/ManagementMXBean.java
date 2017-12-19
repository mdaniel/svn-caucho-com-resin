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
 * @author Scott Ferguson
 */

package com.caucho.management.server;

import com.caucho.jmx.Description;
import com.caucho.jmx.MXAction;
import com.caucho.jmx.MXContentType;
import com.caucho.jmx.MXParam;
import com.caucho.server.admin.AddUserQueryReply;
import com.caucho.server.admin.JmxCallQueryReply;
import com.caucho.server.admin.JmxSetQueryReply;
import com.caucho.server.admin.JsonQueryReply;
import com.caucho.server.admin.ListJmxQueryReply;
import com.caucho.server.admin.ListUsersQueryReply;
import com.caucho.server.admin.PdfReportQueryReply;
import com.caucho.server.admin.RemoveUserQueryReply;
import com.caucho.server.admin.StatServiceValuesQueryReply;
import com.caucho.server.admin.StringQueryReply;
import com.caucho.server.deploy.DeployControllerState;
import com.caucho.server.deploy.DeployTagResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.management.ReflectionException;

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
  /*
  @Description("deploys configuration")
  @MXAction(value = "config-deploy", method = "POST")
  @MXContentType
  public String configDeploy(@MXParam(name = "server") String serverId,
                             @MXParam(name = "stage") String stage,
                             @MXParam(name = "version") String version,
                             @MXParam(name = "message") String message,
                             InputStream is)
    throws ReflectionException;

  @Description("pulls a configuration file")
  @MXAction(value = "config-cat", method = "GET")
  @MXContentType("text/xml")
  public InputStream configCat(@MXParam(name = "server") String serverId,
                          @MXParam(name = "name", required = true) String name,
                          @MXParam(name = "stage") String stage,
                          @MXParam(name = "version") String version)
    throws ReflectionException;

  @Description("list the configuration files")
  @MXAction(value = "config-ls", method = "GET")
  @MXContentType
  public String []configLs(@MXParam(name = "server") String serverId,
                           @MXParam(name = "name") String name,
                           @MXParam(name = "stage") String stage,
                           @MXParam(name = "version") String version)
  throws ReflectionException;

  @Description("undeploy configuration")
  @MXAction(value = "config-undeploy", method = "POST")
  @MXContentType
  public String configUndeploy(@MXParam(name = "server") String serverId,
                               @MXParam(name = "stage") String stage,
                               @MXParam(name = "version") String version,
                               @MXParam(name = "message") String message)
    throws ReflectionException;
    */

  @Description("Produces a complete dump of JMX objects and values")
  @MXAction("jmx-dump")
  public JsonQueryReply doJmxDump(@MXParam(name = "server") String value)
    throws ReflectionException;

  @Description("lists the JMX MBeans in a Resin server (Resin Pro)")
  @MXAction("jmx-list")
  @MXContentType
  public ListJmxQueryReply listJmx(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "pattern") String pattern,
    @MXParam(name = "print-attributes")
    boolean isPrintAttributes,
    @MXParam(name = "print-values")
    boolean isPrintValues,
    @MXParam(name = "print-operations")
    boolean isPrintOperations,
    @MXParam(name = "print-all-beans")
    boolean isPrintAllBeans,
    @MXParam(name = "print-platform-beans")
    boolean isPrintPlatformBeans)
    throws ReflectionException;

  @Description("sets the java.util.logging level for debugging (Resin Pro)")
  @MXAction(value = "log-level", method = "POST")
  @MXContentType

  public StringQueryReply setLogLevel(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "loggers")
    String loggersValue,
    @MXParam(name = "level",
             required = true)
    String levelValue,
    @MXParam(name = "active-time")
    String activeTime)
    throws ReflectionException;

  @Description("creates a PDF report of a Resin server (Resin Pro)")
  @MXAction(value = "pdf-report", method = "GET")
  @MXContentType
  public PdfReportQueryReply pdfReport(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "report") String report,
    @MXParam(name = "period")
    String periodStr,
    @MXParam(name = "log-directory")
    String logDirectory,
    @MXParam(name = "profile-time")
    String profileTimeStr,
    @MXParam(name = "sample-period")
    String samplePeriodStr,
    @MXParam(name = "snapshot",
             defaultValue = "true")
    boolean isSnapshot,
    @MXParam(name = "watchdog")
    boolean isWatchdog,
    @MXParam(name = "load-pdf")
    boolean isLoadPdf)
    throws ReflectionException;

  @Description("outputs stats collected by a named meter")
  @MXAction(value = "stats", method = "GET")
  @MXContentType
  public StatServiceValuesQueryReply getStats(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "meters", required = true) String metersStr,
    @MXParam(name = "period", defaultValue = "7D")
    String periodStr)
  throws ReflectionException;

  @Description("sets JMX Mbean's attribute")
  @MXAction(value = "jmx-set", method = "POST")
  @MXContentType
  public JmxSetQueryReply setJmx(@MXParam(name = "server") String serverId,
                                  @MXParam(name = "pattern") String pattern,
                                  @MXParam(name = "attribute")
                                  String attribute,
                                  @MXParam(name = "value") String value)
  throws ReflectionException;

  @Description("displays a JVM thread dump summary")
  @MXAction("thread-dump")
  @MXContentType
  public JsonQueryReply doThreadDump(
    @MXParam(name = "server") String serverId)
    throws ReflectionException;

  @Description("adds a Resin-Professional license to an installation")
  @MXAction(value = "license-add", method = "POST")
  @MXContentType
  public StringQueryReply addLicense(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "overwrite") boolean isOverwrite,
    @MXParam(name = "to", required = true) String to,
    @MXParam(name = "restart") boolean isRestart,
    InputStream in)
    throws ReflectionException;

  @Description("lists the most recent Resin server restart times")
  @MXAction(value = "list-restarts", method = "GET")
  @MXContentType
  public Date[]listRestarts(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "period", defaultValue = "7D") String periodStr)
    throws ReflectionException;

  @Description("calls a method on a JMX MBean")
  @MXAction(value = "jmx-call", method = "POST")
  @MXContentType
  public JmxCallQueryReply callJmx(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "pattern") String pattern,
    @MXParam(name = "operation") String operation,
    @MXParam(name = "operation-index")
    String operationIdx,
    @MXParam(name = "values") String values)
    throws ReflectionException;

  @Description("starts a deployed application")
  @MXAction(value = "web-app-start", method = "POST")
  @MXContentType
  public DeployControllerState startWebApp(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "tag") String tag,
    @MXParam(name = "context") String context,
    @MXParam(name = "stage",
             defaultValue = "production")
    String stage,
    @MXParam(name = "host",
             defaultValue = "default")
    String host,
    @MXParam(name = "version") String version)
    throws ReflectionException;

  @Description("stops a deployed application")
  @MXAction(value = "web-app-stop", method = "POST")
  @MXContentType
  public DeployControllerState stopWebApp(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "tag") String tag,
    @MXParam(name = "context") String context,
    @MXParam(name = "stage",
             defaultValue = "production")
    String stage,
    @MXParam(name = "host",
             defaultValue = "default")
    String host,
    @MXParam(name = "version") String version)
    throws ReflectionException;

  @Description("restarts a deployed application")
  @MXAction(value = "web-app-restart", method = "POST")
  @MXContentType
  public DeployControllerState restartWebApp(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "tag") String tag,
    @MXParam(name = "context") String context,
    @MXParam(name = "stage",
             defaultValue = "production")
    String stage,
    @MXParam(name = "host", defaultValue = "default") String host,
    @MXParam(name = "version") String version)
    throws ReflectionException;

  @Description("deploys an application")
  @MXAction(value = "web-app-deploy", method = "POST")
  @MXContentType
  public String webappDeploy(@MXParam(name = "server") String serverId,
                             @MXParam(name = "context", required = true)
                             String context,
                             @MXParam(name = "host", defaultValue = "default")
                             String host,
                             @MXParam(name = "stage") String stage,
                             @MXParam(name = "version") String version,
                             @MXParam(name = "message") String message,
                             InputStream is)
    throws ReflectionException;

  @Description("copies a deployment to a new tag name")
  @MXAction(value = "deploy-copy", method = "POST")
  @MXContentType
  public String deployCopy(@MXParam(name = "server") String serverId,
                           @MXParam(name = "source-context")
                           String sourceContext,
                           @MXParam(name = "source-host",
                                    defaultValue = "default") String sourceHost,
                           @MXParam(name = "source-stage") String sourceStage,
                           @MXParam(name = "source-version")
                           String sourceVersion,
                           @MXParam(name = "target-context")
                           String targetContext,
                           @MXParam(name = "target-host",
                                    defaultValue = "default") String targetHost,
                           @MXParam(name = "target-stage") String targetStage,
                           @MXParam(name = "target-version")
                           String targetVersion,
                           @MXParam(name = "message") String message)
    throws ReflectionException;

  @Description("lists deployed applications")
  @MXAction("deploy-list")
  @MXContentType
  public DeployTagResult[] deployList(@MXParam(name = "server") String serverId,
                                @MXParam(name = "pattern", defaultValue = ".*")
                                String pattern)
    throws ReflectionException;

  @Description("undeploys an application")
  @MXAction(value = "web-app-undeploy", method = "POST")
  @MXContentType
  public String undeploy(@MXParam(name = "server") String serverId,
                         @MXParam(name = "context", required = true) String context,
                         @MXParam(name = "host", defaultValue = "default")
                         String host,
                         @MXParam(name = "stage") String stage,
                         @MXParam(name = "version") String version,
                         @MXParam(name = "message") String message)
    throws ReflectionException;

  @Description("adds an administration user and password")
  //@MXAction(value = "user-add", method = "POST")
  @MXContentType
  public AddUserQueryReply addUser(@MXParam(name = "server") String serverId,
                                    @MXParam(name = "user", required = true)
                                    String user,
                                    @MXParam(name = "password") String password,
                                    @MXParam(name = "roles") String rolesStr)
    throws ReflectionException;

  @Description("lists the administration user")
  //@MXAction(value = "user-list", method = "GET")
  @MXContentType
  public ListUsersQueryReply listUsers(
    @MXParam(name = "server") String serverId)
    throws ReflectionException;

  @Description("removes an administration user")
  //@MXAction(value = "user-remove", method = "POST")
  @MXContentType
  public RemoveUserQueryReply removeUser(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "user") String user)
    throws ReflectionException;

  @Description("Prints status of a server")
  @MXAction("status")
  @MXContentType
  public StringQueryReply getStatus(@MXParam(name = "server") String value)
    throws ReflectionException;

  @Description("Enables a server")
  @MXAction(value = "enable", method = "POST")
  public String enable(@MXParam(name = "server") String serverId)
    throws ReflectionException;

  @Description("Disables a server")
  @MXAction(value = "disable", method = "POST")
  public String disable(@MXParam(name = "server") String serverId)
    throws ReflectionException;

  @Description("Disables a server from accepting any new sessions")
  @MXAction(value = "disable-soft", method = "POST")
  public String disableSoft(@MXParam(name = "server") String serverId)
    throws ReflectionException;
}
