/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
 * @author Alex Rojkov
 */

package com.caucho.boot;

import com.caucho.config.types.Period;
import com.caucho.server.admin.ManagerClient;
import com.caucho.util.L10N;

public class PdfReportCommand extends AbstractManagementCommand
{
  private static final L10N L = new L10N(PdfReportCommand.class);

  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       ManagerClient managerClient)
  {
    String path = args.getArg("-path");
    
    String report = args.getArg("-report");

    long period = -1;

    String periodStr = args.getArg("-period");
    if (periodStr != null)
      period = Period.toPeriod(periodStr);

    String logDirectory = args.getArg("-logdir");

    long profileTime = -1;
    String profileTimeArg = args.getArg("-profile-time");
    if (profileTimeArg != null )
      profileTime = Period.toPeriod(profileTimeArg);

    long samplePeriod = -1;
    String samplePeriodArg = args.getArg("-sample-period");
    if (samplePeriodArg != null)
      samplePeriod = Period.toPeriod(samplePeriodArg, 1);

    boolean isSnapshot = true;

    String snapshotArg = args.getArg("-snapshot");

    if ("no".equals(snapshotArg) || "false".equals(snapshotArg))
      isSnapshot = false;

    String result = managerClient.pdfReport(path,
                                            report,
                                            period,
                                            logDirectory,
                                            profileTime,
                                            samplePeriod,
                                            isSnapshot);

    System.out.println(result);

    return 0;
  }

  @Override
  public void usage()
  {
    System.err.println(L.l("usage: bin/resin.sh [-conf <file>] pdf-report -user <user> -password <password> [-path <php path>] [-report <report name>] [-period <period>] [-logdir <log path>] [-snapshot <snapshot>] [-profile-time <profile time>] [-sample-period <sample-period>]"));
    System.err.println(L.l(""));
    System.err.println(L.l("description:"));
    System.err.println(L.l("   generates pdf report (Pro version only)"));
    System.err.println(L.l(""));
    System.err.println(L.l("options:"));
    System.err.println(L.l("   -path            : path to a PDF generating .php file (defaults to ${resin.home}/doc/admin/pdf-gen.php)" ));
    System.err.println(L.l("   -period          : specifies look back period of time. e.g. '-period 1D' create the report since the same time yesterday (default 7D)"));
    System.err.println(L.l("   -report          : report type key (default Summary)" ));
    System.err.println(L.l("   -logdir          : PDF output directory  (defaults to resin log directory)" ));
    System.err.println(L.l("   -snapshot        : includes heap-dump, thread-dump and a snapshot of JMX beans and attributes into report e.g. -snapshot false" ));
    System.err.println(L.l("   -profile-time    : turns code profiling on for specified time (2 min - max) and includes profiling data into report e.g. '-profile-time 30s'" ));
    System.err.println(L.l("   -sample-period   : specifies sampling frequency for the profiler in milliseconds: e.g. '-sample-period 50' (default 100ms)" ));
  }
}
