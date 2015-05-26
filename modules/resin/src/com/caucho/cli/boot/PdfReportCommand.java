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

package com.caucho.cli.boot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.caucho.cli.baratine.ArgsCli;
import com.caucho.config.types.Period;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.server.admin.ManagerClientApi;
import com.caucho.server.admin.PdfReportQueryReply;
import com.caucho.server.config.ServerConfigBoot;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.StreamSource;

public class PdfReportCommand extends ManagementCommandBase
{
  private static final L10N L = new L10N(PdfReportCommand.class);
  
  @Override
  protected void initBootOptions()
  {
    addValueOption("report", "value", "specifies the report-type key (default Snapshot)");
    
    addSpacerOption();

    addValueOption("period", "time", "specifies look-back period of time (default 7D)");
    addValueOption("profile-time", "time", "turns code profiling on for a time before generating report");
    addValueOption("profile-sample", "time", "specifies profiling sampling frequency (100ms)");
    
    addSpacerOption();

    addFlagOption("snapshot", "saves heap-dump, thread-dump, jmx-dump before generating report");
    addFlagOption("watchdog", "specifies look-back period starting at last Resin start");

    addSpacerOption();

    addValueOption("logdir", "dir", "PDF output directory (default to resin log)");
    addValueOption("local-dir", "dir", "writes a local copy of PDF report to a specified directory");
    addFlagOption("local", "writes a local copy of PDF report");
    
    super.initBootOptions();
  }
  
  @Override
  public String getDescription()
  {
    return "creates a PDF report of a Resin server";
  }

  @Override
  public ExitCode doCommand(ArgsCli args,
                            ServerConfigBoot server,
                            ManagerClientApi managerClient)
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
    String samplePeriodArg = null;
    
    if (samplePeriodArg == null)
      samplePeriodArg = args.getArg("-profile-sample");
    if (samplePeriodArg == null)
      samplePeriodArg = args.getArg("-sample-period");
    if (samplePeriodArg != null)
      samplePeriod = Period.toPeriod(samplePeriodArg, 1);

    boolean isSnapshot = args.getArgBoolean("-snapshot", true);
    boolean isWatchdog = args.getArgBoolean("-watchdog", false);
    boolean isLocal = args.getArgBoolean("-local", false);
    String localDir = args.getArg("-local-dir");
    
    if (localDir == null)
      localDir = System.getProperty("user.dir");

    boolean isReturnPdf = isLocal || localDir != null;

    PdfReportQueryReply result = managerClient.pdfReport(path,
                                                         report,
                                                         period,
                                                         logDirectory,
                                                         profileTime,
                                                         samplePeriod,
                                                         isSnapshot,
                                                         isWatchdog,
                                                         isReturnPdf);

    System.out.println(result.getMessage());

    if (isReturnPdf) {
      StreamSource streamSource = result.openPdf();
      try {
        InputStream in = streamSource.getInputStream();
        File file;

        String fileName = result.getFileName();
        if (fileName.lastIndexOf('/') > 0)
          fileName = fileName.substring(fileName.lastIndexOf('/'));
        else if (fileName.lastIndexOf('\\') > 0)
          fileName = fileName.substring(fileName.lastIndexOf('\\'));

        if (localDir != null) {
          file = new File(localDir, fileName);

          file.getParentFile().mkdirs();
        }
        else {
          file = new File(fileName);
        }

        FileOutputStream out = null;
        try {
          out = new FileOutputStream(file);
          byte []buffer = new byte[1024];
          int len = -1;
          while ((len = in.read(buffer)) > 0)
            out.write(buffer, 0, len);

          out.flush();
          System.out.println(L.l("Local copy is written to '{0}'",
                                 file.toString()));
        } catch (IOException e) {
          System.out.println(L.l("Can't write a local copy '{0}'",
                                 file.toString()));
        } finally {
          IoUtil.close(out);
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return ExitCode.OK;
  }
}
