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

import java.io.StringReader;
import java.util.Map;

import com.caucho.v5.health.action.ReportScoreboard;
import com.caucho.v5.json.io.JsonReader;
import com.caucho.v5.json.value.JsonValue;
import com.caucho.v5.pdf.canvas.CanvasPdf;


/**
 * pdf object oriented API facade
 */
public class ScoreboardPdf implements ContentAdminPdf
{
  @Override
  public void write(AdminPdf admin)
  {
    CanvasPdf canvas = admin.getCanvas();
    
    canvas.nextPage();
    canvas.section("Scoreboard", true);
    
    ReportScoreboard report = new ReportScoreboard();
    
    String json = report.executeJson("resin", true);
    
    JsonValue top = (JsonValue) new JsonReader(new StringReader(json)).readObject(JsonValue.class);
    
    canvas.font(CanvasPdf.FontPdf.TEXT);
    canvas.newline();
    
    JsonValue keys = top.get("keys");
    JsonValue scoreboards = top.get("scoreboards");
    
    for (Map.Entry<String,JsonValue> scoreboard : scoreboards.entrySet()) {
      String name = scoreboard.getKey();
      JsonValue value = scoreboard.getValue();

      canvas.font(10, "Courier-Bold")
            .text(name)
            .newline();
            
      canvas.font(10, "Courier")
            .text(value.toString())
            .newline()
            .newline();
    }
    
    canvas.font(10, "Courier-Bold")
          .textLeft(220, "Scoreboard Key")
          .newline();
    
    canvas.font(10, "Courier");
    
    for (Map.Entry<String,JsonValue> entry : keys.entrySet()) {
      String name = entry.getKey();
      JsonValue value = entry.getValue();
      
      //canvas.textCenter(20, name);
      canvas.textLeft(20, name)
            .textLeft(220, value.toString())
            .newline();
    }
  }
}
