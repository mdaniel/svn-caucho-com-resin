/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
 */

package com.caucho.v5.admin.action;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.caucho.v5.env.health.HealthCheckResult;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.health.check.HealthCheck;
import com.caucho.v5.health.check.SystemHealthCheck;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

public class HealthDumpAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(HealthDumpAction.class.getName());

  private static final L10N L = new L10N(HealthDumpAction.class);
  
  public String execute()
  {
    HealthSubSystem healthService = HealthSubSystem.getCurrent();
    
    if (healthService == null) {
      throw new IllegalStateException(L.l("HealthService is not started"));
    }
    
    HealthCheckResult summaryResult = healthService.getSummaryResult();
    
    List<HealthCheck> checks = healthService.getHealthChecks();
    
    int width1 = 6;
    int width2 = 7;
    
    for (HealthCheck check : checks) {
      HealthCheckResult result = healthService.getLastResult(check);
      if(result.getStatus().toString().length() > width1)
        width1 = result.getStatus().toString().length();
      if(check.getName().length() > width2)
        width2 = check.getName().length();
    }

    String columnFormat = "%-" + (width1+1) + "s%-" + (width2+1) + "s%s";
    
    StringBuilder sb = new StringBuilder();
    
    sb.append(String.format(columnFormat, "Status", "Check", "Message"));
    sb.append("\n");

    sb.append(String.format(columnFormat, 
                            summaryResult.getStatus(),
                            "Overall",
                            summaryResult.getMessage()));
    sb.append("\n");
    
    
    for (HealthCheck check : checks) {
      if (check instanceof SystemHealthCheck)
        continue;
      
      HealthCheckResult result = healthService.getLastResult(check);
      
      sb.append(String.format(columnFormat, 
                              result.getStatus(),
                              check.getName(),
                              result.getMessage()));
      sb.append("\n");
    }
    
    return sb.toString();
  }

  public String executeJson()
  {
    StringBuilder sb = new StringBuilder();
    
    long timestamp = CurrentTime.getCurrentTime();
    
    sb.append("{\n");
    sb.append("  \"create_time\": \"" + new Date(timestamp) + "\",\n");
    sb.append("  \"timestamp\": " + timestamp + ",\n");
    sb.append("  \"health\" : [\n");
    
    HealthSubSystem healthService = HealthSubSystem.getCurrent();
    if (healthService != null) {
      HealthCheckResult summaryResult = healthService.getSummaryResult();
      if (summaryResult != null) {
        
        sb.append(jsonDumpCheck("Overall", 
                                summaryResult.getStatus().toString(),
                                summaryResult.getMessage(),
                                new Date(summaryResult.getTimestamp())));
        
        List<HealthCheck> checks = healthService.getHealthChecks();
        if (! checks.isEmpty()) {
          sb.append(",\n");
          
          boolean isFirst = true;
          for (HealthCheck check : checks) {
            if (check instanceof SystemHealthCheck)
              continue;
            
            if (! isFirst)
              sb.append(",\n");
            isFirst = false;
  
            HealthCheckResult result = healthService.getLastResult(check);
            
            sb.append(jsonDumpCheck(check.getName(), 
                                    result.getStatus().toString(), 
                                    result.getMessage(),
                                    new Date(result.getTimestamp())));
          }
        }
      }
    }
    
    sb.append("\n]");
    sb.append("\n}");
    
    return sb.toString();
  }
  
  private static String jsonDumpCheck(String name, 
                                      String status, 
                                      String message,
                                      Date timestamp)
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append("{\n");
    
    sb.append("  \"name\" : \"");
    escapeString(sb, name);
    sb.append("\",\n");
    
    sb.append("  \"status\" : \"");
    escapeString(sb, status);
    sb.append("\",\n");

    sb.append("  \"message\" : \"");
    escapeString(sb, message);
    sb.append("\",\n");
    
    sb.append("  \"timestamp\" : \"");
    sb.append(timestamp);
    sb.append("\"\n");

    sb.append("}");

    return sb.toString();
  }
  
  private static void escapeString(StringBuilder sb, String value)
  {
    int len = value.length();
    
    for (int i = 0; i < len; i++) {
      char ch = value.charAt(i);
      
      switch (ch) {
      case '"':
        sb.append("\\\"");
        break;
      case '\\':
        sb.append("\\\\");
        break;
      default:
        sb.append(ch);
      }
    }
  }
  
}
