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

import java.util.Date;
import java.util.logging.Level;

import com.caucho.v5.health.action.JmxSetQueryReply;
import com.caucho.v5.io.StreamSource;

public interface ManagerServiceApi
{
  AddUserQueryReply addUser(String user, char[] charArray, String[] roles);
  
  ListUsersQueryReply listUsers();

  RemoveUserQueryReply removeUser(String user);

  String doThreadDump(boolean isJson);

  JsonQueryReply doJsonThreadDump();
  
  String doHeapDump(boolean isRaw);

  String doStoreDump(String name);
  
  boolean doStoreRestore(String string, StreamSource source);

  ListJmxQueryReply listJmx(String pattern, 
                            boolean isPrintAttributes,
                            boolean isPrintValues, 
                            boolean isPrintOperations,
                            boolean isPrintAllBeans,
                            boolean isPrintPlatformBeans);
  
  JsonQueryReply doJmxDump();

  JmxSetQueryReply setJmx(JmxSetQuery query);

  JmxCallQueryReply callJmx(JmxCallQuery query);

  StringQueryReply setLogLevel(String[] loggers, Level level, long period);

  StatServiceValuesQueryReply getStats(String[] meters, Date from, Date to);

  PdfReportQueryReply pdfReport(String path,
                                String report, 
                                long period, 
                                String logDirectory, 
                                long profileTime, 
                                long samplePeriod, 
                                boolean isSnapshot, 
                                boolean isWatchdog, 
                                boolean isLoadPdf);

  StringQueryReply profile(long activeTime, long period, int depth);

  Date []listRestarts(long period);
  
  StringQueryReply addLicense(String licenseContent, String to,
                              boolean isOverwrite, boolean isRestart);

  StringQueryReply status();
  
  StringQueryReply scoreboard(String type, boolean isGreedy);

  JmxSetQueryReply setJmx(String pattern, String attribute, String value);

  JmxCallQueryReply callJmx(String pattern, String operation,
                            int operationIndex, String[] params);
  
  String disable(String serverId);
  String enable(String serverId);
}
