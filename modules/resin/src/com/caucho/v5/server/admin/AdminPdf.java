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

package com.caucho.v5.server.admin;

import java.io.IOException;
import java.util.Arrays;

import com.caucho.v5.config.cf.NameCfg;
import com.caucho.v5.env.log.LogSystem;
import com.caucho.v5.json.value.JsonValue;
import com.caucho.v5.pdf.canvas.CanvasPdf;
import com.caucho.v5.util.QDate;
import com.caucho.v5.vfs.PathImpl;

/**
 * pdf object oriented API facade
 */
public class AdminPdf implements AutoCloseable 
{
  private CanvasPdf _canvas;
  private AdminPdfBuilder _builder;

  public AdminPdf(AdminPdfBuilder builder, PathImpl path)
    throws IOException
  {
    _builder = builder;
    
    _canvas = new CanvasPdf(path);
    
    _canvas.marginTop(40)
           .marginRight(30)
           .marginBottom(40)
           .marginLeft(30);
    
    _canvas.setHeaderCenter(builder.getServerDisplayName());
    
    String beginTime = builder.formatDate(builder.getBeginTime());
    _canvas.setFooterLeft(beginTime);
    
    String endTime = builder.formatDate(builder.getEndTime());
    _canvas.setFooterRight(endTime);
  }
  
  long getBeginTime()
  {
    return _builder.getBeginTime();
  }
  
  long getEndTime()
  {
    return _builder.getEndTime();
  }
  
  void writeHeader()
  {
    String title = _builder.getTitle();
    
    _canvas.section(title);
    
    double colLeft = 35;
    double colRight = -50;
    
    _canvas.font(CanvasPdf.FontPdf.TEXT);
    
    String genTime = _builder.formatDate(_builder.getReportTime());
    
    String snapshotTime = _builder.formatDate(_builder.getSnapshotTime());
    
    String beginTime = _builder.formatDate(_builder.getBeginTime());
    String endTime = _builder.formatDate(_builder.getEndTime());
    
    _canvas.textLeft(colLeft, "Snapshot Time:")
           .textRight(colRight, snapshotTime)
           .newline();
    
    _canvas.textLeft(colLeft, "Report Generated:")
           .textRight(colRight, genTime)
           .newline();
    
    _canvas.textLeft(colLeft, "Data Range:")
           .textRight(colRight, beginTime + " to " + endTime)
           .newline();
    
    // XXX: warnings
  }

  public JsonValue loadJsonDump(String key)
  {
    LogSystem log = LogSystem.getCurrent();
    
    if (log == null) {
      return null;
    }
    
    long[] times = log.findMessageTimes(key, "info", getBeginTime(), getEndTime());
    
    System.out.println("TIME-LEN: " + times.length);
    for (int i = 0; i < times.length; i++) {
      System.out.println("TIMES: " + times[0]);
    }

    return null;
  }

  public CanvasPdf getCanvas()
  {
    return _canvas;
  }

  public void writeContent()
  {
    for (ContentAdminPdf content : _builder.getContent()) {
      content.write(this);
    }
  }

  @Override
  public void close()
    throws IOException
  {
    _canvas.close();
  }
}
