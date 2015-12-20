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

import java.util.Date;
import java.util.Objects;

import com.caucho.v5.health.stat.StatSystem;
import com.caucho.v5.management.server.StatServiceValue;
import com.caucho.v5.pdf.canvas.CanvasPdf;
import com.caucho.v5.pdf.canvas.DataBuilderPdf;
import com.caucho.v5.pdf.canvas.GraphBuilderPdf;

/**
 * pdf object oriented API facade
 */
public class GraphStatPdf implements ContentAdminPdf
{
  private String _title;
  private double _width = 200;
  private double _height = 200;
  private String []_meterNames;
  private AdminPdfBuilder _adminBuilder;
  private GraphBuilderAdminPdf _builder;
  
  GraphStatPdf(GraphBuilderAdminPdf graphBuilder,
               String title,
               String []meterNames)
  {
    Objects.requireNonNull(title);
    Objects.requireNonNull(meterNames);

    _adminBuilder = graphBuilder.getAdminBuilder();
    _builder = graphBuilder;
    _title = title;
    _meterNames = meterNames;
  }

  @Override
  public void write(AdminPdf admin)
  {
    CanvasPdf canvas = admin.getCanvas();
    
    GraphBuilderPdf builder = canvas.graphBuilder(_title, _width, _height);
    
    StatSystem statSystem = _adminBuilder.getStatSystem();
    
    StatServiceValue[] data;
    
    for (String meterName : _meterNames) {
      data = statSystem.getStatisticsData(meterName,
                                          _adminBuilder.getBeginTime(), 
                                          _adminBuilder.getEndTime(),
                                          60 * 1000);

      if (data != null && data.length > 0) {
        int p = meterName.lastIndexOf('|');

        String name = meterName;

        if (p > 0) {
          name = meterName.substring(p + 1);
        }

        DataBuilderPdf dataBuilder = builder.dataBuilder(name);

        for (StatServiceValue value : data) {
          dataBuilder.point(value.getTime(), value.getAverage());
        }

        dataBuilder.build();
      }
    }
    
    builder.build();
  }
}
