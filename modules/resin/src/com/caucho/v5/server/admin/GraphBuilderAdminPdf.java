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

import java.util.Objects;

import com.caucho.v5.health.stat.StatSystem;
import com.caucho.v5.jmx.server.MeterGraphInfo;
import com.caucho.v5.jmx.server.MeterGraphPageInfo;
import com.caucho.v5.jmx.server.MeterGraphSectionInfo;

/**
 * pdf object oriented API facade
 */
public class GraphBuilderAdminPdf
{
  private AdminPdfBuilder _adminBuilder;
  
  private String _title;
  private double _width = 200;
  private double _height = 200;
  
  GraphBuilderAdminPdf(AdminPdfBuilder adminBuilder, String title)
  {
    Objects.requireNonNull(adminBuilder);
    
    _adminBuilder = adminBuilder;
    
    _title = title;
  }

  public AdminPdfBuilder getAdminBuilder()
  {
    return _adminBuilder;
  }
  
  public void build()
  {
    StatSystem statSystem = _adminBuilder.getStatSystem();
    
    if (statSystem == null) {
      return;
    }
    
    MeterGraphPageInfo[] pages = statSystem.getMeterGraphPages();
    
    if (pages == null || pages.length == 0) {
      return;
    }
    
    buildPage(pages[0]);
  }
  
  private void buildPage(MeterGraphPageInfo pageInfo)
  {
    for (MeterGraphSectionInfo sectionInfo : pageInfo.getMeterSections()) {
      for (MeterGraphInfo graphInfo : sectionInfo.getMeterGraphs()) {
        String title = graphInfo.getName();
        String []meterNames = graphInfo.getMeterNames();
        
        GraphStatPdf graph = new GraphStatPdf(this, title, meterNames);
    
        _adminBuilder.add(graph);
      }
    }
  }
}
