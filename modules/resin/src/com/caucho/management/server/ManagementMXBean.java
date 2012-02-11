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
import com.caucho.jmx.MXParam;
import com.caucho.quercus.lib.reflection.ReflectionException;
import com.caucho.server.admin.AddUserQueryResult;
import com.caucho.server.admin.ControllerStateActionQueryResult;
import com.caucho.server.admin.ListUsersQueryResult;
import com.caucho.server.admin.PdfReportQueryResult;
import com.caucho.server.admin.RemoveUserQueryResult;
import com.caucho.server.admin.StringQueryResult;
import com.caucho.server.admin.TagResult;

import java.io.IOException;
import java.io.InputStream;

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
  @Description("hello, world test interface")
  @MXAction("hello")
  public String hello();

  @Description("deploys configuration")
  @MXAction(value = "config-deploy", method = "POST")
  public String configDeploy(@MXParam(name = "server") String serverId,
                             @MXParam(name = "stage") String stage,
                             @MXParam(name = "version") String version,
                             @MXParam(name = "message") String message,
                             InputStream is)
    throws ReflectionException;

  @Description("pulls a configuration file")
  @MXAction(value = "config-cat", method = "GET")
  public InputStream configCat(@MXParam(name = "server") String serverId,
                          @MXParam(name = "name", required = true) String name,
                          @MXParam(name = "stage") String stage,
                          @MXParam(name = "version") String version,
                          @MXParam(name = "message") String message)
    throws ReflectionException;

  @Description("list the configuration files")
  @MXAction(value = "config-ls", method = "GET")
  public String []configLs(@MXParam(name = "server") String serverId,
                         @MXParam(name = "name") String name,
                         @MXParam(name = "stage") String stage,
                         @MXParam(name = "version") String version,
                         @MXParam(name = "message") String message)
  throws ReflectionException;

  @Description("undeploy configuration")
  @MXAction(value = "config-undeploy", method = "POST")
  public String configUndeploy(@MXParam(name = "server") String serverId,
                               @MXParam(name = "stage") String stage,
                               @MXParam(name = "version") String version,
                               @MXParam(name = "message") String message)
    throws ReflectionException;

  @Description("Produces a complete dump of JMX objects and values")
  @MXAction("jmx-dump")
  public StringQueryResult doJmxDump(@MXParam(name = "server") String value)
    throws ReflectionException;

  @Description("lists the JMX MBeans in a Resin server (Resin Pro)")
  @MXAction("jmx-list")
  public StringQueryResult listJmx(
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
  public StringQueryResult setLogLevel(
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
  public PdfReportQueryResult pdfReport(
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

  @Description("sets JMX Mbean's attribute")
  @MXAction(value = "jmx-set", method = "POST")
  public StringQueryResult setJmx(@MXParam(name = "server") String serverId,
                                  @MXParam(name = "pattern") String pattern,
                                  @MXParam(name = "attribute")
                                  String attribute,
                                  @MXParam(name = "value") String value)
  throws ReflectionException;

  @Description("displays a JVM thread dump summary")
  @MXAction("thread-dump")
  public StringQueryResult doThreadDump(
    @MXParam(name = "server") String serverId)
    throws ReflectionException;

  @Description("calls a method on a JMX MBean")
  @MXAction(value = "jmx-call", method = "POST")
  public StringQueryResult callJmx(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "pattern") String pattern,
    @MXParam(name = "operation") String operation,
    @MXParam(name = "operation-index")
    String operationIdx,
    @MXParam(name = "values") String values)
    throws ReflectionException;

  @Description("starts a deployed application")
  @MXAction(value = "web-app-start", method = "POST")
  public ControllerStateActionQueryResult startWebApp(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "tag") String tag,
    @MXParam(name = "name") String name,
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
  public ControllerStateActionQueryResult stopWebApp(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "tag") String tag,
    @MXParam(name = "name") String name,
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
  public ControllerStateActionQueryResult restartWebApp(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "tag") String tag,
    @MXParam(name = "name") String name,
    @MXParam(name = "stage",
             defaultValue = "production")
    String stage,
    @MXParam(name = "host", defaultValue = "default") String host,
    @MXParam(name = "version") String version)
    throws ReflectionException;

  @Description("deploys an application")
  @MXAction(value = "web-app-deploy", method = "POST")
  public String webappDeploy(@MXParam(name = "server") String serverId,
                             @MXParam(name = "context") String context,
                             @MXParam(name = "host", defaultValue = "default")
                             String host,
                             @MXParam(name = "stage") String stage,
                             @MXParam(name = "version") String version,
                             @MXParam(name = "message") String message,
                             InputStream is)
    throws ReflectionException;

  @Description("copies a deployment to a new tag name")
  @MXAction(value = "deploy-copy", method = "POST")
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
  public TagResult[] deployList(@MXParam(name = "server") String serverId,
                                @MXParam(name = "pattern", defaultValue = ".*")
                                String pattern)
    throws ReflectionException;

  @Description("undeploys an application")
  @MXAction(value = "web-app-undeploy", method = "POST")
  public String undeploy(@MXParam(name = "server") String serverId,
                         @MXParam(name = "context") String context,
                         @MXParam(name = "host", defaultValue = "default")
                         String host,
                         @MXParam(name = "stage") String stage,
                         @MXParam(name = "version") String version,
                         @MXParam(name = "message") String message)
    throws ReflectionException;

  @Description("adds an administration user and password")
  @MXAction(value = "user-add", method = "POST")
  public AddUserQueryResult addUser(@MXParam(name = "server") String serverId,
                                    @MXParam(name = "user", required = true)
                                    String user,
                                    @MXParam(name = "password") String password,
                                    @MXParam(name = "roles") String rolesStr)
    throws ReflectionException;

  @Description("lists the administration user")
  @MXAction(value = "user-list", method = "GET")
  public ListUsersQueryResult listUsers(
    @MXParam(name = "server") String serverId)
    throws ReflectionException;

  @Description("removes an administration user")
  @MXAction(value = "user-remove", method = "POST")
  public RemoveUserQueryResult removeUser(
    @MXParam(name = "server") String serverId,
    @MXParam(name = "user") String user)
    throws ReflectionException;

  @Description("Prints status of a server")
  @MXAction("status")
  public StringQueryResult getStatus(@MXParam(name = "server") String value)
    throws ReflectionException;

  // XXX: temporary example until we have a real one
  public InputStream test(@MXParam(name = "test-param") String value,
                          InputStream is)
    throws IOException;
}
