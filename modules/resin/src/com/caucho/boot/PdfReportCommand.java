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

    String result = managerClient.pdfReport(path, report, period, logDirectory);

    System.out.println(result);

    return 0;
  }

  @Override
  public void usage()
  {
    System.err.println(L.l("usage: bin/resin.sh [-conf <file>] pdf-report -user <user> -password <password> [-path <php path>] [-report <report name>] [-period <period>] [-logdir <log path>]"));
    System.err.println(L.l(""));
    System.err.println(L.l("description:"));
    System.err.println(L.l("   generates pdf report (Pro version only)"));
    System.err.println(L.l(""));
    System.err.println(L.l("options:"));
    System.err.println(L.l("   -path            : path to a PDF generating .php file (defaults to ${resin.home}/doc/admin/pdf-gen.php)" ));
    System.err.println(L.l("   -period          : specifies look back period of time. e.g. '-period 1D' create the report since the same time yesterday (default 7D)"));
    System.err.println(L.l("   -report          : report type key (default Summary)" ));
    System.err.println(L.l("   -logdir          : PDF output directory  (defaults to resin log directory)" ));
  }
}
