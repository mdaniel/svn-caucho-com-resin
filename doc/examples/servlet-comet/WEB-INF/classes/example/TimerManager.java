package example;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.annotation.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.servlets.comet.*;

public class TimerManager implements Runnable {
  @Resource ScheduledExecutorService _timer;

  private Future _timerFuture;
  
  private ArrayList<CometItem> _itemList
    = new ArrayList<CometItem>();

  public TimerManager(ScheduledExecutorService timer)
  {
    _timer = timer;
    
    _timerFuture = _timer.scheduleAtFixedRate(this, 0, 2, TimeUnit.SECONDS);
  }

  public void addCometItem(CometItem item)
  {
    synchronized (_itemList) {
      _itemList.add(item);
    }
  }

  /**
   * The timer task wakes up every active comet item.
   *
   * A more sophisticated application would notify the comet items
   * as part of an event-based system.
   */
  public void run()
  {
    synchronized (_itemList) {
      for (int i = _itemList.size() - 1; i >= 0; i--) {
        CometItem item = _itemList.get(i);

        if (! item.wake())
          _itemList.remove(i);
      }
    }
  }

  /**
   * Close the timer.
   */
  public void close()
  {
    _timerFuture.cancel(false);
  }
}
