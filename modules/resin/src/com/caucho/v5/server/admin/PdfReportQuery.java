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

@SuppressWarnings("serial")
public class PdfReportQuery implements java.io.Serializable
{
  private String _path;
  private String _report;
  private long _period;
  private String _logDirectory;
  private long _profileTime;
  private long _samplePeriod;
  private boolean _isSnapshot;
  private boolean _isWatchdog;
  private boolean isReturnPdf;
  
  public PdfReportQuery()
  {
  }

  public PdfReportQuery(String path,
                        String report,
                        long period,
                        String logDirectory,
                        long profileTime,
                        long samplePeriod,
                        boolean isSnapshot,
                        boolean isWatchdog,
                        boolean isReturnPdf)
  {
    _path = path;
    _report = report;
    _period = period;
    _logDirectory = logDirectory;
    _profileTime = profileTime;
    _samplePeriod = samplePeriod;
    _isSnapshot = isSnapshot;
    _isWatchdog = isWatchdog;
    this.isReturnPdf = isReturnPdf;
  }
  
  public String getPath()
  {
    return _path;
  }
  
  public void setPath(String path)
  {
    _path = path;
  }
  
  public String getReport()
  {
    return _report;
  }
  
  public void setReport(String report)
  {
    _report = report;
  }
  
  public long getPeriod()
  {
    return _period;
  }
  
  public void setPeriod(long period)
  {
    _period = period;
  }
  
  public String getLogDirectory()
  {
    return _logDirectory;
  }
  
  public void setLogDirectory(String logDirectory)
  {
    _logDirectory = logDirectory;
  }

  public boolean isSnapshot()
  {
    return _isSnapshot;
  }

  public void setSnapshot(boolean snapshot)
  {
    _isSnapshot = snapshot;
  }

  public long getSamplePeriod()
  {
    return _samplePeriod;
  }

  public void setSamplePeriod(long samplePeriod)
  {
    _samplePeriod = samplePeriod;
  }

  public long getProfileTime()
  {
    return _profileTime;
  }

  public void setProfileTime(long profileTime)
  {
    _profileTime = profileTime;
  }
  
  public boolean isWatchdog()
  {
    return _isWatchdog;
  }

  public boolean isReturnPdf()
  {
    return isReturnPdf;
  }

  public void setReturnPdf(boolean returnPdf)
  {
    isReturnPdf = returnPdf;
  }

  @Override
  public String toString()
  {
    return String.format("%s[%s,%s,%s,%s,%s,%s,%s]",
                         this.getClass().getSimpleName(),
                         _path,
                         _report,
                         _period,
                         _logDirectory,
                         _profileTime,
                         _samplePeriod,
                         _isSnapshot);
  }
}
