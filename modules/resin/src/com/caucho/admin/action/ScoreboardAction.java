package com.caucho.admin.action;

import java.util.*;
import java.util.logging.*;

import com.caucho.admin.thread.*;
import com.caucho.util.*;

public class ScoreboardAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(ScoreboardAction.class.getName());

  private static final L10N L = new L10N(ScoreboardAction.class);
  
  public String excute(String type, boolean greedy)
  {
    return excute(type, greedy, 80);
  }

  public String excute(String type, boolean greedy, int lineWidth)
  {
    AbstractThreadActivityReport report = getReportType(type);
    if (report == null) {
      return L.l("Unknown Scoreboard Report type {0}", type);
    }
    
    ThreadActivityGroup []groups = report.execute(greedy);
    
    StringBuilder sb = new StringBuilder();
    
    for (ThreadActivityGroup group : groups) {
      String scoreboard = group.toScoreboard();
      
      sb.append("[");
      sb.append(group.getName());
      sb.append("]");
      sb.append("\n");
      
      sb.append(breakIntoLines(scoreboard, lineWidth));
      sb.append("\n");
      sb.append("\n");
    }
    
    sb.append("[Scoreboard Key]");
    sb.append("\n");
   
    Map<Character, String> key = report.getScoreboardKey();
    for (Map.Entry<Character, String> entry : key.entrySet()) {
      sb.append(entry.getKey());
      sb.append("   ");
      sb.append(entry.getValue());
      sb.append("\n");
    }
    
    return sb.toString();
  }
  
  public String executeJson(String type, boolean greedy)
  {
    StringBuilder sb = new StringBuilder();
    
    long timestamp = CurrentTime.getCurrentTime();
    
    sb.append("{\n");
    sb.append("  \"create_time\": \"" + new Date(timestamp) + "\",\n");
    sb.append("  \"timestamp\": " + timestamp + ",\n");

    AbstractThreadActivityReport report = getReportType(type);
    
    sb.append("  \"scoreboards\": {\n");
    if (report != null) {
      ThreadActivityGroup []groups = report.execute(greedy);
      if (groups != null) {
        boolean isFirst = true;
        for (ThreadActivityGroup group : groups) {
          if (! isFirst)
            sb.append(",\n");
          isFirst = false;
          
          sb.append("    \"" + group.getName() + "\": ");
          sb.append("\"" + group.toScoreboard() + "\"");
        }
      }
    }
    
    sb.append("\n  },\n");
    sb.append("  \"keys\": {\n");
    
    if (report != null) {
      Map<Character, String> key = report.getScoreboardKey();
      
      boolean isFirst = true;
      for (Map.Entry<Character, String> entry : key.entrySet()) {
        if (! isFirst)
          sb.append(",\n");
        isFirst = false;

        sb.append("    \"" + entry.getKey() + "\": ");
        sb.append("\"" + entry.getValue() + "\"");
      }
    }
    
    sb.append("\n  },\n");
    sb.append("}");
    
    return sb.toString();
  }
  
  protected AbstractThreadActivityReport getReportType(String typeName)
  {
    if (typeName.equalsIgnoreCase("resin"))
      return new ResinThreadActivityReport();
    else if (typeName.equalsIgnoreCase("database"))
      return new DatabaseThreadActivityReport();
    
    try {
      Class c = Class.forName("com.caucho.admin.thread." + typeName);
      return (AbstractThreadActivityReport) c.newInstance();
    } catch (Exception e) {
      log.log(Level.FINER, L.l("Failed to load scoreboard report type {0}: {1}", 
                               typeName, e.getMessage()), e);
      return null;
    }
  }
  
  private static String breakIntoLines(String s, int w)
  {
    if (s.length() <= w)
      return s;
    
    StringBuilder sb = new StringBuilder(s);
    for (int i=1; i<Integer.MAX_VALUE; i++) {
      int pos = (i * w) + (i-1);
      if (pos >= sb.length())
        break;
      sb.insert(pos, "\n");
    }
    
    return sb.toString();
  }
}