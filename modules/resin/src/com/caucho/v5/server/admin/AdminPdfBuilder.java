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
import java.util.ArrayList;
import java.util.Objects;

import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.health.stat.StatSystem;
import com.caucho.v5.pdf.PdfException;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.QDate;
import com.caucho.v5.vfs.Path;

/**
 * pdf object oriented API facade
 */
public class AdminPdfBuilder
{
  private long _snapshotTime;
  private long _reportTime;
  
  private ServerBartender _server;
  
  private ArrayList<ContentAdminPdf> _contentList = new ArrayList<>();
  private StatSystem _statSystem;
  
  public AdminPdfBuilder()
  {
    long now = CurrentTime.getCurrentTime();
    
    _snapshotTime = now; 
    _reportTime = now;
    
    _server = BartenderSystem.getCurrentSelfServer();
    
    _statSystem = StatSystem.getCurrent();
  }
  
  StatSystem getStatSystem()
  {
    return _statSystem;
  }

  String getTitle()
  {
    return "Snapshot Report";
  }
  
  ArrayList<ContentAdminPdf> getContent()
  {
    return _contentList;
  }
  
  String getServerDisplayName()
  {
    String id = _server.getId();
    String displayName = _server.getDisplayName();
    
    if (CurrentTime.isTest()) {
      return displayName;
    }
    else if (id.equals(displayName)) {
      return id;
    }
    else {
      return displayName + "/" + id;
    }
  }
  
  long getSnapshotTime()
  {
    return _snapshotTime;
  }

  long getEndTime()
  {
    return _snapshotTime;
  }
  
  long getBeginTime()
  {
    return _snapshotTime - 6 * 3600 * 1000L;
  }
  
  long getReportTime()
  {
    return _reportTime;
  }

  public String formatDate(long time)
  {
    return QDate.formatLocal(time, "%Y-%m-%d %H:%M (%z)");
  }

  public AdminPdfBuilder add(ContentAdminPdf content)
  {
    _contentList.add(content);
    
    return this;
  }
  
  public GraphBuilderAdminPdf graphBuilder(String title)
  {
    return new GraphBuilderAdminPdf(this, title);
  }
  
  public AdminPdfBuilder scoreboard()
  {
    ScoreboardPdf scoreboard = new ScoreboardPdf();
    
    add(scoreboard);
    
    return this;
  }
  
  public AdminPdfBuilder threadDump()
  {
    ThreadDumpPdf threadDump = new ThreadDumpPdf();
    
    add(threadDump);
    
    return this;
  }
  
  public AdminPdfBuilder profile()
  {
    ProfilePdf profile = new ProfilePdf();
    
    add(profile);
    
    return this;
  }
  
  public void build(Path path)
  {
    try (AdminPdf pdf = new AdminPdf(this, path)) {
      pdf.writeHeader();
      
      pdf.writeContent();
    } catch (IOException e) {
      throw new PdfException(e);
    }
  }

}
