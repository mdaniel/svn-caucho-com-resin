package com.caucho.resin.admin.service;

import java.util.logging.Level;

import com.caucho.jmx.Jmx;
import com.caucho.management.server.LogServiceMXBean;

import io.baratine.core.Remote;
import io.baratine.core.OnActive;
import io.baratine.core.Service;

@Remote
@Service("/log")
public class ResinLogService
{
  private LogServiceMXBean _logBean;

  @OnActive
  public void onStart()
  {
    Thread.dumpStack();

    try {
      _logBean = Jmx.find("caucho:type=LogService", LogServiceMXBean.class);

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Object []getLog(String serverId,
                         String level, long startTime, long maxTime)
  {
    try {
      System.err.println(getClass().getSimpleName() + ".getLog0: " + level);

      if (level == null) {
        level = Level.ALL.getName().toLowerCase();
      }

      return _logBean.findMessages(level, startTime, maxTime);
    }
    catch (Exception e) {
      e.printStackTrace();

      return null;
    }
  }
}
