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
import com.caucho.jmx.MXDefaultValue;
import com.caucho.jmx.MXName;
import com.caucho.jmx.MXValueRequired;
import com.caucho.quercus.lib.reflection.ReflectionException;
import com.caucho.server.admin.ManagementQueryResult;
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

  @Description("Produces a complete dump of JMX objects and values")
  @MXAction("jmx-dump")
  public ManagementQueryResult dumpJmx(@MXName("server") String value)
    throws ReflectionException;

  @Description("lists the JMX MBeans in a Resin server (Resin Pro)")
  @MXAction("jmx-list")
  public ManagementQueryResult listJmx(@MXName("server") String serverId,
                                       @MXName("pattern") String pattern,
                                       @MXName("print-attributes")
                                       boolean isPrintAttributes,
                                       @MXName("print-values")
                                       boolean isPrintValues,
                                       @MXName("print-operations")
                                       boolean isPrintOperations,
                                       @MXName("print-all-beans")
                                       boolean isPrintAllBeans,
                                       @MXName("print-platform-beans")
                                       boolean isPrintPlatformBeans)
    throws ReflectionException;

  @Description("sets the java.util.logging level for debugging (Resin Pro)")
  @MXAction(value = "log-level", method = "POST")
  public ManagementQueryResult logLevel(@MXName("server") String serverId,
                                        @MXName("loggers") String loggersValue,
                                        @MXValueRequired
                                        @MXName("level") String levelValue,
                                        @MXName("active-time")
                                        String activeTime)
    throws ReflectionException;

  @Description("creates a PDF report of a Resin server (Resin Pro)")
  @MXAction("pdf-report")
  public ManagementQueryResult pdfReport(@MXName("server") String serverId,
                                         @MXName("path") String path,
                                         @MXName("report") String report,
                                         @MXName("period") String periodStr,
                                         @MXName("log-directory")
                                         String logDirectory,
                                         @MXName("profile-time")
                                         String profileTimeStr,
                                         @MXName("sample-period")
                                         String samplePeriodStr,
                                         @MXDefaultValue("true")
                                         @MXName("snapshot") boolean isSnapshot,
                                         @MXName("watchdog")
                                         boolean isWatchdog,
                                         @MXName("load-pdf")
                                         boolean isLoadPdf)
    throws ReflectionException;

  @Description("sets JMX Mbean's attribute")
  @MXAction(value = "jmx-set", method = "POST")
  public ManagementQueryResult setJmx(@MXName("server") String serverId,
                                      @MXName("pattern") String pattern,
                                      @MXName("attribute") String attribute,
                                      @MXName("value") String value)
    throws ReflectionException;

  @Description("displays a JVM thread dump summary")
  @MXAction("thread-dump")
  public ManagementQueryResult dumpThreads(@MXName("server") String serverId)
    throws ReflectionException;

  @Description("calls a method on a JMX MBean")
  @MXAction(value = "jmx-call", method = "POST")
  public ManagementQueryResult callJmx(@MXName("server") String serverId,
                                       @MXName("pattern") String pattern,
                                       @MXName("operation") String operation,
                                       @MXName("operation-index")
                                       String operationIdx,
                                       @MXName("values") String values)
    throws ReflectionException;

  @Description("starts a deployed application")
  @MXAction(value = "web-app-start", method = "POST")
  public ManagementQueryResult startWebApp(@MXName("server") String serverId,
                                           @MXName("tag") String tag,
                                           @MXName("name") String name,
                                           @MXDefaultValue("production")
                                           @MXName("stage") String stage,
                                           @MXDefaultValue("default")
                                           @MXName("host") String host,
                                           @MXName("version") String version)
    throws ReflectionException;

  @Description("stops a deployed application")
  @MXAction(value = "web-app-stop", method = "POST")
  public ManagementQueryResult stopWebApp(@MXName("server") String serverId,
                                          @MXName("tag") String tag,
                                          @MXName("name") String name,
                                          @MXDefaultValue("production")
                                          @MXName("stage") String stage,
                                          @MXDefaultValue("default")
                                          @MXName("host") String host,
                                          @MXName("version") String version)
    throws ReflectionException;

  @Description("restarts a deployed application")
  @MXAction(value = "web-app-restart", method = "POST")
  public ManagementQueryResult restartWebApp(@MXName("server") String serverId,
                                             @MXName("tag") String tag,
                                             @MXName("name") String name,
                                             @MXDefaultValue("production")
                                             @MXName("stage") String stage,
                                             @MXDefaultValue("default")
                                             @MXName("host") String host,
                                             @MXName("version") String version)
    throws ReflectionException;

  @Description("deploys an application")
  @MXAction(value = "web-app-deploy", method = "POST")
  public String webappDeploy(@MXName("server") String serverId,
                             @MXName("context") String context,
                             @MXDefaultValue("default")
                             @MXName("host") String host,
                             @MXName("stage") String stage,
                             @MXName("version") String version,
                             @MXName("message") String message,
                             InputStream is)
    throws ReflectionException;

  @Description("copies a deployment to a new tag name")
  @MXAction(value = "deploy-copy", method = "POST")
  public String deployCopy(@MXName("server") String serverId,
                           @MXName("source-context") String sourceContext,
                           @MXDefaultValue("default")
                           @MXName("source-host") String sourceHost,
                           @MXName("source-stage") String sourceStage,
                           @MXName("source-version") String sourceVersion,
                           @MXName("target-context") String targetContext,
                           @MXDefaultValue("default")
                           @MXName("target-host") String targetHost,
                           @MXName("target-stage") String targetStage,
                           @MXName("target-version") String targetVersion,
                           @MXName("message") String message)
  throws ReflectionException;

  @Description("lists deployed applications")
  @MXAction("deploy-list")
  public TagResult []deployList(@MXName("server") String serverId,
                                @MXDefaultValue(".*")
                                @MXName("pattern") String pattern)
    throws ReflectionException;

  @Description("undeploys an application")
  @MXAction(value = "web-app-undeploy", method = "POST")
  public String undeploy(@MXName("server") String serverId,
                         @MXName("context") String context,
                         @MXDefaultValue("default")
                         @MXName("host") String host,
                         @MXName("stage") String stage,
                         @MXName("version") String version,
                         @MXName("message") String message)
    throws ReflectionException;

  @Description("Prints status of a server")
  @MXAction("status")
  public ManagementQueryResult getStatus(@MXName("server") String value)
    throws ReflectionException;

  // XXX: temporary example until we have a real one
  public InputStream test(@MXName("test-param") String value,
                          InputStream is)
    throws IOException;
}
